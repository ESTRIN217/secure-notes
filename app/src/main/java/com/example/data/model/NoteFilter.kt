package com.example.data.model

interface NoteFilter {
    fun matches(note: Note): Boolean
}

object HomeFilter : NoteFilter {
    override fun matches(note: Note) = !note.isArchived && !note.isDeleted
}

object FavoritesFilter : NoteFilter {
    override fun matches(note: Note) = note.isFavorite && !note.isArchived && !note.isDeleted
}

object ArchivedFilter : NoteFilter {
    override fun matches(note: Note) = note.isArchived && !note.isDeleted
}

object TrashFilter : NoteFilter {
    override fun matches(note: Note) = note.isDeleted
}

object SettingsFilter : NoteFilter {
    override fun matches(note: Note) = false
}

val sectionFilters: Map<NavigationSection, NoteFilter> = mapOf(
    NavigationSection.HOME to HomeFilter,
    NavigationSection.FAVORITES to FavoritesFilter,
    NavigationSection.ARCHIVED to ArchivedFilter,
    NavigationSection.TRASH to TrashFilter,
    NavigationSection.SETTINGS to SettingsFilter
)

fun NoteFilter.forSection(section: NavigationSection): NoteFilter {
    return sectionFilters[section] ?: HomeFilter
}
