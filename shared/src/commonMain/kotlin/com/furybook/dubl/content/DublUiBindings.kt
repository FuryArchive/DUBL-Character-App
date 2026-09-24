package com.furybook.dubl.content

import com.furybook.content.FcpComposition
import com.furybook.content.FcpUiContribution
import com.furybook.content.matchesUi

object DublUiBinding {
    const val CHI = "dubl.chi"
}

enum class DublUiFeature {
    CHI,
}

data class DublUiMount(
    val feature: DublUiFeature,
    val contribution: FcpUiContribution,
)

object DublUiRegistry {
    fun mounts(
        composition: FcpComposition,
        surface: String,
        component: String,
    ): List<DublUiMount> = composition.ui(surface)
        .asSequence()
        .filter { contribution -> contribution.matchesUi(surface, component) }
        .mapNotNull { contribution ->
            val feature = when (contribution.binding) {
                DublUiBinding.CHI -> DublUiFeature.CHI
                else -> null
            }
            feature?.let { DublUiMount(it, contribution) }
        }
        .toList()
}


fun Iterable<DublUiMount>.forFeature(feature: DublUiFeature): FcpUiContribution? =
    firstOrNull { it.feature == feature }?.contribution
