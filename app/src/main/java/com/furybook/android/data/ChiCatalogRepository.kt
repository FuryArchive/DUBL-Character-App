package com.furybook.android.data

import com.furybook.dubl.data.parseChiCatalog
import android.content.Context
import com.furybook.dubl.model.ChiCatalog

class ChiCatalogRepository(context: Context) {
    private val appContext = context.applicationContext

    fun load(): ChiCatalog {
        cached?.let { return it }
        return synchronized(lock) {
            cached?.let { return@synchronized it }
            loadUncached().also { cached = it }
        }
    }

    private fun loadUncached(): ChiCatalog {
        val raw = appContext.assets.open("chi_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return parseChiCatalog(raw)
    }

    private companion object {
        private val lock = Any()
        @Volatile
        private var cached: ChiCatalog? = null
    }
}
