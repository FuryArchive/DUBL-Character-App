from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import zipfile

FORMAT = "fury.content-pack"
FORMAT_VERSION = 1
ZIP_TIMESTAMP = (1980, 1, 1, 0, 0, 0)


def load_manifest(source: Path) -> dict:
    manifest_path = source / "manifest.json"
    if not manifest_path.is_file():
        raise ValueError(f"missing FCP manifest: {manifest_path}")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("format") != FORMAT:
        raise ValueError(f"unsupported FCP format: {manifest.get('format')!r}")
    if manifest.get("formatVersion") != FORMAT_VERSION:
        raise ValueError(f"unsupported FCP format version: {manifest.get('formatVersion')!r}")
    for key in ("id", "name", "version", "ruleset", "entries"):
        if key not in manifest:
            raise ValueError(f"FCP manifest is missing {key}")

    for key in ("id", "name", "version"):
        if not str(manifest.get(key, "")).strip():
            raise ValueError(f"FCP {key} must not be blank")

    ruleset = manifest.get("ruleset")
    if not isinstance(ruleset, dict):
        raise ValueError("FCP ruleset must be an object")
    for key in ("id", "version"):
        if not str(ruleset.get(key, "")).strip():
            raise ValueError(f"FCP ruleset {key} must not be blank")
    engine_api = ruleset.get("engineApi")
    if not isinstance(engine_api, int) or isinstance(engine_api, bool) or engine_api <= 0:
        raise ValueError("FCP ruleset engineApi must be a positive integer")

    dependencies = manifest.get("dependencies", [])
    dependency_ids = []
    for dependency in dependencies:
        if not isinstance(dependency, dict):
            raise ValueError("FCP dependency must be an object")
        dep_id = str(dependency.get("id", "")).strip()
        dep_version = str(dependency.get("version", "")).strip()
        if not dep_id or not dep_version:
            raise ValueError("FCP dependency id/version must not be blank")
        if dep_id == manifest["id"]:
            raise ValueError(f"FCP cannot depend on itself: {dep_id}")
        dependency_ids.append(dep_id)
    if len(dependency_ids) != len(set(dependency_ids)):
        raise ValueError("duplicate FCP dependency ids")

    modules = manifest.get("modules", [])
    module_ids = [str(item.get("id", "")).strip() for item in modules if isinstance(item, dict)]
    if len(module_ids) != len(modules) or any(not item for item in module_ids):
        raise ValueError("FCP module id must not be blank")
    if len(module_ids) != len(set(module_ids)):
        raise ValueError("duplicate FCP module ids")

    entry_keys = set()
    for entry in manifest["entries"]:
        if not isinstance(entry, dict):
            raise ValueError("FCP entry must be an object")
        kind = str(entry.get("kind", "")).strip()
        path = str(entry.get("path", "")).strip()
        if not kind:
            raise ValueError("FCP entry kind must not be blank")
        key = (kind, path)
        if key in entry_keys:
            raise ValueError(f"duplicate FCP entry: {kind} -> {path}")
        entry_keys.add(key)

    claim_keys = set()
    for claim in manifest.get("claims", []):
        if not isinstance(claim, dict):
            raise ValueError("FCP content claim must be an object")
        kind = str(claim.get("kind", "")).strip()
        content_id = str(claim.get("id", "")).strip()
        if not kind or not content_id:
            raise ValueError("FCP content claim kind/id must not be blank")
        key = (kind, content_id)
        if key in claim_keys:
            raise ValueError(f"duplicate FCP content claim: {kind} -> {content_id}")
        claim_keys.add(key)

    ui_ids = set()
    for contribution in manifest.get("ui", []):
        if not isinstance(contribution, dict):
            raise ValueError("FCP UI contribution must be an object")
        ui_id = str(contribution.get("id", "")).strip()
        if not ui_id:
            raise ValueError("FCP UI contribution id must not be blank")
        if ui_id in ui_ids:
            raise ValueError(f"duplicate FCP UI contribution id: {ui_id}")
        ui_ids.add(ui_id)
        for key in ("surface", "component", "binding", "label"):
            if not str(contribution.get(key, "")).strip():
                raise ValueError(f"FCP UI contribution {ui_id} has blank {key}")

    return manifest


def declared_paths(source: Path, manifest: dict) -> list[str]:
    paths = ["manifest.json"]
    seen = set(paths)
    module_ids = {item["id"] for item in manifest.get("modules", [])}
    for entry in manifest["entries"]:
        path = str(entry.get("path", "")).strip()
        if not path or path.startswith("/") or "\\" in path:
            raise ValueError(f"invalid FCP entry path: {path!r}")
        parts = Path(path).parts
        if any(part in {"", ".", ".."} for part in parts):
            raise ValueError(f"unsafe FCP entry path: {path!r}")
        if path in seen:
            raise ValueError(f"duplicate FCP path: {path}")
        for module in entry.get("modules", []):
            if module not in module_ids:
                raise ValueError(f"entry {path} references unknown module {module}")
        file_path = source / path
        if not file_path.is_file():
            raise ValueError(f"missing declared FCP entry: {file_path}")
        seen.add(path)
        paths.append(path)
    return paths


def _zip_info(path: str) -> zipfile.ZipInfo:
    info = zipfile.ZipInfo(path, ZIP_TIMESTAMP)
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o100644 << 16
    info.create_system = 3
    return info


def build_fcp(source: Path, output: Path) -> str:
    source = source.resolve()
    manifest = load_manifest(source)
    paths = declared_paths(source, manifest)

    payloads = {path: (source / path).read_bytes() for path in paths}
    checksums = "".join(
        f"{hashlib.sha256(payloads[path]).hexdigest()}  {path}\n"
        for path in sorted(payloads)
    ).encode("utf-8")

    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for path in sorted(payloads):
            archive.writestr(_zip_info(path), payloads[path])
        archive.writestr(_zip_info("checksums.sha256"), checksums)

    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    output.with_suffix(output.suffix + ".sha256").write_text(
        f"{digest}  {output.name}\n",
        encoding="utf-8",
    )
    return digest


def main() -> int:
    parser = argparse.ArgumentParser(description="Build a deterministic Fury Content Pack (.fcp)")
    parser.add_argument("--source", type=Path, required=True, help="Unpacked FCP source directory")
    parser.add_argument("--output", type=Path, required=True, help="Output .fcp archive")
    args = parser.parse_args()

    digest = build_fcp(args.source, args.output)
    print(f"Built {args.output} ({digest})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
