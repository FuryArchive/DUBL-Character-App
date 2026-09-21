# Current project status

Last refreshed: 2026-09-22

This is the living status document for the repository. Historical milestone reports and design plans remain useful context but must not override current code, tests, tracked resolutions or the DUBL rulebooks.

## Product

- Product: **FURY**
- Current public product line: **0.5**
- Canonical ruleset: **DUBL 3.69**
- Frontends: Android + Compose Desktop (Linux/Windows)
- Web/Wasm, accounts and sync: out of current scope

## Shared foundation

Android and Desktop use the same Kotlin Multiplatform model/rules/application layer.

Current hard boundaries:

- `DublApplication` is the public state-changing application boundary.
- canonical runtime catalogs live in shared resources.
- executable formulas belong in shared rules, not platform UI.
- typed shared golden/parity scenarios protect platform behavior.
- local user overrides are separate from immutable canonical ruleset data.

## Persistence and transfer

- current snapshot schema: **11**
- ruleset identity: `dubl / 3.69`
- transfer format: `dubl.character` version **1**
- Android and Desktop share snapshot/transfer codecs.
- portable transfer deliberately excludes platform-local portrait paths/URIs.

## Rulebook-first pipeline

The tracked ruleset control plane lives in `rulesets/dubl-3.69`.

The importer:

- extracts deterministic source IR from the approved books
- preserves source-qualified provenance
- surfaces ambiguity/conflicts as diagnostics
- requires explicit tracked resolutions for executable interpretations
- keeps full generated bundles under ignored `build/rulesets/dubl-3.69`

Do not infer a rule from current UI behavior when the rulebook source is ambiguous.

## Active implementation priorities

1. DUBL 3.69 correctness and domain promotion
2. Android/Desktop parity through shared application contracts
3. Development-screen performance and interaction polish
4. persistence / transfer compatibility
5. reliable Android, Linux and Windows release gates

## Legacy / historical material

The following are retained for context, not as current implementation authority:

- `docs/KMP_FOUNDATION_REPORT.md`
- `docs/DESKTOP_0_2_PARITY.md`
- `docs/DESKTOP_CHARACTER_SHEET_REPORT.md`
- `docs/superpowers/plans/`
- `docs/superpowers/specs/`
- `packaging/linux/portable-src/`
- `packaging/linux/build-portable-appimage.sh`

## Verification

```bash
./gradlew :shared:desktopTest :desktopApp:compileKotlin
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

For release/rulebook work, also run the relevant Python contract suite and importer validation documented by the workflow or README.
