package com.furybook.dubl.content

import com.furybook.content.FcpComposition
import com.furybook.content.FcpDependency
import com.furybook.content.FcpFormat
import com.furybook.content.FcpManifest
import com.furybook.content.FcpRulesetRef
import com.furybook.content.FcpUiAccentToken
import com.furybook.content.FcpUiComponent
import com.furybook.content.FcpUiContribution
import com.furybook.content.FcpUiIconToken
import com.furybook.content.FcpUiProperty
import com.furybook.content.FcpUiSurface
import com.furybook.dubl.model.CharacterSheetResourceId
import com.furybook.dubl.model.DublCharacter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DublUiRenderModelsTest {
    @Test
    fun resolvesChiContributionIntoResourceModels() {
        val composition = composition()
        val character = DublCharacter(id = "c", chiEnabled = true, chiCurrent = 2)

        val meter = DublUiRenderModels.resourceMeters(composition, character).single()
        assertEquals(CharacterSheetResourceId.CHI, meter.resourceId)
        assertEquals(2, meter.current)
        assertEquals(character.chiMaximum, meter.maximum)
        assertTrue(meter.available)
        assertTrue(meter.restoreable)
        assertEquals("Internal Energy", meter.presentation.label)
        assertEquals(FcpUiIconToken.CHI, meter.presentation.icon)
        assertEquals(FcpUiAccentToken.FURY_ACCENT, meter.presentation.accent)

        val toggle = DublUiRenderModels.resourceToggles(composition, character).single()
        assertEquals(CharacterSheetResourceId.CHI, toggle.resourceId)
        assertTrue(toggle.available)
        assertEquals("Internal Energy", toggle.presentation.label)
    }

    @Test
    fun keepsMountedResourceButMarksItUnavailableForCharacter() {
        val composition = composition()
        val character = DublCharacter(id = "c")

        assertFalse(DublUiRenderModels.resourceMeters(composition, character).single().available)
        assertFalse(DublUiRenderModels.resourceToggles(composition, character).single().available)
    }

    private fun composition(): FcpComposition {
        val core = manifest("dubl-3.69")
        val addon = manifest(
            id = "dubl-chi-3.69",
            dependencies = listOf(FcpDependency("dubl-3.69", "3.69")),
            ui = listOf(
                contribution(
                    id = "chi.resource",
                    surface = FcpUiSurface.CHARACTER_RESOURCES,
                    component = FcpUiComponent.RESOURCE_METER,
                ),
                contribution(
                    id = "chi.toggle",
                    surface = FcpUiSurface.CHARACTER_RESOURCE_SETTINGS,
                    component = FcpUiComponent.RESOURCE_TOGGLE,
                ),
            ),
        )
        return FcpComposition.resolve(
            manifests = listOf(core, addon),
            requiredPackIds = setOf("dubl-3.69"),
            enabledPackIds = setOf("dubl-chi-3.69"),
        )
    }

    private fun manifest(
        id: String,
        dependencies: List<FcpDependency> = emptyList(),
        ui: List<FcpUiContribution> = emptyList(),
    ) = FcpManifest(
        format = FcpFormat.ID,
        formatVersion = FcpFormat.VERSION,
        id = id,
        name = id,
        version = "3.69",
        ruleset = FcpRulesetRef("dubl", "3.69", 1),
        dependencies = dependencies,
        modules = emptyList(),
        entries = emptyList(),
        claims = emptyList(),
        ui = ui,
    )

    private fun contribution(
        id: String,
        surface: String,
        component: String,
    ) = FcpUiContribution(
        id = id,
        surface = surface,
        component = component,
        binding = DublUiBinding.CHI,
        label = "Internal Energy",
        order = 40,
        properties = mapOf(
            FcpUiProperty.ICON to FcpUiIconToken.CHI,
            FcpUiProperty.ACCENT to FcpUiAccentToken.FURY_ACCENT,
        ).filterKeys { key -> component == FcpUiComponent.RESOURCE_METER || key != FcpUiProperty.ACCENT },
    )
}
