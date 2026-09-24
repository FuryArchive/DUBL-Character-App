package com.furybook.dubl.content

import com.furybook.content.FcpComposition
import com.furybook.content.FcpFormat
import com.furybook.content.FcpManifest
import com.furybook.content.FcpRulesetRef
import com.furybook.content.FcpUiComponent
import com.furybook.content.FcpUiContribution
import com.furybook.content.FcpUiSurface
import kotlin.test.Test
import kotlin.test.assertEquals

class DublUiRegistryTest {
    @Test
    fun mapsKnownBindingToTypedFeatureAndIgnoresUnknownBinding() {
        val core = manifest("core")
        val addon = manifest(
            id = "addon",
            ui = listOf(
                contribution("chi", DublUiBinding.CHI),
                contribution("unknown", "third.party.binding"),
            ),
        )
        val composition = FcpComposition.resolve(
            manifests = listOf(core, addon),
            requiredPackIds = setOf("core"),
            enabledPackIds = setOf("addon"),
        )

        val mounts = DublUiRegistry.mounts(
            composition = composition,
            surface = FcpUiSurface.CHARACTER_RESOURCES,
            component = FcpUiComponent.RESOURCE_METER,
        )

        assertEquals(1, mounts.size)
        assertEquals(DublUiFeature.CHI, mounts.single().feature)
        assertEquals("chi", mounts.single().contribution.id)
        assertEquals("chi", mounts.forFeature(DublUiFeature.CHI)?.id)
    }

    private fun manifest(
        id: String,
        ui: List<FcpUiContribution> = emptyList(),
    ) = FcpManifest(
        format = FcpFormat.ID,
        formatVersion = FcpFormat.VERSION,
        id = id,
        name = id,
        version = "1",
        ruleset = FcpRulesetRef("dubl", "3.69", 1),
        dependencies = emptyList(),
        modules = emptyList(),
        entries = emptyList(),
        claims = emptyList(),
        ui = ui,
    )

    private fun contribution(id: String, binding: String) = FcpUiContribution(
        id = id,
        surface = FcpUiSurface.CHARACTER_RESOURCES,
        component = FcpUiComponent.RESOURCE_METER,
        binding = binding,
        label = id,
        order = 40,
        properties = emptyMap(),
    )
}
