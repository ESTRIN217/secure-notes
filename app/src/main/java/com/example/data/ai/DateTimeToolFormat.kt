package com.example.data.ai

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Formato de las respuestas de las dos herramientas nuevas (spec 043, RF-310, RF-311, RNF-198):
 * decisiones puras, sin `android`, sin recursos y sin librerías nuevas (RNF-197). El «hoy», la zona,
 * la versión y la nota llegan como parámetros: quien llama recoge los valores del sistema y aquí
 * solo se decide cómo se serializan.
 */

/** Contenido de `get_datetime`: ISO-8601 local con desplazamiento y nombre de zona IANA. */
fun getDatetimeResult(instant: Instant, zoneId: ZoneId): String =
    """{"result":${jsonString(localIso(instant, zoneId))},"timezone":${jsonString(zoneId.id)}}"""

/** `xxx` da ±HH:MM y `+00:00` a cero (nunca `Z`), con el desplazamiento vigente en esa fecha. */
private fun localIso(instant: Instant, zoneId: ZoneId): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx").format(instant.atZone(zoneId))

/** Contenido de `get_info`: solo `note` y `os`, sin cwd ni identificadores del dispositivo. */
fun getInfoResult(os: String, api: Int, note: String): String =
    """{"note":${jsonString(note)},"os":${jsonString("Android $os (API $api)")}}"""

/** Escapa comillas, contrabarra y controles a mano: la app no declara un serializador JSON. */
private fun jsonString(value: String): String = buildString {
    append('"')
    value.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (char < ' ') append("\\u%04x".format(char.code)) else append(char)
        }
    }
    append('"')
}
