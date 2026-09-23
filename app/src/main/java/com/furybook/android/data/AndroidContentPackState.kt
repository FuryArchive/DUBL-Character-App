package com.furybook.android.data

import android.content.Context
import com.furybook.content.FcpComposition
import com.furybook.content.FcpUiContribution
import com.furybook.dubl.content.DublChiFcp
import com.furybook.dubl.content.DublFcp
import com.furybook.dubl.content.DublChiUi

object AndroidContentPackState {
    private const val PREFS = "fury-content-packs"

    fun isChiEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(DublChiFcp.PACK_ID, false)

    fun setChiEnabled(context: Context, enabled: Boolean) {
        if (enabled) AndroidDublFcp.chiLoader(context).verifyContent()
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(DublChiFcp.PACK_ID, enabled)
            .apply()
    }

    fun composition(context: Context, chiEnabled: Boolean): FcpComposition = FcpComposition.resolve(
        manifests = listOf(
            AndroidDublFcp.loader(context).pack.manifest,
            AndroidDublFcp.chiLoader(context).pack.manifest,
        ),
        requiredPackIds = setOf(DublFcp.PACK_ID),
        enabledPackIds = if (chiEnabled) setOf(DublChiFcp.PACK_ID) else emptySet(),
    )

    fun chiUi(context: Context, enabled: Boolean, surface: String): FcpUiContribution? =
        composition(context, enabled).ui(surface, DublChiUi.BINDING).firstOrNull()

    fun chiDevelopmentIds(context: Context): Set<String> = linkedSetOf<String>().apply {
        addAll(AndroidDublFcp.chiLoader(context).loadDevelopment().entries.map { it.id })
        addAll(AndroidDublFcp.chiLoader(context).claimedDevelopmentIds())
    }
}
