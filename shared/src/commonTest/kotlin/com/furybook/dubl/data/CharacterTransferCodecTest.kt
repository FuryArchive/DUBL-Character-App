package com.furybook.dubl.data

import com.furybook.dubl.model.AttributeId
import com.furybook.dubl.model.CharacterConditionId
import com.furybook.dubl.model.CharacterSheetExtras
import com.furybook.dubl.model.CharacterSheetResourceId
import com.furybook.dubl.model.ConditionLocalOverride
import com.furybook.dubl.model.CustomCondition
import com.furybook.dubl.model.CustomResource
import com.furybook.dubl.model.DublCharacter
import com.furybook.dubl.model.RulesetRef
import com.furybook.dubl.model.SheetGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CharacterTransferCodecTest {
    @Test
    fun roundTripPreservesCharacterAndPortableExtrasButDropsPortraitReference() {
        val character = DublCharacter(
            id = "android-character",
            name = "Радана",
            concept = "Странствующий кузнец",
            experience = 3_500,
            customResources = listOf(CustomResource("resource-1", "Ци", current = 2, maximum = 4)),
        )
        val extras = CharacterSheetExtras(
            portraitUri = "content://android.provider/portrait/42",
            activeConditions = setOf(CharacterConditionId.TIRED),
            hiddenResourceIds = setOf(CharacterSheetResourceId.MANA),
            preferredSkillAttributes = mapOf("smithing" to AttributeId.INTELLIGENCE),
            skillGroups = listOf(SheetGroup("craft", "Ремесло", listOf("smithing"))),
            developmentGroups = listOf(SheetGroup("martial", "Боевые", listOf("parry"))),
            conditionOverrides = mapOf(CharacterConditionId.TIRED to ConditionLocalOverride(title = "Измотана", description = "После дороги")),
            customConditions = listOf(CustomCondition("custom-road", "В дороге", "Настороже", active = true)),
            notes = "Не забыть купить уголь",
        )

        val raw = CharacterTransferCodec.encode(character, extras)
        val decoded = assertIs<CharacterTransferDecodeResult.Success>(
            CharacterTransferCodec.decode(raw) { "fallback-id" },
        ).payload

        assertEquals(character, decoded.character)
        assertEquals(extras.copy(portraitUri = null), decoded.extras)
        assertFalse(raw.contains("content://android.provider/portrait/42"))
        assertTrue(raw.contains("\"format\":\"dubl.character\""))
        assertTrue(raw.contains("\"version\":1"))
    }

    @Test
    fun unsupportedTransferVersionIsRejected() {
        val raw = CharacterTransferCodec.encode(DublCharacter("source"), CharacterSheetExtras())
            .replace("\"version\":1", "\"version\":999")

        val result = assertIs<CharacterTransferDecodeResult.Failure>(
            CharacterTransferCodec.decode(raw) { "fallback-id" },
        )

        assertEquals(CharacterTransferRejectReason.UNSUPPORTED_FORMAT_VERSION, result.reason)
    }

    @Test
    fun futureEmbeddedSnapshotSchemaIsRejected() {
        val raw = CharacterTransferCodec.encode(DublCharacter("source"), CharacterSheetExtras())
            .replace("\"schema\":${SnapshotCodec.SCHEMA}", "\"schema\":${SnapshotCodec.SCHEMA + 1}")

        val result = assertIs<CharacterTransferDecodeResult.Failure>(
            CharacterTransferCodec.decode(raw) { "fallback-id" },
        )

        assertEquals(CharacterTransferRejectReason.UNSUPPORTED_FORMAT_VERSION, result.reason)
    }

    @Test
    fun foreignRulesetIsRejected() {
        val raw = CharacterTransferCodec.encode(
            DublCharacter("source", ruleset = RulesetRef("lancer", "1")),
            CharacterSheetExtras(),
        )

        val result = assertIs<CharacterTransferDecodeResult.Failure>(
            CharacterTransferCodec.decode(raw) { "fallback-id" },
        )

        assertEquals(CharacterTransferRejectReason.UNSUPPORTED_RULESET, result.reason)
    }

    @Test
    fun malformedDocumentIsRejected() {
        val result = assertIs<CharacterTransferDecodeResult.Failure>(
            CharacterTransferCodec.decode("{definitely-not-json") { "fallback-id" },
        )

        assertEquals(CharacterTransferRejectReason.INVALID_FILE, result.reason)
    }
}
