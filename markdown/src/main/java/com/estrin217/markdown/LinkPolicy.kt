package com.estrin217.markdown

/**
 * Qué direcciones se pueden abrir al pulsarlas, y con qué forma se abren (spec 017, RF-100; spec 018, RF-106).
 *
 * Vive en el módulo del motor y **no** en el del chat porque lo usan las cinco superficies de Markdown, no solo el
 * chat. Es lo que permite que todas compartan **una sola** política de toque en vez de cinco.
 *
 * **No importa nada de `android`, ni de `androidx.compose`, ni el reloj**: sus pruebas son puras y se ejecutan en
 * la plataforma de desarrollo, donde Robolectric no tiene runtime nativo.
 */

/**
 * Qué direcciones se pueden abrir al pulsarlas (spec 017, RF-100).
 *
 * Solo web: `http`, `https` y lo que se escribe sin esquema pero **parece** un dominio. Se rechazan
 * `javascript:`, `intent:`, `file:`, `content:`, `tel:`, `mailto:` y cualquier otra cosa, y eso no es
 * purismo: es lo que impide que una respuesta del modelo pueda lanzar lo que sea al pulsar sobre ella.
 *
 * `mailto:` está deliberadamente **fuera** aunque el motor pueda producirlo con su máscara por defecto:
 * por eso el motor se construye con máscara de solo URLs web (ver `MarkdownEngine`).
 *
 * Sin esquema se acepta lo que tenga un punto con algo antes y después, que es lo que separa un dominio
 * de una frase con punto. `a.b` sí cuenta; `no tengo punto`, no.
 */
fun isWebLink(url: String): Boolean {
    val limpia = url.trim()
    if (limpia.isEmpty()) return false
    val esquema = esquemaDe(limpia)
    if (esquema != null) return esquema == "http" || esquema == "https"
    return pareceDominio(limpia)
}

/** El esquema en minúsculas, o `null` si la dirección no lleva ninguno. */
private fun esquemaDe(url: String): String? {
    val separador = url.indexOf(':')
    if (separador <= 0) return null
    val posible = url.substring(0, separador)
    if (!posible.all { it.isLetter() || it == '+' || it == '-' || it == '.' }) return null
    return posible.lowercase()
}

/** Un dominio tiene punto, algo antes y algo después, y nada de espacios. */
private fun pareceDominio(url: String): Boolean {
    if (url.any { it.isWhitespace() }) return false
    val punto = url.indexOf('.')
    if (punto <= 0 || punto == url.lastIndex) return false
    val sinCamino = url.substringBefore('/')
    return sinCamino.contains('.')
}

/**
 * La dirección **tal y como se abre**: sin la puntuación de la frase en la que estaba metida (spec 018,
 * RF-106).
 *
 * ## Por qué hace falta
 *
 * Un enlace dentro de la pantalla legal sale así, en los nueve idiomas:
 *
 * ```
 * Contact the developer privately via the GitHub profile (https://github.com/ESTRIN217) with the app…
 * ```
 *
 * Un `URLSpan` es **un texto con posición**, no una dirección: si el detector de enlaces metió el paréntesis
 * de cierre dentro, la dirección que se abre es `https://github.com/ESTRIN217)`, que no existe. Y que lo
 * metiera o no **no se puede comprobar en esta plataforma**, porque `Linkify` y `Patterns` son framework de
 * Android y aquí no hay runtime nativo de Robolectric. Es de la familia «una expectativa de memoria es una
 * suposición», y por eso el recorte es una función pura con sus pruebas, en vez de una confianza.
 *
 * ## Las dos mitades de la regla
 *
 * - `.` `,` `;` `:` `!` `?` `"` `'` se quitan **siempre**: son de la frase, no de la dirección.
 * - `)` `]` `}` `>` se quitan **solo si quedan sin parear**. Con un `(`/`[`/`{`/`<` que los abre, **son**
 *   parte de la dirección: `https://ejemplo.com/a_(b)` es válida, y quitarle el `)` abriría otra cosa
 *   (caso límite 6 de la spec 017).
 *
 * ## Las dos propiedades de las que depende el que la llama
 *
 * - **Solo recorta.** Nunca añade un carácter, así que no puede convertir una dirección inocua en otra que no
 *   lo sea: un `javascript:` recortado sigue sin ser web.
 * - **Es idempotente.** Se aplica antes de [isWebLink] y el mismo texto puede pasar dos veces por el
 *   camino; si no lo fuera, la segunda pasada dejaría otra cosa, y la segunda es la que se abriría.
 */
fun direccionParaAbrir(url: String): String {
    var salida = url
    // Se recorta **de uno en uno** y se vuelve a mirar el carácter nuevo, porque «un `)` suelto» y «un `)`
    // detrás de otro `)` suelto» tienen que caer los dos y la decisión es de cada carácter, no del texto.
    //
    // El `when` es exhaustivo a propósito: cada rama dice **qué hace**, y `else` es «este carácter no se
    // quita». La versión anterior era dos guardas encadenadas sobre la misma lista y tenía el fallo de que
    // un `)` —que es cierre, no puntuación— no estaba en ella y salía sin recortar; las pruebas lo cazaron.
    while (salida.isNotEmpty()) {
        val ultimo = salida.last()
        when {
            esPuntuacion(ultimo) -> salida = salida.dropLast(1)
            esCierre(ultimo) && !estaPareado(salida, ultimo) -> salida = salida.dropLast(1)
            else -> break
        }
    }
    return salida
}

/** Los caracteres que son de la frase y nunca de la dirección. */
private fun esPuntuacion(caracter: Char): Boolean = caracter in PUNTUACION_DE_FRASE

/** Los cierres que pueden ser **de la dirección** si algo los abre. */
private fun esCierre(caracter: Char): Boolean = caracter in CIERRES

/**
 * `true` si el **último** carácter del texto es un cierre que algo de antes abrió.
 *
 * Cuenta de izquierda a derecha **parando antes del último carácter**, porque lo que se pregunta es si ese
 * último carácter concreto tiene quién lo abra: si al llegar a él queda un cierre sin pareja, es que lo hay.
 *
 * El saldo **no baja de cero**. Bajar lo convertiría en «cuenta la diferencia entre abrir y cerrar», que es
 * otra pregunta y da la respuesta equivocada en cuanto hay dos: `https://ejemplo.com/a_(b)/c)` tiene un `)`
 * de la ruta y otro de sobra, y el de sobra es el del final; comparar totales no diría cuál de los dos sobra.
 */
private fun estaPareado(texto: String, cierre: Char): Boolean {
    val abre = aperturaDe(cierre)
    var saldo = 0
    for (indice in 0 until texto.length - 1) {
        when (texto[indice]) {
            abre -> saldo++
            cierre -> if (saldo > 0) saldo--
        }
    }
    return saldo > 0
}

private fun aperturaDe(cierre: Char): Char = when (cierre) {
    ')' -> '('
    ']' -> '['
    '}' -> '{'
    else -> '<'
}

/** Puntuación de la frase: se quita siempre. */
private val PUNTUACION_DE_FRASE = charArrayOf('.', ',', ';', ':', '!', '?', '"', '\'', '»')

/** Cierres: se quitan solo si están sin parear. */
private val CIERRES = charArrayOf(')', ']', '}', '>')
