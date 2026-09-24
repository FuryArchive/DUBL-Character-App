package com.furybook.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FcpUiContractTest {
    @Test
    fun presentationReadsOnlySemanticManifestTokens() {
        val contribution = FcpUiContribution(
            id = "demo.resource",
            surface = FcpUiSurface.CHARACTER_RESOURCES,
            component = FcpUiComponent.RESOURCE_METER,
            binding = "demo.energy",
            label = "Energy",
            order = 10,
            properties = mapOf(
                FcpUiProperty.ICON to FcpUiIconToken.CHI,
                FcpUiProperty.ACCENT to FcpUiAccentToken.FURY_ACCENT,
                "ignored" to "#ff00ff",
            ),
        )

        val presentation = contribution.presentation()
        assertEquals("Energy", presentation.label)
        assertEquals(FcpUiIconToken.CHI, presentation.icon)
        assertEquals(FcpUiAccentToken.FURY_ACCENT, presentation.accent)
        assertEquals(10, presentation.order)
    }

    @Test
    fun presentationAllowsHostFallbackWhenTokensAreAbsent() {
        val contribution = FcpUiContribution(
            id = "demo.resource",
            surface = FcpUiSurface.CHARACTER_RESOURCES,
            component = FcpUiComponent.RESOURCE_METER,
            binding = "demo.energy",
            label = "Energy",
            order = 10,
            properties = emptyMap(),
        )

        val presentation = contribution.presentation()
        assertNull(presentation.icon)
        assertNull(presentation.accent)
    }

    @Test
    fun orderedItemsMergeHostSlotsAndPackOrderDeterministically() {
        val ordered = orderedUiItems(
            listOf(
                FcpOrderedUiItem(FcpUiHostOrder.TERTIARY, "mana", "mana"),
                FcpOrderedUiItem(15, "addon", "addon"),
                FcpOrderedUiItem(FcpUiHostOrder.PRIMARY, "health", "health"),
                FcpOrderedUiItem(FcpUiHostOrder.SECONDARY, "endurance", "endurance"),
                FcpOrderedUiItem(15, "addon-b", "addon-b"),
            ),
        )

        assertEquals(listOf("health", "addon", "addon-b", "endurance", "mana"), ordered)
    }
    @Test
    fun hostCapabilitiesAcceptSupportedRendererAndSemanticTokens() {
        val contribution = FcpUiContribution(
            id = "demo.resource",
            surface = FcpUiSurface.CHARACTER_RESOURCES,
            component = FcpUiComponent.RESOURCE_METER,
            binding = "demo.energy",
            label = "Energy",
            order = 10,
            properties = mapOf(
                FcpUiProperty.ICON to FcpUiIconToken.CHI,
                FcpUiProperty.ACCENT to FcpUiAccentToken.FURY_ACCENT,
            ),
        )

        assertTrue(FcpUiHostCapabilities.problems(contribution).isEmpty())
    }

    @Test
    fun hostCapabilitiesRejectUnknownRendererPropertiesAndTokens() {
        val unknownRenderer = FcpUiContribution(
            id = "unknown.renderer",
            surface = "character.unknown",
            component = "mystery-widget",
            binding = "demo",
            label = "Mystery",
            order = 10,
            properties = emptyMap(),
        )
        val unknownProperty = FcpUiContribution(
            id = "unknown.property",
            surface = FcpUiSurface.CHARACTER_RESOURCE_SETTINGS,
            component = FcpUiComponent.RESOURCE_TOGGLE,
            binding = "demo",
            label = "Toggle",
            order = 10,
            properties = mapOf(FcpUiProperty.ACCENT to FcpUiAccentToken.FURY_ACCENT),
        )
        val unknownToken = FcpUiContribution(
            id = "unknown.token",
            surface = FcpUiSurface.CHARACTER_RESOURCES,
            component = FcpUiComponent.RESOURCE_METER,
            binding = "demo",
            label = "Resource",
            order = 10,
            properties = mapOf(FcpUiProperty.ICON to "future-icon"),
        )

        assertTrue(FcpUiHostCapabilities.problems(unknownRenderer).single().contains("surface/component"))
        assertTrue(FcpUiHostCapabilities.problems(unknownProperty).single().contains("properties"))
        assertTrue(FcpUiHostCapabilities.problems(unknownToken).single().contains("icon token"))
    }

}
