package com.estrin217.visormedia.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas **puras** de la clasificación de URLs de vídeo (spec 023, RF-141, RF-144, RF-145, RF-151).
 *
 * No usan Robolectric ni el entorno de pruebas: `VideoUrlHelper` no importa `android`, y es la única forma de
 * que la suite se ejecute en esta plataforma. Miden la **decisión** y el **rótulo**, no el reproductor: que el
 * vídeo se vea dentro se comprueba en dispositivo (T7).
 */
class VideoUrlHelperTest {

    private val extensionesSoportadas =
        listOf("mp4", "m4v", "mov", "webm", "mkv", "m3u8", "ts", "flv", "ogv", "ogg")

    // ── Regresión: lo que ya existía no se mueve (RNF-88) ──────────────────────────────────

    @Test
    fun `isWebVideoUrl solo acepta http y https`() {
        assertTrue(VideoUrlHelper.isWebVideoUrl("https://ejemplo.com/v.mp4"))
        assertTrue(VideoUrlHelper.isWebVideoUrl("http://ejemplo.com/v.mp4"))
        assertTrue(VideoUrlHelper.isWebVideoUrl("  https://ejemplo.com/v.mp4  "))
        assertFalse(VideoUrlHelper.isWebVideoUrl("/data/user/0/com.estrin217.securenotes/files/v.mp4"))
        assertFalse(VideoUrlHelper.isWebVideoUrl("content://media/external/video/1"))
        assertFalse(VideoUrlHelper.isWebVideoUrl("file:///storage/emulated/0/v.mp4"))
        assertFalse(VideoUrlHelper.isWebVideoUrl(""))
        assertFalse(VideoUrlHelper.isWebVideoUrl("   "))
    }

    @Test
    fun `isYouTubeUrl reconoce watch, corto, shorts, embed y music`() {
        assertTrue(VideoUrlHelper.isYouTubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(VideoUrlHelper.isYouTubeUrl("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(VideoUrlHelper.isYouTubeUrl("youtu.be/dQw4w9WgXcQ"))
        assertTrue(VideoUrlHelper.isYouTubeUrl("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertTrue(VideoUrlHelper.isYouTubeUrl("https://www.youtube.com/embed/dQw4w9WgXcQ"))
        assertTrue(VideoUrlHelper.isYouTubeUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertFalse(VideoUrlHelper.isYouTubeUrl("https://vimeo.com/123456"))
        assertFalse(VideoUrlHelper.isYouTubeUrl("/data/user/0/v.mp4"))
    }

    // ── RF-141, RF-145: archivo de vídeo directo ───────────────────────────────────────────

    @Test
    fun `todas las extensiones de la lista son archivo de video directo`() {
        for (extension in extensionesSoportadas) {
            assertTrue(
                "falta la extensión .$extension en la lista cerrada (RF-141)",
                VideoUrlHelper.isDirectVideoFileUrl("https://cdn.ejemplo.com/v.$extension")
            )
        }
    }

    @Test
    fun `la extension se reconoce sin distinguir mayusculas`() {
        assertTrue(VideoUrlHelper.isDirectVideoFileUrl("https://cdn.ejemplo.com/V.MP4"))
        assertTrue(VideoUrlHelper.isDirectVideoFileUrl("https://cdn.ejemplo.com/V.Mp4"))
        assertTrue(VideoUrlHelper.isDirectVideoFileUrl("https://cdn.ejemplo.com/v.M3U8"))
    }

    @Test
    fun `la consulta y el fragmento no estorban`() {
        assertTrue(VideoUrlHelper.isDirectVideoFileUrl("https://cdn.ejemplo.com/v.MP4?token=abc"))
        assertTrue(VideoUrlHelper.isDirectVideoFileUrl("https://cdn.ejemplo.com/v.m3u8#t=30"))
        assertTrue(VideoUrlHelper.isDirectVideoFileUrl("https://cdn.ejemplo.com/v.mp4?token=abc#t=10"))
    }

    @Test
    fun `una pagina web no es un archivo de video`() {
        assertFalse(VideoUrlHelper.isDirectVideoFileUrl("https://vimeo.com/123456"))
        assertFalse(VideoUrlHelper.isDirectVideoFileUrl("https://ejemplo.com/ver/video"))
        assertFalse(VideoUrlHelper.isDirectVideoFileUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `una extension no soportada no es un archivo de video`() {
        assertFalse(VideoUrlHelper.isDirectVideoFileUrl("https://ejemplo.com/pelicula.avi"))
        assertFalse(VideoUrlHelper.isDirectVideoFileUrl("https://ejemplo.com/video.mp4.exe"))
    }

    @Test
    fun `la extension tiene que estar al final de la ruta`() {
        assertFalse(VideoUrlHelper.isDirectVideoFileUrl("https://ejemplo.com/mp4/listado"))
        assertFalse(VideoUrlHelper.isDirectVideoFileUrl("https://ejemplo.com/mp4"))
    }

    @Test
    fun `una ruta local no es un archivo de video web`() {
        assertFalse(VideoUrlHelper.isDirectVideoFileUrl("/data/user/0/com.estrin217.securenotes/files/v.mp4"))
        assertFalse(VideoUrlHelper.isDirectVideoFileUrl("content://media/external/video/1"))
        assertFalse(VideoUrlHelper.isDirectVideoFileUrl("file:///storage/emulated/0/v.mp4"))
        assertFalse(VideoUrlHelper.isDirectVideoFileUrl(""))
    }

    // ── RF-140…RF-144: la decisión única ───────────────────────────────────────────────────

    @Test
    fun `un video de YouTube se abre dentro`() {
        assertTrue(VideoUrlHelper.opensInApp("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(VideoUrlHelper.opensInApp("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(VideoUrlHelper.opensInApp("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertTrue(VideoUrlHelper.opensInApp("https://www.youtube.com/embed/dQw4w9WgXcQ"))
        assertTrue(VideoUrlHelper.opensInApp("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `youtu punto be sin esquema se sigue abriendo dentro`() {
        // Se abría dentro **antes** de esta spec (no era `isWebVideoUrl`). Comprobar la web primero lo sacaría
        // fuera: es el caso que no se puede romper.
        assertTrue(VideoUrlHelper.opensInApp("youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun `un archivo de video directo se abre dentro`() {
        assertTrue(VideoUrlHelper.opensInApp("https://cdn.ejemplo.com/v.mp4"))
        assertTrue(VideoUrlHelper.opensInApp("https://cdn.ejemplo.com/v.m3u8"))
        assertTrue(VideoUrlHelper.opensInApp("https://cdn.ejemplo.com/V.MP4?token=abc#t=10"))
    }

    @Test
    fun `una URL web que no se reproduce se abre fuera`() {
        assertFalse(VideoUrlHelper.opensInApp("https://vimeo.com/123456"))
        assertFalse(VideoUrlHelper.opensInApp("https://ejemplo.com/ver/video"))
        assertFalse(VideoUrlHelper.opensInApp("https://ejemplo.com/pelicula.avi"))
        assertFalse(VideoUrlHelper.opensInApp("https://ejemplo.com/mp4/listado"))
    }

    @Test
    fun `una fuente local se abre dentro como hasta ahora`() {
        assertTrue(VideoUrlHelper.opensInApp("/data/user/0/com.estrin217.securenotes/files/v.mp4"))
        assertTrue(VideoUrlHelper.opensInApp("file:///storage/emulated/0/v.mp4"))
        assertTrue(VideoUrlHelper.opensInApp("content://media/external/video/1"))
    }

    @Test
    fun `una fuente vacia no es una URL web`() {
        // El bloque con la fuente vacía enseña el marcador «reemplazar», no la decisión; aquí solo se fija que
        // la regla no la confunde con una URL web (RF-143).
        assertTrue(VideoUrlHelper.opensInApp(""))
        assertTrue(VideoUrlHelper.opensInApp("   "))
    }

    // ── RF-151, RNF-91: el rótulo de la pestaña de un vídeo web ────────────────────────────

    @Test
    fun `las cuatro formas del enlace de YouTube dan el mismo rotulo`() {
        // Es lo que se espera al mirar dos pestañas del mismo vídeo: el identificador, no la ruta.
        assertEquals("dQw4w9WgXcQ", VideoUrlHelper.webVideoLabel("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", VideoUrlHelper.webVideoLabel("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", VideoUrlHelper.webVideoLabel("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", VideoUrlHelper.webVideoLabel("https://www.youtube.com/embed/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", VideoUrlHelper.webVideoLabel("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `el rotulo de YouTube no se queda con los parametros de detras`() {
        assertEquals(
            "dQw4w9WgXcQ",
            VideoUrlHelper.webVideoLabel("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PLabc&t=30")
        )
        assertEquals("dQw4w9WgXcQ", VideoUrlHelper.webVideoLabel("https://youtu.be/dQw4w9WgXcQ?t=30"))
    }

    @Test
    fun `un enlace de YouTube sin esquema tambien se rotula con el identificador`() {
        // Abre dentro por `opensInApp` (no es `isWebVideoUrl`), así que también tiene que rotularse por regla y
        // no por casualidad de `substringAfterLast`.
        assertEquals("dQw4w9WgXcQ", VideoUrlHelper.webVideoLabel("youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun `un archivo directo se rotula con el ultimo tramo de la ruta`() {
        assertEquals("v.mp4", VideoUrlHelper.webVideoLabel("https://cdn.ejemplo.com/videos/v.mp4"))
        assertEquals("v.mp4", VideoUrlHelper.webVideoLabel("https://cdn.ejemplo.com/videos/v.mp4?token=abc"))
        assertEquals("stream.m3u8", VideoUrlHelper.webVideoLabel("https://cdn.ejemplo.com/stream.m3u8#t=10"))
        // La caja se conserva tal como está en la dirección: el rótulo no se compara con nada, se enseña.
        assertEquals("V.MP4", VideoUrlHelper.webVideoLabel("https://cdn.ejemplo.com/V.MP4"))
    }

    @Test
    fun `sin ruta util el rotulo es el host`() {
        assertEquals("ejemplo.com", VideoUrlHelper.webVideoLabel("https://ejemplo.com/"))
        assertEquals("ejemplo.com", VideoUrlHelper.webVideoLabel("https://ejemplo.com?v=1"))
    }

    @Test
    fun `lo que no es una URL web no tiene rotulo web`() {
        // `null` significa «aplica tu regla de siempre», que es el nombre del archivo: la fuente local y el
        // `content://` no cambian de rótulo en esta spec (RF-151, RNF-88).
        assertNull(VideoUrlHelper.webVideoLabel("/data/user/0/com.estrin217.securenotes/files/v.mp4"))
        assertNull(VideoUrlHelper.webVideoLabel("content://media/external/video/1"))
        assertNull(VideoUrlHelper.webVideoLabel("file:///storage/emulated/0/v.mp4"))
        assertNull(VideoUrlHelper.webVideoLabel(""))
        assertNull(VideoUrlHelper.webVideoLabel("   "))
    }
}
