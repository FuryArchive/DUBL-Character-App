package com.furybook.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FcpCompositionTest {
    private fun manifest(
        id: String,
        version: String = "1",
        dependencies: List<FcpDependency> = emptyList(),
        claims: List<FcpContentClaim> = emptyList(),
        ui: List<FcpUiContribution> = emptyList(),
        rulesetId: String = "demo",
    ) = FcpManifest(
        format = FcpFormat.ID,
        formatVersion = FcpFormat.VERSION,
        id = id,
        name = id,
        version = version,
        ruleset = FcpRulesetRef(rulesetId, "1", 1),
        dependencies = dependencies,
        modules = emptyList(),
        entries = emptyList(),
        claims = claims,
        ui = ui,
    )

    @Test
    fun enablingAddonActivatesDependencyAndContributesUi() {
        val core = manifest("core")
        val addon = manifest(
            id = "addon",
            dependencies = listOf(FcpDependency("core", "1")),
            claims = listOf(FcpContentClaim("demo.data", "mixed-entry")),
            ui = listOf(
                FcpUiContribution(
                    id = "addon.energy",
                    surface = "character.resources",
                    component = "resource-meter",
                    binding = "demo.energy",
                    label = "Energy",
                    order = 20,
                    properties = emptyMap(),
                ),
            ),
        )

        val disabled = FcpComposition.resolve(
            manifests = listOf(core, addon),
            requiredPackIds = setOf("core"),
            enabledPackIds = emptySet(),
        )
        assertTrue(disabled.isActive("core"))
        assertFalse(disabled.isActive("addon"))
        assertEquals(emptyList(), disabled.ui("character.resources"))
        assertEquals(setOf("mixed-entry"), disabled.inactiveClaims("demo.data"))

        val enabled = FcpComposition.resolve(
            manifests = listOf(core, addon),
            requiredPackIds = setOf("core"),
            enabledPackIds = setOf("addon"),
        )
        assertEquals(listOf("core", "addon"), enabled.active.map { it.id })
        assertEquals(setOf("mixed-entry"), enabled.activeClaims("demo.data"))
        assertEquals(emptySet(), enabled.inactiveClaims("demo.data"))
        assertEquals("Energy", enabled.ui("character.resources", "demo.energy").single().label)
    }

    @Test
    fun rejectsDependencyVersionMismatch() {
        val core = manifest("core", version = "2")
        val addon = manifest("addon", dependencies = listOf(FcpDependency("core", "1")))
        assertFailsWith<IllegalArgumentException> {
            FcpComposition.resolve(
                manifests = listOf(core, addon),
                requiredPackIds = setOf("core"),
                enabledPackIds = setOf("addon"),
            )
        }
    }

    @Test
    fun rejectsMixedRulesetsInOneActiveComposition() {
        val core = manifest("core")
        val alien = manifest("alien", rulesetId = "alien")
        assertFailsWith<IllegalArgumentException> {
            FcpComposition.resolve(
                manifests = listOf(core, alien),
                requiredPackIds = setOf("core"),
                enabledPackIds = setOf("alien"),
            )
        }
    }
}
