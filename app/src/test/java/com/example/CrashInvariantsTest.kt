package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Invariantes del registro de fallos (spec 020).
 *
 * Son **pruebas puras** que leen el texto de los ficheros y no necesitan emulador: miden el **texto**, no el
 * comportamiento, así que se complementan con un `compileDebugKotlin` en verde y no sustituyen a probar en
 * dispositivo (RNF-70).
 *
 * Dos reglas del proyecto, aprendidas por la vía mala (ver `MEMORY.md`):
 *
 * 1. **Cada recorrido quita comentarios antes de buscar.** Un invariante que busca `UncaughtExceptionHandler`
 *    se pone verde con el KDoc que explica por qué existe, y ese fue el error de cuatro invariantes de la
 *    spec 019 con el código **correcto**.
 * 2. **Cada recorrido se autocomprueba.** Sin ella, una ruta equivocada hace que `readText` lance y la
 *    prueba se vaya por un motivo que no es el que vigila, y un recorrido vacío pasa **por vacío**.
 */
class CrashInvariantsTest {

    private companion object {
        const val APP = "src/main"
        const val JAVA = "$APP/java/com/example"
        const val RES = "$APP/res"

        const val APLICACION = "$JAVA/SecureNotesApplication.kt"
        const val MAIN = "$JAVA/MainActivity.kt"
        const val REPORTER = "$JAVA/crash/CrashReporter.kt"
        const val NOTIFIER = "$JAVA/crash/CrashNotifier.kt"
        const val ACTIVIDAD = "$JAVA/CrashReportActivity.kt"
        const val PANTALLA = "$JAVA/ui/crash/CrashReportScreen.kt"

        /** Los tres ficheros que **no** pueden importar `android` (RNF-64). */
        val PUROS = listOf(
            "$JAVA/crash/CrashReportText.kt",
            "$JAVA/crash/CrashIssueUrl.kt",
            "$JAVA/crash/CrashStore.kt"
        )

        const val MANIFIESTO = "$APP/AndroidManifest.xml"
        val RESPALDO = listOf("$RES/xml/backup_rules.xml", "$RES/xml/data_extraction_rules.xml")

        /** Los nueve idiomas (constitución 6). */
        val LOCALES = listOf(
            "values", "values-es-rVE", "values-pt-rBR", "values-fr", "values-it",
            "values-en-rGB", "values-es-rES", "values-pt-rPT", "values-b+es+419"
        )

        val CLAVES_CRASH = listOf(
            "crash_channel_name", "crash_channel_description", "crash_notification_title",
            "crash_notification_text", "crash_report_title", "crash_copy", "crash_share",
            "crash_send_github", "crash_discard", "crash_discard_title", "crash_discard_text",
            "crash_copied", "crash_no_browser", "crash_empty_title", "crash_empty_text",
            "crash_send_privacy_warning", "crash_notice_title", "crash_notice_text", "crash_notice_view"
        )

        /** Piso de cualquier recorrido: si baja de aquí, el recorrido está roto, no el código. */
        const val MINIMO_DE_LINEAS = 5

        /** Un cliente de red. `java.net.URLEncoder` **no** cuenta: no abre nada, solo escapa texto. */
        val CLIENTES_DE_RED = listOf(
            "okhttp3", "retrofit2", "HttpURLConnection", "openConnection", "Socket(", "WebSocket"
        )

        private val COMENTARIOS = Regex("/\\*.*?\\*/|//[^\\n]*", RegexOption.DOT_MATCHES_ALL)

        fun leer(ruta: String): String = File(ruta).readText()

        /** Lee el fichero **quitando comentarios**, para que un KDoc no pueda dar verde un invariante. */
        fun leerCodigo(ruta: String): String = leer(ruta).replace(COMENTARIOS, "")
    }

    private fun leerComprobando(ruta: String, etiqueta: String): String {
        val texto = leer(ruta)
        assertTrue(
            "no se está leyendo $etiqueta ($ruta): el recorrido está roto, no el código",
            texto.lines().size >= MINIMO_DE_LINEAS
        )
        return texto
    }

    private fun leerCodigoComprobando(ruta: String, etiqueta: String): String {
        val codigo = leerCodigoComprobandoSinQuitar(ruta)
        assertTrue("el recorrido de $etiqueta ($ruta) se ha quedado sin código", codigo.isNotBlank())
        return codigo
    }

    private fun leerCodigoComprobandoSinQuitar(ruta: String): String {
        leerComprobando(ruta, ruta)
        return leerCodigo(ruta)
    }

    // ── RF-117 y RNF-69: el manejador, instalado y delegando ─────────────────────────────────

    @Test
    fun `el manejador se instala en onCreate de la Application`() {
        val codigo = leerCodigoComprobando(APLICACION, "la Application")

        assertTrue("CrashReporter.instalar no está en SecureNotesApplication", codigo.contains("CrashReporter.instalar(this)"))
        assertTrue("no está dentro de onCreate",bloqueDesde(codigo, "override fun onCreate()").contains("CrashReporter.instalar(this)"))
    }

    @Test
    fun `guarda el manejador ANTERIOR antes de sustituirlo`() {
        val codigo = leerCodigoComprobando(REPORTER, "el reporter")

        val leer = codigo.indexOf("getDefaultUncaughtExceptionHandler()")
        val poner = codigo.indexOf("setDefaultUncaughtExceptionHandler")
        assertTrue("no guarda el manejador anterior", leer >= 0)
        assertTrue("no instala ninguno", poner >= 0)
        assertTrue("guarda el anterior DESPUÉS de sustituirlo: el fallo se escribiría dos veces", leer < poner)
    }

    @Test
    fun `delega en el manejador anterior SIEMPRE y DESPUES de registrar`() {
        val codigo = leerCodigoComprobando(REPORTER, "el reporter")

        val registrar = codigo.indexOf("registrar(context, hilo, fallo)")
        val delegar = codigo.indexOf("anterior.uncaughtException(hilo, fallo)")
        assertTrue("registrar y delegar tienen que estar los dos", registrar >= 0 && delegar >= 0)
        assertTrue("delega antes de registrar: el proceso muere sin informe", registrar < delegar)
    }

    @Test
    fun `NO mata el proceso a mano y registra sin poder lanzar`() {
        val codigo = leerCodigoComprobando(REPORTER, "el reporter")

        // Matar a mano perdería el manejador que hubiera puesto el sistema o un runner de pruebas, y
        // rompería la premisa de RF-117: «la app se cierra como se cerraba».
        assertFalse("mata el proceso a mano en vez de delegar", codigo.contains("killProcess") || codigo.contains("exitProcess"))
        // Registrar no puede lanzar: si falla, el proceso tiene que morir igualmente (caso límite 4).
        assertTrue("registrar no va protegido", codigo.contains("catch (t: Throwable)"))
    }

    @Test
    fun `la Application solo anade el manejador y la notificacion al arranque`() {
        val codigo = leerCodigoComprobando(APLICACION, "la Application")
        val arranque = bloqueDesde(codigo, "override fun onCreate()")

        assertTrue("no anuncia el fallo pendiente", arranque.contains("CrashNotifier.anunciar(this)"))
        // El canal tiene que existir **antes** de notificar: `notify()` sobre un canal que no existe es un
        // no-op silencioso, o sea, avisar sin avisar (caso límite 3).
        val canal = arranque.indexOf("CrashNotifier.crearCanal(this)")
        val anunciar = arranque.indexOf("CrashNotifier.anunciar(this)")
        assertTrue("no crea el canal de notificaciones", canal >= 0)
        assertTrue("notifica antes de crear el canal: el aviso no saldría nunca", canal < anunciar)
        // RNF-69: el manejador es lo único global que añade la spec.
        assertFalse("hace algo más en onCreate de lo que dice la spec", arranque.contains("Coil") || arranque.contains("NoteDatabase"))
    }

    // ── RNF-64: lo puro, sin android ────────────────────────────────────────────────────────

    @Test
    fun `los tres ficheros puros NO importan android`() {
        PUROS.forEach { ruta ->
            val codigo = leerCodigoComprobando(ruta, ruta)
            assertFalse("$ruta importa android: sus pruebas no correrían sin Robolectric", codigo.contains("import android"))
            assertFalse("$ruta importa Compose", codigo.contains("androidx.compose"))
        }
    }

    @Test
    fun `el paquete del fallo NO usa ningun cliente de red`() {
        val ficheros = File("$JAVA/crash").listFiles { f -> f.extension == "kt" }!!.toList()
        assertTrue("no hay ficheros en el paquete del fallo: el recorrido está roto", ficheros.size >= 5)

        ficheros.forEach { fichero ->
            val codigo = leerCodigoComprobando(fichero.path, fichero.name)
            CLIENTES_DE_RED.forEach { cliente ->
                assertFalse("${fichero.name} usa $cliente: el informe no sale solo del dispositivo", codigo.contains(cliente))
            }
        }
    }

    // ── RF-118 y caso límite 9: la pantalla no toca la bóveda ───────────────────────────────

    @Test
    fun `la pantalla del informe NO toca la base de datos ni el ViewModel de notas`() {
        listOf(ACTIVIDAD, PANTALLA).forEach { ruta ->
            val codigo = leerCodigoComprobando(ruta, ruta)
            assertFalse("$ruta importa la base de datos: el informe no necesita la bóveda", codigo.contains("data.local"))
            assertFalse("$ruta usa el ViewModel de notas", codigo.contains("NotesViewModel"))
        }
    }

    @Test
    fun `la pantalla enseña el aviso de privacidad junto a las acciones`() {
        val codigo = leerCodigoComprobando(PANTALLA, "la pantalla del informe")

        assertTrue("no pinta el aviso de privacidad (RNF-67)", codigo.contains("R.string.crash_send_privacy_warning"))
        assertTrue("no ofrece Enviar", codigo.contains("R.string.crash_send_github"))
        assertTrue("no ofrece Copiar", codigo.contains("R.string.crash_copy"))
        assertTrue("no ofrece Compartir", codigo.contains("R.string.crash_share"))
    }

    // ── RF-119, RF-120 y RF-122: notificación, aviso y consumo ──────────────────────────────

    @Test
    fun `la notificacion abre la pantalla del informe y se puede cancelar`() {
        val codigo = leerCodigoComprobando(NOTIFIER, "el notificador")

        assertTrue("la notificación no abre la pantalla del informe", codigo.contains("Intent(context, CrashReportActivity::class.java)"))
        assertTrue("no hay forma de cancelar la notificación", codigo.contains("fun cancelar("))
        // Sin el permiso, `notify()` es un no-op silencioso: avisar y no avisar sería lo mismo (caso límite 3).
        assertTrue("no comprueba el permiso de notificaciones", codigo.contains("POST_NOTIFICATIONS"))
        assertFalse("pide el permiso: molestaría en cada arranque", codigo.contains("requestPermissions"))
    }

    @Test
    fun `la pantalla del informe esta declarada y NO es exported`() {
        val manifiesto = leerComprobando(MANIFIESTO, "el manifiesto")

        assertTrue("no declara CrashReportActivity", manifiesto.contains("android:name=\".CrashReportActivity\""))
        assertFalse("un informe de fallo no lo abre cualquiera que ponga el nombre de la clase", esExportada(manifiesto))
    }

    @Test
    fun `el aviso de dentro de la app esta FUERA del if de la boveda`() {
        val codigo = leerCodigoComprobando(MAIN, "el contenido principal")

        assertTrue("no lee el informe pendiente", codigo.contains("CrashReporter.hayInformePendiente"))
        assertTrue("no ofrece ver el informe", codigo.contains("R.string.crash_notice_view"))
        // El aviso tiene que verse **también bloqueada**: si viviera dentro del `if`, saldría justo después de
        // un fallo, que es cuando más falta hace (caso límite 9).
        val cierreDeLaBoveda = codigo.indexOf("} else {")
        val aviso = codigo.indexOf("if (avisoDeFallo)")
        assertTrue("el aviso no se pinta", aviso > 0)
        assertTrue("el aviso está dentro del if de la bóveda: con la bóveda bloqueada no se ve", aviso > cierreDeLaBoveda)
    }

    @Test
    fun `abrir el informe NO lo consume antes de que la pantalla lo lea`() {
        val codigo = leerCodigoComprobando(MAIN, "el contenido principal")
        val aviso = bloqueDesde(codigo, "if (avisoDeFallo)")
        val abrir = bloqueDesde(aviso, "onClick = {")

        assertFalse(
            "consume antes de abrir la pantalla: la abriría sin nada que enseñar",
            abrir.contains("CrashReporter.consumir")
        )
        assertTrue("no abre la pantalla del informe", abrir.contains("CrashReportActivity::class.java"))
    }

    @Test
    fun `consumir borra el informe Y cancela la notificacion`() {
        val codigo = leerCodigoComprobando(REPORTER, "el reporter")

        assertTrue("consumir no borra el informe", codigo.contains("almacenDe(context).consumir()"))
        assertTrue("consumir no cancela la notificación", codigo.contains("CrashNotifier.cancelar(context)"))
    }

    // ── RNF-66: el informe no viaja en el respaldo ──────────────────────────────────────────

    @Test
    fun `los dos XML de respaldo excluyen el dominio file entero`() {
        RESPALDO.forEach { ruta ->
            val xml = leerComprobando(ruta, ruta)
            assertTrue(
                "$ruta ya no excluye domain=\"file\": un informe de fallo empezaría a viajar al respaldo",
                Regex("""<exclude\s+domain="file"\s+path="\."\s*/>""").containsMatchIn(xml)
            )
        }
    }

    // ── RNF-68: los nueve idiomas ───────────────────────────────────────────────────────────

    @Test
    fun `las diecinueve claves estan en los nueve locales`() {
        LOCALES.forEach { local ->
            val ruta = "$RES/$local/strings.xml"
            val xml = leerComprobando(ruta, ruta)
            CLAVES_CRASH.forEach { clave ->
                assertTrue("$local no tiene la clave $clave", xml.contains("name=\"$clave\""))
            }
        }
    }

    @Test
    fun `ningun strings xml repite un name`() {
        // Una clave duplicada no rompe `compileDebugKotlin`: revienta en `mergeDebugResources`, después.
        LOCALES.forEach { local ->
            val ruta = "$RES/$local/strings.xml"
            val nombres = Regex("""name="([^"]+)"""").findAll(leer(ruta)).map { it.groupValues[1] }.toList()
            val repetidos = nombres.groupBy { it }.filterValues { it.size > 1 }.keys
            assertTrue("$local repite: $repetidos", repetidos.isEmpty())
        }
    }

    @Test
    fun `apostrofos crudos escapados en los textos nuevos de crash`() {
        // AAPT2 rechaza un apóstrofo crudo, y solo en mergeDebugResources: un Kotlin verde no lo dice.
        LOCALES.forEach { local ->
            val xml = leer("$RES/$local/strings.xml")
            val nuevos = Regex("""<string name="crash_[^"]+">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(xml).map { it.groupValues[1] }.toList()
            assertTrue("$local no ha leído ningún texto de crash: el recorrido está roto", nuevos.size >= 10)
            nuevos.forEach { valor ->
                // Se busca un apóstrofo **sin barra delante**: `d\'erreur` es la forma correcta y contiene
                // igualmente el carácter. Un `'` solo rompe en mergeDebugResources, no al compilar Kotlin.
                val crudo = Regex("(?<!\\\\)'").findAll(valor).count()
                assertTrue("$local tiene un apóstrofo crudo en: $valor", crudo == 0)
            }
        }
    }

    // ── Utilidades ──────────────────────────────────────────────────────────────────────────

    /** El trozo de código que va desde `marcador` hasta la llave que cierra ese nivel. */
    private fun bloqueDesde(codigo: String, marcador: String): String {
        val desde = codigo.indexOf(marcador)
        assertTrue("no se encuentra `$marcador`: el recorrido está roto, no el código", desde >= 0)
        var nivel = 0
        var i = desde
        while (i < codigo.length) {
            when (codigo[i]) {
                '{' -> nivel++
                '}' -> {
                    nivel--
                    if (nivel == 0) return codigo.substring(desde, i + 1)
                }
            }
            i++
        }
        return codigo.substring(desde)
    }

    /** `exported="false"` **de la Activity del informe**, no de cualquiera. */
    private fun esExportada(manifiesto: String): Boolean = Regex(
        """android:name="\.CrashReportActivity"[\s\S]{0,200}?android:exported="true""""
    ).containsMatchIn(manifiesto)
}