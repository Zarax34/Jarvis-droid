package com.markxxxv.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Browser
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BrowserTools(private val context: Context) {
    
    fun openUrl(url: String): String {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Opened $url"
        } catch (e: Exception) {
            "Error opening url: ${e.message}"
        }
    }
    
    fun searchGoogle(query: String): String {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Searching for $query"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
    
    fun goBack(): String {
        return try {
            val intent = Intent(context, WebView::class.java).apply {
                putExtra("action", "go_back")
            }
            context.startActivity(intent)
            "Going back"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

class AppTools(private val context: Context) {
    
    fun openApp(packageName: String): String {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                "Opened app"
            } else {
                "App not found"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
    
    fun getInstalledApps(): List<String> {
        return context.packageManager.getInstalledApplications(0)
            .filter { it.packageName.startsWith("com.") || it.packageName.startsWith("org.") }
            .map { it.packageName }
            .take(50)
    }
    
    fun closeApp(packageName: String): String {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Force closing app"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

class FileTools(private val context: Context) {
    
    fun listFiles(path: String = "/storage/emulated/0"): List<String> {
        return try {
            val dir = java.io.File(path)
            if (dir.exists() && dir.isDirectory) {
                dir.list()?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            listOf("Error: ${e.message}")
        }
    }
    
    fun readFile(path: String): String {
        return try {
            val file = java.io.File(path)
            if (file.exists() && file.isFile) {
                file.readText().take(5000)
            } else {
                "File not found"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
    
    fun writeFile(path: String, content: String): String {
        return try {
            val file = java.io.File(path)
            file.writeText(content)
            "File saved"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

class SystemTools(private val context: Context) {
    
    fun setVolume(level: Int): String {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val volume = (level * maxVolume / 100)
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, volume, 0)
            "Volume set to $level%"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
    
    fun getVolume(): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val current = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        return "${current * 100 / max}%"
    }
    
    fun openSettings(): String {
        return try {
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Opening settings"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
    
    fun takeScreenshot(): String {
        return try {
            val process = Runtime.getRuntime().exec("screencap -p /sdcard/screenshot.png")
            process.waitFor()
            "Screenshot saved"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}