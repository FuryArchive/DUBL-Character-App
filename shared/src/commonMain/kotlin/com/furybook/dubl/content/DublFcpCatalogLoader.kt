package com.furybook.dubl.content

import com.furybook.content.FcpContentPack
import com.furybook.content.FcpTextSource
import com.furybook.dubl.data.mergeDevelopmentCatalogs
import com.furybook.dubl.data.parseChiCatalog
import com.furybook.dubl.data.parseConditionCatalog
import com.furybook.dubl.data.parseDevelopmentCatalog
import com.furybook.dubl.data.parseMagicEquipmentCatalog
import com.furybook.dubl.data.parseSkillEffectCatalog
import com.furybook.dubl.model.ChiCatalog
import com.furybook.dubl.model.ConditionCatalog
import com.furybook.dubl.model.DevelopmentCatalog
import com.furybook.dubl.model.DublRuleset
import com.furybook.dubl.model.MagicEquipmentCatalog
import com.furybook.dubl.model.SkillEffectCatalog

object DublFcp {
    const val BUNDLED_ROOT = "fcp/dubl-3.69"
    const val PACK_ID = "dubl-3.69"

    fun open(source: FcpTextSource): DublFcpCatalogLoader =
        DublFcpCatalogLoader(FcpContentPack.load(BUNDLED_ROOT, source))
}

class DublFcpCatalogLoader(
    val pack: FcpContentPack,
) {
    init {
        require(pack.manifest.id == DublFcp.PACK_ID) {
            "Unexpected bundled DUBL FCP id: ${pack.manifest.id}"
        }
        require(pack.manifest.ruleset.id == DublRuleset.ID) {
            "FCP ruleset ${pack.manifest.ruleset.id} is not supported by the DUBL engine"
        }
        require(pack.manifest.ruleset.version == DublRuleset.VERSION) {
            "FCP ruleset version ${pack.manifest.ruleset.version} does not match DUBL ${DublRuleset.VERSION}"
        }
    }

    fun loadConditions(): ConditionCatalog =
        parseConditionCatalog(pack.readSingle("dubl.conditions"))

    fun loadDevelopment(): DevelopmentCatalog =
        mergeDevelopmentCatalogs(
            *pack.readAll("dubl.development")
                .map(::parseDevelopmentCatalog)
                .toTypedArray(),
        )

    fun loadChi(): ChiCatalog =
        parseChiCatalog(pack.readSingle("dubl.chi"))

    fun loadMagicEquipment(): MagicEquipmentCatalog =
        parseMagicEquipmentCatalog(pack.readSingle("dubl.magic-equipment"))

    fun loadSkillEffects(): SkillEffectCatalog =
        parseSkillEffectCatalog(pack.readSingle("dubl.skill-effects"))

    /**
     * Canonical declarative skill payload. The current DUBL engine still compiles
     * this into GeneratedSkillCatalog for fast typed access, but this JSON is the
     * pack-owned source representation.
     */
    fun loadSkillsPayload(): String =
        pack.readSingle("dubl.skills")
}
