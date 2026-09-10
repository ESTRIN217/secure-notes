package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icono Material según la extensión del nombre de archivo.
 * Sin extensión o desconocida: [AttachFile] genérico.
 */
fun fileIconForName(name: String): ImageVector {
    return when (name.substringAfterLast('.', "").lowercase()) {
        "pdf" -> Icons.Default.PictureAsPdf
        "doc", "docx", "odt", "rtf", "txt", "log", "md", "markdown" -> Icons.Default.Description
        "xls", "xlsx", "csv", "ods" -> Icons.Default.TableChart
        "ppt", "pptx", "odp" -> Icons.Default.Slideshow
        "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "ico" -> Icons.Default.Image
        "mp3", "wav", "ogg", "m4a", "flac", "opus" -> Icons.Default.AudioFile
        "mp4", "mkv", "webm", "mov", "avi" -> Icons.Default.VideoFile
        "zip", "rar", "7z", "tar", "gz" -> Icons.Default.FolderZip
        "apk" -> Icons.Default.Android
        "kt", "kts", "java", "js", "mjs", "cjs", "ts", "tsx", "py", "sh",
        "json", "xml", "yml", "yaml", "html", "htm", "css", "sql", "c",
        "h", "cpp", "hpp", "go", "rs", "php", "rb", "swift", "dart",
        "lua", "cs" -> Icons.Default.Code
        else -> Icons.Default.AttachFile
    }
}
