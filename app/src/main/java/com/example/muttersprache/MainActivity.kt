package com.example.muttersprache

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.Timer
import java.util.TimerTask
import kotlin.math.log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private lateinit var dbHelper: DatabaseHelper
    private var speed: Float = 0.7f
    private var repeatTime: Long = 60000
    private var repeatSentence: String = ""
    private var flagTimer: Boolean = false
    private var language: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализируем один раз
        dbHelper = DatabaseHelper(this)
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
        setupListViewAdapter(databasePath)

        textToSpeech = TextToSpeech(this, this)

        val editText: EditText = findViewById(R.id.editText)
        val translateText: EditText = findViewById(R.id.translateText)
        val addButton: Button = findViewById(R.id.addButton)
        addButton.setOnClickListener {
            val text = editText.text.toString()
            val translation = translateText.text.toString()
            if (text.isNotEmpty()) {
                dbHelper.addSentence(text, translation, 0, language)
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

        val manageLangsButton: Button = findViewById(R.id.manageLangsButton)
        manageLangsButton.setOnClickListener {
            val intent = Intent(this, LanguagesActivity::class.java)
            startActivity(intent)
        }

        val settingsButton: View = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupListViewAdapter(databasePath: String) {
        val adapter = object : BaseAdapter() {
            override fun getCount(): Int = sentences.size
            override fun getItem(position: Int): Quintuple<String, String, String, String, String> = sentences[position]
            override fun getItemId(position: Int): Long = sentences[position].first.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: LayoutInflater.from(this@MainActivity)
                    .inflate(R.layout.list_item_sentence, parent, false)

                val item = sentences[position]
                view.findViewById<TextView>(R.id.tvSentenceText).text = item.second
                view.findViewById<TextView>(R.id.tvSentenceTranslation).text = item.third
                
                val countTv = view.findViewById<TextView>(R.id.tvSentenceCount)
                countTv.text = "Повторений: ${item.fourth}"

                view.findViewById<ImageButton>(R.id.btnDeleteSentence).setOnClickListener {
                    val deleteID = item.first.toInt()
                    dbHelper.deleteSentence(deleteID)
                    showSentences(databasePath)
                }

                view.setOnClickListener {
                    repeatSentence = item.second
                    repeatSpeech()
                }

                return view
            }
        }
        listView.adapter = adapter
    }

    private fun loadSentencesFromDatabase(databasePath: String) {
        val database: SQLiteDatabase = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READWRITE)
        val cursor: Cursor = database.rawQuery("SELECT rowid, text, translation, count, language FROM sentences where language=? AND (isDeleted IS NULL OR isDeleted = 0)", arrayOf(language))
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
                    // Обертываем вызов в runOnUiThread
                    runOnUiThread {
                        speakRandomSentence()
                    }
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
            val translation = sentences[index].third
            val id = sentences[index].first
            val count = sentences[index].fourth

            repeatSentence = sentence
            textToSpeech.setSpeechRate(speed)
            textToSpeech.speak(sentence, TextToSpeech.QUEUE_FLUSH, null)

            StatsManager.incrementTodayCount(this)

            // Всё обновление UI строго внутри runOnUiThread
            runOnUiThread {
                val textView: TextView = findViewById(R.id.textViewSentences)
                textView.text = "$sentence\n$translation"

                // Запускаем обновление БД и списка во вторичном потоке, чтобы не вешать экран
                Thread {
                    dbHelper.updateSentence(id, count)
                    val path = applicationContext.getDatabasePath("new.db").path

                    // После обновления БД, возвращаемся в UI поток обновить список
                    runOnUiThread {
                        showSentences(path)
                    }
                }.start()
            }
        }
    }

    private fun loadSentencesList(databasePath: String): MutableList<Quintuple<String, String, String, String, String>> {
        val database: SQLiteDatabase = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READWRITE)
        val cursor: Cursor = database.rawQuery(
            "SELECT rowid, text, translation, count, language FROM sentences where language=? AND (isDeleted IS NULL OR isDeleted = 0) ORDER BY count DESC",
            arrayOf(language)
        )
        val list = mutableListOf<Quintuple<String, String, String, String, String>>()
        while (cursor.moveToNext()) {
            list.add(Quintuple(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)))
        }
        cursor.close()
        database.close()
        return list
    }
    private fun showSentences(databasePath: String) {
        // Запускаем тяжелую работу в отдельном потоке
        Thread {
            val list = loadSentencesList(databasePath)

            // Возвращаемся в главный поток, чтобы обновить UI
            runOnUiThread {
                sentences.clear()
                sentences.addAll(list)

                if (listView.adapter == null) {
                    setupListViewAdapter(databasePath)
                } else {
                    (listView.adapter as BaseAdapter).notifyDataSetChanged()
                }
            }
        }.start()
    }

    private fun showLanguageSelectionDialog() {
        val dbLanguages = dbHelper.getAllLanguages()
        if (dbLanguages.isEmpty()) {
            Toast.makeText(this, "No languages added. Go to Manage Languages.", Toast.LENGTH_SHORT).show()
            return
        }
        val names = dbLanguages.map { it.name }.toTypedArray()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Language")
            .setItems(names) { _, which ->
                val selected = dbLanguages[which]
                textToSpeech.language = Locale(selected.code)
                language = selected.code
                showSentences(applicationContext.getDatabasePath("new.db").path)
            }
        builder.create().show()
    }

    override fun onResume() {
        super.onResume()
        // Обновить список предложений при возврате из LanguagesActivity
        showSentences(applicationContext.getDatabasePath("new.db").path)
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
