package com.furybook.android.data

import android.content.Context
import com.furybook.content.FcpTextSource
import com.furybook.dubl.content.DublFcp
import com.furybook.dubl.content.DublFcpCatalogLoader

internal object AndroidDublFcp {
    @Volatile
    private var cached: DublFcpCatalogLoader? = null

    fun loader(context: Context): DublFcpCatalogLoader {
        cached?.let { return it }
        return synchronized(this) {
            cached?.let { return@synchronized it }
            val appContext = context.applicationContext
            DublFcp.open(
                FcpTextSource { path ->
                    runCatching {
                        appContext.assets.open(path)
                            .bufferedReader(Charsets.UTF_8)
                            .use { it.readText() }
                    }.getOrNull()
                },
            ).also { cached = it }
        }
    }
}
