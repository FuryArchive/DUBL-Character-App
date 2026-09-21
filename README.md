# FURY

**Cross-platform tabletop character manager for DUBL 3.69, built with Kotlin Multiplatform and Compose.**

[![Android CI](https://github.com/FuryArchive/DUBL-Character-App/actions/workflows/android-ci.yml/badge.svg?branch=main)](https://github.com/FuryArchive/DUBL-Character-App/actions/workflows/android-ci.yml)
[![Linux Desktop](https://github.com/FuryArchive/DUBL-Character-App/actions/workflows/linux-appimage.yml/badge.svg?branch=main)](https://github.com/FuryArchive/DUBL-Character-App/actions/workflows/linux-appimage.yml)
[![Windows Desktop](https://github.com/FuryArchive/DUBL-Character-App/actions/workflows/windows-desktop.yml/badge.svg?branch=main)](https://github.com/FuryArchive/DUBL-Character-App/actions/workflows/windows-desktop.yml)

FURY keeps Android and Desktop on the same shared application/rules layer instead of reimplementing game logic per platform. The **DUBL 3.69 rulebooks are authoritative for rules and canonical content**. Android is the mature UX reference, but it does not override the books.

> Status: active development. The current public product line is **FURY 0.5**.

## What is implemented

- Character sheet, identity, XP and creation economy
- Derived stats, resources, conditions and quick checks
- Skills, ranks, modifiers, specialization and rule-aware rolls
- Development trees, prerequisites, martial arts and Chi
- Magic, mana, schools and spellbook management
- Equipment, carrying state, load and burden
- Multiple characters with persistent local storage
- Shared undo semantics and typed parity/golden scenarios
- Android plus Compose Desktop frontends over shared Kotlin code

Android and Desktop currently write snapshot schema **11** with explicit `dubl / 3.69` ruleset identity. Older snapshots are decoded through the shared compatibility path; both platforms use the same codec.

## Architecture

```text
┌─────────────────┐      ┌────────────────────┐
│ Android / app   │      │ Desktop / desktopApp│
└────────┬────────┘      └─────────┬──────────┘
         │                         │
         └────────────┬────────────┘
                      ▼
             ┌─────────────────┐
             │ shared          │
             │ application     │
             │ rules           │
             │ persistence     │
             │ catalogs        │
             └─────────────────┘
                      │
                      ▼
             DUBL 3.69 rulebooks
             + audited imports
```

The shared application boundary is `DublApplication`. Platform code observes state and invokes typed application operations; executable game formulas should not be duplicated in UI code.

### Repository layout

| Path | Purpose |
| --- | --- |
| `shared/` | KMP model, rules, application layer, codecs, catalogs, shared UI primitives |
| `app/` | Android application and Android-specific persistence/UI |
| `desktopApp/` | Compose Desktop application |
| `rulesets/dubl-3.69/` | tracked ruleset config, resolutions, baseline and promoted generated artifacts |
| `tools/rulebook/` | rulebook import / validation tooling |
| `packaging/linux/` | canonical Linux AppImage packaging |
| `docs/` | architecture, parity, audits and engineering notes |
| `ci/` | CI-only signing/test fixtures |

`packaging/linux/portable-src` is legacy fallback/oracle code and is not the canonical desktop implementation.

## Build and verify

Project toolchain:

- Kotlin 2.4.20
- Compose Multiplatform 1.12.0
- Android Gradle Plugin 9.3.0
- Gradle 9.7.0
- JVM toolchain 17
- Android compileSdk 37 / targetSdk 36

### Shared + Desktop

```bash
./gradlew :shared:desktopTest :desktopApp:compileKotlin
./gradlew :desktopApp:run
```

### Android

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The platform workflows under `.github/workflows/` are the authoritative networked build gates.

## Rulebook-first content pipeline

The stock DUBL book and approved module books are development inputs, not runtime dependencies. Imported material is built into an auditable intermediate representation with provenance and diagnostics before promotion into executable catalogs.

Current authoritative inputs:

- `core` — DUBL 3.69 core rules
- `melee` — Martial Arts / Chi module
- `archmage` — approved additions to supported magic schools

Typical local rebuild:

```bash
python3 -m tools.rulebook.build_ruleset \
  --source 'core=/path/to/core.docx' \
  --source 'melee=/path/to/melee.docx' \
  --source 'archmage=/path/to/archmage.docx' \
  --repo-root .

python3 -m tools.rulebook.validate_ruleset build/rulesets/dubl-3.69

python3 -m tools.rulebook.check_baseline build/rulesets/dubl-3.69 \
  --baseline rulesets/dubl-3.69/baseline.json
```

Ambiguous or contradictory rules are resolved only through `rulesets/dubl-3.69/resolutions.json`; importers must not infer a ruling from existing platform behavior.

## Releases

FURY uses one product version and one Git tag for all shipped platforms.

Example:

```bash
git tag -a v0.5 -m "FURY 0.5"
git push origin v0.5
```

`.github/workflows/release.yml` validates the tag and builds the Android APK, Linux AppImage, and Windows packages into one GitHub Release.

For Linux, the canonical packaging entrypoint is:

```bash
DUBL_VERSION=0.5.0 packaging/linux/build-appimage.sh
```

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before changing rules, catalogs, persistence, shared application behavior, or platform parity contracts.

Useful engineering references:

- [Architecture](docs/ARCHITECTURE.md)
- [Desktop parity](docs/DESKTOP_0_2_PARITY.md)
- [GitHub workflow](docs/GITHUB_WORKFLOW.md)
- [Current project status](docs/STATUS.md)
- [Engineering handoff](HANDOFF.md)

## Scope

Current priorities are DUBL 3.69 correctness, Android/Desktop parity, shared application behavior, persistence compatibility, performance, and release reliability.

Web/Wasm, server accounts and sync are intentionally out of scope for the current milestone.
