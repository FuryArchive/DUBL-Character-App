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
