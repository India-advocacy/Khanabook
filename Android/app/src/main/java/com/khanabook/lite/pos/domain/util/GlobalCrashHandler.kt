package com.khanabook.lite.pos.domain.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.khanabook.lite.pos.ui.MainActivity
import kotlin.system.exitProcess

class GlobalCrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

  private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

  override fun uncaughtException(thread: Thread, throwable: Throwable) {
    // 1. Log the crash details
    val stackTrace = Log.getStackTraceString(throwable)
    Log.e("KhanaBookCrash", "CRITICAL ERROR: Uncaught exception in thread ${thread.name}")
    Log.e("KhanaBookCrash", stackTrace)

    // 2. Save crash info for next launch (Optional - for debugging)
    try {
        saveCrashLog(stackTrace)
    } catch (e: Exception) {
        Log.e("KhanaBookCrash", "Secondary error in saveCrashLog: ${e.message}")
    }

    // 3. Crash Loop Detection (CRIT-3)
    try {
        val prefs = context.getSharedPreferences("crash_reports", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastCrashTime = prefs.getLong("last_crash_time", 0L)
        val crashCount = prefs.getInt("crash_count", 0)

        val newCrashCount = if (now - lastCrashTime < 10000) {
            crashCount + 1
        } else {
            1
        }

        prefs.edit().apply {
            putLong("last_crash_time", now)
            putInt("crash_count", newCrashCount)
            apply()
        }

        if (newCrashCount >= 3) {
            Log.e("KhanaBookCrash", "Crash loop detected. Falling back to default system handler.")
            // Reset count so user can try again later
            prefs.edit().putInt("crash_count", 0).apply()
            defaultHandler?.uncaughtException(thread, throwable)
            return
        }
    } catch (e: Exception) {
        Log.e("KhanaBookCrash", "Secondary error in crash loop detection: ${e.message}")
        // If loop detection fails, we err on the side of caution and let the system handle it
        defaultHandler?.uncaughtException(thread, throwable)
        return
    }

    // 4. Prevent the system "App has stopped" dialog if possible, or just restart
    // Filter out errors that shouldn't auto-restart (OOM, StackOverflow)
    if (throwable is OutOfMemoryError || throwable is StackOverflowError) {
        defaultHandler?.uncaughtException(thread, throwable)
        return
    }

    try {
      restartApp()
    } catch (e: Exception) {
      // If restart fails, fall back to default handler
      defaultHandler?.uncaughtException(thread, throwable)
    }
  }

  private fun saveCrashLog(stackTrace: String) {
    try {
      val prefs = context.getSharedPreferences("crash_reports", Context.MODE_PRIVATE)
      prefs.edit().apply {
        putString("last_crash_log", stackTrace)
        putLong("last_crash_time", System.currentTimeMillis())
        apply()
      }
    } catch (e: Exception) {
      Log.e("KhanaBookCrash", "Failed to save crash log", e)
    }
  }

  private fun restartApp() {
    val intent =
            Intent(context, MainActivity::class.java).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
    context.startActivity(intent)

    // Kill current process
    android.os.Process.killProcess(android.os.Process.myPid())
    exitProcess(10)
  }

  companion object {
    fun initialize(context: Context) {
      val handler = GlobalCrashHandler(context)
      Thread.setDefaultUncaughtExceptionHandler(handler)
    }
  }
}


