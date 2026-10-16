package com.estrin217.markdown

/**
 * Qué imagen se puede leer desde la vista previa, decidido **sin tocar el disco** (spec 019, RF-111, RF-112,
 * RF-113, RNF-58).
 *
 * ## El fichero no importa nada de `android`, ni de `androidx.compose`, ni el reloj
 *
 * No es purismo: es lo que permite ejercitarlo con JUnit en una plataforma donde la mitad de la suite no corre por
 * falta de runtime nativo de Robolectric. Un invariante lo comprueba. Por eso **no puede** comprobar si un
 * fichero existe —eso ya no sería una función pura— y por eso hay dos pasos: aquí se decide si el destino es
 * *legible*, y luego quien lo intente leer descubre si estaba.
 */
enum class ImagenPermitida {

    /** Es una imagen y está dentro de la carpeta: se intenta leer (RF-111). */
    PERMITIDA,

    /** No es un destino de imagen, o no hay de dónde resolverlo. No se intenta leer nada. */
    NO_ES_IMAGEN,

    /**
     * Es una ruta, pero **se sale** de la carpeta del archivo (RF-113). Se marca como bloqueada.
     *
     * Es un caso aparte de [BLOQUEADA] porque la carpeta propia es lo que se está respetando, y el motivo que se
     * puede dar es otro: no «es remota» sino «está fuera».
     */
    FUERA_DE_CARPETA,

    /** No es local: `http`, `data`, `javascript`, sin carpeta base… No se intenta leer nada (RF-112). */
    BLOQUEADA
}

/**
 * Clasifica el destino de una imagen del Markdown.
 *
 * ## El orden de las cuatro comprobaciones
 *
 * No es arbitrario, y **cambiarlo rompe cosas**:
 *
 * 1. **Vacío** → `NO_ES_IMAGEN`. No hay nada que decidir.
 * 2. **Esquema que no sea `file`** → `BLOQUEADA`. Va **antes** de mirar la extensión porque
 *    `data:image/png;base64,AAAA` **no termina en una extensión de imagen** —termina en `,AAAA`— y mirarla antes
 *    devolvería `NO_ES_IMAGEN` para algo que en realidad es una imagen **remota**, que es justo el error que
 *    importa. Es la comprobación más fuerte y va primero.
 * 3. **Extensión que no es de imagen** → `NO_ES_IMAGEN`. Evita handingle a `BitmapFactory` un `.apk` o un `.zip`.
 * 4. **Ruta resuelta dentro de la carpeta** → `PERMITIDA`; si no, `FUERA_DE_CARPETA`.
 *
 * @param destino El destino tal cual viene en el Markdown: relativa, absoluta, con `file://` o con otro esquema.
 * @param carpetaBase La ruta del **archivo** que se está previsualizando. No la carpeta: de ella se deduce el
 *   directorio, que es contra lo que se resuelve una ruta relativa.
 */
fun clasificarImagen(destino: String, carpetaBase: String?): ImagenPermitida {
    if (destino.isBlank()) return ImagenPermitida.NO_ES_IMAGEN
    if (carpetaBase.isNullOrBlank()) return ImagenPermitida.BLOQUEADA

    val ruta = desenvolver(destino.trim())

    // `//host/ruta` es una URL sin esquema: no tiene `:` y una comprobación de esquema sola lo dejaría pasar como
    // ruta local absoluta, que acaba en un `FUERA_DE_CARPETA` en lugar de en un `BLOQUEADA` explícito.
    if (ruta.startsWith("//")) return ImagenPermitida.BLOQUEADA

    val esquema = esquemaDe(ruta)
    if (esquema != null && esquema != "file") return ImagenPermitida.BLOQUEADA
    val sinEsquema = if (esquema == "file") ruta.removePrefix("file://") else ruta

    if (!tieneExtensionDeImagen(sinEsquema)) return ImagenPermitida.NO_ES_IMAGEN

    val base = directorioDe(carpetaBase.trim())
    val absoluta = normalizar(if (sinEsquema.startsWith("/")) sinEsquema else "$base/$sinEsquema")
    return if (absoluta == base || absoluta.startsWith("$base/")) {
        ImagenPermitida.PERMITIDA
    } else {
        ImagenPermitida.FUERA_DE_CARPETA
    }
}

/** El directorio que contiene al archivo, ya normalizado y **sin barra final**. */
private fun directorioDe(carpetaBase: String): String {
    val sinFile = carpetaBase.substringBeforeLast('/', "")
    return normalizar(sinFile.ifEmpty { "/" })
}

/**
 * Las extensiones que `BitmapFactory` sabe decodificar.
 *
 * **No está `svg`.** Es una pérdida real y pequeña —un SVG incrustado salía en el `WebView`— y aquí se verá el
 * marcador de que no se pudo leer. Meter un SVG obligaría a un parser de vector, que es un parser de XML con
 * scripting en algunos motores: no es un paso que quepa en esta spec.
 */
private val EXTENSIONES_DE_IMAGEN = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp")

private fun tieneExtensionDeImagen(ruta: String): Boolean {
    val nombre = ruta.substringAfterLast('/')
    if (!nombre.contains('.')) return false
    return nombre.substringAfterLast('.').lowercase() in EXTENSIONES_DE_IMAGEN
}

/** El esquema en minúsculas, o `null` si no lo hay. */
private fun esquemaDe(url: String): String? {
    val separador = url.indexOf(':')
    // Antes de la `:` tiene que haber un nombre de esquema válido, o `a:b` —una ruta con dos puntos— no sería un
    // destino de red sino un nombre de fichero rarísimo.
    if (separador <= 0) return null
    val posible = url.substring(0, separador)
    if (!posible.all { it.isLetter() || it == '+' || it == '-' || it == '.' }) return null
    return posible.lowercase()
}

/** `%20` y `%C3%B1` vuelven a ser un espacio y una `ñ`. Un `%` suelto se deja como estaba. */
private fun desenvolver(texto: String): String {
    if ('%' !in texto) return texto
    val salida = StringBuilder(texto.length)
    var i = 0
    while (i < texto.length) {
        val caracter = texto[i]
        if (caracter == '%' && i + 2 < texto.length) {
            val hex = texto.substring(i + 1, i + 3)
            val valor = hex.toIntOrNull(16)
            if (valor != null) {
                salida.append(valor.toChar())
                i += 3
                continue
            }
        }
        salida.append(caracter)
        i++
    }
    return salida.toString()
}

/**
 * Resuelve `.` y `..` y quita barras de más.
 *
 * No toca `File.canonicalize` a propósito: eso **toca el disco** —consulta la ruta real del sistema de ficheros— y
 * esta función es pura porque sus pruebas tienen que correr en la plataforma de desarrollo.
 */
private fun normalizar(ruta: String): String {
    val esAbsoluta = ruta.startsWith("/")
    val partes = mutableListOf<String>()
    for (parte in ruta.split('/')) {
        when (parte) {
            "", "." -> Unit
            ".." -> if (partes.isNotEmpty() && partes.last() != "..") partes.removeAt(partes.lastIndex) else if (!esAbsoluta) partes.add(parte)
            else -> partes.add(parte)
        }
    }
    return (if (esAbsoluta) "/" else "") + partes.joinToString("/")
}