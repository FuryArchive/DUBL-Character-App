package com.furybook.android.data

import android.content.Context
import com.furybook.content.FcpTextSource
import com.furybook.dubl.content.DublChiFcp
import com.furybook.dubl.content.DublChiFcpCatalogLoader
import com.furybook.dubl.content.DublFcp
import com.furybook.dubl.content.DublFcpCatalogLoader

internal object AndroidDublFcp {
    @Volatile
    private var cached: DublFcpCatalogLoader? = null

    @Volatile
    private var cachedChi: DublChiFcpCatalogLoader? = null

    private fun source(context: Context): FcpTextSource {
        val appContext = context.applicationContext
        return FcpTextSource { path ->
            runCatching {
                appContext.assets.open(path)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            }.getOrNull()
        }
    }

    fun loader(context: Context): DublFcpCatalogLoader {
        cached?.let { return it }
        return synchronized(this) {
            cached?.let { return@synchronized it }
            DublFcp.open(source(context)).also { cached = it }
        }
    }

    fun chiLoader(context: Context): DublChiFcpCatalogLoader {
        cachedChi?.let { return it }
        return synchronized(this) {
            cachedChi?.let { return@synchronized it }
            DublChiFcp.open(source(context)).also { cachedChi = it }
        }
    }
}
