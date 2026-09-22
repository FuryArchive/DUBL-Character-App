from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]

PACKAGE_RE = re.compile(r"^package\s+([A-Za-z0-9_.]+)", re.MULTILINE)


def package_of(path: Path) -> str:
    match = PACKAGE_RE.search(path.read_text(encoding="utf-8"))
    assert match, f"missing Kotlin package declaration: {path.relative_to(ROOT)}"
    return match.group(1)


def kotlin_files(relative_root: str):
    root = ROOT / relative_root
    assert root.exists(), f"missing source root: {relative_root}"
    return list(root.rglob("*.kt"))


def test_shared_common_has_no_android_or_legacy_dubl_package_ownership():
    violations = []
    for path in kotlin_files("shared/src/commonMain/kotlin"):
        package = package_of(path)
        if package.startswith("com.furybook.android") or package.startswith("com.dubl.character.android"):
            violations.append(f"{path.relative_to(ROOT)} -> {package}")
    assert not violations, "platform package leaked into shared/commonMain:\n" + "\n".join(violations)


def test_shared_common_packages_are_owned_by_fury_book():
    violations = []
    allowed = ("com.furybook.core", "com.furybook.content", "com.furybook.dubl", "com.furybook.ui")
    for path in kotlin_files("shared/src/commonMain/kotlin"):
        package = package_of(path)
        if not any(package == root or package.startswith(root + ".") for root in allowed):
            violations.append(f"{path.relative_to(ROOT)} -> {package}")
    assert not violations, "unexpected shared package ownership:\n" + "\n".join(violations)


def test_android_sources_are_platform_owned():
    violations = []
    for path in kotlin_files("app/src/main/java"):
        package = package_of(path)
        if not package.startswith("com.furybook.android"):
            violations.append(f"{path.relative_to(ROOT)} -> {package}")
    assert not violations, "Android source escaped com.furybook.android:\n" + "\n".join(violations)


def test_desktop_sources_are_platform_owned():
    violations = []
    for path in kotlin_files("desktopApp/src/main/kotlin"):
        package = package_of(path)
        if not package.startswith("com.furybook.desktop"):
            violations.append(f"{path.relative_to(ROOT)} -> {package}")
    assert not violations, "Desktop source escaped com.furybook.desktop:\n" + "\n".join(violations)


def test_android_namespace_changed_but_stable_application_id_did_not():
    build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
    assert 'namespace = "com.furybook.android"' in build
    assert 'applicationId = "com.dubl.character.android"' in build
    assert 'android:name="com.furybook.android.MainActivity"' in manifest


def test_desktop_entrypoint_uses_fury_book_namespace():
    build = (ROOT / "desktopApp/build.gradle.kts").read_text(encoding="utf-8")
    assert 'mainClass = "com.furybook.desktop.MainKt"' in build
