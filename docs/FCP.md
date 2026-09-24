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
- ordered typed content entries;
- pack dependencies;
- content ownership claims for IDs physically shared with a dependency;
- declarative UI contributions for host-defined surfaces/components.

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
- `FcpComposition`

That package must remain ruleset-agnostic.

Ruleset adapters interpret typed entry kinds. DUBL uses `DublFcpCatalogLoader`; future Lancer support can import native `.lcp` data into Fury content packs without depending on DUBL models.

Platform code supplies bytes/text only. Android reads bundled resources through assets; Desktop reads through the classloader. Neither platform owns catalog filenames or parsing semantics.

## Typed resource render models

`resource-meter` and `resource-toggle` no longer require platform screens to identify the Chi feature or its resource ID. The shared DUBL adapter resolves active FCP UI mounts into typed `DublResourceMeterModel` and `DublResourceToggleModel` values containing the resource ID, availability, current/maximum values, presentation, order, and supported secondary behavior.

Android Overview and Desktop Character Sheet now render those models generically alongside host-owned health/endurance/mana resources. Resource mutations use the shared `CharacterSheetResourceId` path, so the platform renderer no longer contains a Chi-specific resource branch. The raw mapping from `dubl.chi` to `CharacterSheetResourceId.CHI` remains inside the shared DUBL adapter.

## Control presentation from FCP

The `resource-toggle` and `development-browser` renderers now use the same manifest-driven presentation contract as `resource-meter`. Their visible label and semantic icon come from the active FCP contribution, with host-owned rendering and safe fallback behavior on Android and Desktop.

This removes the remaining visible Chi-specific label/icon wiring from those controls. Changing the Chi FCP label or icon token updates the resource visibility control and Development tab without screen-code changes.

## UI contribution ordering

Each UI contribution's `order` now participates in real host layout ordering. Fury Book assigns stable host slots to built-in items and merges FCP contributions into the same ordered sequence. The same shared sorter is used on Android and Desktop.

For the current DUBL vertical slice this applies to `development.tabs` and `character.resources`. With the bundled Chi manifest at `order=40`, the visual order remains unchanged. Changing that manifest order changes the mounted position without editing screen code. Equal orders are resolved deterministically by a stable key.

## Host UI capability validation

The FCP file format and the current Fury Book renderer capabilities are deliberately separate. A manifest can be parsed as valid FCP syntax even if it targets UI that a future host version may introduce, but the current installer, bundled-pack verification, and `.fcp` builder enforce the renderer capabilities that this Fury Book build actually supports.

The current host accepts four surface/component pairs: `character.resources/resource-meter`, `character.resource-settings/resource-toggle`, `development.tabs/development-browser`, and `character.economy/xp-line`. Each renderer has its own allowed property set, and semantic `icon`/`accent` tokens must be known to the host. Unsupported renderer pairs, properties, or tokens fail explicitly instead of being silently ignored.

## UI presentation tokens

UI contributions may provide semantic presentation tokens in `properties`. Fury Book currently recognizes `icon` and `accent` for the `resource-meter` renderer. The manifest never supplies raw Compose classes or arbitrary executable styling; each platform maps known tokens to host-owned renderers and falls back safely for unknown or missing values.

The bundled Chi FCP demonstrates this with `icon=chi` and `accent=fury.accent`. Android renders the resource marker and accent through its resource card, while Desktop maps the same tokens to `DesktopIconKind` and the desktop theme palette.

## Generic UI mounting

FCP UI is mounted through host-defined surface/component contracts rather than pack-specific screen hooks. `FcpUiSurface` names stable host attachment points, `FcpUiComponent` names supported renderer families, and the manifest supplies the concrete `binding`, label, order, and properties.

For DUBL Chi, the manifest still declares the semantic binding `dubl.chi`, but that raw string is interpreted only inside the shared DUBL adapter. `DublUiRegistry` converts active surface/component contributions into typed `DublUiMount` values such as `DublUiFeature.CHI`. Android and Desktop screens consume those typed mounts and no longer reference manifest binding strings directly. This keeps pack protocol details out of platform UI while preserving a typed renderer boundary.

## Optional pack composition

DUBL 3.69 is now split into two bundled Fury Content Packs:

- `dubl-3.69` is the required core rules/content pack.
- `dubl-chi-3.69` is an optional companion pack that owns Chi development, techniques, resource UI, resource settings, development navigation, and Chi XP presentation.

The companion pack declares a dependency on `dubl-3.69`, publishes declarative `ui` contributions, and may declare `claims` for mixed content IDs whose runtime ownership belongs to the optional pack. `FcpComposition` resolves dependency closure, rejects incompatible rulesets and active ownership/UI conflicts, and exposes active/inactive claims to ruleset adapters. Android and Desktop only mount those supported UI/content surfaces while the pack is active. Turning the pack off leaves persisted character fields intact, but removes Chi-owned runtime content, claimed mixed entries, mechanics and presentation from the active Fury Book composition.

Pack activation is persisted per installation. This is the first vertical proof that Fury Book is the host and FCPs own rules/content/UI contributions rather than the host hardcoding a complete DUBL sheet.

## File import and installation

The pack manager can import external `.fcp` archives on Android and Desktop.

Before anything is installed, Fury Book validates:

- ZIP paths and duplicate entries;
- per-entry and total unpacked size limits;
- `manifest.json` format and install-safe pack identity;
- the exact archive payload declared by the manifest;
- every SHA-256 entry in `checksums.sha256`;
- that the imported pack does not replace a bundled Fury Book pack ID.

Validated packs are unpacked into app-local data storage and then appear in the Fury Content Packs list. Installation does **not** imply activation. Packs that do not yet have an engine/adapter path remain visible but inactive, so importing an unknown pack cannot silently alter character rules or state.


## External DUBL data-only activation

Installed external packs can now be activated when they fit the first safe DUBL addon profile:

- ruleset `dubl / 3.69`, engine API `1`;
- exactly one dependency: `dubl-3.69 / 3.69`;
- one or more `dubl.development` entries only;
- no `claims`;
- no `ui` contributions;
- no development entries owned by the optional Chi subsystem.

Activation is persistent per installation. Before the enabled flag is written, Fury Book parses the installed payload and merges it against core DUBL, the full Chi development catalog, and all already-active external addons. Duplicate development IDs reject activation instead of creating ambiguous runtime behavior.

Disabling an external addon removes its entries from the active development catalog but does not rewrite the character snapshot. Any ranks previously stored under those IDs remain dormant and become visible again if the same compatible pack is re-enabled.

This is intentionally additive-only. External overrides/claims, custom UI surfaces, Chi extensions, magic/equipment replacement, and executable logic are separate future capabilities rather than implicit privileges of file import.


## Source of truth

For DUBL, promoted canonical runtime content now lives inside the unpacked FCP tree. Rulebook DOCX files remain development inputs and the DUBL importer remains responsible for provenance, diagnostics and explicit resolutions.

The generated `GeneratedSkillCatalog.kt` is a compiled projection of `content/skills_catalog.json`, not an independent source of truth.

## Building

```bash
python3 -m tools.fcp.build_fcp \
  --source shared/src/commonMain/resources/fcp/dubl-3.69 \
  --output dist/fcp/dubl-3.69.fcp

python3 -m tools.fcp.build_fcp \
  --source shared/src/commonMain/resources/fcp/dubl-chi-3.69 \
  --output dist/fcp/dubl-chi-3.69.fcp
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
