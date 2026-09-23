package com.furybook.dubl.content

import com.furybook.content.FcpContentClaim
import com.furybook.content.FcpDependency
import com.furybook.content.FcpEntry
import com.furybook.content.FcpManifest
import com.furybook.content.FcpModule
import com.furybook.content.FcpRulesetRef
import com.furybook.content.FcpUiContribution
import com.furybook.content.FcpFormat
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DublDevelopmentAddonTest {
    private fun manifest(
        entries: List<FcpEntry> = listOf(FcpEntry("dubl.development", "content/dev.json", listOf("core"), 10)),
        dependencies: List<FcpDependency> = listOf(FcpDependency(DublFcp.PACK_ID, "3.69")),
        claims: List<FcpContentClaim> = emptyList(),
        ui: List<FcpUiContribution> = emptyList(),
        ruleset: FcpRulesetRef = FcpRulesetRef("dubl", "3.69", 1),
    ) = FcpManifest(
        format = FcpFormat.ID,
        formatVersion = FcpFormat.VERSION,
        id = "community-feats",
        name = "Community Feats",
        version = "1.0",
        ruleset = ruleset,
        dependencies = dependencies,
        modules = listOf(FcpModule("core", "Core", true, true)),
        entries = entries,
        claims = claims,
        ui = ui,
    )

    @Test
    fun acceptsStrictAdditiveDublDevelopmentPack() {
        assertTrue(DublDevelopmentAddon.isSupported(manifest()))
    }

    @Test
    fun rejectsOtherKindsClaimsUiAndWrongRuleset() {
        assertFalse(DublDevelopmentAddon.isSupported(manifest(
            entries = listOf(FcpEntry("dubl.chi", "content/chi.json", listOf("core"), 10)),
        )))
        assertFalse(DublDevelopmentAddon.isSupported(manifest(
            claims = listOf(FcpContentClaim("dubl.development", "x")),
        )))
        assertFalse(DublDevelopmentAddon.isSupported(manifest(
            ui = listOf(FcpUiContribution("x", "development.tabs", "x", "x", "x", 1, emptyMap())),
        )))
        assertFalse(DublDevelopmentAddon.isSupported(manifest(
            ruleset = FcpRulesetRef("other", "1", 1),
        )))
    }
}
