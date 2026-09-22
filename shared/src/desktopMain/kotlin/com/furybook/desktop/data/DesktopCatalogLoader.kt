package com.furybook.desktop.data

import com.furybook.dubl.data.parseChiCatalog
import com.furybook.dubl.data.parseConditionCatalog
import com.furybook.dubl.data.parseDevelopmentCatalog
import com.furybook.dubl.data.parseMagicEquipmentCatalog
import com.furybook.dubl.data.mergeDevelopmentCatalogs
import com.furybook.dubl.data.parseSkillEffectCatalog
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
    fun loadConditions(): ConditionCatalog = parseConditionCatalog(read("conditions_catalog.json"))
    fun loadDevelopment(): DevelopmentCatalog = mergeDevelopmentCatalogs(
        parseDevelopmentCatalog(read("development_regular_catalog.json")),
        parseDevelopmentCatalog(read("development_special_catalog.json")),
        parseDevelopmentCatalog(read("development_ability_roots_catalog.json")),
        parseDevelopmentCatalog(read("development_martial_catalog.json")),
        parseDevelopmentCatalog(read("development_chi_catalog.json")),
        parseDevelopmentCatalog(read("development_magic_catalog.json")),
        parseDevelopmentCatalog(read("development_catalog.json")),
    )
    fun loadChi(): ChiCatalog = parseChiCatalog(read("chi_catalog.json"))
    fun loadMagicEquipment(): MagicEquipmentCatalog = parseMagicEquipmentCatalog(read("magic_equipment_catalog.json"))
    fun loadSkillEffects(): SkillEffectCatalog = parseSkillEffectCatalog(read("skill_effects_catalog.json"))

    private fun read(name: String): String = openResource(name)
        ?.bufferedReader(Charsets.UTF_8)
        ?.use { it.readText() }
        ?: error("Missing bundled catalog resource: $name")
}
