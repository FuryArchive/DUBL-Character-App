package com.furybook.android.data

import android.content.Context
import com.furybook.dubl.model.SkillEffectCatalog

class SkillEffectCatalogRepository(private val context: Context) {
    fun load(): SkillEffectCatalog =
        AndroidDublFcp.loader(context).loadSkillEffects()
}
