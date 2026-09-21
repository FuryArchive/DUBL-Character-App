package com.furybook.dubl.data

import com.furybook.dubl.model.CharacterSheetExtras

interface CharacterExtrasStore {
    fun load(characterId: String): CharacterSheetExtras
    fun save(characterId: String, extras: CharacterSheetExtras)
    fun delete(characterId: String) = Unit
}
