# Contributing to Fury Book

Fury Book is a cross-platform tabletop character/rules companion. DUBL 3.69 is the currently shipped ruleset, whose executable behavior must stay aligned with its source material and the shared Android/Desktop application contract.

## Source-of-truth order

When changing behavior, use this order:

1. DUBL 3.69 rulebooks and approved module books
2. explicit tracked resolutions in `rulesets/dubl-3.69/resolutions.json`
3. shared rules/application code and typed golden scenarios
4. platform UI behavior

Android is the mature UX reference. It is not a rules authority when it conflicts with the books.

## Change boundaries

Prefer changes in `shared/` for:

- game formulas and rule evaluation
- application mutations
- persistence codecs/contracts
- catalog parsing
- cross-platform behavior

Keep platform modules focused on presentation, platform services and adapters.

Do not duplicate a formula in Android and Desktop to make the screens agree.

## Rulebook and catalog changes

For rulebook-driven changes:

- preserve provenance
- do not silently resolve ambiguous wording
- add/update diagnostics or `resolutions.json` when interpretation is required
- update the ruleset baseline only after reviewing the generated diff
- update shared tests/golden scenarios when executable behavior intentionally changes

The rulebook DOCX files are development inputs and must not be added as runtime assets.

## Verification

Run the relevant gates before opening a PR.

Shared/Desktop:

```bash
./gradlew :shared:desktopTest :desktopApp:compileKotlin
```

Android:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

For rulebook import changes, also run the importer validation/baseline checks documented in the README.

If a full Gradle build cannot be run locally because dependencies are unavailable, state that explicitly in the PR and rely on GitHub Actions for the authoritative networked compile.

## Pull requests

Keep each PR focused. In the description, include:

- what changed
- why it changed
- affected platform(s)
- rules/source references when behavior changed
- migration/persistence impact
- tests or verification performed
- screenshots for visible UI changes

Avoid unrelated formatting churn in behavior-changing PRs.

## Compatibility expectations

Treat these as compatibility boundaries unless a deliberate migration is included:

- character persistence schema
- ruleset identity
- Android/Desktop application semantics
- canonical catalog IDs
- typed golden scenarios
- public release packaging

## Legacy code

`packaging/linux/portable-src` is retained as a restricted-environment fallback/parity oracle. Do not add new product behavior there unless the change is specifically about that fallback path.
