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
          "id": "demo",
          "name": "Demo Pack",
          "version": "1.0",
          "ruleset": {"id": "demo", "version": "1", "engineApi": 1},
          "modules": [
            {"id": "core", "name": "Core", "required": true, "enabledByDefault": true}
          ],
          "entries": [
            {"kind": "demo.data", "path": "content/data.json", "modules": ["core"], "order": 10}
          ]
        }
    """.trimIndent()

    @Test
    fun loadsManifestAndResolvesPackRelativeEntries() {
        val files = mapOf(
            "packs/demo/manifest.json" to manifest,
            "packs/demo/content/data.json" to """{"value": 7}""",
        )
        val pack = FcpContentPack.load(
            root = "packs/demo",
            source = FcpTextSource(files::get),
        )

        assertEquals("demo", pack.manifest.id)
        assertEquals(setOf("core"), pack.manifest.defaultEnabledModules)
        assertEquals("""{"value": 7}""", pack.readSingle("demo.data"))
        assertEquals("packs/demo/content/data.json", pack.resolve("content/data.json"))
    }

    @Test
    fun rejectsUnsafeEntryPaths() {
        val unsafe = manifest.replace("content/data.json", "../escape.json")
        assertFailsWith<IllegalArgumentException> {
            parseFcpManifest(unsafe)
        }
    }

    @Test
    fun validatesEntryModules() {
        val invalid = manifest.replace("[\"core\"]", "[\"missing\"]")
        val error = assertFailsWith<IllegalArgumentException> {
            parseFcpManifest(invalid)
        }
        assertTrue(error.message.orEmpty().contains("unknown module"))
    }
}
