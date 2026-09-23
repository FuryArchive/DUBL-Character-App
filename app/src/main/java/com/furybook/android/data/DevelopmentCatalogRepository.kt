package com.furybook.android.data

import android.content.Context
import com.furybook.dubl.content.DublChiFcp
import com.furybook.dubl.content.DublDevelopmentAddon
import com.furybook.dubl.data.mergeDevelopmentCatalogs
import com.furybook.dubl.model.DevelopmentCatalog
import com.furybook.dubl.model.withoutEntries

class DevelopmentCatalogRepository(context: Context) {
    private val appContext = context.applicationContext

    fun load(includeChi: Boolean = false): DevelopmentCatalog {
        val composition = AndroidContentPackState.composition(appContext, includeChi)
        val cacheKey = composition.active.joinToString("|") { "${it.id}@${it.version}" }
        synchronized(lock) {
            cache[cacheKey]?.let { return it }
        }

        val chiClaims = if (composition.isActive(DublChiFcp.PACK_ID)) {
            emptySet()
        } else {
            AndroidDublFcp.chiLoader(appContext).claimedDevelopmentIds()
        }
        val layers = mutableListOf(
            AndroidDublFcp.loader(appContext).loadDevelopment().withoutEntries(chiClaims),
        )
        if (composition.isActive(DublChiFcp.PACK_ID)) {
            layers += AndroidDublFcp.chiLoader(appContext).loadDevelopment()
        }
        composition.active
            .asSequence()
            .filter { it.id !in AndroidContentPackState.bundledPackIds() }
            .forEach { manifest ->
                layers += DublDevelopmentAddon.load(AndroidFcpInstaller.openInstalled(appContext, manifest))
            }

        val loaded = mergeDevelopmentCatalogs(*layers.toTypedArray())
        synchronized(lock) {
            cache[cacheKey] = loaded
        }
        return loaded
    }

    companion object {
        private val lock = Any()
        private val cache = linkedMapOf<String, DevelopmentCatalog>()

        fun invalidate() {
            synchronized(lock) { cache.clear() }
        }
    }
}
