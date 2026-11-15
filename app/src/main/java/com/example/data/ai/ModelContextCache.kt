package com.example.data.ai

/**
 * Caché persistente del `context_length` de cada modelo on-device (spec 033, RF-241).
 *
 * Cada `.gguf` trae su propio **context_length** en sus metadatos; leerlo del disco cuesta milisegundos
 * por archivo, así que se cachea **una única vez por archivo** en SharedPreferences y se reutiliza para
 * siempre (el archivo no cambia; si se borra y descarga de nuevo, se borra la entrada).
 *
 * Formato del texto de la preferencia: una línea por modelo, `ggufId=contextLength`. Las líneas sin
 * `=` o con clave vacía se ignoran; duplicados: gana la última.
 *
 * Es **puro**: no importa `android` ni Compose (RNF-146/RNF-140).
 */

private const val SEPARATOR = '='
private const val LINE_BREAK = '\n'

/** Parsea el texto de la preferencia (`ggufId=context`). */
fun parseContextLengths(text: String): Map<String, Long> =
    text.split(LINE_BREAK)
        .mapNotNull { line ->
            val sep = line.indexOf(SEPARATOR)
            if (sep <= 0) return@mapNotNull null
            val id = line.take(sep).trim()
            val value = line.substring(sep + 1).trim().toLongOrNull() ?: return@mapNotNull null
            if (id.isEmpty() || value <= 0) return@mapNotNull null
            id to value
        }
        .toMap()

/** Serializa el mapa al formato de la preferencia. Los ids no pueden contener `=`. */
fun serializeContextLengths(map: Map<String, Long>): String =
    map.entries.joinToString(LINE_BREAK.toString()) { (id, length) -> "$id$SEPARATOR$length" }
