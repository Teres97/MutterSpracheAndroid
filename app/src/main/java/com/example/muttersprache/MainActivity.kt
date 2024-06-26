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

data class Quintuple<T1, T2, T3, T4, T5>(val first: T1, val second: T2, val third: T3, val fourth: T4, val fifth: T5){
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
    private var repeatTime:Long = 60000
    private var repeatSentence: String = ""
    private var flagTimer: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dbHelper = DatabaseHelper(this)

        listView = findViewById(R.id.listView)
        // Инициализация базы данных и получение всех строк
        val DATABASE_NAME = "new.db"

        // Путь к файлу базы данных внутри внутреннего хранилища
        val databasePath = applicationContext.getDatabasePath(DATABASE_NAME).path
        dbHelper.addColumnIfNeeded()
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
        val cursor: Cursor = database.rawQuery("SELECT rowid, text, translation, count, language FROM sentences", null)
        sentences = mutableListOf()
        while (cursor.moveToNext()) {
            val value1: String = cursor.getString(0)
            val value2: String = cursor.getString(1)
            val value3: String = cursor.getString(2)
            val value4: String = cursor.getString(3)
            val value5: String = cursor.getString(4)

            sentences.add(Quintuple(value1, value2, value3, value4, value5))
        }
        cursor.close()
        database.close()

        val adapter = ArrayAdapter(this, R.layout.list_item_layout, R.id.textViewText, sentences)
        listView.adapter = adapter

        // Инициализация TTS
        textToSpeech = TextToSpeech(this, this)

        val editText: EditText = findViewById(R.id.editText)

        val tranlsateText: EditText = findViewById(R.id.translateText)

        val addButton: Button = findViewById(R.id.addButton)
        addButton.setOnClickListener {
            val text = editText.text.toString()
            val tranlsatetext = tranlsateText.text.toString()
            if (text.isNotEmpty()) {
                dbHelper.addSentence(text, tranlsatetext, 0, "ger") // вставка данных, 0 - начальное значение счетчика
                editText.text.clear()
                tranlsateText.text.clear()// очистка поля ввода
                showSentences()
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
            if(deleteIDInput.text.isNotEmpty()){
                val deleteID = deleteIDInput.text.toString().toInt()
                dbHelper.deleteSentence(deleteID)
                showSentences()
            }
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

    private fun repeatSpeech() {


        // Установка языка воспроизведения на немецкий
        textToSpeech.language = Locale.GERMAN
        textToSpeech.setSpeechRate(speed)
        // Воспроизведение выбранной строки
        textToSpeech.speak(repeatSentence, TextToSpeech.QUEUE_FLUSH, null)
    }

    private fun startSpeech() {
        if(flagTimer == false) {
            timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    speakRandomSentence()
                }
            }, 0, repeatTime)
            flagTimer = true// Воспроизводить каждые 60 секунд
        }
    }

    private fun stopSpeech() {
        if(flagTimer) {
            timer.cancel()
            flagTimer = false
        }
    }

    private fun speakRandomSentence(){
        if (sentences.isNotEmpty()) {
            val random = Random()
            val index = random.nextInt(sentences.size)
            val sentence = sentences[index].second
            repeatSentence = sentence
            print(repeatSentence)
            // Установка языка воспроизведения на немецкий
            textToSpeech.language = Locale.GERMAN
            textToSpeech.setSpeechRate(speed)
            // Воспроизведение выбранной строки
            textToSpeech.speak(sentence, TextToSpeech.QUEUE_FLUSH, null)
            val textView: TextView = findViewById(R.id.textViewSentences)
            runOnUiThread{
                textView.text = sentence
            }
            val dbHelper = DatabaseHelper(this)
            dbHelper.updateSentence(sentences[index].first, sentences[index].fourth)
            currentDeleteID = sentences[index].first
            deleteIDInput.setText(currentDeleteID)
            showSentences()
        }
    }

    private fun showSentences() {
        val DATABASE_NAME = "new.db"
        val databasePath = applicationContext.getDatabasePath(DATABASE_NAME).path
        val database: SQLiteDatabase = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READWRITE)
        val cursor: Cursor = database.rawQuery("SELECT rowid, text, translation, count, language FROM sentences ORDER BY count DESC", null)
        sentences = mutableListOf()
        while (cursor.moveToNext()) {
            val value1: String = cursor.getString(0)
            val value2: String = cursor.getString(1)
            val value3: String = cursor.getString(2)
            val value4: String = cursor.getString(3)
            val value5: String = cursor.getString(4)

            sentences.add(Quintuple(value1, value2, value3, value4, value5))
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

