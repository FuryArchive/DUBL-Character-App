# Documentation

Use this page to distinguish current documentation from historical implementation records.

## Living documentation

- [Current status](STATUS.md) — current product, compatibility boundaries and priorities
- [Architecture](ARCHITECTURE.md) — authority order, shared boundaries, persistence and frontend ownership
- [GitHub workflow](GITHUB_WORKFLOW.md) — development CI and unified release process
- [Rulebook audit](rulebook-audit/2026-09-16-core-skills-rolls.md) — semantic audit ledger for the audited core/skills/rolls slice

Repository-wide contributor rules live in [CONTRIBUTING.md](../CONTRIBUTING.md).

## Historical records

These documents describe milestones or implementation plans at a point in time. They may mention old schemas, versions, temporary constraints or superseded architecture.

- `KMP_FOUNDATION_REPORT.md`
- `DESKTOP_0_2_PARITY.md`
- `DESKTOP_CHARACTER_SHEET_REPORT.md`
- `superpowers/plans/`
- `superpowers/specs/`

Use them for rationale/history only. When they disagree with living docs, code/tests, tracked resolutions or rulebooks, the newer authoritative source wins.

## Documentation rule

Do not create another root-level status dump or generated file inventory. Update `STATUS.md`, `ARCHITECTURE.md`, the changelog, or a focused design/audit document instead.
