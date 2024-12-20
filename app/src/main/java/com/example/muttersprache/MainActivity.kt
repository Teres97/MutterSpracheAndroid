package com.example.muttersprache

import android.app.AlertDialog
import android.content.DialogInterface
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.activity.ComponentActivity
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.Timer
import java.util.TimerTask
import kotlin.math.log

data class Quintuple<T1, T2, T3, T4, T5>(val first: T1, val second: T2, val third: T3, val fourth: T4, val fifth: T5) {
    override fun toString(): String {
        return "$first $second $third $fourth $fifth"
    }
}

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var sentences: MutableList<Quintuple<String, String, String, String, String>>
    private lateinit var timer: Timer
    private lateinit var listView: ListView
    private lateinit var deleteIDInput: EditText

    private var speed: Float = 0.7f
    private var currentDeleteID: String = ""
    private var repeatTime: Long = 60000
    private var repeatSentence: String = ""
    private var flagTimer: Boolean = false
    private var language: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dbHelper = DatabaseHelper(this)
        listView = findViewById(R.id.listView)

        val DATABASE_NAME = "new.db"
        val databasePath = applicationContext.getDatabasePath(DATABASE_NAME).path
        dbHelper.addColumnIfNeeded()

        if (!File(databasePath).exists()) {
            val inputStream = applicationContext.assets.open(DATABASE_NAME)
            val outputStream = FileOutputStream(databasePath)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
        }
        language = "en"
        loadSentencesFromDatabase(databasePath)
        val adapter = ArrayAdapter(this, R.layout.list_item_layout, R.id.textViewText, sentences)
        listView.adapter = adapter

        textToSpeech = TextToSpeech(this, this)

        val editText: EditText = findViewById(R.id.editText)
        val translateText: EditText = findViewById(R.id.translateText)
        val addButton: Button = findViewById(R.id.addButton)
        addButton.setOnClickListener {
            val text = editText.text.toString()
            val translation = translateText.text.toString()
            if (text.isNotEmpty()) {
                when (language) {
                    "de" -> dbHelper.addSentence(text, translation, 0, "de")
                    "en" -> dbHelper.addSentence(text, translation, 0, "en")
                    else -> dbHelper.addSentence(text, translation, 0, "en")
                }
                editText.text.clear()
                translateText.text.clear()
                showSentences(databasePath)
            }
        }

        val startButton: Button = findViewById(R.id.start_button)
        startButton.setOnClickListener { startSpeech() }

        val stopButton: Button = findViewById(R.id.stop_button)
        stopButton.setOnClickListener { stopSpeech() }

        val repeatButton: Button = findViewById(R.id.repeat_button)
        repeatButton.setOnClickListener { repeatSpeech() }

        deleteIDInput = findViewById(R.id.deleteId)

        val deleteButton: Button = findViewById(R.id.deleteIdButton)
        deleteButton.setOnClickListener {
            if (deleteIDInput.text.isNotEmpty()) {
                val deleteID = deleteIDInput.text.toString().toInt()
                dbHelper.deleteSentence(deleteID)
                showSentences(databasePath)
            }
        }

        val speedSeekBar: SeekBar = findViewById(R.id.speedSeekBar)
        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                speed = ((progress.toFloat() / 100) + 0.0).toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val languageButton: Button = findViewById(R.id.languageButton)
        languageButton.setOnClickListener { showLanguageSelectionDialog() }
    }

    private fun loadSentencesFromDatabase(databasePath: String) {
        val database: SQLiteDatabase = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READWRITE)
        val cursor: Cursor = database.rawQuery("SELECT rowid, text, translation, count, language FROM sentences where language=?", arrayOf(language))
        sentences = mutableListOf()
        while (cursor.moveToNext()) {
            sentences.add(Quintuple(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)))
        }
        cursor.close()
        database.close()
    }

    private fun repeatSpeech() {
        textToSpeech.setSpeechRate(speed)
        textToSpeech.speak(repeatSentence, TextToSpeech.QUEUE_FLUSH, null)
    }

    private fun startSpeech() {
        if (!flagTimer) {
            timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    speakRandomSentence()
                }
            }, 0, repeatTime)
            flagTimer = true
        }
    }

    private fun stopSpeech() {
        if (flagTimer) {
            timer.cancel()
            flagTimer = false
        }
    }

    private fun speakRandomSentence() {
        if (sentences.isNotEmpty()) {
            val index = Random().nextInt(sentences.size)
            val sentence = sentences[index].second
            repeatSentence = sentence
            textToSpeech.setSpeechRate(speed)
            textToSpeech.speak(sentence, TextToSpeech.QUEUE_FLUSH, null)

            val textView: TextView = findViewById(R.id.textViewSentences)
            runOnUiThread { textView.text = sentence }

            val dbHelper = DatabaseHelper(this)
            dbHelper.updateSentence(sentences[index].first, sentences[index].fourth)
            currentDeleteID = sentences[index].first
            deleteIDInput.setText(currentDeleteID)
            showSentences(applicationContext.getDatabasePath("new.db").path)
        }
    }

    private fun showSentences(databasePath: String) {
        val database: SQLiteDatabase = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READWRITE)
        val cursor: Cursor = database.rawQuery("SELECT rowid, text, translation, count, language FROM sentences where language=? ORDER BY count DESC", arrayOf(language))
        sentences = mutableListOf()
        while (cursor.moveToNext()) {
            sentences.add(Quintuple(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)))
        }
        cursor.close()
        database.close()

        val adapter = ArrayAdapter(this, R.layout.list_item_layout, R.id.textViewText, sentences)
        runOnUiThread { listView.adapter = adapter }
        print(language)
    }

    private fun showLanguageSelectionDialog() {
        val languages = arrayOf("English", "German")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Language")
            .setItems(languages) { _, which ->
                val selectedLanguage = when (which) {
                    0 -> Locale.ENGLISH
                    1 -> Locale.GERMAN
                    else -> Locale.ENGLISH
                }
                textToSpeech.language = selectedLanguage
                language = when (selectedLanguage) {
                    Locale.ENGLISH -> "en"
                    Locale.GERMAN -> "de"
                    else -> "en"
                }
                showSentences(applicationContext.getDatabasePath("new.db").path)

            }
        builder.create().show()
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Toast.makeText(this, "TTS Initialization Failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
        if (::timer.isInitialized) timer.cancel()
    }
}
