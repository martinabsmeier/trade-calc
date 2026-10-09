#!/usr/bin/env python3
"""Find real item IDs by substring match."""
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

print(f"Total items: {len(flat)}")

# Show first 30 keys to learn the naming convention
for k in sorted(flat.keys())[:40]:
    print(f"  {k}")

# Look for LONG and CROSSBOW variants
print("\n--- contains 'LONG' ---")
for k in sorted(flat.keys()):
    if "LONG" in k:
        print(f"  {k}  craftable={'craftingrequirements' in flat[k]}")

print("\n--- contains 'CROSSBOW' ---")
for k in sorted(flat.keys()):
    if "CROSSBOW" in k:
        print(f"  {k}  craftable={'craftingrequirements' in flat[k]}")
