package com.furybook.dubl.content

import com.furybook.content.FcpComposition
import com.furybook.content.FcpUiComponent
import com.furybook.content.FcpUiPresentation
import com.furybook.content.FcpUiSurface
import com.furybook.content.presentation
import com.furybook.dubl.model.CharacterSheetResourceId
import com.furybook.dubl.model.DublCharacter

data class DublResourceMeterModel(
    val resourceId: CharacterSheetResourceId,
    val current: Int,
    val maximum: Int,
    val available: Boolean,
    val presentation: FcpUiPresentation,
)

data class DublResourceToggleModel(
    val resourceId: CharacterSheetResourceId,
    val available: Boolean,
    val presentation: FcpUiPresentation,
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
}
