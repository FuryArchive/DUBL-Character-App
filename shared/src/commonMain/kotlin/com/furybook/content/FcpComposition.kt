package com.furybook.content

class FcpComposition private constructor(
    val available: List<FcpManifest>,
    val active: List<FcpManifest>,
    val requiredPackIds: Set<String>,
) {
    private val activeIds = active.mapTo(linkedSetOf()) { it.id }

    fun isActive(packId: String): Boolean = packId in activeIds

    fun ui(surface: String): List<FcpUiContribution> = active
        .flatMap { manifest -> manifest.ui(surface) }
        .sortedWith(compareBy<FcpUiContribution>({ it.order }, { it.id }))

    fun ui(surface: String, binding: String): List<FcpUiContribution> =
        ui(surface).filter { it.binding == binding }

    companion object {
        fun resolve(
            manifests: List<FcpManifest>,
            requiredPackIds: Set<String>,
            enabledPackIds: Set<String>,
        ): FcpComposition {
            val byId = linkedMapOf<String, FcpManifest>()
            manifests.forEach { manifest ->
                require(byId.put(manifest.id, manifest) == null) {
                    "Duplicate FCP id in composition: ${manifest.id}"
                }
            }

            val requested = linkedSetOf<String>()
            requested += requiredPackIds
            requested += enabledPackIds
            requested.forEach { id ->
                require(id in byId) { "Unknown FCP requested for composition: $id" }
            }

            val visiting = linkedSetOf<String>()
            val visited = linkedSetOf<String>()
            val ordered = mutableListOf<FcpManifest>()

            fun activate(id: String) {
                if (id in visited) return
                require(visiting.add(id)) {
                    "FCP dependency cycle: ${visiting.joinToString(" -> ")} -> $id"
                }
                val manifest = byId[id] ?: error("Missing FCP dependency: $id")
                manifest.dependencies.forEach { dependency ->
                    val dependencyManifest = byId[dependency.id]
                        ?: error("FCP ${manifest.id} requires missing dependency ${dependency.id} ${dependency.version}")
                    require(dependencyManifest.version == dependency.version) {
                        "FCP ${manifest.id} requires ${dependency.id} ${dependency.version}, found ${dependencyManifest.version}"
                    }
                    activate(dependency.id)
                }
                visiting.remove(id)
                visited += id
                ordered += manifest
            }

            requested.forEach(::activate)

            val rulesets = ordered.map {
                Triple(it.ruleset.id, it.ruleset.version, it.ruleset.engineApi)
            }.distinct()
            require(rulesets.size <= 1) {
                "Active FCPs target incompatible rulesets: ${rulesets.joinToString()}"
            }

            return FcpComposition(
                available = manifests.toList(),
                active = ordered,
                requiredPackIds = requiredPackIds.toSet(),
            )
        }
    }
}
