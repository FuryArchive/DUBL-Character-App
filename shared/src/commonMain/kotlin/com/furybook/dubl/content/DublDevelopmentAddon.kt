package com.furybook.dubl.content

import com.furybook.content.FcpContentPack
import com.furybook.content.FcpDependency
import com.furybook.content.FcpManifest
import com.furybook.dubl.data.mergeDevelopmentCatalogs
import com.furybook.dubl.data.parseDevelopmentCatalog
import com.furybook.dubl.model.DevelopmentCatalog
import com.furybook.dubl.model.DublRuleset

object DublDevelopmentAddon {
    const val ENTRY_KIND = "dubl.development"
    const val ENGINE_API = 1

    fun supportProblem(manifest: FcpManifest): String? {
        if (manifest.id == DublFcp.PACK_ID || manifest.id == DublChiFcp.PACK_ID) return "bundled pack"
        if (manifest.ruleset.id != DublRuleset.ID || manifest.ruleset.version != DublRuleset.VERSION) {
            return "requires DUBL ${DublRuleset.VERSION}"
        }
        if (manifest.ruleset.engineApi != ENGINE_API) return "unsupported engine API ${manifest.ruleset.engineApi}"
        if (manifest.dependencies != listOf(FcpDependency(DublFcp.PACK_ID, DublRuleset.VERSION))) {
            return "external development packs must depend only on ${DublFcp.PACK_ID} ${DublRuleset.VERSION}"
        }
        if (manifest.entries.isEmpty() || manifest.entries.any { it.kind != ENTRY_KIND }) {
            return "only $ENTRY_KIND entries are supported"
        }
        if (manifest.claims.isNotEmpty()) return "content claims are not supported for external packs yet"
        if (manifest.ui.isNotEmpty()) return "external UI contributions are not supported yet"
        return null
    }

    fun isSupported(manifest: FcpManifest): Boolean = supportProblem(manifest) == null

    fun requireSupported(manifest: FcpManifest) {
        val problem = supportProblem(manifest)
        require(problem == null) { "Unsupported DUBL data-only FCP ${manifest.id}: $problem" }
    }

    fun load(pack: FcpContentPack): DevelopmentCatalog {
        requireSupported(pack.manifest)
        val catalog = mergeDevelopmentCatalogs(
            *pack.readAll(ENTRY_KIND)
                .map(::parseDevelopmentCatalog)
                .toTypedArray(),
        )
        require(catalog.entries.none { it.isChiDevelopment }) {
            "External core-only DUBL development pack ${pack.manifest.id} cannot add Chi development"
        }
        return catalog
    }
}
