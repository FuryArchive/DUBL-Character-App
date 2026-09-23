package com.furybook.content

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream

data class DesktopFcpInstallResult(
    val manifest: FcpManifest,
    val installDirectory: Path,
)

object DesktopFcpInstaller {
    private const val MAX_ENTRIES = 512
    private const val MAX_ENTRY_BYTES = 16 * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 64 * 1024 * 1024

    fun install(archive: Path, installRoot: Path): DesktopFcpInstallResult {
        val files = Files.newInputStream(archive).use(::readArchive)
        val validated = validateFcpArchive(files, ::sha256Hex)
        val target = installRoot.resolve(validated.manifest.id).resolve(validated.manifest.version)
        val temp = installRoot.resolve(".install-${UUID.randomUUID()}")
        Files.createDirectories(temp)
        try {
            validated.files.forEach { (relative, bytes) ->
                val destination = temp.resolve(relative).normalize()
                require(destination.startsWith(temp)) { "Unsafe FCP install path: $relative" }
                Files.createDirectories(destination.parent)
                Files.write(destination, bytes)
            }
            Files.createDirectories(target.parent)
            if (Files.exists(target)) deleteRecursively(target)
            runCatching {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE)
            }.getOrElse {
                Files.move(temp, target)
            }
        } finally {
            if (Files.exists(temp)) deleteRecursively(temp)
        }
        return DesktopFcpInstallResult(validated.manifest, target)
    }

    fun listInstalled(installRoot: Path): List<FcpManifest> {
        if (!Files.isDirectory(installRoot)) return emptyList()
        return Files.list(installRoot).use { ids ->
            ids.filter(Files::isDirectory).flatMap { idDir ->
                Files.list(idDir).use { versions ->
                    versions.filter(Files::isDirectory).toList().stream()
                }
            }.mapNotNull { versionDir ->
                val manifest = versionDir.resolve("manifest.json")
                if (!Files.isRegularFile(manifest)) null else runCatching {
                    parseFcpManifest(Files.readString(manifest, StandardCharsets.UTF_8))
                }.getOrNull()
            }.toList()
        }
    }

    private fun readArchive(input: java.io.InputStream): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val name = entry.name
                require(name.isNotBlank() && !name.startsWith("/") && "\\" !in name) { "Unsafe FCP ZIP path: $name" }
                require(name.split('/').none { it.isBlank() || it == "." || it == ".." }) { "Unsafe FCP ZIP path: $name" }
                require(result.size < MAX_ENTRIES) { "FCP archive contains too many files" }
                require(name !in result) { "Duplicate FCP ZIP entry: $name" }
                val bytes = readLimited(zip, MAX_ENTRY_BYTES)
                total += bytes.size
                require(total <= MAX_TOTAL_BYTES) { "FCP archive is too large" }
                result[name] = bytes
            }
        }
        return result
    }

    private fun readLimited(input: java.io.InputStream, limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= limit) { "FCP archive entry exceeds size limit" }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun sha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
