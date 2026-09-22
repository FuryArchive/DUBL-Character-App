package com.furybook.android.data

import android.content.Context
import com.furybook.dubl.model.DevelopmentCatalog

class DevelopmentCatalogRepository(context: Context) {
    private val appContext = context.applicationContext

    fun load(): DevelopmentCatalog {
        cached?.let { return it }
        return synchronized(lock) {
            cached?.let { return@synchronized it }
            AndroidDublFcp.loader(appContext).loadDevelopment().also { cached = it }
        }
    }

    private companion object {
        private val lock = Any()
        @Volatile
        private var cached: DevelopmentCatalog? = null
    }
}
