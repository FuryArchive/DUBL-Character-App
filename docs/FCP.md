# Fury Content Pack (FCP)

Fury Content Pack (`.fcp`) is Fury Book's declarative ruleset-content container.

A ruleset integration has two independent parts:

1. an executable rules engine / adapter implemented in code;
2. one or more declarative content packs.

FCP deliberately contains no executable Kotlin, JavaScript, Python, or platform code.

## Container

An `.fcp` file is a deterministic ZIP archive.

Required root files:

- `manifest.json`
- the content files declared by the manifest

Release-built packs also contain:

- `checksums.sha256`

The repository may keep a bundled pack unpacked for review. The unpacked directory must have the exact same paths as the archive root.

DUBL 3.69 currently lives at:

`shared/src/commonMain/resources/fcp/dubl-3.69/`

## Manifest v1

`format` is `fury.content-pack` and `formatVersion` is `1`.

The manifest owns:

- pack identity and version;
- target ruleset identity and engine API;
- optional/required module metadata;
- ordered typed content entries.

Example:

```json
{
  "format": "fury.content-pack",
  "formatVersion": 1,
  "id": "dubl-3.69",
  "name": "DUBL 3.69",
  "version": "3.69",
  "ruleset": {
    "id": "dubl",
    "version": "3.69",
    "engineApi": 1
  },
  "modules": [
    {
      "id": "core",
      "name": "DUBL 3.69 Core",
      "required": true,
      "enabledByDefault": true
    }
  ],
  "entries": [
    {
      "kind": "dubl.conditions",
      "path": "content/conditions_catalog.json",
      "modules": ["core"],
      "order": 20
    }
  ]
}
```

Entry paths are pack-relative, use forward slashes, and may not contain `.` or `..` segments.

## Runtime boundary

`com.furybook.content` owns the format-level API:

- `FcpManifest`
- `FcpContentPack`
- `FcpTextSource`

That package must remain ruleset-agnostic.

Ruleset adapters interpret typed entry kinds. DUBL uses `DublFcpCatalogLoader`; future Lancer support can import native `.lcp` data into Fury content packs without depending on DUBL models.

Platform code supplies bytes/text only. Android reads bundled resources through assets; Desktop reads through the classloader. Neither platform owns catalog filenames or parsing semantics.

## Optional pack composition

DUBL 3.69 is now split into two bundled Fury Content Packs:

- `dubl-3.69` is the required core rules/content pack.
- `dubl-chi-3.69` is an optional companion pack that owns Chi development, techniques, resource UI, resource settings, development navigation, and Chi XP presentation.

The companion pack declares a dependency on `dubl-3.69` and publishes declarative `ui` contributions. Android and Desktop only mount those supported UI surfaces while the pack is active. Turning the pack off leaves persisted character fields intact, but removes Chi-owned runtime content and presentation from the active Fury Book composition.

Pack activation is persisted per installation. This is the first vertical proof that Fury Book is the host and FCPs own rules/content/UI contributions rather than the host hardcoding a complete DUBL sheet.

## Source of truth

For DUBL, promoted canonical runtime content now lives inside the unpacked FCP tree. Rulebook DOCX files remain development inputs and the DUBL importer remains responsible for provenance, diagnostics and explicit resolutions.

The generated `GeneratedSkillCatalog.kt` is a compiled projection of `content/skills_catalog.json`, not an independent source of truth.

## Building

```bash
python3 -m tools.fcp.build_fcp \
  --source shared/src/commonMain/resources/fcp/dubl-3.69 \
  --output dist/fcp/dubl-3.69.fcp
```

The builder:

- validates the manifest;
- includes only declared content;
- writes deterministic ZIP metadata/order;
- writes `checksums.sha256` inside the pack;
- writes `<pack>.sha256` beside the archive.

CI builds the DUBL FCP independently from Android/Desktop packages, and unified releases publish it as a first-class artifact.

## Compatibility

Moving DUBL content into FCP does not change:

- Android application ID;
- DUBL ruleset identity `dubl / 3.69`;
- character snapshot schema;
- `dubl.character` transfer format;
- canonical content IDs.

FCP is a content packaging and composition boundary, not a persistence migration.
