package com.novatube.app.extractor

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.novatube.app.data.model.MediaInfo

/**
 * Extracts media metadata (title, formats, thumbnail, etc.) from a public URL
 * by delegating to [YtDlpEngine] which calls the yt-dlp binary directly.
 */
class MediaExtractor(private val gson: Gson = Gson()) {

    private val engine = YtDlpEngine(gson)

    /**
     * Runs `yt-dlp -J --no-warnings --no-playlist <url>` and parses the resulting JSON
     * metadata blob. Throws on failure.
     */
    suspend fun extract(context: Context, url: String): MediaInfo {
        Log.i(TAG, "extract() $url")
        if (!engine.initialized) engine.init(context)
        return engine.extract(url)
    }

    companion object {
        private const val TAG = "MediaExtractor"
    }
}
