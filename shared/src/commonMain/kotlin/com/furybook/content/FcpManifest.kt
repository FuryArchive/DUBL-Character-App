package com.furybook.content

import com.furybook.core.json.*

object FcpFormat {
    const val ID = "fury.content-pack"
    const val VERSION = 1
}

data class FcpRulesetRef(
    val id: String,
    val version: String,
    val engineApi: Int,
)

data class FcpModule(
    val id: String,
    val name: String,
    val required: Boolean,
    val enabledByDefault: Boolean,
)

data class FcpEntry(
    val kind: String,
    val path: String,
    val modules: List<String>,
    val order: Int,
)

data class FcpManifest(
    val format: String,
    val formatVersion: Int,
    val id: String,
    val name: String,
    val version: String,
    val ruleset: FcpRulesetRef,
    val modules: List<FcpModule>,
    val entries: List<FcpEntry>,
) {
    val defaultEnabledModules: Set<String>
        get() = modules.filter { it.required || it.enabledByDefault }.mapTo(linkedSetOf()) { it.id }

    fun entries(kind: String): List<FcpEntry> = entries
        .asSequence()
        .filter { it.kind == kind }
        .sortedWith(compareBy<FcpEntry>({ it.order }, { it.path }))
        .toList()
}

fun parseFcpManifest(raw: String): FcpManifest {
    val root = parseRoot(raw)
    val rulesetRoot = root.objectValue("ruleset") ?: error("FCP manifest is missing ruleset")
    val modules = root.array("modules").map { value ->
        val item = value.asObject() ?: error("FCP module must be an object")
        FcpModule(
            id = item.string("id").trim(),
            name = item.string("name").trim(),
            required = item.bool("required"),
            enabledByDefault = item.bool("enabledByDefault"),
        )
    }
    val entries = root.array("entries").map { value ->
        val item = value.asObject() ?: error("FCP entry must be an object")
        FcpEntry(
            kind = item.string("kind").trim(),
            path = item.string("path").trim(),
            modules = item.stringList("modules"),
            order = item.int("order"),
        )
    }
    return FcpManifest(
        format = root.string("format").trim(),
        formatVersion = root.int("formatVersion"),
        id = root.string("id").trim(),
        name = root.string("name").trim(),
        version = root.string("version").trim(),
        ruleset = FcpRulesetRef(
            id = rulesetRoot.string("id").trim(),
            version = rulesetRoot.string("version").trim(),
            engineApi = rulesetRoot.int("engineApi"),
        ),
        modules = modules,
        entries = entries,
    ).also(::validateFcpManifest)
}

fun validateFcpManifest(manifest: FcpManifest) {
    require(manifest.format == FcpFormat.ID) { "Unsupported FCP format: ${manifest.format}" }
    require(manifest.formatVersion == FcpFormat.VERSION) {
        "Unsupported FCP format version: ${manifest.formatVersion}"
    }
    require(manifest.id.isNotBlank()) { "FCP id must not be blank" }
    require(manifest.name.isNotBlank()) { "FCP name must not be blank" }
    require(manifest.version.isNotBlank()) { "FCP version must not be blank" }
    require(manifest.ruleset.id.isNotBlank()) { "FCP ruleset id must not be blank" }
    require(manifest.ruleset.version.isNotBlank()) { "FCP ruleset version must not be blank" }
    require(manifest.ruleset.engineApi > 0) { "FCP engineApi must be positive" }

    val moduleIds = manifest.modules.map { it.id }
    require(moduleIds.all(String::isNotBlank)) { "FCP module id must not be blank" }
    require(moduleIds.size == moduleIds.toSet().size) { "Duplicate FCP module ids" }

    val entryKeys = linkedSetOf<Pair<String, String>>()
    manifest.entries.forEach { entry ->
        require(entry.kind.isNotBlank()) { "FCP entry kind must not be blank" }
        validateRelativeFcpPath(entry.path)
        require(entryKeys.add(entry.kind to entry.path)) {
            "Duplicate FCP entry: ${entry.kind} -> ${entry.path}"
        }
        entry.modules.forEach { moduleId ->
            require(moduleId in moduleIds) {
                "FCP entry ${entry.path} references unknown module ${moduleId}"
            }
        }
    }
}

internal fun validateRelativeFcpPath(path: String) {
    require(path.isNotBlank()) { "FCP path must not be blank" }
    require(!path.startsWith('/')) { "FCP path must be relative: $path" }
    require('\\' !in path) { "FCP path must use forward slashes: $path" }
    val segments = path.split('/')
    require(segments.none { it.isBlank() || it == "." || it == ".." }) {
        "Unsafe FCP path: $path"
    }
}
