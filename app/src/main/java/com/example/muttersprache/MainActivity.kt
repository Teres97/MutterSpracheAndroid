package com.example.muttersprache

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.Random
import java.util.Timer
import java.util.TimerTask


class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var sentences: MutableList<String>
    private lateinit var timer: Timer
    private lateinit var listView: ListView

    private var speed: Float = 0.7f
    private var repeatTime:Long = 60000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dbHelper = DatabaseHelper(this)

        listView = findViewById(R.id.listView)
        // Инициализация базы данных и получение всех строк
        val DATABASE_NAME = "new.db"

        // Путь к файлу базы данных внутри внутреннего хранилища
        val databasePath = applicationContext.getDatabasePath(DATABASE_NAME).path

        // Проверяем, существует ли уже файл базы данных
        if (!File(databasePath).exists()) {
            // Открываем поток для копирования файла из assets во внутреннее хранилище
            val inputStream = applicationContext.assets.open("new.db")
            val outputStream = FileOutputStream(databasePath)

            // Копируем файл
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            // Закрываем потоки
            outputStream.flush()
            outputStream.close()
            inputStream.close()
        }

        val database: SQLiteDatabase = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READWRITE)
        val cursor: Cursor = database.rawQuery("SELECT rowid, text, count FROM sentences", null)
        sentences = ArrayList()
        while (cursor.moveToNext()) {
            val sentence: String = cursor.getString(0) + " " + cursor.getString(1) + " " + cursor.getString(2)
            sentences.add(sentence)
        }
        cursor.close()
        database.close()

        val adapter = ArrayAdapter(this, R.layout.list_item_layout, R.id.textViewText, sentences)
        listView.adapter = adapter

        // Инициализация TTS
        textToSpeech = TextToSpeech(this, this)

        val editText: EditText = findViewById(R.id.editText)

        val addButton: Button = findViewById(R.id.addButton)
        addButton.setOnClickListener {
            val text = editText.text.toString()
            if (text.isNotEmpty()) {
                dbHelper.addSentence(text, 0) // вставка данных, 0 - начальное значение счетчика
                editText.text.clear() // очистка поля ввода
                showSentences()
            }
        }

        val startButton: Button = findViewById(R.id.start_button)
        startButton.setOnClickListener { startSpeech() }

        val stopButton: Button = findViewById(R.id.stop_button)
        stopButton.setOnClickListener { stopSpeech() }

        val deleteIDInput: EditText = findViewById(R.id.deleteId)

        val deleteButton: Button = findViewById(R.id.deleteIdButton)
        deleteButton.setOnClickListener {
            val deleteID = deleteIDInput.text.toString().toInt()
            dbHelper.deleteSentence(deleteID)
            showSentences()
        }

        val speedSeekBar: SeekBar = findViewById(R.id.speedSeekBar)
        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Масштабируем значение в диапазоне от 0.5 до 1.5
                speed = ((progress.toFloat() / 100) + 0.0).toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Ничего не требуется при начале трекинга
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Ничего не требуется при окончании трекинга
            }
        })
    }

    private fun startSpeech() {
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                speakRandomSentence()
            }
        }, 0, repeatTime) // Воспроизводить каждые 60 секунд
    }

    private fun stopSpeech() {
        timer.cancel()
    }

    private fun speakRandomSentence() {
        if (sentences.isNotEmpty()) {
            val random = Random()
            val index = random.nextInt(sentences.size)
            val sentence = sentences[index]

            // Установка языка воспроизведения на немецкий
            textToSpeech.language = Locale.GERMAN
            textToSpeech.setSpeechRate(speed)
            // Воспроизведение выбранной строки
            textToSpeech.speak(sentence, TextToSpeech.QUEUE_FLUSH, null)
            val textView: TextView = findViewById(R.id.textViewSentences)
            runOnUiThread{
                textView.text = sentence
            }
            showSentences()
        }
    }

    private fun showSentences() {
        val DATABASE_NAME = "new.db"
        val databasePath = applicationContext.getDatabasePath(DATABASE_NAME).path
        val database: SQLiteDatabase = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READWRITE)
        val cursor: Cursor = database.rawQuery("SELECT rowid, text, count FROM sentences ORDER BY count DESC", null)
        sentences = ArrayList()
        while (cursor.moveToNext()) {
            val sentence: String = cursor.getString(0) + " " + cursor.getString(1) + " " + cursor.getString(2)
            sentences.add(sentence)
        }
        cursor.close()
        database.close()

        val adapter = ArrayAdapter(this, R.layout.list_item_layout, R.id.textViewText, sentences)
        runOnUiThread {
            listView.adapter = adapter
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Успешная инициализация TTS
        } else {
            // Ошибка инициализации TTS
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
        timer.cancel()
    }
}

