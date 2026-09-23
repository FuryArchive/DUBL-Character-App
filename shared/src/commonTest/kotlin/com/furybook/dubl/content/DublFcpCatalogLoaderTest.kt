package com.furybook.dubl.content

import com.furybook.content.FcpContentPack
import com.furybook.content.FcpTextSource
import kotlin.test.Test
import kotlin.test.assertEquals

class DublFcpCatalogLoaderTest {
    @Test
    fun coreAndChiAreComposedFromSeparatePacks() {
        val files = mapOf(
            "packs/dubl/manifest.json" to """
                {
                  "format":"fury.content-pack","formatVersion":1,"id":"dubl-3.69","name":"DUBL 3.69","version":"3.69",
                  "ruleset":{"id":"dubl","version":"3.69","engineApi":1},
                  "modules":[{"id":"core","name":"Core","required":true,"enabledByDefault":true}],
                  "entries":[
                    {"kind":"dubl.skills","path":"content/skills.json","modules":["core"],"order":10},
                    {"kind":"dubl.conditions","path":"content/conditions.json","modules":["core"],"order":20},
                    {"kind":"dubl.development","path":"content/dev.json","modules":["core"],"order":100},
                    {"kind":"dubl.magic-equipment","path":"content/magic.json","modules":["core"],"order":300},
                    {"kind":"dubl.skill-effects","path":"content/effects.json","modules":["core"],"order":400}
                  ]
                }
            """.trimIndent(),
            "packs/dubl/content/skills.json" to """{"rankCosts":[0],"skills":[]}""",
            "packs/dubl/content/conditions.json" to """{"conditions":[{"id":"hurt","name":"Hurt","description":"x"}]}""",
            "packs/dubl/content/dev.json" to """{"version":"core","entries":[{"id":"a","name":"A","cost":1}]}""",
            "packs/dubl/content/magic.json" to """{"version":"1","spells":[],"gear":[]}""",
            "packs/dubl/content/effects.json" to """{"version":"1","effects":[]}""",
            "packs/chi/manifest.json" to """
                {
                  "format":"fury.content-pack","formatVersion":1,"id":"dubl-chi-3.69","name":"DUBL Chi","version":"3.69",
                  "ruleset":{"id":"dubl","version":"3.69","engineApi":1},
                  "dependencies":[{"id":"dubl-3.69","version":"3.69"}],
                  "modules":[{"id":"core","name":"Chi","required":true,"enabledByDefault":true}],
                  "entries":[
                    {"kind":"dubl.development","path":"content/dev-chi.json","modules":["core"],"order":10},
                    {"kind":"dubl.chi","path":"content/chi.json","modules":["core"],"order":20}
                  ],
                  "ui":[
                    {"id":"chi.resource","surface":"character.resources","component":"resource-meter","binding":"dubl.chi","label":"ЦИ","order":40,"properties":{}},
                    {"id":"chi.tab","surface":"development.tabs","component":"development-browser","binding":"dubl.chi","label":"ЦИ","order":40,"properties":{}}
                  ]
                }
            """.trimIndent(),
            "packs/chi/content/dev-chi.json" to """{"version":"chi","entries":[{"id":"chi-a","name":"Chi A","section":"ЦИ","cost":1}]}""",
            "packs/chi/content/chi.json" to """{"version":"1","schools":[],"techniques":[]}""",
        )

        val source = FcpTextSource(files::get)
        val core = DublFcpCatalogLoader(FcpContentPack.load("packs/dubl", source))
        val chi = DublChiFcpCatalogLoader(FcpContentPack.load("packs/chi", source))

        assertEquals(listOf("a"), core.loadDevelopment().entries.map { it.id })
        assertEquals("hurt", core.loadConditions().conditions.single().id)
        assertEquals(0, core.loadMagicEquipment().spells.size)
        assertEquals(0, core.loadSkillEffects().effects.size)
        assertEquals("""{"rankCosts":[0],"skills":[]}""", core.loadSkillsPayload())
        assertEquals(listOf("chi-a"), chi.loadDevelopment().entries.map { it.id })
        assertEquals(0, chi.loadChi().techniques.size)
        chi.verifyContent()
    }
}
