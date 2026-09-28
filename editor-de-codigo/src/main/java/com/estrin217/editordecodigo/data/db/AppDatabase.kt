package com.estrin217.editordecodigo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.estrin217.editordecodigo.data.model.CodeFile
import com.estrin217.editordecodigo.syntax.SupportedLanguage

class Converters {
    @TypeConverter
    fun fromLanguage(language: SupportedLanguage): String = language.name

    @TypeConverter
    fun toLanguage(value: String): SupportedLanguage = try {
        SupportedLanguage.valueOf(value)
    } catch (_: Exception) {
        SupportedLanguage.PLAIN_TEXT
    }
}

@Database(entities = [CodeFile::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun codeFileDao(): CodeFileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "code_tools_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
