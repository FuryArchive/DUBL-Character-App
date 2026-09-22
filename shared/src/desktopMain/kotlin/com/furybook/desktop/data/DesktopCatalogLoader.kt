package com.furybook.desktop.data

import com.furybook.content.FcpTextSource
import com.furybook.dubl.content.DublFcp
import com.furybook.dubl.content.DublFcpCatalogLoader
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

    private val fcp: DublFcpCatalogLoader by lazy {
        DublFcp.open(
            FcpTextSource { path ->
                openResource(path)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
            },
        )
    }

    fun loadConditions(): ConditionCatalog = fcp.loadConditions()
    fun loadDevelopment(): DevelopmentCatalog = fcp.loadDevelopment()
    fun loadChi(): ChiCatalog = fcp.loadChi()
    fun loadMagicEquipment(): MagicEquipmentCatalog = fcp.loadMagicEquipment()
    fun loadSkillEffects(): SkillEffectCatalog = fcp.loadSkillEffects()
}
