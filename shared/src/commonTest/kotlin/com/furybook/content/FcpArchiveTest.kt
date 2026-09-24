package com.furybook.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FcpArchiveTest {
    private val digest = "a".repeat(64)
    private val manifest = """
        {
          "format":"fury.content-pack",
          "formatVersion":1,
          "id":"demo-addon",
          "name":"Demo",
          "version":"1.0",
          "ruleset":{"id":"demo","version":"1","engineApi":1},
          "dependencies":[],
          "modules":[{"id":"core","name":"Core","required":true,"enabledByDefault":true}],
          "entries":[{"kind":"demo.data","path":"content/data.json","modules":["core"],"order":10}],
          "claims":[],
          "ui":[]
        }
    """.trimIndent()

    private fun validFiles(): Map<String, ByteArray> = mapOf(
        "manifest.json" to manifest.encodeToByteArray(),
        "content/data.json" to """{"value":7}""".encodeToByteArray(),
        "checksums.sha256" to (
            "$digest  content/data.json\n" +
                "$digest  manifest.json\n"
            ).encodeToByteArray(),
    )

    @Test
    fun validatesExactPayloadAndChecksumIndex() {
        val archive = validateFcpArchive(validFiles()) { digest }
        assertEquals("demo-addon", archive.manifest.id)
        assertEquals(setOf("manifest.json", "content/data.json", "checksums.sha256"), archive.files.keys)
    }

    @Test
    fun rejectsUnexpectedArchiveFiles() {
        val files = validFiles() + ("evil.txt" to byteArrayOf(1))
        assertFailsWith<IllegalArgumentException> { validateFcpArchive(files) { digest } }
    }

    @Test
    fun rejectsChecksumMismatch() {
        assertFailsWith<IllegalArgumentException> {
            validateFcpArchive(validFiles()) { "b".repeat(64) }
        }
    }

    @Test
    fun rejectsUnsafeInstallIdentity() {
        val unsafe = manifest.replace("\"demo-addon\"", "\"../demo\"")
        val files = validFiles().toMutableMap().also { it["manifest.json"] = unsafe.encodeToByteArray() }
        assertFailsWith<IllegalArgumentException> { validateFcpArchive(files) { digest } }
    }
    @Test
    fun rejectsUiUnsupportedByCurrentHostBeforeInstallation() {
        val unsupportedManifest = manifest.replace(
            "\"ui\":[]",
            "\"ui\":[{\"id\":\"future.ui\",\"surface\":\"future.surface\",\"component\":\"future-widget\",\"binding\":\"demo\",\"label\":\"Future\",\"order\":10,\"properties\":{}}]",
        )
        val files = validFiles().toMutableMap().also {
            it["manifest.json"] = unsupportedManifest.encodeToByteArray()
        }

        assertFailsWith<IllegalArgumentException> {
            validateFcpArchive(files) { digest }
        }
    }

}
