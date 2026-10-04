package com.estrin217.editordecodigo.utils

import android.util.Log
import java.util.concurrent.atomic.AtomicLong

/**
 * Identificador opaco para las trazas técnicas del registro del sistema, y compositor del
 * mensaje de reemplazo de un fallo.
 *
 * El identificador es un contador monotónico por operación: corto, no derivado del contenido,
 * no reversible y sin ataque por diccionario. Dos contenidos distintos pueden compartir
 * identificador, y por eso correlaciona la traza sin identificar el material de la persona.
 * **Nunca** se le añade el identificador de la nota: un id de nota es estable y único, y volver
 * a ponerlo en el registro reintroduce la correlación que la spec elimina.
 *
 * Este objeto se copia tal cual a cada módulo librería con sitios del alcance, porque `:app`
 * depende de ellas y no al revés: un helper solo en `:app` no sería visible desde allí.
 */
object OpaqueTrace {

    private val operationCounter = AtomicLong(0)

    /** Empieza una operación y devuelve el ámbito al que colgar sus elementos. */
    fun begin(): Op = Op(operationCounter.incrementAndGet())

    /** Una llamada aislada es su propia operación, en un solo paso. */
    fun isolatedNext(): String = begin().next()

    /**
     * Un ámbito de operación. Cada elemento que se registra dentro de la misma operación pide
     * aquí su identificador, para que varias líneas de la traza compartan el mismo `opN`.
     */
    class Op internal constructor(private val n: Long) {

        private val itemCounter = AtomicLong(0)

        /** El siguiente elemento de esta operación. */
        fun next(): String = "op$n/i${itemCounter.incrementAndGet()}"
    }

    /**
     * Compone el mensaje de reemplazo de un fallo.
     *
     * **Función pura**: no lee el estado de ninguna parte, no toca Android y no depende del
     * contenido de la nota. Recibe el estado actual del fallo (`hoy`) ya reducido a su clase:
     * por eso es imposible que el texto de la excepción llegue al registro por accidente, y por
     * eso se puede comprobar sin Robolectric.
     *
     * @param accion literal en inglés que describe la acción que falló, no el síntoma.
     * @param categoria categoría del elemento afectado (`image`, `block`, `attachment`, …).
     * @param idOpaque identificador devuelto por [begin]/[isolatedNext].
     * @param hoy la clase de la excepción, nunca la instancia: `null` si no la hay.
     */
    fun failureMessage(
        accion: String,
        categoria: String,
        idOpaque: String,
        hoy: Class<*>?
    ): String {
        val base = "$accion failed: kind=$categoria id=$idOpaque"
        val tipo = hoy ?: return base
        return "$base exc=${tipo.simpleName}"
    }

    /**
     * Fachada de registro: escribe el mensaje de reemplazo con `Log.e`.
     *
     * Lo único que se extrae de la excepción es `javaClass`. No se lee `message`,
     * `localizedMessage`, `cause` ni `stackTraceToString`, y el objeto `e` no se registra.
     */
    fun registrarFallo(
        tag: String,
        accion: String,
        categoria: String,
        id: String,
        e: Throwable?
    ) {
        Log.e(tag, failureMessage(accion, categoria, id, e?.javaClass))
    }
}