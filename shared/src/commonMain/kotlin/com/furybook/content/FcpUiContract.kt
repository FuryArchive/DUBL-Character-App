package com.furybook.content

object FcpUiSurface {
    const val CHARACTER_RESOURCES = "character.resources"
    const val CHARACTER_RESOURCE_SETTINGS = "character.resource-settings"
    const val DEVELOPMENT_TABS = "development.tabs"
    const val CHARACTER_ECONOMY = "character.economy"
}

object FcpUiComponent {
    const val RESOURCE_METER = "resource-meter"
    const val RESOURCE_TOGGLE = "resource-toggle"
    const val DEVELOPMENT_BROWSER = "development-browser"
    const val XP_LINE = "xp-line"
}

fun FcpUiContribution.matchesUi(
    surface: String,
    component: String,
    binding: String? = null,
): Boolean = this.surface == surface && this.component == component &&
    (binding == null || this.binding == binding)

fun FcpComposition.firstUi(
    surface: String,
    component: String,
    binding: String? = null,
): FcpUiContribution? = ui(surface).firstOrNull { it.matchesUi(surface, component, binding) }

fun FcpManifest.firstUi(
    surface: String,
    component: String,
    binding: String? = null,
): FcpUiContribution? = ui(surface).firstOrNull { it.matchesUi(surface, component, binding) }


object FcpUiProperty {
    const val ICON = "icon"
    const val ACCENT = "accent"
}

object FcpUiIconToken {
    const val CHI = "chi"
}

object FcpUiAccentToken {
    const val FURY_ACCENT = "fury.accent"
}

data class FcpUiPresentation(
    val label: String,
    val icon: String?,
    val accent: String?,
)

fun FcpUiContribution.presentation(): FcpUiPresentation = FcpUiPresentation(
    label = label,
    icon = properties[FcpUiProperty.ICON],
    accent = properties[FcpUiProperty.ACCENT],
)
