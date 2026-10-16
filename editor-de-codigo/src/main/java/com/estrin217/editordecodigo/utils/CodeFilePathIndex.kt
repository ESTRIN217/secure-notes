package com.estrin217.editordecodigo.utils

import com.estrin217.editordecodigo.data.model.CodeFile

/**
 * Rutas locales que el editor todavía lista en sus recientes (spec 024, RF-152/RF-153).
 *
 * Pieza **pura** (no importa `android` ni `androidx`): recibe las filas de `code_files` y devuelve
 * las rutas a proteger del borrado de huérfanos del administrador de almacenamiento. El `Context`
 * se queda en quien consulta el índice (`CodeEditor`), que es lo que permite probar esta regla sin
 * dispositivo (RNF-92).
 *
 * Solo cuentan las rutas **locales**: una fila que solo apunta al almacenamiento compartido
 * (`content://`) no vive en el disco de la app y no protege nada de lo que el escaneo ve.
 */
object CodeFilePathIndex {

    private const val FILE_SCHEME = "file://"
    private const val CONTENT_SCHEME = "content://"

    /** Rutas locales absolutas, sin duplicados, tal como las guardó el editor. */
    fun localPaths(files: List<CodeFile>): Set<String> {
        val paths = linkedSetOf<String>()
        for (file in files) {
            val raw = file.filePath?.takeIf { it.isNotBlank() } ?: continue
            if (raw.startsWith(CONTENT_SCHEME)) continue
            paths.add(raw.removePrefix(FILE_SCHEME))
        }
        return paths
    }
}
