package com.furybook.android.data

import android.content.Context
import com.furybook.dubl.model.ConditionCatalog

class ConditionCatalogRepository(private val context: Context) {
    fun load(): ConditionCatalog =
        AndroidDublFcp.loader(context).loadConditions()
}
