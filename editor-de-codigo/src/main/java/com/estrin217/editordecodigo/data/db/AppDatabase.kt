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

        const val DB_NAME = "code_tools_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * True si el editor ya creó su base de datos. Quien solo **consulta** los recientes
         * (el administrador de almacenamiento) lo comprueba antes, porque abrir Room **crea** el
         * fichero y un escaneo no debe estrenar datos (spec 024, RNF-94).
         */
        fun databaseExists(context: Context): Boolean =
            context.getDatabasePath(DB_NAME).exists()

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
