package com.example.muttersprache

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream

data class Language(val id: Long, val name: String, val code: String)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "new.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "sentences"
        private const val COLUMN_ID = "rowid"
        private const val COLUMN_TEXT = "text"
        private const val COLUMN_TRANSLATION_TEXT = "translation"
        private const val COLUMN_INT_FIELD = "count"
        private const val COLUMN_NAME_TRANSLATION = "translation"
        private const val COLUMN_NAME_LANGUAGE = "language"

        private const val TABLE_LANGUAGES = "languages"
        private const val LANG_COLUMN_ID = "id"
        private const val LANG_COLUMN_NAME = "name"
        private const val LANG_COLUMN_CODE = "code"
    }

    override fun onCreate(db: SQLiteDatabase) {

    }

    fun addColumnIfNeeded() {
        val db = this.writableDatabase
        var cursor = db.rawQuery("PRAGMA table_info($TABLE_NAME)", null)
        var translationColumnExists = false
        var isDeletedColumnExists = false

        // Проверка наличия столбцов "translation" и "isDeleted" в таблице
        while (cursor.moveToNext()) {
            val columnIndex = cursor.getColumnIndex("name")
            if (columnIndex >= 0) {
                val columnName = cursor.getString(columnIndex)
                if (columnName == COLUMN_NAME_TRANSLATION) {
                    translationColumnExists = true
                }
                if (columnName == "isDeleted") {
                    isDeletedColumnExists = true
                }
            }
        }
        cursor.close()

        // Если столбца "translation" нет, то добавляем его
        if (!translationColumnExists) {
            val addColumnQuery1 = "ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_NAME_TRANSLATION TEXT"
            val addColumnQuery2 = "ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_NAME_LANGUAGE TEXT"
            val addColumnQuery3 = "UPDATE $TABLE_NAME SET $COLUMN_NAME_TRANSLATION = ''"
            val addColumnQuery4 = "UPDATE $TABLE_NAME SET $COLUMN_NAME_LANGUAGE = 'de'"
            db.execSQL(addColumnQuery1)
            db.execSQL(addColumnQuery2)
            db.execSQL(addColumnQuery3)
            db.execSQL(addColumnQuery4)
        }

        // Если столбца "isDeleted" нет, то добавляем его
        if (!isDeletedColumnExists) {
            val addColumnQueryIsDeleted = "ALTER TABLE $TABLE_NAME ADD COLUMN isDeleted INTEGER DEFAULT 0"
            val updateQueryIsDeleted = "UPDATE $TABLE_NAME SET isDeleted = 0"
            db.execSQL(addColumnQueryIsDeleted)
            db.execSQL(updateQueryIsDeleted)
        }

        // Создание таблицы languages если не существует
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_LANGUAGES (" +
                    "$LANG_COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$LANG_COLUMN_NAME TEXT NOT NULL, " +
                    "$LANG_COLUMN_CODE TEXT NOT NULL)"
        )

        // Добавляем дефолтные языки если таблица пуста
        val langCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_LANGUAGES", null)
        langCursor.moveToFirst()
        val count = langCursor.getInt(0)
        langCursor.close()

        if (count == 0) {
            val enValues = ContentValues().apply {
                put(LANG_COLUMN_NAME, "English")
                put(LANG_COLUMN_CODE, "en")
            }
            db.insert(TABLE_LANGUAGES, null, enValues)

            val deValues = ContentValues().apply {
                put(LANG_COLUMN_NAME, "German")
                put(LANG_COLUMN_CODE, "de")
            }
            db.insert(TABLE_LANGUAGES, null, deValues)
        }

        db.close()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addSentence(sentence: String, translation: String, count: Int, language: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_TEXT, sentence)
        contentValues.put(COLUMN_TRANSLATION_TEXT, translation)
        contentValues.put(COLUMN_INT_FIELD, count)
        contentValues.put(COLUMN_NAME_LANGUAGE, language)
        contentValues.put("isDeleted", 0)
        val id = db.insert(TABLE_NAME, null, contentValues)
        db.close()
        return id
    }

    fun deleteSentence(id: Int): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("isDeleted", 1)
        }
        val updatedRows = db.update(TABLE_NAME, contentValues, "rowid = ?", arrayOf(id.toString()))
        db.close()
        return updatedRows
    }

    fun updateSentence(id: String, count: String): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put("count", count.toInt() + 1)
        val updatedRows = db.update(TABLE_NAME, contentValues, "rowid = ?", arrayOf(id))
        db.close()
        return updatedRows
    }

    // ========== Languages CRUD ==========

    fun addLanguage(name: String, code: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(LANG_COLUMN_NAME, name)
            put(LANG_COLUMN_CODE, code)
        }
        val id = db.insert(TABLE_LANGUAGES, null, values)
        db.close()
        return id
    }

    fun deleteLanguage(id: Long): Int {
        val db = this.writableDatabase
        val deletedRows = db.delete(TABLE_LANGUAGES, "$LANG_COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
        return deletedRows
    }

    fun getAllLanguages(): List<Language> {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT $LANG_COLUMN_ID, $LANG_COLUMN_NAME, $LANG_COLUMN_CODE FROM $TABLE_LANGUAGES ORDER BY $LANG_COLUMN_NAME",
            null
        )
        val languages = mutableListOf<Language>()
        while (cursor.moveToNext()) {
            languages.add(
                Language(
                    id = cursor.getLong(0),
                    name = cursor.getString(1),
                    code = cursor.getString(2)
                )
            )
        }
        cursor.close()
        db.close()
        return languages
    }

    fun getLanguageStats(): List<Pair<String, Int>> {
        val list = mutableListOf<Pair<String, Int>>()
        val db = this.readableDatabase
        val query = """
            SELECT l.name, COALESCE(SUM(s.count), 0) 
            FROM languages l 
            LEFT JOIN sentences s ON l.code = s.language 
            GROUP BY l.code, l.name
            ORDER BY l.name
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        while (cursor.moveToNext()) {
            val langName = cursor.getString(0)
            val sumCount = cursor.getInt(1)
            list.add(Pair(langName, sumCount))
        }
        cursor.close()
        db.close()
        return list
    }
}
