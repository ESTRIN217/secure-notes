package com.estrin217.codetools.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.estrin217.codetools.data.model.CodeFile
import com.estrin217.codetools.data.samples.CodeSamples
import com.estrin217.codetools.syntax.SupportedLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

@Database(entities = [CodeFile::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class CodeToolsDatabase : RoomDatabase() {

    abstract fun codeFileDao(): CodeFileDao

    companion object {
        @Volatile
        private var INSTANCE: CodeToolsDatabase? = null

        fun getInstance(context: Context): CodeToolsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CodeToolsDatabase::class.java,
                    "code_tools_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate preset sample scripts & configs on first launch
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).codeFileDao().insertFiles(CodeSamples.samples)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
