package com.furybook.android.data

import android.content.Context
import android.net.Uri
import com.furybook.content.FcpContentPack
import com.furybook.content.FcpManifest
import com.furybook.content.FcpTextSource
import com.furybook.content.parseFcpManifest
import com.furybook.content.validateFcpArchive
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream

data class AndroidFcpInstallResult(
    val manifest: FcpManifest,
    val installDirectory: File,
)

object AndroidFcpInstaller {
    private const val MAX_ENTRIES = 512
    private const val MAX_ENTRY_BYTES = 16 * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 64 * 1024 * 1024

    fun install(
        context: Context,
        uri: Uri,
        reservedPackIds: Set<String> = emptySet(),
    ): AndroidFcpInstallResult {
        val stream = context.contentResolver.openInputStream(uri) ?: error("Cannot open FCP file")
        val files = stream.use(::readArchive)
        val validated = validateFcpArchive(files, ::sha256Hex)
        require(validated.manifest.id !in reservedPackIds) {
            "FCP ${validated.manifest.id} is bundled with Fury Book and cannot be replaced by file import"
        }

        val root = installRoot(context)
        val idRoot = File(root, validated.manifest.id)
        val target = File(idRoot, validated.manifest.version)
        val temp = File(root, ".install-${UUID.randomUUID()}")
        require(temp.mkdirs() || temp.isDirectory) { "Cannot create FCP install directory" }
        try {
            validated.files.forEach { (relative, bytes) ->
                val destination = File(temp, relative).canonicalFile
                require(destination.path.startsWith(temp.canonicalPath + File.separator)) {
                    "Unsafe FCP install path: $relative"
                }
                destination.parentFile?.mkdirs()
                destination.writeBytes(bytes)
            }
            if (idRoot.exists()) idRoot.deleteRecursively()
            target.parentFile?.mkdirs()
            require(temp.renameTo(target)) { "Cannot finalize FCP installation" }
        } finally {
            if (temp.exists()) temp.deleteRecursively()
        }
        return AndroidFcpInstallResult(validated.manifest, target)
    }

    fun listInstalled(context: Context): List<FcpManifest> {
        val root = installRoot(context)
        if (!root.isDirectory) return emptyList()
        return root.listFiles().orEmpty()
            .asSequence()
            .filter(File::isDirectory)
            .flatMap { idDir -> idDir.listFiles().orEmpty().asSequence() }
            .filter(File::isDirectory)
            .mapNotNull { versionDir ->
                val manifest = File(versionDir, "manifest.json")
                if (!manifest.isFile) null else runCatching { parseFcpManifest(manifest.readText()) }.getOrNull()
            }
            .sortedBy { it.id }
            .toList()
    }

    fun findInstalled(context: Context, packId: String): FcpManifest? =
        listInstalled(context).firstOrNull { it.id == packId }

    fun openInstalled(context: Context, manifest: FcpManifest): FcpContentPack {
        val root = File(installRoot(context), "${manifest.id}/${manifest.version}").canonicalFile
        require(root.isDirectory) { "Installed FCP directory is missing: ${manifest.id} ${manifest.version}" }
        val source = FcpTextSource { relative ->
            val file = File(root, relative).canonicalFile
            if (!file.path.startsWith(root.path + File.separator) || !file.isFile) null else file.readText()
        }
        val pack = FcpContentPack.load("", source)
        require(pack.manifest.id == manifest.id && pack.manifest.version == manifest.version) {
            "Installed FCP identity changed on disk: ${manifest.id} ${manifest.version}"
        }
        return pack
    }

    fun installRoot(context: Context): File = File(context.applicationContext.filesDir, "fcp")

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
        val output = ByteArrayOutputStream()
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
}
