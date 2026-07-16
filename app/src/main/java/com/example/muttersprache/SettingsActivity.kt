package com.example.muttersprache

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.graphics.Color
import android.graphics.Typeface
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : ComponentActivity() {

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { importDatabase(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.btnExport).setOnClickListener {
            exportDatabase()
        }

        findViewById<Button>(R.id.btnImport).setOnClickListener {
            showImportConfirmation()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStats()
    }

    private fun updateStats() {
        val count = StatsManager.getTodayCount(this)
        findViewById<TextView>(R.id.tvDailyCount).text = count.toString()

        val container = findViewById<LinearLayout>(R.id.llLanguageStatsContainer)
        container.removeAllViews()

        val dbHelper = DatabaseHelper(this)
        val stats = dbHelper.getLanguageStats()
        for (stat in stats) {
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4.dpToPx(), 0, 4.dpToPx())
                }
            }

            val nameTv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = stat.first
                textSize = 15f
                setTextColor(Color.parseColor("#1A1C1E"))
            }

            val countTv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                text = stat.second.toString()
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#FF6200EE")) // purple_500 color
            }

            itemLayout.addView(nameTv)
            itemLayout.addView(countTv)
            container.addView(itemLayout)
        }
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    private fun showImportConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Import")
            .setMessage("This will replace all your current data. Are you sure you want to continue?")
            .setPositiveButton("Yes") { _, _ ->
                importLauncher.launch(arrayOf("*/*"))
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun importDatabase(uri: Uri) {
        try {
            val dbFile = getDatabasePath("new.db")
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Toast.makeText(this, "Database imported successfully. Please restart the app.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportDatabase() {
        try {
            val dbFile = getDatabasePath("new.db")
            if (!dbFile.exists()) {
                Toast.makeText(this, "Database not found", Toast.LENGTH_SHORT).show()
                return
            }

            // Copy to cache dir for sharing
            val exportDir = File(cacheDir, "export")
            exportDir.mkdirs()
            val exportFile = File(exportDir, "muttersprache_backup.db")
            dbFile.copyTo(exportFile, overwrite = true)

            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                exportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Export Database"))
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
