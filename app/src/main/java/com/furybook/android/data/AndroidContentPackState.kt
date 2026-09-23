package com.furybook.android.data

import android.content.Context
import com.furybook.content.FcpUiContribution
import com.furybook.dubl.content.DublChiFcp
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

    fun chiUi(context: Context, enabled: Boolean, surface: String): FcpUiContribution? =
        if (enabled) DublChiUi.contribution(AndroidDublFcp.chiLoader(context).pack.manifest, surface) else null

    fun chiDevelopmentIds(context: Context): Set<String> =
        AndroidDublFcp.chiLoader(context).loadDevelopment().entries.mapTo(linkedSetOf()) { it.id }
}
