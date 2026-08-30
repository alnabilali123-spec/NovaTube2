package com.novatube.app.extractor

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.novatube.app.data.model.MediaInfo
import com.novatube.app.data.model.SearchKind
import com.novatube.app.data.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Download backend that calls the yt-dlp **binary** directly (no JitPack dependency).
 *
 * Why not the [com.github.yausername.youtubedl-android] library?  Because the JitPack
 * build for the `library` artifact is unreliable (frequently ships an empty `classes.jar`).
 * Calling the binary ourselves is smaller, faster, and only needs the FFmpeg native
 * libraries which we bundle ourselves.
 *
 * The binary is fetched once on first use, stored in the app's private files dir, and
 * made executable.  Subsequent runs reuse the cached binary.
 */
class YtDlpEngine(
    private val gson: Gson = Gson(),
    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    @Volatile var initialized: Boolean = false
        private set
    @Volatile var binaryVersion: String? = null
        private set
    @Volatile var lastError: String? = null
        private set

    /** Resolves the path to the yt-dlp binary, downloading it on first use. */
    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val binFile = File(app.filesDir, "yt-dlp")
        if (!binFile.exists() || binFile.length() < 10_000L) {
            try {
                downloadBinary(binFile)
            } catch (t: Throwable) {
                lastError = "yt-dlp binary download failed: ${t.javaClass.simpleName}: ${t.message}"
                Log.e(TAG, lastError!!, t)
                initialized = false
                return@withContext
            }
        }
        if (!binFile.canExecute()) binFile.setExecutable(true)
        val version = runCatching { executeRaw(binFile, listOf("--version"), null) }.getOrNull()?.trim()
        if (version.isNullOrBlank()) {
            lastError = "yt-dlp binary present but not executable (downloaded ${binFile.length()} bytes)"
            Log.e(TAG, lastError!!)
            initialized = false
        } else {
            binaryVersion = version
            lastError = null
            initialized = true
            Log.i(TAG, "yt-dlp ready: $version (${binFile.length()} bytes)")
        }
    }

    private fun downloadBinary(target: File) {
        val url = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"
        Log.i(TAG, "Downloading yt-dlp binary from $url")
        val req = Request.Builder().url(url).build()
        okHttp.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $url")
            target.outputStream().use { out -> resp.body!!.byteStream().copyTo(out) }
        }
        target.setExecutable(true)
    }

    /**
     * Runs `yt-dlp -J --no-warnings --no-playlist <url>` and parses the JSON metadata.
     */
    suspend fun extract(url: String): MediaInfo = withContext(Dispatchers.IO) {
        ensureReady()
        val json = execute(listOf(url, "-J", "--no-warnings", "--no-playlist", "--no-color", "--no-progress"))
        try {
            gson.fromJson(json, MediaInfo::class.java) ?: throw MediaExtractionException("Empty response")
        } catch (e: Exception) {
            throw MediaExtractionException("Could not parse metadata: ${e.message}", e)
        }
    }

    /** Returns parsed search hits for a `ytsearchN:` / `scsearchN:` prefix. */
    suspend fun flatSearch(queryPrefix: String, platform: String, kind: SearchKind): List<SearchResult> =
        withContext(Dispatchers.IO) {
            ensureReady()
            val json = runCatching {
                execute(
                    listOf(
                        queryPrefix,
                        "--flat-playlist",
                        "--skip-download",
                        "-J",
                        "--no-warnings",
                        "--no-playlist"
                    )
                )
            }.getOrNull() ?: return@withContext emptyList()
            val root = gson.fromJson(json, JsonObject::class.java) ?: return@withContext emptyList()
            val entries = root.getAsJsonArray("entries") ?: return@withContext emptyList()
            entries.mapNotNull { el ->
                val obj = el.asJsonObject
                val id = obj.get("id")?.asString ?: return@mapNotNull null
                val title = obj.get("title")?.asString ?: return@mapNotNull null
                val url = obj.get("url")?.asString
                    ?: obj.get("webpage_url")?.asString
                    ?: "https://www.youtube.com/watch?v=$id"
                val uploader = obj.get("uploader")?.asString
                val duration = obj.get("duration")?.asLong
                val thumb = obj.get("thumbnails")?.asJsonArray
                    ?.lastOrNull()?.asJsonObject?.get("url")?.asString
                val viewCount = obj.get("view_count")?.asLong
                SearchResult(
                    id = id,
                    title = title,
                    uploader = uploader,
                    duration = duration,
                    thumbnail = thumb,
                    url = url,
                    kind = kind,
                    platform = platform,
                    viewCount = viewCount
                )
            }
        }

    suspend fun titleSuggestions(queryPrefix: String, limit: Int = 8): List<String> =
        withContext(Dispatchers.IO) {
            ensureReady()
            runCatching {
                execute(
                    listOf(
                        queryPrefix,
                        "--skip-download",
                        "--flat-playlist",
                        "--print", "%(title)s",
                        "--no-warnings",
                        "--no-playlist"
                    )
                ).split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(limit)
            }.getOrDefault(emptyList())
        }

    /**
     * Performs a download.  Returns the produced file (largest `targetPrefix*` in the
     * output directory) or `null` if no file was produced.
     */
    suspend fun download(
        url: String,
        formatId: String?,
        outputTemplate: String,
        audioOnly: Boolean,
        audioFormat: String = "mp3",
        progress: (line: String) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        ensureReady()
        val args = buildList {
            add(url)
            add("-o"); add(outputTemplate)
            add("--no-playlist")
            add("--no-mtime")
            add("--no-part")
            add("--newline")
            add("--no-color")
            add("--no-warnings")
            if (audioOnly) {
                add("-x")
                add("--audio-format"); add(audioFormat.ifBlank { "mp3" })
                add("--audio-quality"); add("0")
            } else if (!formatId.isNullOrBlank()) {
                add("-f"); add(formatId)
            }
        }
        val outDir = File(outputTemplate.substringBeforeLast('/', ""))
        runCatching { execute(args, onLine = progress) }
        // Find produced file
        val baseName = outputTemplate.substringAfterLast('/')
            .substringBefore("%(ext)s")
        outDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(baseName) && it.length() > 0 }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun ensureReady() {
        if (!initialized) {
            throw MediaExtractionException(
                lastError ?: "yt-dlp engine not initialized yet. Call init(context) first."
            )
        }
    }

    private fun execute(args: List<String>, onLine: ((String) -> Unit)? = null): String {
        val bin = File(currentBinaryPath())
        val output = StringBuilder()
        executeRaw(bin, args, onLine)?.let { output.append(it) }
        return output.toString()
    }

    private fun executeRaw(bin: File, args: List<String>, onLine: ((String) -> Unit)?): String? {
        val cmd = listOf(bin.absolutePath) + args
        val pb = ProcessBuilder(cmd).redirectErrorStream(true)
        val env = pb.environment()
        // Tell yt-dlp to behave in our constrained Android environment.
        env["LC_ALL"] = "C"
        env["PYTHONUNBUFFERED"] = "1"
        env["HOME"] = bin.parentFile?.absolutePath ?: "/data/local/tmp"
        val proc = pb.start()
        proc.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                onLine?.invoke(line)
                if (onLine == null) {
                    // Collect output for callers that want the body.
                } else {
                    // Caller handles progress; we don't accumulate output to keep memory bounded.
                }
            }
        }
        val exit = proc.waitFor()
        if (exit != 0 && onLine != null) {
            Log.w(TAG, "yt-dlp exited with code $exit for ${args.firstOrNull()}")
        }
        return if (onLine == null) proc.inputStream.bufferedReader().readText() else null
    }

    private fun currentBinaryPath(): String = "/data/data/com.novatube.app/files/yt-dlp"

    companion object {
        private const val TAG = "YtDlpEngine"
    }
}

class MediaExtractionException(message: String, cause: Throwable? = null) : Exception(message, cause)
