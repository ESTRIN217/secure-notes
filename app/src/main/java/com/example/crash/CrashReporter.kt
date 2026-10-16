package com.example.crash

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Registra el fallo no capturado antes de que el proceso muera (spec 020, RF-117).
 *
 * La app **no tenía** manejador de excepciones: un fallo mataba el proceso y el usuario volvía a la lista de
 * notas como si nada. Esto no cambia cómo se muere la app —manda **siempre** al manejador que hubiera sin él,
 * que es lo que garantiza RF-117— solo deja una pista antes.
 *
 * Lo único que escribe es [CrashStore], en `filesDir`, que las reglas de respaldo ya excluyen (RNF-66).
 */
object CrashReporter {

    private const val TAG = "CrashReporter"

    /** Ruta del informe, dentro de `filesDir`. */
    private const val RUTA_INFORME = "crash/last_crash.txt"

    private var instalado = false

    /**
     * Instala el manejador por defecto de todos los hilos.
     *
     * Se llama **la primera cosa** de `Application.onCreate`, antes de que nada más pueda caerse: si el fallo
     * ocurre al arrancar la propia app, es justo el informe que más sirve y el menos se registraría si se
     * instalara más tarde.
     *
     * Solo lo guarda [instalado] cuando **de verdad** se ha instalado: en un proceso de pruebas la
     * `Application` se crea varias veces y un segundo manejador envolvería al primero, con lo que cada fallo se
     * escribiría dos veces.
     */
    fun instalar(context: Context) {
        if (instalado) return
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { hilo, fallo ->
            registrar(context, hilo, fallo)
            if (anterior != null) anterior.uncaughtException(hilo, fallo)
        }
        instalado = true
    }

    /**
     * Escribe el informe del fallo actual.
     *
     * **Nunca** lanza: si el registro falla (disco lleno, `OutOfMemoryError` justo ahora, un fallo de este
     * mismo código), el proceso tiene que morir igualmente y no morir **por no haber podido escribir**
     * (caso límite 4). Por eso el `catch` está aquí y no en el manejador, que solo tiene dos líneas.
     */
    private fun registrar(context: Context, hilo: Thread, fallo: Throwable) {
        try {
            almacenDe(context).guardar(CrashReportText.construir(datosDe(context, hilo, fallo)))
        } catch (t: Throwable) {
            Log.e(TAG, "no se pudo registrar el fallo", t)
        }
    }

    /**
     * El almacén del informe de este proceso.
     *
     * Vive aquí y no en [CrashStore] porque ese fichero **no** puede importar `android` (RNF-64): es lo que
     * hace que sus pruebas corran sin Robolectric. La ruta la pone quien sabe de `filesDir`, y este es el
     * único sitio que lo sabe.
     */
    fun almacenDe(context: Context): CrashStore = CrashStore(File(context.filesDir, RUTA_INFORME))

    /**
     * ¿Hay un informe sin mirar todavía?
     *
     * `suspend` y en [Dispatchers.IO] porque toca el disco, y quien la llama es un `LaunchedEffect` de la
     * pantalla principal: leer en el hilo principal es justo lo que `AGENTS.md` prohíbe.
     */
    suspend fun hayInformePendiente(context: Context): Boolean =
        withContext(Dispatchers.IO) { almacenDe(context).hayPendiente() }

    /**
     * Consume el informe pendiente y cancela su notificación (RF-122).
     *
     * Las dos mitades **siempre** van juntas: un informe borrado con su notificación puesta volvería a
     * avisar en el arranque siguiente, y es lo que haría el aviso inservible.
     */
    suspend fun consumir(context: Context) {
        withContext(Dispatchers.IO) {
            almacenDe(context).consumir()
            CrashNotifier.cancelar(context)
        }
    }

    private fun datosDe(context: Context, hilo: Thread, fallo: Throwable) = DatosInforme(
        versionApp = BuildConfig.VERSION_NAME,
        versionAndroid = Build.VERSION.RELEASE,
        dispositivo = "${Build.MANUFACTURER} ${Build.MODEL}",
        tipoBuild = if (BuildConfig.DEBUG) "DEBUG" else "RELEASE",
        hilo = hilo.name,
        marcaTiempo = System.currentTimeMillis(),
        traza = fallo.stackTraceToString()
    )
}