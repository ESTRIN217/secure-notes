package com.estrin217.editordecodigo.utils

/**
 * Decisión **pura** de qué URIs merecen que el editor pida el permiso persistente (spec 024, RF-158).
 *
 * Solo el almacenamiento compartido (`content://`) concede un permiso que sobreviva al cierre de la
 * app; una ruta local (`file://` o absoluta) no lo necesita. La regla vive aquí, fuera de
 * `FileImportExportHelper` —que sí importa `android`— para poder probarse sin dispositivo, que es lo
 * único que corre en esta plataforma (ver `AGENTS.md`).
 */
object PersistablePermission {

    private const val SHARED_STORAGE_SCHEME = "content"

    /** True si el esquema de la URI es del almacenamiento compartido. */
    fun appliesTo(scheme: String?): Boolean = scheme == SHARED_STORAGE_SCHEME
}
