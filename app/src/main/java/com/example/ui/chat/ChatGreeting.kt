package com.example.ui.chat

/**
 * L3 — qué saludo se elige (RF-36).
 *
 * El saludo es rotatorio, **sin nombre de persona** y **cíclico**: el mismo día produce siempre el
 * mismo saludo, los días consecutivos recorren el conjunto y, al recorrerlo entero, vuelve a empezar.
 * La elección es una **función pura** del número de día, no una tirada al azar ni el número de
 * composición: así el conjunto se puede ejercitar con una prueba pura y el texto no cambia bajo los
 * ojos de quien está mirando.
 *
 * El número de día **no se calcula aquí**: lo calcula la pantalla una sola vez, con `remember` sin
 * claves, y lo pasa por parámetro (decisión D-3). Este archivo no lee el reloj, no importa
 * `androidx.compose` y no accede a `R`: el texto lo resuelve el Composable con `stringResource`.
 *
 * `count` es el tamaño del conjunto **disponible en el idioma actual**, que puede ser menor que el de
 * otro idioma (caso límite 11): por eso la función siempre devuelve un índice válido para el tamaño
 * recibido, y nunca dos días cualesquiera requieren saludos distintos.
 */
fun greetingIndexFor(day: Long, count: Int): Int {
    // Sin entradas de saludo en el idioma: 0, y sin excepción.
    if (count <= 0) return 0
    val remainder = day % count
    // Un día anterior a época da resto negativo: se corrige para que el índice caiga en 0 until count.
    if (remainder < 0) return (remainder + count).toInt()
    return remainder.toInt()
}
