import json
from pathlib import Path

from tools.rulebook.import_skill_effects import import_skill_effects

ROOT = Path(__file__).resolve().parents[2]
CORE = ROOT / "shared/src/commonMain/resources/fcp/dubl-3.69/content"
CHI = ROOT / "shared/src/commonMain/resources/fcp/dubl-chi-3.69/content"
BINDINGS = ROOT / "rulesets/dubl-3.69/bindings/skill_effects.json"
DEV_LAYERS = [
    (CORE, "development_regular_catalog.json"),
    (CORE, "development_special_catalog.json"),
    (CORE, "development_ability_roots_catalog.json"),
    (CORE, "development_martial_catalog.json"),
    (CHI, "development_chi_catalog.json"),
    (CORE, "development_magic_catalog.json"),
]


def _load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def test_runtime_skill_effect_catalog_is_generated_from_composed_development_and_annotation_bindings():
    entries = []
    for root, name in DEV_LAYERS:
        entries.extend(_load(root / name).get("entries", []))
    generated, diagnostics = import_skill_effects(
        {"entries": entries},
        _load(BINDINGS),
    )

    assert diagnostics == []
    assert len(generated["effects"]) == 283
    assert generated == _load(CORE / "skill_effects_catalog.json")
