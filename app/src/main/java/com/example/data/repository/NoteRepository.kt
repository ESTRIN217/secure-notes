package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.model.Note
import com.example.data.model.Tag
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotesFlow: Flow<List<Note>> = noteDao.getAllNotesFlow()
    val allTagsFlow: Flow<List<Tag>> = noteDao.getAllTagsFlow()

    suspend fun getNoteById(id: Int): Note? {
        return noteDao.getNoteById(id)
    }

    suspend fun insertNote(note: Note): Long {
        return noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun insertTag(tag: Tag) {
        noteDao.insertTag(tag)
    }

    suspend fun deleteTag(tag: Tag) {
        noteDao.deleteTag(tag)
    }
}
