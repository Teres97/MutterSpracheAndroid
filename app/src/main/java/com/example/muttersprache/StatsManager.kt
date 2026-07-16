package com.example.muttersprache

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatsManager {
    private const val PREFS_NAME = "MutterSprachePrefs"
    private const val KEY_LAST_SPEECH_DATE = "last_speech_date"
    private const val KEY_TODAY_SPEECH_COUNT = "today_speech_count"

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun incrementTodayCount(context: Context): Int {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = getTodayDateString()
        val savedDate = sharedPrefs.getString(KEY_LAST_SPEECH_DATE, "")
        var count = sharedPrefs.getInt(KEY_TODAY_SPEECH_COUNT, 0)

        if (savedDate == todayStr) {
            count++
        } else {
            count = 1
        }

        sharedPrefs.edit()
            .putString(KEY_LAST_SPEECH_DATE, todayStr)
            .putInt(KEY_TODAY_SPEECH_COUNT, count)
            .apply()

        return count
    }

    fun getTodayCount(context: Context): Int {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = getTodayDateString()
        val savedDate = sharedPrefs.getString(KEY_LAST_SPEECH_DATE, "")
        return if (savedDate == todayStr) {
            sharedPrefs.getInt(KEY_TODAY_SPEECH_COUNT, 0)
        } else {
            0
        }
    }
}
