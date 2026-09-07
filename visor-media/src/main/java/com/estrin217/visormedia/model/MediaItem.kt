package com.estrin217.visormedia.model

data class MediaItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val type: String, // "image", "audio", "video"
    val uri: String,
    val thumbnailUrl: String = "",
    val isSample: Boolean = false
)
