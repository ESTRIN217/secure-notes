package com.estrin217.visormedia.util

import java.util.regex.Pattern

object VideoUrlHelper {

    /**
     * Extensiones que el reproductor interno reproduce con las dependencias ya declaradas (spec 023, RF-141).
     * La lista es **cerrada** y por eso no entra `.mpd`: DASH no está en el catálogo, y prometer lo que no se
     * puede reproducir deja una pantalla de error donde antes había un navegador.
     */
    private val VIDEO_EXTENSIONS =
        setOf("mp4", "m4v", "mov", "webm", "mkv", "m3u8", "ts", "flv", "ogv", "ogg")

    fun isWebVideoUrl(src: String): Boolean {
        val trimmed = src.trim().lowercase()
        return trimmed.startsWith("http://") || trimmed.startsWith("https://")
    }

    fun isYouTubeUrl(src: String): Boolean {
        val trimmed = src.trim().lowercase()
        return trimmed.contains("youtube.com") || trimmed.contains("youtu.be")
    }

    /**
     * Una URL `http(s)` que apunta a un archivo de vídeo directo (spec 023, RF-141, RF-145). La extensión se
     * mira **sin distinguir mayúsculas** y **después de quitar la consulta y el fragmento**; una página web
     * —aunque lleve la palabra `mp4` en la ruta— no lo es.
     */
    fun isDirectVideoFileUrl(src: String): Boolean {
        if (!isWebVideoUrl(src)) return false
        val ruta = src.trim().lowercase().takeWhile { it != '?' && it != '#' }
        return VIDEO_EXTENSIONS.any { ruta.endsWith(".$it") }
    }

    /**
     * La **decisión única** de las dos superficies que pintan un bloque de vídeo (spec 023, RF-140…RF-144): se
     * reproduce dentro si es de YouTube o un archivo de vídeo directo, y también si no es una URL web (una
     * ruta local, que ya se abría dentro). El resto de URLs web se abren fuera, como hasta ahora.
     *
     * El orden importa: `youtu.be/ID` sin esquema **no** es [isWebVideoUrl], pero **sí** es de YouTube, y el
     * bloque ya lo abría dentro. Comprobar la web primero lo sacaría fuera.
     */
    fun opensInApp(src: String): Boolean {
        if (isYouTubeUrl(src)) return true
        if (isDirectVideoFileUrl(src)) return true
        if (isWebVideoUrl(src)) return false
        return true
    }

    fun extractYouTubeId(url: String): String? {
        val patterns = listOf(
            "(?:https?:\\/\\/)?(?:www\\.)?youtu\\.be\\/([a-zA-Z0-9_-]{11})",
            "(?:https?:\\/\\/)?(?:www\\.)?youtube\\.com\\/(?:watch\\?v=|embed\\/|shorts\\/|v\\/)([a-zA-Z0-9_-]{11})"
        )
        for (p in patterns) {
            val matcher = Pattern.compile(p).matcher(url)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    fun youTubeThumbnail(src: String): String {
        val id = extractYouTubeId(src) ?: return ""
        return "https://img.youtube.com/vi/$id/hqdefault.jpg"
    }

    /**
     * Rótulo corto para la pestaña de un vídeo web (spec 023, RF-151, RNF-91).
     *
     * El identificador si es de YouTube —así las **cuatro** formas del enlace (`watch?v=`, `youtu.be/`,
     * `/shorts/`, `/embed/`) dan el mismo rótulo, que es lo que se espera al mirar dos pestañas del mismo
     * vídeo—; y si no, el último tramo de la **ruta**, con la consulta y el fragmento fuera, para que
     * `v.mp4?token=abc` se lea `v.mp4`. Sin ruta útil (`https://ejemplo.com/`) devuelve el host.
     *
     * Devuelve **`null`** cuando la fuente no es una URL web ni un enlace de YouTube: lo local y el
     * `content://` conservan la regla de siempre, que es el nombre del archivo (RNF-88), y por eso la decisión
     * no se toma aquí.
     *
     * Sale de la propia dirección: **ni red ni disco**. Preguntar los metadatos del vídeo sería una petición
     * por cada pestaña que se abre (RNF-91, constitution 5).
     */
    fun webVideoLabel(src: String): String? {
        val limpia = src.trim()
        if (!isWebVideoUrl(limpia) && !isYouTubeUrl(limpia)) return null
        extractYouTubeId(limpia)?.let { return it }
        val sinConsulta = limpia.takeWhile { it != '?' && it != '#' }
        val tramo = sinConsulta.substringAfterLast('/')
        if (tramo.isNotBlank()) return tramo
        val host = sinConsulta.substringAfter("://").substringBefore('/')
        return if (host.isBlank()) null else host
    }
}
