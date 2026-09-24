package com.furybook.dubl.content

import com.furybook.content.FcpComposition
import com.furybook.content.FcpUiComponent
import com.furybook.content.FcpUiPresentation
import com.furybook.content.FcpUiSurface
import com.furybook.content.presentation
import com.furybook.dubl.model.CharacterEconomy
import com.furybook.dubl.model.CharacterEconomyBreakdown
import com.furybook.dubl.model.CharacterSheetResourceId
import com.furybook.dubl.model.DevelopmentCatalog
import com.furybook.dubl.model.DublCharacter

data class DublResourceMeterModel(
    val resourceId: CharacterSheetResourceId,
    val current: Int,
    val maximum: Int,
    val available: Boolean,
    val restoreable: Boolean,
    val presentation: FcpUiPresentation,
)

data class DublResourceToggleModel(
    val resourceId: CharacterSheetResourceId,
    val available: Boolean,
    val presentation: FcpUiPresentation,
)

data class DublXpLineModel(
    val xp: Int,
    val presentation: FcpUiPresentation,
)

data class DublEconomyRenderModel(
    val breakdown: CharacterEconomyBreakdown,
    val xpLines: List<DublXpLineModel>,
)

object DublUiRenderModels {
    fun resourceMeters(
        composition: FcpComposition,
        character: DublCharacter,
    ): List<DublResourceMeterModel> = DublUiRegistry.mounts(
        composition,
        FcpUiSurface.CHARACTER_RESOURCES,
        FcpUiComponent.RESOURCE_METER,
    ).mapNotNull { mount ->
        when (mount.feature) {
            DublUiFeature.CHI -> DublResourceMeterModel(
                resourceId = CharacterSheetResourceId.CHI,
                current = character.chiCurrent,
                maximum = character.chiMaximum,
                available = character.chiActive,
                restoreable = true,
                presentation = mount.contribution.presentation(),
            )
        }
    }

    fun resourceToggles(
        composition: FcpComposition,
        character: DublCharacter,
    ): List<DublResourceToggleModel> = DublUiRegistry.mounts(
        composition,
        FcpUiSurface.CHARACTER_RESOURCE_SETTINGS,
        FcpUiComponent.RESOURCE_TOGGLE,
    ).mapNotNull { mount ->
        when (mount.feature) {
            DublUiFeature.CHI -> DublResourceToggleModel(
                resourceId = CharacterSheetResourceId.CHI,
                available = character.chiActive,
                presentation = mount.contribution.presentation(),
            )
        }
    }
    fun economy(
        composition: FcpComposition,
        character: DublCharacter,
        catalog: DevelopmentCatalog,
    ): DublEconomyRenderModel {
        val mounts = DublUiRegistry.mounts(
            composition,
            FcpUiSurface.CHARACTER_ECONOMY,
            FcpUiComponent.XP_LINE,
        )
        val includeChi = mounts.any { it.feature == DublUiFeature.CHI }
        val breakdown = CharacterEconomy.breakdown(character, catalog, includeChi = includeChi)
        val lines = mounts.mapNotNull { mount ->
            when (mount.feature) {
                DublUiFeature.CHI -> DublXpLineModel(
                    xp = breakdown.chiXp,
                    presentation = mount.contribution.presentation(),
                )
            }
        }.sortedWith(compareBy({ it.presentation.order }, { it.presentation.label }))
        return DublEconomyRenderModel(breakdown, lines)
    }

}


fun DublCharacter.resourceCurrent(resourceId: CharacterSheetResourceId): Int = when (resourceId) {
    CharacterSheetResourceId.HEALTH -> hpCurrent
    CharacterSheetResourceId.ENDURANCE -> enduranceCurrent
    CharacterSheetResourceId.MANA -> manaCurrent
    CharacterSheetResourceId.CHI -> chiCurrent
}

fun DublCharacter.resourceMaximum(resourceId: CharacterSheetResourceId): Int = when (resourceId) {
    CharacterSheetResourceId.HEALTH -> healthMaximum
    CharacterSheetResourceId.ENDURANCE -> enduranceMaximum
    CharacterSheetResourceId.MANA -> effectiveManaMaximum
    CharacterSheetResourceId.CHI -> chiMaximum
}
