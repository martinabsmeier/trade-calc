#!/usr/bin/env python3
"""Print a single item in full so we can see the exact shape of craftingrequirements."""
import json
from pathlib import Path

path = Path("/Users/martinabsmeier/develop/trade-calc/data/raw/items.json")
data = json.loads(path.read_text(encoding="utf-8"))

flat = {}
for bucket, payload in data["items"].items():
    if isinstance(payload, list):
        for it in payload:
            if isinstance(it, dict) and "@uniquename" in it:
                flat[it["@uniquename"]] = it

uid = "T4_2H_LONGBOW"
it = flat[uid]
print(json.dumps(it, indent=2, ensure_ascii=False)[:4000])
