package com.example.ui.editor

/**
 * Qué es un archivo que entra en la nota, y con qué aplicación se abre (spec 014, RF-87).
 *
 * Las dos preguntas salían de funciones que mezclaban la decisión con el I/O: leer el nombre de un
 * archivo de la plataforma, copiarlo a disco y lanzar un `Intent` no se prueban, y las buryan entre
 * las dos líneas que **sí** importan —si esto es un audio o un archivo cualquiera, y si se abre
 * dentro de la app o se le pasa a otra.
 *
 * Aquí están esas dos líneas, solas. Quien llama hace el I/O y llama a estas.
 */

/** Las extensiones que el sistema no siempre declara con un tipo `audio/`. */
private val AUDIO_EXTENSIONS = setOf(
    "mp3", "m4a", "aac", "wav", "ogg", "opus", "flac", "amr", "3gp", "3gpp", "mid", "midi"
)

/**
 * Si un archivo es un audio.
 *
 * Se mira **el tipo y la extensión** porque los dos fallan por separado: hay grabadoras que
 * declaran `application/octet-stream` para un `.m4a`, y hay tipos de audio con la extensión
 * rara. Con cualquiera de los dos que case, es audio.
 *
 * La comparación va en minúsculas porque las extensiones llegan como las traiga el sistema de
 * archivos, que no es siempre con minúsculas.
 */
fun isAudioFile(mime: String, ext: String): Boolean =
    mime.startsWith("audio/") || ext.lowercase() in AUDIO_EXTENSIONS

/** Con qué se abre un archivo que ya está en disco. */
enum class ExternalTarget {
    /** Es un PDF: se abre en su propia pestaña. */
    PDF,

    /**
     * No se abre con nada: la plataforma no sabe qué es y no hay con qué probarlo.
     *
     * Se distingue de [VER_CON_OTRA_APP] porque sin tipo no hay ningún `Intent` que construir: lo
     * que se le pase a otra aplicación fallaría igual, y callar es mejor que abrir un error.
     */
    SIN_TIPO,

    /** Es texto: se abre en el editor de código, en su propia pestaña. */
    TEXTO,

    /** Se le pasa a la aplicación que lo sepa abrir. */
    VER_CON_OTRA_APP
}

/**
 * Decide con qué se abre un archivo, en este orden.
 *
 * El orden importa: un PDF llega como `application/pdf` y no debe caer en «se lo paso a otra
 * aplicación», y un texto con tipo raro debe abrirse aquí antes de salir de la app.
 */
fun externalTargetFor(mime: String, ext: String): ExternalTarget = when {
    mime == "application/pdf" -> ExternalTarget.PDF
    mime == "application/octet-stream" -> ExternalTarget.SIN_TIPO
    isTextLikeFile(mime, ext) -> ExternalTarget.TEXTO
    else -> ExternalTarget.VER_CON_OTRA_APP
}