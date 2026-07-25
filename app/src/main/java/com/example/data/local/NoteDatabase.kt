package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Note
import com.example.data.model.Tag

@Database(entities = [Note::class, Tag::class, ConversationEntity::class, ChatSessionEntity::class], version = 7, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {
    abstract val noteDao: NoteDao
    abstract val tagDao: TagDao
    abstract val conversationDao: ConversationDao
    abstract val chatSessionDao: ChatSessionDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS conversations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        noteId INTEGER NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        processingTimeMs INTEGER,
                        timestamp INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL DEFAULT 'New Chat',
                        noteId INTEGER,
                        noteTitle TEXT,
                        backend TEXT NOT NULL DEFAULT 'ollama',
                        modelName TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        messageCount INTEGER NOT NULL DEFAULT 0,
                        isPinned INTEGER NOT NULL DEFAULT 0
                    )
                """)
                database.execSQL("ALTER TABLE conversations ADD COLUMN sessionId INTEGER NOT NULL DEFAULT 0")
                database.execSQL("""
                    INSERT INTO chat_sessions (title, noteId, noteTitle, backend, createdAt, updatedAt, messageCount)
                    SELECT 
                        COALESCE((SELECT title FROM notes WHERE id = c.noteId), 'Chat'),
                        c.noteId,
                        (SELECT title FROM notes WHERE id = c.noteId),
                        'ollama',
                        MIN(c.timestamp),
                        MAX(c.timestamp),
                        COUNT(c.id)
                    FROM conversations c
                    GROUP BY c.noteId
                """)
                database.execSQL("""
                    UPDATE conversations SET sessionId = (
                        SELECT id FROM chat_sessions WHERE chat_sessions.noteId = conversations.noteId
                    ) WHERE sessionId = 0
                """)
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE conversations ADD COLUMN modelName TEXT")
            }
        }

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "secure_notes_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}