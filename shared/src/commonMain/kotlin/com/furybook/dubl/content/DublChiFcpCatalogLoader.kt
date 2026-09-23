package com.furybook.dubl.content

import com.furybook.content.FcpContentPack
import com.furybook.content.FcpManifest
import com.furybook.content.FcpTextSource
import com.furybook.content.FcpUiContribution
import com.furybook.dubl.data.mergeDevelopmentCatalogs
import com.furybook.dubl.data.parseChiCatalog
import com.furybook.dubl.data.parseDevelopmentCatalog
import com.furybook.dubl.model.ChiCatalog
import com.furybook.dubl.model.DevelopmentCatalog
import com.furybook.dubl.model.DublRuleset

object DublChiFcp {
    const val BUNDLED_ROOT = "fcp/dubl-chi-3.69"
    const val PACK_ID = "dubl-chi-3.69"

    fun open(source: FcpTextSource): DublChiFcpCatalogLoader =
        DublChiFcpCatalogLoader(FcpContentPack.load(BUNDLED_ROOT, source))
}

object DublChiUi {
    const val BINDING = "dubl.chi"
    const val CHARACTER_RESOURCES = "character.resources"
    const val CHARACTER_RESOURCE_SETTINGS = "character.resource-settings"
    const val DEVELOPMENT_TABS = "development.tabs"
    const val CHARACTER_ECONOMY = "character.economy"

    fun contribution(manifest: FcpManifest, surface: String): FcpUiContribution? =
        manifest.ui(surface).firstOrNull { it.binding == BINDING }
}

class DublChiFcpCatalogLoader(
    val pack: FcpContentPack,
) {
    init {
        require(pack.manifest.id == DublChiFcp.PACK_ID) {
            "Unexpected bundled DUBL Chi FCP id: ${pack.manifest.id}"
        }
        require(pack.manifest.ruleset.id == DublRuleset.ID) {
            "FCP ruleset ${pack.manifest.ruleset.id} is not supported by the DUBL engine"
        }
        require(pack.manifest.ruleset.version == DublRuleset.VERSION) {
            "FCP ruleset version ${pack.manifest.ruleset.version} does not match DUBL ${DublRuleset.VERSION}"
        }
        require(pack.manifest.dependencies.any { it.id == DublFcp.PACK_ID && it.version == DublRuleset.VERSION }) {
            "DUBL Chi FCP must depend on ${DublFcp.PACK_ID} ${DublRuleset.VERSION}"
        }
    }

    fun claimedDevelopmentIds(): Set<String> =
        pack.manifest.claims("dubl.development").mapTo(linkedSetOf()) { it.id }

    fun loadDevelopment(): DevelopmentCatalog =
        mergeDevelopmentCatalogs(
            *pack.readAll("dubl.development")
                .map(::parseDevelopmentCatalog)
                .toTypedArray(),
        )

    fun loadChi(): ChiCatalog =
        parseChiCatalog(pack.readSingle("dubl.chi"))

    fun verifyContent() {
        loadDevelopment()
        loadChi()
        require(DublChiUi.contribution(pack.manifest, DublChiUi.CHARACTER_RESOURCES) != null)
        require(DublChiUi.contribution(pack.manifest, DublChiUi.DEVELOPMENT_TABS) != null)
    }
}
