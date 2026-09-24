package com.furybook.content

data class FcpValidatedArchive(
    val manifest: FcpManifest,
    val files: Map<String, ByteArray>,
)

fun validateFcpArchive(
    files: Map<String, ByteArray>,
    sha256Hex: (ByteArray) -> String,
): FcpValidatedArchive {
    val manifestBytes = files["manifest.json"] ?: error("FCP archive is missing manifest.json")
    val checksumBytes = files["checksums.sha256"] ?: error("FCP archive is missing checksums.sha256")
    val manifest = parseFcpManifest(manifestBytes.decodeToString())
    FcpUiHostCapabilities.requireSupported(manifest)

    require(manifest.id.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))) {
        "FCP id is not safe for installation: ${manifest.id}"
    }
    require(manifest.version.matches(Regex("[A-Za-z0-9][A-Za-z0-9._+-]{0,127}"))) {
        "FCP version is not safe for installation: ${manifest.version}"
    }

    val declared = linkedSetOf("manifest.json", "checksums.sha256")
    manifest.entries.forEach { entry -> declared += entry.path }
    require(files.keys == declared) {
        val missing = declared - files.keys
        val extra = files.keys - declared
        "FCP archive contents do not match manifest; missing=$missing extra=$extra"
    }

    val checksums = parseFcpChecksums(checksumBytes.decodeToString())
    val checksummedFiles = declared - "checksums.sha256"
    require(checksums.keys == checksummedFiles) {
        val missing = checksummedFiles - checksums.keys
        val extra = checksums.keys - checksummedFiles
        "FCP checksum index does not match payload; missing=$missing extra=$extra"
    }
    checksummedFiles.forEach { path ->
        val actual = sha256Hex(files.getValue(path)).lowercase()
        val expected = checksums.getValue(path)
        require(actual == expected) { "FCP checksum mismatch: $path" }
    }

    return FcpValidatedArchive(manifest, files)
}

private fun parseFcpChecksums(raw: String): Map<String, String> {
    val result = linkedMapOf<String, String>()
    raw.lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .forEach { line ->
            val separator = line.indexOf("  ")
            require(separator == 64) { "Invalid FCP checksum line: $line" }
            val digest = line.substring(0, separator).lowercase()
            val path = line.substring(separator + 2)
            require(digest.matches(Regex("[0-9a-f]{64}"))) { "Invalid FCP SHA-256 digest: $digest" }
            validateRelativeFcpPath(path)
            require(path != "checksums.sha256") { "checksums.sha256 must not checksum itself" }
            require(result.put(path, digest) == null) { "Duplicate FCP checksum path: $path" }
        }
    return result
}
