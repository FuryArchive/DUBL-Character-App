package com.furybook.android.data

import android.content.Context
import com.furybook.dubl.model.ChiCatalog

class ChiCatalogRepository(context: Context) {
    private val appContext = context.applicationContext

    fun load(): ChiCatalog {
        cached?.let { return it }
        return synchronized(lock) {
            cached?.let { return@synchronized it }
            AndroidDublFcp.loader(appContext).loadChi().also { cached = it }
        }
    }

    private companion object {
        private val lock = Any()
        @Volatile
        private var cached: ChiCatalog? = null
    }
}
