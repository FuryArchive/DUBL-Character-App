package com.furybook.android.data

import android.content.Context
import com.furybook.content.FcpComposition
import com.furybook.content.FcpUiContribution
import com.furybook.dubl.content.DublChiFcp
import com.furybook.dubl.content.DublFcp
import com.furybook.dubl.content.DublChiUi

object AndroidContentPackState {
    private const val PREFS = "fury-content-packs"

    fun isPackEnabled(context: Context, packId: String): Boolean = when (packId) {
        DublFcp.PACK_ID -> true
        DublChiFcp.PACK_ID -> context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(packId, false)
        else -> false
    }

    fun setPackEnabled(context: Context, packId: String, enabled: Boolean) {
        when (packId) {
            DublFcp.PACK_ID -> require(enabled) { "Required FCP ${DublFcp.PACK_ID} cannot be disabled" }
            DublChiFcp.PACK_ID -> if (enabled) AndroidDublFcp.chiLoader(context).verifyContent()
            else -> error("Unknown bundled FCP: $packId")
        }
        if (packId != DublFcp.PACK_ID) {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(packId, enabled)
                .apply()
        }
    }

    fun isChiEnabled(context: Context): Boolean = isPackEnabled(context, DublChiFcp.PACK_ID)

    fun setChiEnabled(context: Context, enabled: Boolean) =
        setPackEnabled(context, DublChiFcp.PACK_ID, enabled)

    fun composition(context: Context, chiEnabled: Boolean): FcpComposition = FcpComposition.resolve(
        manifests = buildList {
            add(AndroidDublFcp.loader(context).pack.manifest)
            add(AndroidDublFcp.chiLoader(context).pack.manifest)
            addAll(AndroidFcpInstaller.listInstalled(context))
        },
        requiredPackIds = setOf(DublFcp.PACK_ID),
        enabledPackIds = if (chiEnabled) setOf(DublChiFcp.PACK_ID) else emptySet(),
    )

    fun chiUi(context: Context, enabled: Boolean, surface: String): FcpUiContribution? =
        composition(context, enabled).ui(surface, DublChiUi.BINDING).firstOrNull()

    fun canActivatePack(packId: String): Boolean =
        packId == DublFcp.PACK_ID || packId == DublChiFcp.PACK_ID

    fun bundledPackIds(): Set<String> = setOf(DublFcp.PACK_ID, DublChiFcp.PACK_ID)

    fun chiDevelopmentIds(context: Context): Set<String> = linkedSetOf<String>().apply {
        addAll(AndroidDublFcp.chiLoader(context).loadDevelopment().entries.map { it.id })
        addAll(AndroidDublFcp.chiLoader(context).claimedDevelopmentIds())
    }
}
