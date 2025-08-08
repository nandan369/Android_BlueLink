package com.example.bluelink.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object LogUtils {

    // Listener to update GUI (e.g., RecyclerView)
    var logListener: ((String) -> Unit)? = null

    // Tracks whether a test case's log file has already been initialized (cleared)
    private val initializedTestCases = mutableSetOf<String>()

    // Helper to generate log file for the test case
    private fun getLogFile(context: Context, testCaseName: String): File? {
        return try {
            val logDir = File(context.getExternalFilesDir(null), "test_logs")
            if (!logDir.exists()) logDir.mkdirs()
            File(logDir, "${testCaseName}_log.txt")
        } catch (e: Exception) {
            Log.e("LogUtils", "Error creating log file: ${e.message}")
            null
        }
    }

    // Main logging method: writes to file + updates GUI
    fun logToFile(context: Context, testCaseName: String, message: String) {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "$timeStamp - $message\n"

        try {
            val logFile = getLogFile(context, testCaseName)
            if (logFile == null) {
                Log.e("LogUtils", "Log file is null.")
                return
            }

            // Clear the file only once for each test case
            val append = if (initializedTestCases.contains(testCaseName)) true else false

            OutputStreamWriter(FileOutputStream(logFile, append), Charsets.UTF_8).use { writer ->
                writer.write(logEntry)
            }

            // Mark test case as initialized so future writes are appended
            initializedTestCases.add(testCaseName)

            Log.d("LogUtils", "Logged to file: ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("LogUtils", "Error writing to log file: ${e.message}", e)
        }

        // Notify GUI listener (e.g., RecyclerView adapter)
        logListener?.invoke(logEntry.trimEnd())
    }

    // Optional: Clear log for a specific test case manually (e.g., on test retry)
    fun clearLogForTestCase(context: Context, testCaseName: String) {
        try {
            val logFile = getLogFile(context, testCaseName)
            logFile?.writeText("")
            initializedTestCases.remove(testCaseName)
            Log.d("LogUtils", "Cleared log for $testCaseName")
        } catch (e: Exception) {
            Log.e("LogUtils", "Error clearing log for $testCaseName: ${e.message}")
        }
    }
}
