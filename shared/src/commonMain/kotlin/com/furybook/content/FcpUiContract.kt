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

fun FcpComposition.ui(
    surface: String,
    component: String,
): List<FcpUiContribution> = ui(surface).filter { it.matchesUi(surface, component) }

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
    val order: Int,
)

fun FcpUiContribution.presentation(): FcpUiPresentation = FcpUiPresentation(
    label = label,
    icon = properties[FcpUiProperty.ICON],
    accent = properties[FcpUiProperty.ACCENT],
    order = order,
)

object FcpUiHostOrder {
    const val PRIMARY = 10
    const val SECONDARY = 20
    const val TERTIARY = 30
    const val EXTENSION = 40
    const val TRAILING = 90
    const val CUSTOM = 100
}

data class FcpOrderedUiItem<T>(
    val order: Int,
    val stableKey: String,
    val value: T,
)

fun <T> orderedUiItems(items: Iterable<FcpOrderedUiItem<T>>): List<T> = items
    .sortedWith(compareBy<FcpOrderedUiItem<T>>({ it.order }, { it.stableKey }))
    .map { it.value }


data class FcpUiHostCapability(
    val surface: String,
    val component: String,
    val allowedProperties: Set<String>,
)

object FcpUiHostCapabilities {
    private val capabilities = listOf(
        FcpUiHostCapability(
            FcpUiSurface.CHARACTER_RESOURCES,
            FcpUiComponent.RESOURCE_METER,
            setOf(FcpUiProperty.ICON, FcpUiProperty.ACCENT),
        ),
        FcpUiHostCapability(
            FcpUiSurface.CHARACTER_RESOURCE_SETTINGS,
            FcpUiComponent.RESOURCE_TOGGLE,
            setOf(FcpUiProperty.ICON),
        ),
        FcpUiHostCapability(
            FcpUiSurface.DEVELOPMENT_TABS,
            FcpUiComponent.DEVELOPMENT_BROWSER,
            setOf(FcpUiProperty.ICON),
        ),
        FcpUiHostCapability(
            FcpUiSurface.CHARACTER_ECONOMY,
            FcpUiComponent.XP_LINE,
            emptySet(),
        ),
    ).associateBy { it.surface to it.component }

    private val iconTokens = setOf(FcpUiIconToken.CHI)
    private val accentTokens = setOf(FcpUiAccentToken.FURY_ACCENT)

    fun problems(manifest: FcpManifest): List<String> = manifest.ui.flatMap(::problems)

    fun problems(contribution: FcpUiContribution): List<String> {
        val capability = capabilities[contribution.surface to contribution.component]
            ?: return listOf(
                "Unsupported FCP UI surface/component for ${contribution.id}: " +
                    "${contribution.surface} / ${contribution.component}",
            )

        val problems = mutableListOf<String>()
        val unsupportedProperties = contribution.properties.keys - capability.allowedProperties
        if (unsupportedProperties.isNotEmpty()) {
            problems += "Unsupported FCP UI properties for ${contribution.id}: ${unsupportedProperties.sorted().joinToString()}"
        }

        contribution.properties[FcpUiProperty.ICON]?.let { token ->
            if (token !in iconTokens) {
                problems += "Unsupported FCP UI icon token for ${contribution.id}: $token"
            }
        }
        contribution.properties[FcpUiProperty.ACCENT]?.let { token ->
            if (token !in accentTokens) {
                problems += "Unsupported FCP UI accent token for ${contribution.id}: $token"
            }
        }
        return problems
    }

    fun requireSupported(manifest: FcpManifest) {
        val problems = problems(manifest)
        require(problems.isEmpty()) {
            "FCP UI is not supported by this Fury Book host: ${problems.joinToString("; ")}"
        }
    }
}
