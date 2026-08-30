package com.novatube.app.util

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object OkHttpProvider {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .build()
    }
}
