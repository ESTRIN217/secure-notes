package com.example.crash

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Datos técnicos de un fallo, ya en texto. Deliberadamente **no** lleva el `Throwable`: la traza se convierte a
 * cadena en el hilo que se está muriendo, y que el formateo no esté en ese camino es lo que deja el informe
 * terminado en microsegundos (decisión 3 del plan).
 */
data class DatosInforme(
    val versionApp: String,
    val versionAndroid: String,
    val dispositivo: String,
    val tipoBuild: String,
    val hilo: String,
    val marcaTiempo: Long,
    val traza: String
)

/** Una traza que puede no ser la traza entera. [recortada] dice si le falta algo. */
data class TrazaRecortada(val texto: String, val recortada: Boolean)

/**
 * Texto del informe de fallo (spec 020, RF-118).
 *
 * **Puro**: sin `android` y sin Compose, para que sus pruebas corran en la plataforma de desarrollo (RNF-64).
 *
 * Va en **inglés** a propósito, y no por costume: la constitución 6 pide que los mensajes de log estén en
 * inglés, y quien lee este texto es quien mantiene la app, no quien la usa.
 */
object CrashReportText {

    /**
     * Techo de líneas de traza. Sin él, un `StackOverflowError` —que repite el mismo marco miles de veces—
     * produce un informe de megabytes que ni se copia ni cabe en la URL de una incidencia (caso límite 5).
     */
    const val MAX_LINEAS_TRAZA = 200

    /** Marca de un informe recortado. Distingue «no trae ese tramo» de «el fallo no tenía eso». */
    const val MARCA_DE_RECORTE = "traza recortada"

    private val SELLO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Deja la traza tal cual si cabe y la corta por arriba si no.
     *
     * Las líneas en blanco **del final** no cuentan: una traza con dos saltos de cola no es una traza más
     * larga, y declararla recortada sería mentir sobre el informe.
     */
    fun recortarTraza(traza: String, maximo: Int = MAX_LINEAS_TRAZA): TrazaRecortada {
        val lineas = traza.trimEnd('\n', '\r', ' ', '\t').lines()
        if (lineas.size <= maximo) return TrazaRecortada(traza.trimEnd('\n', '\r'), false)
        return TrazaRecortada(lineas.take(maximo).joinToString("\n"), true)
    }

    /** Monta el informe entero. Es lo que ve, copia y manda el usuario. */
    fun construir(datos: DatosInforme): String {
        val recortada = recortarTraza(datos.traza)
        return buildString {
            appendLine("Secure Notes - crash report")
            appendLine("When: ${sello(datos.marcaTiempo)}")
            appendLine("App version: ${datos.versionApp} (${datos.tipoBuild})")
            appendLine("Android: ${datos.versionAndroid}")
            appendLine("Device: ${datos.dispositivo}")
            appendLine("Thread: ${datos.hilo}")
            appendLine()
            appendLine("Stack trace:")
            appendLine(recortada.texto)
            if (recortada.recortada) {
                appendLine("[$MARCA_DE_RECORTE: ${recortadaLineas(datos.traza)}]")
            }
        }
    }

    private fun recortadaLineas(traza: String): String {
        val total = traza.trimEnd('\n', '\r', ' ', '\t').lines().size
        return "$MAX_LINEAS_TRAZA of $total lines"
    }

    private fun sello(millis: Long): String =
        SELLO.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}