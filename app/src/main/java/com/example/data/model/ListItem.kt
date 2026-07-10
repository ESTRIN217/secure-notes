package com.example.data.model

import java.time.Instant
import java.time.format.DateTimeFormatter

data class ListItem(
    val id: String,
    val title: String,
    val summary: String,
    val lastModified: Instant,
    val backgroundColor: Int? = null,
    val backgroundImagePath: String? = null,
    val tags: List<String> = emptyList(),
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val categoryId: String? = null,
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false
)
