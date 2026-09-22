import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PACK = ROOT / 'shared/src/commonMain/resources/fcp/dubl-3.69'
SHARED_RESOURCES = PACK / 'content'
MANIFEST = PACK / 'manifest.json'
ANDROID_ASSETS = ROOT / 'app/src/main/assets'
APP_BUILD = ROOT / 'app/build.gradle.kts'
ANDROID_DATA = ROOT / 'app/src/main/java/com/furybook/android/data'
ANDROID_FCP = ANDROID_DATA / 'AndroidDublFcp.kt'

CATALOGS = {
    'development_regular_catalog.json': ('entries', 287),
    'development_special_catalog.json': ('entries', 304),
    'development_ability_roots_catalog.json': ('entries', 41),
    'development_martial_catalog.json': ('entries', 122),
    'development_chi_catalog.json': ('entries', 27),
    'development_magic_catalog.json': ('entries', 15),
    'development_catalog.json': ('entries', 0),
    'chi_catalog.json': (('schools', 9), ('techniques', 68)),
    'magic_equipment_catalog.json': (('spells', 265), ('gear', 260)),
    'skill_effects_catalog.json': ('effects', 283),
}


def test_fcp_content_is_the_single_physical_canonical_catalog_copy():
    manifest = json.loads(MANIFEST.read_text(encoding='utf-8'))
    declared = {Path(entry['path']).name for entry in manifest['entries']}

    for name in CATALOGS:
        assert name in declared, f'{name} is not declared by the DUBL FCP manifest'
        assert (SHARED_RESOURCES / name).exists(), name
        assert not (ANDROID_ASSETS / name).exists(), f'duplicate Android catalog asset remains: {name}'

    build = APP_BUILD.read_text(encoding='utf-8')
    assert 'assets.srcDir(rootProject.file("shared/src/commonMain/resources"))' in build


def test_android_catalog_repositories_delegate_to_shared_fcp_loader():
    expected = {
        'DevelopmentCatalogRepository.kt': 'loadDevelopment()',
        'ChiCatalogRepository.kt': 'loadChi()',
        'ConditionCatalogRepository.kt': 'loadConditions()',
        'MagicEquipmentCatalogRepository.kt': 'loadMagicEquipment()',
        'SkillEffectCatalogRepository.kt': 'loadSkillEffects()',
    }
    provider = ANDROID_FCP.read_text(encoding='utf-8')
    assert 'DublFcp.open(' in provider
    assert 'appContext.assets.open(path)' in provider

    for filename, loader_call in expected.items():
        source = (ANDROID_DATA / filename).read_text(encoding='utf-8')
        assert 'AndroidDublFcp.loader(' in source
        assert loader_call in source
        assert 'org.json.' not in source


def test_canonical_catalog_counts_ids_and_archmage_content_are_locked():
    roots = {name: json.loads((SHARED_RESOURCES / name).read_text(encoding='utf-8')) for name in CATALOGS}
    for name, expectation in CATALOGS.items():
        root = roots[name]
        pairs = (expectation,) if isinstance(expectation[0], str) else expectation
        for key, count in pairs:
            assert len(root[key]) == count, (name, key, len(root[key]))
            ids = [item['id'] for item in root[key]]
            assert len(ids) == len(set(ids)), f'duplicate ids in {name}:{key}'

    spells = {item['name']: item for item in roots['magic_equipment_catalog.json']['spells']}
    expected_archmage = {
        'Клинок Короля-Ворона',
        'Копьё Последней Надежды',
        'Секира Ледяного Гиганта',
        'Лук Эльфийского Принца',
        'Щит Утерянного Легиона (реакция)',
    }
    assert expected_archmage <= spells.keys()
    assert all(spells[name]['school'] == 'Боевая магия' for name in expected_archmage)


def test_importers_write_to_fcp_canonical_content_by_default():
    importers = [
        ROOT / 'tools/import_android_0_4_content.py',
        ROOT / 'tools/import_android_0_5_chi.py',
        ROOT / 'tools/import_android_0_5_skill_effects.py',
    ]
    target = 'shared/src/commonMain/resources/fcp/dubl-3.69/content/'
    for path in importers:
        source = path.read_text(encoding='utf-8')
        assert 'app/src/main/assets/' not in source, f'{path.name} still writes duplicate Android assets'
        assert target in source, f'{path.name} must target canonical FCP content'
