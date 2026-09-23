package com.furybook.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FcpContentPackTest {
    private val manifest = """
        {
          "format": "fury.content-pack",
          "formatVersion": 1,
          "id": "demo-addon",
          "name": "Demo Pack",
          "version": "1.0",
          "ruleset": {"id": "demo", "version": "1", "engineApi": 1},
          "dependencies": [
            {"id": "demo-core", "version": "1.0"}
          ],
          "modules": [
            {"id": "core", "name": "Core", "required": true, "enabledByDefault": true}
          ],
          "entries": [
            {"kind": "demo.data", "path": "content/data.json", "modules": ["core"], "order": 10}
          ],
          "ui": [
            {
              "id": "demo.energy",
              "surface": "character.resources",
              "component": "resource-meter",
              "binding": "demo.energy",
              "label": "Energy",
              "order": 20,
              "properties": {"icon": "energy"}
            }
          ]
        }
    """.trimIndent()

    @Test
    fun loadsCompositionMetadataAndPackRelativeEntries() {
        val files = mapOf(
            "packs/demo/manifest.json" to manifest,
            "packs/demo/content/data.json" to """{"value": 7}""",
        )
        val pack = FcpContentPack.load(
            root = "packs/demo",
            source = FcpTextSource(files::get),
        )

        assertEquals("demo-addon", pack.manifest.id)
        assertEquals(listOf(FcpDependency("demo-core", "1.0")), pack.manifest.dependencies)
        assertEquals(setOf("core"), pack.manifest.defaultEnabledModules)
        assertEquals("Energy", pack.manifest.ui("character.resources").single().label)
        assertEquals("energy", pack.manifest.ui.single().properties["icon"])
        assertEquals("""{"value": 7}""", pack.readSingle("demo.data"))
        assertEquals("packs/demo/content/data.json", pack.resolve("content/data.json"))
    }

    @Test
    fun rejectsUnsafeEntryPaths() {
        val unsafe = manifest.replace("content/data.json", "../escape.json")
        assertFailsWith<IllegalArgumentException> { parseFcpManifest(unsafe) }
    }

    @Test
    fun validatesEntryModules() {
        val invalid = manifest.replace("[\"core\"]", "[\"missing\"]")
        val error = assertFailsWith<IllegalArgumentException> { parseFcpManifest(invalid) }
        assertTrue(error.message.orEmpty().contains("unknown module"))
    }

    @Test
    fun rejectsDuplicateUiContributionIds() {
        val duplicate = manifest.replace(
            "\"ui\": [",
            "\"ui\": [{\"id\":\"demo.energy\",\"surface\":\"x\",\"component\":\"x\",\"binding\":\"x\",\"label\":\"x\",\"order\":1,\"properties\":{}},",
        )
        val error = assertFailsWith<IllegalArgumentException> { parseFcpManifest(duplicate) }
        assertTrue(error.message.orEmpty().contains("Duplicate FCP UI contribution id"))
    }
}
