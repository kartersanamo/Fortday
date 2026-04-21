#!/usr/bin/env python3
from pathlib import Path
import hashlib
import json
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PACK_DIR = ROOT / "resourcepack"
DIST_DIR = ROOT / "dist"
DIST_DIR.mkdir(exist_ok=True)
OUT = DIST_DIR / "fortday-pack.zip"

manifest = {"assets": []}
with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as zf:
    for path in sorted(PACK_DIR.rglob("*")):
        if path.is_file():
            rel = path.relative_to(PACK_DIR)
            zf.write(path, rel.as_posix())
            manifest["assets"].append(rel.as_posix())

sha1 = hashlib.sha1(OUT.read_bytes()).hexdigest()
(DIST_DIR / "fortday-pack.sha1").write_text(sha1 + "\n", encoding="utf-8")
manifest["sha1"] = sha1
(DIST_DIR / "pack-manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
print(f"Built {OUT} ({sha1})")
