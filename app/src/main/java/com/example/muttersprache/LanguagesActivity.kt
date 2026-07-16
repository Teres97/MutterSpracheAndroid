package com.example.muttersprache

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity

class LanguagesActivity : ComponentActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var listView: ListView
    private lateinit var languages: MutableList<Language>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_languages)

        dbHelper = DatabaseHelper(this)
        listView = findViewById(R.id.languagesListView)

        loadLanguages()

        val nameInput: EditText = findViewById(R.id.languageNameInput)
        val codeInput: EditText = findViewById(R.id.languageCodeInput)
        val addButton: Button = findViewById(R.id.addLanguageButton)

        addButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val code = codeInput.text.toString().trim().lowercase()

            if (name.isEmpty() || code.isEmpty()) {
                Toast.makeText(this, "Fill in both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dbHelper.addLanguage(name, code)
            nameInput.text.clear()
            codeInput.text.clear()
            loadLanguages()
        }
    }

    private fun loadLanguages() {
        languages = dbHelper.getAllLanguages().toMutableList()

        val adapter = object : BaseAdapter() {
            override fun getCount(): Int = languages.size
            override fun getItem(position: Int): Language = languages[position]
            override fun getItemId(position: Int): Long = languages[position].id

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: LayoutInflater.from(this@LanguagesActivity)
                    .inflate(R.layout.list_item_language, parent, false)

                val lang = languages[position]
                view.findViewById<TextView>(R.id.languageNameText).text = lang.name
                view.findViewById<TextView>(R.id.languageCodeText).text = lang.code

                view.findViewById<Button>(R.id.deleteLanguageButton).setOnClickListener {
                    dbHelper.deleteLanguage(lang.id)
                    loadLanguages()
                }

                return view
            }
        }
        listView.adapter = adapter
    }
}
