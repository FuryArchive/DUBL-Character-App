import hashlib
import json
from pathlib import Path
import tempfile
import zipfile

from tools.fcp.build_fcp import build_fcp, declared_paths, load_manifest

ROOT = Path(__file__).resolve().parents[2]
PACK = ROOT / "shared/src/commonMain/resources/fcp/dubl-3.69"
CONTENT = PACK / "content"
LEGACY_ROOT = ROOT / "shared/src/commonMain/resources"


def test_dubl_is_declared_as_a_fury_content_pack():
    manifest = load_manifest(PACK)
    assert manifest["id"] == "dubl-3.69"
    assert manifest["ruleset"] == {"id": "dubl", "version": "3.69", "engineApi": 1}
    assert {module["id"] for module in manifest["modules"]} == {"core", "melee", "archmage"}

    kinds = [entry["kind"] for entry in manifest["entries"]]
    assert "dubl.skills" in kinds
    assert "dubl.conditions" in kinds
    assert "dubl.development" in kinds
    assert "dubl.chi" in kinds
    assert "dubl.magic-equipment" in kinds
    assert "dubl.skill-effects" in kinds

    for path in declared_paths(PACK, manifest):
        assert (PACK / path).is_file()


def test_catalogs_no_longer_live_as_loose_shared_resources():
    legacy_names = (
        "skills_catalog.json",
        "conditions_catalog.json",
        "development_catalog.json",
        "development_regular_catalog.json",
        "development_special_catalog.json",
        "development_ability_roots_catalog.json",
        "development_martial_catalog.json",
        "development_chi_catalog.json",
        "development_magic_catalog.json",
        "chi_catalog.json",
        "magic_equipment_catalog.json",
        "skill_effects_catalog.json",
    )
    for name in legacy_names:
        assert not (LEGACY_ROOT / name).exists()
        assert (CONTENT / name).is_file()


def test_fcp_builder_is_deterministic_and_contains_checksums():
    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        first = tmp / "first.fcp"
        second = tmp / "second.fcp"
        first_hash = build_fcp(PACK, first)
        second_hash = build_fcp(PACK, second)

        assert first.read_bytes() == second.read_bytes()
        assert first_hash == second_hash == hashlib.sha256(first.read_bytes()).hexdigest()
        assert first.with_suffix(".fcp.sha256").is_file()

        manifest = json.loads((PACK / "manifest.json").read_text(encoding="utf-8"))
        expected = {"manifest.json", "checksums.sha256"} | {
            entry["path"] for entry in manifest["entries"]
        }
        with zipfile.ZipFile(first) as archive:
            assert set(archive.namelist()) == expected
            checksums = archive.read("checksums.sha256").decode("utf-8")
            for path in expected - {"checksums.sha256"}:
                digest = hashlib.sha256(archive.read(path)).hexdigest()
                assert f"{digest}  {path}\n" in checksums


def test_generic_fcp_runtime_does_not_depend_on_dubl():
    content_root = ROOT / "shared/src/commonMain/kotlin/com/furybook/content"
    sources = list(content_root.rglob("*.kt"))
    assert sources
    for path in sources:
        source = path.read_text(encoding="utf-8")
        assert "com.furybook.dubl" not in source, f"generic FCP layer depends on DUBL: {path}"


def test_fury_book_surfaces_the_bundled_fcp_as_a_rules_import_probe():
    android = (ROOT / "app/src/main/java/com/furybook/android/ui/screens/CharactersScreen.kt").read_text(encoding="utf-8")
    desktop = (ROOT / "desktopApp/src/main/kotlin/com/furybook/desktop/screens/CharactersScreen.kt").read_text(encoding="utf-8")
    desktop_loader = (ROOT / "shared/src/desktopMain/kotlin/com/furybook/desktop/data/DesktopCatalogLoader.kt").read_text(encoding="utf-8")
    dubl_loader = (ROOT / "shared/src/commonMain/kotlin/com/furybook/dubl/content/DublFcpCatalogLoader.kt").read_text(encoding="utf-8")

    for source in (android, desktop):
        assert "Fury Content Packs" in source
        assert "Пробный импорт правил" in source
        assert "Switch(" in source
        assert "fcpManifest.ruleset.id" in source
        assert "fcpManifest.formatVersion" in source

    assert "AndroidDublFcp.loader(context)" in android
    assert "fcpLoader.verifyContent()" in android
    assert "val manifest get() = fcp.pack.manifest" in desktop_loader
    assert "fun verifyBundledPack() = fcp.verifyContent()" in desktop_loader
    assert "fcpLoader.verifyBundledPack()" in desktop

    for call in (
        "loadConditions()",
        "loadDevelopment()",
        "loadChi()",
        "loadMagicEquipment()",
        "loadSkillEffects()",
        "loadSkillsPayload()",
    ):
        assert call in dubl_loader
