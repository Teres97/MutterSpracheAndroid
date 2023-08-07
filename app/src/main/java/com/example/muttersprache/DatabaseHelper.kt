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
        private const val COLUMN_INT_FIELD = "count"
    }
    override fun onCreate(db: SQLiteDatabase) {
//        val databasePath = applicationContext.getDatabasePath(DATABASE_NAME).path
//        // Проверяем, существует ли уже файл базы данных
//        if (!File(databasePath).exists()) {
//            // Открываем поток для копирования файла из assets во внутреннее хранилище
//            val inputStream = applicationContext.assets.open("new.db")
//            val outputStream = FileOutputStream(databasePath)
//
//            // Копируем файл
//            val buffer = ByteArray(1024)
//            var length: Int
//            while (inputStream.read(buffer).also { length = it } > 0) {
//                outputStream.write(buffer, 0, length)
//            }
//
//            // Закрываем потоки
//            outputStream.flush()
//            outputStream.close()
//            inputStream.close()
//        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addSentence(sentence: String, count: Int): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_TEXT, sentence)
        contentValues.put(COLUMN_INT_FIELD, count)
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

    fun getAllSentences(): List<String> {
        val sentenceList = mutableListOf<String>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_NAME"
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val text = cursor.getString(1)
                val intField = cursor.getString(2)
                val sentence: String = id.toString() + " " + text + " " + intField
                sentenceList.add(sentence)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return sentenceList
    }
}
