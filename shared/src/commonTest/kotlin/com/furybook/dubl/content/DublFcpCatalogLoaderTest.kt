package com.furybook.dubl.content

import com.furybook.content.FcpContentPack
import com.furybook.content.FcpTextSource
import kotlin.test.Test
import kotlin.test.assertEquals

class DublFcpCatalogLoaderTest {
    @Test
    fun loadsTypedDublCatalogsFromManifestEntries() {
        val root = "packs/dubl"
        val manifest = """
            {
              "format": "fury.content-pack",
              "formatVersion": 1,
              "id": "dubl-3.69",
              "name": "DUBL 3.69",
              "version": "3.69",
              "ruleset": {"id": "dubl", "version": "3.69", "engineApi": 1},
              "modules": [
                {"id": "core", "name": "Core", "required": true, "enabledByDefault": true},
                {"id": "melee", "name": "Melee", "required": false, "enabledByDefault": true}
              ],
              "entries": [
                {"kind": "dubl.skills", "path": "content/skills.json", "modules": ["core"], "order": 10},
                {"kind": "dubl.conditions", "path": "content/conditions.json", "modules": ["core"], "order": 20},
                {"kind": "dubl.development", "path": "content/dev-a.json", "modules": ["core"], "order": 100},
                {"kind": "dubl.development", "path": "content/dev-b.json", "modules": ["melee"], "order": 110},
                {"kind": "dubl.chi", "path": "content/chi.json", "modules": ["melee"], "order": 200},
                {"kind": "dubl.magic-equipment", "path": "content/magic.json", "modules": ["core"], "order": 300},
                {"kind": "dubl.skill-effects", "path": "content/effects.json", "modules": ["core"], "order": 400}
              ]
            }
        """.trimIndent()

        val files = mapOf(
            "$root/manifest.json" to manifest,
            "$root/content/skills.json" to """{"rankCosts":[0],"skills":[]}""",
            "$root/content/conditions.json" to """{"conditions":[{"id":"hurt","name":"Hurt","description":"x"}]}""",
            "$root/content/dev-a.json" to """{"version":"a","entries":[{"id":"a","name":"A","cost":1}]}""",
            "$root/content/dev-b.json" to """{"version":"b","entries":[{"id":"b","name":"B","cost":2}]}""",
            "$root/content/chi.json" to """{"version":"1","schools":[],"techniques":[]}""",
            "$root/content/magic.json" to """{"version":"1","spells":[],"gear":[]}""",
            "$root/content/effects.json" to """{"version":"1","effects":[]}""",
        )
        val loader = DublFcpCatalogLoader(
            FcpContentPack.load(root, FcpTextSource(files::get)),
        )

        assertEquals(listOf("a", "b"), loader.loadDevelopment().entries.map { it.id })
        assertEquals("hurt", loader.loadConditions().conditions.single().id)
        assertEquals(0, loader.loadChi().techniques.size)
        assertEquals(0, loader.loadMagicEquipment().spells.size)
        assertEquals(0, loader.loadSkillEffects().effects.size)
        assertEquals("""{"rankCosts":[0],"skills":[]}""", loader.loadSkillsPayload())
    }
}
