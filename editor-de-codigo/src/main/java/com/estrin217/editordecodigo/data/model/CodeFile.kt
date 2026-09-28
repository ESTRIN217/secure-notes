package com.estrin217.editordecodigo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.estrin217.editordecodigo.syntax.SupportedLanguage

/**
 * Entidad de metadatos e historial de archivos.
 * El contenido vive fuera en almacenamiento del dispositivo o URIs externas,
 * manteniéndose aquí solo metadatos (ruta, URI, fecha, tipo, tamaño) para el historial de recientes.
 */
@Entity(tableName = "code_files")
data class CodeFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val content: String = "",
    val language: SupportedLanguage,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSample: Boolean = false,
    val isPinned: Boolean = false,
    val filePath: String? = null,
    val uriString: String? = null,
    val sizeBytes: Long = 0L
)
