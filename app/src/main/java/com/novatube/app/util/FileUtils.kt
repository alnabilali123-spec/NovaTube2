package com.novatube.app.util

import android.content.Context
import android.os.Environment
import java.io.File

object FileUtils {

    fun downloadDir(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: context.filesDir
        val dir = File(base, "NovaTube")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun audioDir(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: context.filesDir
        val dir = File(base, "NovaTube")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun sanitizeFileName(name: String): String {
        val trimmed = name.trim()
        val cleaned = buildString {
            trimmed.forEach { ch ->
                when {
                    ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == ' ' || ch == '.' -> append(ch)
                    else -> append('_')
                }
            }
        }.take(120).ifBlank { "media" }
        return cleaned
    }

    fun humanReadableSize(bytes: Long): String {
        if (bytes <= 0) return "—"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var v = bytes.toDouble()
        var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        return String.format("%.1f %s", v, units[i])
    }

    fun listDownloads(context: Context, audioOnly: Boolean? = null): List<File> {
        val out = mutableListOf<File>()
        for (dir in listOf(audioDir(context), downloadDir(context))) {
            dir.listFiles()?.forEach { f ->
                if (!f.isFile) return@forEach
                val isAudio = f.extension.lowercase() in setOf("mp3", "m4a", "aac", "opus", "wav", "flac", "ogg")
                when (audioOnly) {
                    true -> if (isAudio) out += f
                    false -> if (!isAudio) out += f
                    null -> out += f
                }
            }
        }
        return out.sortedByDescending { it.lastModified() }
    }
}
