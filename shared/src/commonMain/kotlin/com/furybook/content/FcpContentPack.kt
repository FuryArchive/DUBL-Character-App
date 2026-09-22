package com.furybook.content

fun interface FcpTextSource {
    fun readText(path: String): String?
}

class FcpContentPack private constructor(
    val root: String,
    val manifest: FcpManifest,
    private val source: FcpTextSource,
) {
    fun read(entry: FcpEntry): String {
        require(entry in manifest.entries) { "Entry does not belong to FCP ${manifest.id}" }
        return source.readText(resolve(entry.path))
            ?: error("Missing FCP entry ${entry.path} in ${manifest.id}")
    }

    fun readSingle(kind: String): String {
        val matches = manifest.entries(kind)
        require(matches.size == 1) {
            "Expected one FCP entry for $kind in ${manifest.id}, found ${matches.size}"
        }
        return read(matches.single())
    }

    fun readAll(kind: String): List<String> = manifest.entries(kind).map(::read)

    fun resolve(relativePath: String): String {
        validateRelativeFcpPath(relativePath)
        return if (root.isBlank()) relativePath else "$root/$relativePath"
    }

    companion object {
        fun load(root: String, source: FcpTextSource): FcpContentPack {
            val normalizedRoot = root.trim('/').also { value ->
                if (value.isNotBlank()) validateRelativeFcpPath(value)
            }
            val manifestPath = if (normalizedRoot.isBlank()) "manifest.json" else "$normalizedRoot/manifest.json"
            val raw = source.readText(manifestPath)
                ?: error("Missing FCP manifest: $manifestPath")
            return FcpContentPack(
                root = normalizedRoot,
                manifest = parseFcpManifest(raw),
                source = source,
            )
        }
    }
}
