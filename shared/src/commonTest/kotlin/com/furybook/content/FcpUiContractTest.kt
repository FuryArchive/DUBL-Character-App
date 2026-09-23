package com.furybook.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
}
