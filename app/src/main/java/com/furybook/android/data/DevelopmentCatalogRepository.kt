package com.furybook.android.data

import android.content.Context
import com.furybook.dubl.data.mergeDevelopmentCatalogs
import com.furybook.dubl.model.DevelopmentCatalog
import com.furybook.dubl.model.withoutEntries

class DevelopmentCatalogRepository(context: Context) {
    private val appContext = context.applicationContext

    fun load(includeChi: Boolean = false): DevelopmentCatalog {
        val cachedValue = if (includeChi) cachedWithChi else cachedCore
        cachedValue?.let { return it }
        return synchronized(lock) {
            val synchronizedCached = if (includeChi) cachedWithChi else cachedCore
            synchronizedCached?.let { return@synchronized it }
            val composition = AndroidContentPackState.composition(appContext, includeChi)
            val core = AndroidDublFcp.loader(appContext)
                .loadDevelopment()
                .withoutEntries(composition.inactiveClaims("dubl.development"))
            val loaded = if (includeChi) {
                mergeDevelopmentCatalogs(core, AndroidDublFcp.chiLoader(appContext).loadDevelopment())
            } else core
            if (includeChi) cachedWithChi = loaded else cachedCore = loaded
            loaded
        }
    }

    private companion object {
        private val lock = Any()
        @Volatile private var cachedCore: DevelopmentCatalog? = null
        @Volatile private var cachedWithChi: DevelopmentCatalog? = null
    }
}
