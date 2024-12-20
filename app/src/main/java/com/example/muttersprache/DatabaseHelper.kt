package com.example.muttersprache

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream

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
    }
    override fun onCreate(db: SQLiteDatabase) {

    }

    fun addColumnIfNeeded() {
        val db = this.writableDatabase
        val cursor = db.rawQuery("PRAGMA table_info($TABLE_NAME)", null)
        var translationColumnExists = false

        // Проверка наличия столбца "translation" в таблице
        while (cursor.moveToNext()) {
            val columnIndex = cursor.getColumnIndex("name")
            if (columnIndex >= 0) {
                val columnName = cursor.getString(columnIndex)
                if (columnName == COLUMN_NAME_TRANSLATION) {
                    translationColumnExists = true
                    break
                }
            }
        }
        cursor.close()

        // Если столбца "translation" нет, то добавляем его
        if (!translationColumnExists) {
            val addColumnQuery1 = "ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_NAME_TRANSLATION TEXT"
            val addColumnQuery2 = "ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_NAME_LANGUAGE TEXT"
            val addColumnQuery4 = "UPDATE $TABLE_NAME SET $COLUMN_NAME_LANGUAGE = 'de'"
            db.execSQL(addColumnQuery4)
            db.execSQL(addColumnQuery1)
            db.execSQL(addColumnQuery2)

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
        val id = db.insert(TABLE_NAME, null, contentValues)
        db.close()
        return id
    }

    fun deleteSentence(id: Int): Int {
        val db = this.writableDatabase
        val selection = "$COLUMN_ID = ?"
        val selectionArgs = arrayOf(id.toString())
        val deletedRows = db.delete(TABLE_NAME, selection, selectionArgs)
        db.close()
        return deletedRows
    }

    fun updateSentence(id: String, count: String): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put("count", count.toInt()+1)
        val updatedRows = db.update(TABLE_NAME, contentValues, "rowid = ?", arrayOf(id))
        db.close()
        return updatedRows
    }
}
