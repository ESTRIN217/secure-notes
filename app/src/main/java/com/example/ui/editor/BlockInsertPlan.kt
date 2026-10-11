package com.example.ui.editor

/**
 * Dónde va un bloque nuevo cuando el usuario inserta una imagen, un vídeo, un audio, un archivo o
 * un dibujo (spec 014, RF-89).
 *
 * La decisión es la misma en los cinco casos y antes estaba **copiada cinco veces** en el editor:
 *
 * - Si el bloque que está bajo el cursor **está vacío**, se sustituye. Quien escribe una barra
 *   `/imagen` y acepta quiere el bloque en el sitio donde estaba escribiendo, no un hueco antes.
 * - Si **tiene texto**, se inserta detrás y el cursor se queda en el bloque nuevo, que es donde
 *   se va a seguir escribiendo.
 *
 * Es una función pura porque es una decisión y no un efecto: recibe el índice activo, el texto del
 * bloque activo (`null` si no hay bloque) y cuántos hay, y devuelve dónde insertar y si hay que
 * sustituir. Quien la usa se encarga de construir la lista.
 *
 * El texto llega **desformateado**, no como bloque: comprobar si un bloque está vacío exige pasar
 * por el conversor de texto enriquecido, y eso es de quien llama, no de aquí.
 */
data class InsertPlan(
    /** Si el bloque activo se sustituye en vez de insertar otro detrás. */
    val replaceInPlace: Boolean,
    /** Dónde acaba el bloque nuevo, ya acotado a la lista. */
    val index: Int
)

/**
 * @param activeIndex índice del bloque con el cursor.
 * @param activePlainText texto plano del bloque activo, o `null` si no hay bloque activo.
 * @param blockCount cuántos bloques hay ahora.
 */
fun insertPlanFor(activeIndex: Int, activePlainText: String?, blockCount: Int): InsertPlan {
    val sustituir = activePlainText != null && activePlainText.isBlank()
    return InsertPlan(replaceInPlace = sustituir, index = insertionIndex(activeIndex, sustituir, blockCount))
}

/**
 * Dónde acaba un bloque que entra en la lista: donde estaba el cursor si se sustituye, un hueco más
 * abajo si se inserta, siempre acotado al final.
 *
 * Lo comparten los **seis** insertadores del editor, que son la misma forma. No la misma regla:
 * al insertar un medio se sustituye el bloque cuando está **vacío** ([insertPlanFor]), y al
 * resolver una barra «/» se sustituye cuando el bloque **empieza por la barra**, que es el texto
 * que se estaba escribiendo. Por eso la regla se queda en quien llama y aquí solo el índice.
 */
fun insertionIndex(activeIndex: Int, replaceInPlace: Boolean, blockCount: Int): Int {
    val actual = activeIndex.coerceIn(0, blockCount)
    return if (replaceInPlace) actual else (actual + 1).coerceAtMost(blockCount)
}