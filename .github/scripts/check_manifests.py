"""Check that HPM manifests are valid and agree with the drivers they ship.

- every JSON file parses
- every driver/package `location` on this repo points to a file that exists
- each manifest `version` matches DRIVER_VERSION / APP_VERSION in the code it lists
- each driver's importUrl (if set) points at its own path
- every .groovy file under Drivers/ and Apps/ has balanced { } ( ) [ ] outside strings and
  comments (a stray brace makes the hub refuse the upload, and HPM then reports
  only "Failed to upgrade driver")
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RAW = "https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/"
VERSION = re.compile(r'(?:DRIVER|APP)_VERSION\s*=\s*"([^"]+)"')
IMPORT_URL = re.compile(r'importUrl:\s*"([^"]+)"')

errors: list[str] = []


PAIRS = {"}": "{", ")": "(", "]": "["}


def unbalanced(text: str) -> str | None:
    """First bracket problem in Groovy source, skipping strings and comments."""
    stack: list[tuple[str, int]] = []
    i, line, n = 0, 1, len(text)
    while i < n:
        c = text[i]
        if c == "\n":
            line += 1
        elif text.startswith("//", i):
            i = text.find("\n", i)
            if i < 0:
                break
            continue
        elif text.startswith("/*", i):
            end = text.find("*/", i + 2)
            end = n if end < 0 else end + 2
            line += text.count("\n", i, end)
            i = end
            continue
        elif c in "'\"":
            quote = text[i:i + 3] if text[i:i + 3] in ("'''", '"""') else c
            j = i + len(quote)
            while j < n and not text.startswith(quote, j):
                j += 2 if text[j] == "\\" else 1
            end = min(n, j + len(quote))
            line += text.count("\n", i, end)
            i = end
            continue
        elif c in "{([":
            stack.append((c, line))
        elif c in "})]":
            if not stack or stack[-1][0] != PAIRS[c]:
                return f"unexpected '{c}' on line {line}"
            stack.pop()
        i += 1
    if stack:
        c, opened = stack[-1]
        return f"'{c}' opened on line {opened} is never closed"
    return None


for groovy in sorted([*(ROOT / "Drivers").rglob("*.groovy"), *(ROOT / "Apps").rglob("*.groovy")]):
    problem = unbalanced(groovy.read_text(encoding="utf-8-sig"))
    if problem:
        errors.append(f"{groovy.relative_to(ROOT)}: {problem}")


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
    for driver in [*manifest.get("apps", []), *manifest.get("drivers", [])]:
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
            errors.append(f"{src.relative_to(ROOT)}: no DRIVER_VERSION / APP_VERSION")
        elif found.group(1) != want:
            errors.append(
                f"{name}: version {want} but {src.relative_to(ROOT)} has version {found.group(1)}"
            )
        url = IMPORT_URL.search(text)
        if url and local(url.group(1)) != src:
            errors.append(f"{src.relative_to(ROOT)}: importUrl points elsewhere: {url.group(1)}")

for problem in errors:
    print(f"::error::{problem}")
print(f"{len(manifests)} package manifest(s) checked, {len(errors)} problem(s)")
sys.exit(1 if errors else 0)
