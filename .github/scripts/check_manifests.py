"""Check that HPM manifests are valid and agree with the drivers they ship.

- every JSON file parses
- every driver/package `location` on this repo points to a file that exists
- each manifest `version` matches DRIVER_VERSION in the drivers it lists
- each driver's importUrl (if set) points at its own path
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RAW = "https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/"
VERSION = re.compile(r'DRIVER_VERSION\s*=\s*"([^"]+)"')
IMPORT_URL = re.compile(r'importUrl:\s*"([^"]+)"')

errors: list[str] = []


def local(url: str) -> Path | None:
    return ROOT / url[len(RAW):] if url.startswith(RAW) else None


def load(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as err:
        errors.append(f"{path.relative_to(ROOT)}: {err}")
        return {}


repo = load(ROOT / "repository.json")
manifests = [local(p.get("location", "")) for p in repo.get("packages", [])]
for path in manifests:
    if path is None:
        continue
    if not path.is_file():
        errors.append(f"repository.json: package manifest missing: {path.relative_to(ROOT)}")
        continue
    manifest = load(path)
    want = manifest.get("version")
    for driver in manifest.get("drivers", []):
        src = local(driver.get("location", ""))
        if src is None:
            continue
        name = path.relative_to(ROOT)
        if not src.is_file():
            errors.append(f"{name}: driver missing: {src.relative_to(ROOT)}")
            continue
        text = src.read_text(encoding="utf-8-sig")
        found = VERSION.search(text)
        if found is None:
            errors.append(f"{src.relative_to(ROOT)}: no DRIVER_VERSION")
        elif found.group(1) != want:
            errors.append(
                f"{name}: version {want} but {src.relative_to(ROOT)} has DRIVER_VERSION {found.group(1)}"
            )
        url = IMPORT_URL.search(text)
        if url and local(url.group(1)) != src:
            errors.append(f"{src.relative_to(ROOT)}: importUrl points elsewhere: {url.group(1)}")

for problem in errors:
    print(f"::error::{problem}")
print(f"{len(manifests)} package manifest(s) checked, {len(errors)} problem(s)")
sys.exit(1 if errors else 0)
