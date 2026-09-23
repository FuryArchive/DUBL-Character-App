package com.furybook.desktop.data

import com.furybook.content.FcpTextSource
import com.furybook.dubl.content.DublChiFcp
import com.furybook.dubl.content.DublChiFcpCatalogLoader
import com.furybook.dubl.content.DublFcp
import com.furybook.dubl.content.DublFcpCatalogLoader
import com.furybook.dubl.data.mergeDevelopmentCatalogs
import com.furybook.dubl.model.ChiCatalog
import com.furybook.dubl.model.ConditionCatalog
import com.furybook.dubl.model.DevelopmentCatalog
import com.furybook.dubl.model.MagicEquipmentCatalog
import com.furybook.dubl.model.SkillEffectCatalog
import java.io.InputStream

class DesktopCatalogLoader(
    private val openResource: (String) -> InputStream? = { name ->
        DesktopCatalogLoader::class.java.classLoader.getResourceAsStream(name)
    },
) {
    constructor(classLoader: ClassLoader) : this({ name -> classLoader.getResourceAsStream(name) })

    private val source = FcpTextSource { path ->
        openResource(path)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    }
    private val fcp: DublFcpCatalogLoader by lazy { DublFcp.open(source) }
    private val chiFcp: DublChiFcpCatalogLoader by lazy { DublChiFcp.open(source) }

    val manifest get() = fcp.pack.manifest
    val chiManifest get() = chiFcp.pack.manifest

    fun loadConditions(): ConditionCatalog = fcp.loadConditions()
    fun loadDevelopment(includeChi: Boolean = false): DevelopmentCatalog {
        val core = fcp.loadDevelopment()
        return if (includeChi) mergeDevelopmentCatalogs(core, chiFcp.loadDevelopment()) else core
    }
    fun loadChi(): ChiCatalog = chiFcp.loadChi()
    fun loadMagicEquipment(): MagicEquipmentCatalog = fcp.loadMagicEquipment()
    fun loadSkillEffects(): SkillEffectCatalog = fcp.loadSkillEffects()
    fun verifyBundledPack() = fcp.verifyContent()
    fun verifyChiPack() = chiFcp.verifyContent()
}
