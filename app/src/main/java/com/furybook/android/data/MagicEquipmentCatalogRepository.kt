package com.furybook.android.data

import android.content.Context
import com.furybook.dubl.model.MagicEquipmentCatalog

class MagicEquipmentCatalogRepository(private val context: Context) {
    fun load(): MagicEquipmentCatalog =
        AndroidDublFcp.loader(context).loadMagicEquipment()
}
