package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.R
import com.example.util.OpaqueTrace

/**
 * Abre la fuente de un bloque de vídeo en la app de fuera (spec 023, RF-142).
 *
 * Es el **único** sitio de las dos superficies del bloque (`EditableVideoBlock`,
 * `NoteContentBlockRenderer`) que lanza `Intent.ACTION_VIEW`: tenerlo aparte es lo que permite
 * exigir por invariante que ninguna de las dos lo contenga, en vez de comprobar a ojo en qué
 * rama está.
 *
 * La URL que no se puede abrir **no** se registra: es material de la persona. Al registro va
 * solo la clase de la excepción, con el identificador opaco de [OpaqueTrace] (constitution 5).
 */
internal fun abrirVideoFuera(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        OpaqueTrace.registrarFallo(
            "VideoBlockOpen", "open video externally", "video", OpaqueTrace.isolatedNext(), e
        )
        Toast.makeText(context, context.getString(R.string.toast_cannot_open_url), Toast.LENGTH_SHORT).show()
    }
}
