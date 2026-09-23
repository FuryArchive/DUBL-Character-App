package com.furybook.dubl.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OptionalContentProjectionTest {
    @Test
    fun disablingChiPackSuppressesMechanicsWithoutDeletingPersistedProgress() {
        val chiIds = setOf(
            DevelopmentEffectIds.INTERNAL_CHI,
            DevelopmentEffectIds.MASTER_CHI,
            DevelopmentEffectIds.STILL_MOUNTAIN_SCHOOL,
            DevelopmentEffectIds.STORM_LORD_SCHOOL,
        )
        val stored = DublCharacter(
            id = "chi-character",
            chiEnabled = false,
            chiCurrent = 3,
            chiBonusRanks = 2,
            development = chiIds.associateWith { OwnedDevelopment(rank = 1) },
        )

        assertTrue(stored.chiActive)
        assertEquals(1, stored.stormLordBonus)
        assertEquals(1, stored.stillMountainBonus)

        val runtime = stored.withoutRuntimeDevelopmentEffects(
            suppressedEntryIds = chiIds,
            suppressChiResource = true,
        )

        assertFalse(runtime.chiActive)
        assertEquals(0, runtime.stormLordBonus)
        assertEquals(0, runtime.stillMountainBonus)
        assertTrue(runtime.development.keys.intersect(chiIds).isEmpty())

        // The persisted character remains untouched and can recover all Chi state
        // immediately when its optional FCP is mounted again.
        assertEquals(chiIds, stored.development.keys)
        assertEquals(3, stored.chiCurrent)
        assertEquals(2, stored.chiBonusRanks)
        assertTrue(stored.chiAutomaticAccess)
    }
}
