package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * `SimpleDateFormat` NO es thread-safe y crearlo cuesta bastante (parsea el patrón cada vez).
 * Las tarjetas de nota, las burbujas de chat y la exportación creaban uno por item y por
 * recomposición; con scroll rápido eso son cientos de objetos por segundo solo para pintar texto.
 *
 * Aquí hay una instancia por hilo y patrón, invalidada si cambia el locale del sistema.
 * Solo formatea: nunca se usa para parseo.
 */
object CachedDateFormatters {

    private const val NOTE_PATTERN = "LLL dd, yyyy HH:mm"
    private const val TIME_PATTERN = "hh:mm a"
    private const val CHAT_EXPORT_PATTERN = "yyyy-MM-dd HH:mm"

    private val localeRef = AtomicReference(Locale.getDefault())
    private val cache = ThreadLocal.withInitial { HashMap<String, SimpleDateFormat>(3) }

    fun noteDate(timestamp: Long): String = format(NOTE_PATTERN, timestamp)

    fun timeOnly(timestamp: Long): String = format(TIME_PATTERN, timestamp)

    fun exportStamp(timestamp: Long): String = format(CHAT_EXPORT_PATTERN, timestamp)

    private fun format(pattern: String, timestamp: Long): String =
        formatter(pattern).format(Date(timestamp))

    internal fun formatter(pattern: String): SimpleDateFormat {
        val current = Locale.getDefault()
        if (current != localeRef.get()) {
            localeRef.set(current)
            cache.get().clear()
        }
        // `withInitial` garantiza que nunca es null.
        return cache.get()!!.getOrPut(pattern) { SimpleDateFormat(pattern, current) }
    }
}

/**
 * Formateo numérico cacheado para las etiquetas de duración de las respuestas de la IA
 * (`"%.1fs".format(...)` crea un `Formatter` + `StringBuilder` en cada llamada).
 */
object CachedTextFormat {

    private const val SECONDS_PATTERN = "0.0"

    private val decimal = ThreadLocal.withInitial { DecimalFormat(SECONDS_PATTERN) }

    fun chatTime(timestamp: Long): String = CachedDateFormatters.timeOnly(timestamp)

    fun seconds(millis: Long): String = "${decimal.get()!!.format(millis / 1000.0)}s"
}
