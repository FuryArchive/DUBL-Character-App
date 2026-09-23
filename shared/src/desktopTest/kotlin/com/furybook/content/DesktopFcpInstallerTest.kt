package com.furybook.content

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopFcpInstallerTest {
    @Test
    fun installsListsAndReplacesPreviousVersion() {
        val temp = Files.createTempDirectory("fcp-installer-test")
        val installs = temp.resolve("installed")
        try {
            val one = temp.resolve("one.fcp")
            writePack(one, "addon", "1.0")
            val first = DesktopFcpInstaller.install(one, installs)
            assertEquals("1.0", first.manifest.version)
            assertTrue(Files.isRegularFile(first.installDirectory.resolve("manifest.json")))
            assertEquals(listOf("1.0"), DesktopFcpInstaller.listInstalled(installs).map { it.version })

            val two = temp.resolve("two.fcp")
            writePack(two, "addon", "2.0")
            DesktopFcpInstaller.install(two, installs)
            assertEquals(listOf("2.0"), DesktopFcpInstaller.listInstalled(installs).map { it.version })
            assertFalse(Files.exists(installs.resolve("addon").resolve("1.0")))
        } finally {
            Files.walk(temp).use { paths ->
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }

    @Test
    fun refusesToReplaceBundledPackId() {
        val temp = Files.createTempDirectory("fcp-reserved-test")
        try {
            val archive = temp.resolve("reserved.fcp")
            writePack(archive, "dubl-3.69", "3.69")
            assertFailsWith<IllegalArgumentException> {
                DesktopFcpInstaller.install(archive, temp.resolve("installed"), setOf("dubl-3.69"))
            }
        } finally {
            Files.walk(temp).use { paths ->
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }

    private fun writePack(path: java.nio.file.Path, id: String, version: String) {
        val manifest = """
            {"format":"fury.content-pack","formatVersion":1,"id":"$id","name":"Addon","version":"$version",
             "ruleset":{"id":"dubl","version":"3.69","engineApi":1},"dependencies":[],
             "modules":[{"id":"core","name":"Core","required":true,"enabledByDefault":true}],
             "entries":[{"kind":"dubl.development","path":"content/data.json","modules":["core"],"order":10}],
             "claims":[],"ui":[]}
        """.trimIndent().replace("\n", "")
        val data = """{"version":"x","entries":[]}"""
        val payload = linkedMapOf(
            "manifest.json" to manifest.toByteArray(StandardCharsets.UTF_8),
            "content/data.json" to data.toByteArray(StandardCharsets.UTF_8),
        )
        val checksums = payload.entries.sortedBy { it.key }.joinToString("") { (name, bytes) ->
            "${sha256(bytes)}  $name\n"
        }.toByteArray(StandardCharsets.UTF_8)
        ZipOutputStream(Files.newOutputStream(path)).use { zip ->
            (payload + ("checksums.sha256" to checksums)).toSortedMap().forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
