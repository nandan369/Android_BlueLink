package com.example.bluelink.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object LogUtils {

    private fun getLogFile(context: Context, testCaseName: String): File? {
        return try {
            // Create app-specific external storage directory
            val logDir = File(context.getExternalFilesDir(null), "test_logs")
            if (!logDir.exists()) logDir.mkdirs()

            File(logDir, "${testCaseName}_log.txt")
        } catch (e: Exception) {
            Log.e("LogUtils", "Error creating log file: ${e.message}")
            null
        }
    }

    fun logToFile(context: Context, testCaseName: String, message: String) {
        val logFile = getLogFile(context, testCaseName)
        if (logFile == null) {
            Log.e("LogUtils", "Log file is null. Cannot write log.")
            return
        }

        try {
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val logEntry = "$timeStamp - $message\n"

            FileWriter(logFile, true).use { it.write(logEntry) }

            Log.d("LogUtils", "Wrote log to ${logFile.absolutePath}")
        } catch (e: IOException) {
            Log.e("LogUtils", "Error writing to log file: ${e.message}")
        }
    }
}
