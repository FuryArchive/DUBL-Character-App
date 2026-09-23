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

    fun install(
        archive: Path,
        installRoot: Path,
        reservedPackIds: Set<String> = emptySet(),
    ): DesktopFcpInstallResult {
        val files = Files.newInputStream(archive).use(::readArchive)
        val validated = validateFcpArchive(files, ::sha256Hex)
        require(validated.manifest.id !in reservedPackIds) {
            "FCP ${validated.manifest.id} is bundled with Fury Book and cannot be replaced by file import"
        }
        val idRoot = installRoot.resolve(validated.manifest.id)
        val target = idRoot.resolve(validated.manifest.version)
        val temp = installRoot.resolve(".install-${UUID.randomUUID()}")
        Files.createDirectories(temp)
        try {
            validated.files.forEach { (relative, bytes) ->
                val destination = temp.resolve(relative).normalize()
                require(destination.startsWith(temp)) { "Unsafe FCP install path: $relative" }
                Files.createDirectories(destination.parent)
                Files.write(destination, bytes)
            }
            Files.createDirectories(installRoot)
            if (Files.exists(idRoot)) deleteRecursively(idRoot)
            Files.createDirectories(target.parent)
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
        return Files.walk(installRoot, 3).use { paths ->
            paths.filter { path ->
                Files.isRegularFile(path) && path.fileName.toString() == "manifest.json"
            }.map { manifest ->
                runCatching { parseFcpManifest(Files.readString(manifest, StandardCharsets.UTF_8)) }.getOrNull()
            }.filter { it != null }
                .map { it!! }
                .toList()
        }.sortedBy { it.id }
    }

    fun findInstalled(installRoot: Path, packId: String): FcpManifest? =
        listInstalled(installRoot).firstOrNull { it.id == packId }

    fun openInstalled(installRoot: Path, manifest: FcpManifest): FcpContentPack {
        val root = installRoot.resolve(manifest.id).resolve(manifest.version).toAbsolutePath().normalize()
        require(Files.isDirectory(root)) { "Installed FCP directory is missing: ${manifest.id} ${manifest.version}" }
        val source = FcpTextSource { relative ->
            val file = root.resolve(relative).normalize()
            if (!file.startsWith(root) || !Files.isRegularFile(file)) null else Files.readString(file, StandardCharsets.UTF_8)
        }
        val pack = FcpContentPack.load("", source)
        require(pack.manifest.id == manifest.id && pack.manifest.version == manifest.version) {
            "Installed FCP identity changed on disk: ${manifest.id} ${manifest.version}"
        }
        return pack
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
