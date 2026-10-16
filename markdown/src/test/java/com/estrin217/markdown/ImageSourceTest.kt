package com.estrin217.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Qué imagen se puede leer desde la vista previa (spec 019, RF-111, RF-112, RF-113, RNF-58).
 *
 * Son **pruebas puras** que no necesitan emulador ni entorno de pruebas, que es la única forma de que corran en
 * la plataforma de desarrollo: `ImageSource.kt` no importa nada de `android`, ni de `androidx.compose`, ni el
 * reloj. Un invariante lo comprueba, pero el motivo de que sea así está aquí.
 *
 * ## Por qué cuatro respuestas y no un `Boolean`
 *
 * RF-111, RF-112 y RF-113 piden **tres comportamientos distintos** —cargar, marcar como bloqueada, marcar como
 * bloqueada por salir de la carpeta— y con un `Boolean` no se puede decir por qué se está marcando. Y el cuarto caso
 * no es un marcador: es **no intentar siquiera**.
 *
 * ## Lo que esta función NO puede hacer
 *
 * No comprueba si el fichero existe. Si lo hiciera dejaría de ser pura y sus pruebas no correrían en esta
 * plataforma, que es justo donde corren las suyas. Que el destino *sea* local y *sea* una imagen es todo lo que
 * puede decir; que exista lo descubre quien lo intente leer, y entonces el marcador cambia a «no se pudo leer».
 */
class ImageSourceTest {

    // ── RF-111: lo local se lee ────────────────────────────────────────────────────────────

    @Test
    fun `una ruta relativa dentro de la carpeta se permite`() {
        assertEquals(
            ImagenPermitida.PERMITIDA,
            clasificarImagen("imagenes/flujo.png", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `la misma imagen con su ruta absoluta se permite`() {
        // Las dos formas que puede escribir una persona tienen que dar lo mismo, o el resultado dependería de
        // cómo lo escribiera.
        assertEquals(
            ImagenPermitida.PERMITIDA,
            clasificarImagen("/home/usuario/notas/imagenes/flujo.png", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `un file completo dentro de la carpeta se permite`() {
        assertEquals(
            ImagenPermitida.PERMITIDA,
            clasificarImagen("file:///home/usuario/notas/imagenes/flujo.png", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `una imagen en la propia carpeta se permite`() {
        assertEquals(
            ImagenPermitida.PERMITIDA,
            clasificarImagen("diagrama.png", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `una subcarpeta profunda se permite`() {
        assertEquals(
            ImagenPermitida.PERMITIDA,
            clasificarImagen("a/b/c/d.png", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `un puntoCurrentDirectory no cambia nada`() {
        assertEquals(
            ImagenPermitida.PERMITIDA,
            clasificarImagen("./imagenes/./flujo.png", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `una ruta con espacios y acentos se permite`() {
        // Es de lo más normal en un nombre de fichero real, y es donde falla un `split(' ')` improvisado.
        assertEquals(
            ImagenPermitida.PERMITIDA,
            clasificarImagen("mis imágenes/año 2026.png", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `una ruta codificada con porcentaje se permite`() {
        // Un destino Markdown puede llegar con `%20` en vez de un espacio, y con `%C3%B1` en vez de `ñ`. Si no se
        // desenvuelve antes de comparar, la comparación falla y **una imagen local se marca como bloqueada**.
        assertEquals(
            ImagenPermitida.PERMITIDA,
            clasificarImagen("mis%20imagenes/a%C3%B1o.png", "/home/usuario/notas/guia.md")
        )
    }

    // ── RF-113: lo de fuera no ─────────────────────────────────────────────────────────────

    @Test
    fun `un puntoPunto que se sale se bloquea`() {
        assertEquals(
            ImagenPermitida.FUERA_DE_CARPETA,
            clasificarImagen("../fuera.png", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `un puntoPunto en medio que termina saliendo se bloquea`() {
        // Este es el que falla con una comparación de texto sin normalizar: la ruta **empieza** dentro de la
        // carpeta y aun así se sale.
        assertEquals(
            ImagenPermitida.FUERA_DE_CARPETA,
            clasificarImagen("imagenes/../../fuera.png", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `un puntoPunto codificado con porcentaje se bloquea igual`() {
        // El mismo ataque, escrito de otra forma. **Desenvolver antes de normalizar** es lo que lo para: si se
        // normalizara primero, `%2e%2e` no es `..` y la comprobación lo dejaría pasar.
        assertEquals(
            ImagenPermitida.FUERA_DE_CARPETA,
            clasificarImagen("imagenes/%2e%2e/%2e%2e/fuera.png", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `un puntoPunto que se queda dentro se permite`() {
        // Y el otro lado del mismo caso: un `..` que **no** se sale no puede bloquearse, o `../notas/imagen.png`
        // —que es la carpeta de al lado, con el mismo nombre— se marcaría sin razón.
        assertEquals(
            ImagenPermitida.PERMITIDA,
            clasificarImagen("../notas/imagen.png", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `un file completo fuera de la carpeta se bloquea`() {
        // El caso que justifica decidir **por ruta y no por esquema**: esto es un `file://` y no es del archivo.
        assertEquals(
            ImagenPermitida.FUERA_DE_CARPETA,
            clasificarImagen("file:///sdcard/Download/secreto.png", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `la carpeta base se normaliza antes de comparar`() {
        // Una base con `..` o con barra final tiene que dar lo mismo que la base limpia.
        listOf("/home/usuario/notas/", "/home/usuario/notas", "/home/usuario/./notas").forEach { base ->
            assertEquals(
                "la base '$base' debería comportarse como la canónica",
                ImagenPermitida.PERMITIDA,
                clasificarImagen("imagenes/flujo.png", base)
            )
        }
    }

    // ── RF-112: lo remoto no ──────────────────────────────────────────────────────────────

    @Test
    fun `un destino remoto se bloquea`() {
        listOf(
            "http://ejemplo.com/x.png",
            "https://ejemplo.com/x.png",
            "//ejemplo.com/x.png",
            "data:image/png;base64,AAAA",
            "javascript:alert(1)",
            "content://media/external/images/1",
            "intent://scan/#Intent;scheme=zxing;end"
        ).forEach { destino ->
            assertEquals(
                "el destino '$destino' debería estar bloqueado",
                ImagenPermitida.BLOQUEADA,
                clasificarImagen(destino, "/home/usuario/notas/guia.md")
            )
        }
    }

    @Test
    fun `un ftp tambien se bloquea`() {
        // No es web ni local: no hay ninguna razón para abrirlo, y la regla es «o es local o se bloquea».
        assertEquals(
            ImagenPermitida.BLOQUEADA,
            clasificarImagen("ftp://servidor/x.png", "/home/usuario/notas/guia.md")
        )
    }

    // ── Sin carpeta base: nunca se busca en la raíz del disco ──────────────────────────────

    @Test
    fun `sin carpeta base se bloquea siempre`() {
        // El panel de vista previa solo existe con un archivo abierto, así que esto no debería pasar. Si pasa,
        // la respuesta tiene que ser «no hagas nada» y no «busca en la raíz del disco».
        listOf("imagenes/flujo.png", "/sdcard/x.png", "file:///sdcard/x.png", "x.png").forEach { destino ->
            assertEquals(
                "sin carpeta base, '$destino' no se puede leer",
                ImagenPermitida.BLOQUEADA,
                clasificarImagen(destino, null)
            )
        }
    }

    @Test
    fun `una carpeta base vacia cuenta como que no hay`() {
        assertEquals(
            ImagenPermitida.BLOQUEADA,
            clasificarImagen("x.png", "")
        )
    }

    // ── Lo que no es un destino de imagen ──────────────────────────────────────────────────

    @Test
    fun `un destino vacio no es una imagen`() {
        assertEquals(ImagenPermitida.NO_ES_IMAGEN, clasificarImagen("", "/home/usuario/notas/guia.md"))
        assertEquals(ImagenPermitida.NO_ES_IMAGEN, clasificarImagen("   ", "/home/usuario/notas/guia.md"))
    }

    @Test
    fun `un destino sin extension de imagen no se intenta leer`() {
        // Decidirlo aquí evita handingle a `BitmapFactory` cualquier cosa: un `.apk`, un `.zip`, un directorio.
        assertEquals(
            ImagenPermitida.NO_ES_IMAGEN,
            clasificarImagen("nota", "/home/usuario/notas/guia.md")
        )
        assertEquals(
            ImagenPermitida.NO_ES_IMAGEN,
            clasificarImagen("/home/usuario/notas/guia.md", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `una extension de imagen en mayusculas se acepta`() {
        // `FLUJO.PNG` es el mismo fichero, y una comparación en minúsculas sin normalizar lo perdería.
        assertEquals(
            ImagenPermitida.PERMITIDA,
            clasificarImagen("imagenes/FLUJO.PNG", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `clasificar no muta sus entradas`() {
        // Una función de decisión no toca lo suyo. Se comprueba con una entrada que tiene `..`, que es la que un
        // `normalize` descuidado modificaría en el sitio.
        val destino = "imagenes/../x.png"
        val base = "/home/usuario/notas/guia.md"
        clasificarImagen(destino, base)
        assertEquals("imagenes/../x.png", destino)
        assertEquals("/home/usuario/notas/guia.md", base)
    }

    // ── Propiedades que tienen que valer siempre ──────────────────────────────────────────

    @Test
    fun `un destino local permitido cae dentro de la carpeta`() {
        // Lo permitido cae dentro y lo no permitido cae fuera. Si alguna vez se cumplieran las dos mitades a la
        // vez, la comparación estaría mal.
        assertEquals(
            ImagenPermitida.PERMITIDA,
            clasificarImagen("imagenes/flujo.png", "/home/usuario/notas/guia.md")
        )
        assertEquals(
            ImagenPermitida.FUERA_DE_CARPETA,
            clasificarImagen("imagenes/../../flujo.png", "/home/usuario/notas/guia.md")
        )
    }

    @Test
    fun `la decision no depende del sistema de ficheros`() {
        // Dos llamadas seguidas con la misma entrada dan lo mismo: no hay estado ni caché dentro.
        val base = "/home/usuario/notas/guia.md"
        assertEquals(
            clasificarImagen("imagenes/flujo.png", base),
            clasificarImagen("imagenes/flujo.png", base)
        )
    }

    @Test
    fun `clasificar solo depende de la ruta, no del disco`() {
        // Lo que se **intenta** leer puede no existir, y eso no cambia la clasificación: existe o no, el destino
        // sigue siendo local y válido.
        assertEquals(
            clasificarImagen("imagenes/flujo.png", "/home/usuario/notas/guia.md"),
            clasificarImagen("imagenes/no-existe-nada.png", "/home/usuario/notas/guia.md")
        )
    }
}