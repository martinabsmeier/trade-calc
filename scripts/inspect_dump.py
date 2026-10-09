#!/usr/bin/env python3
"""Inspect ao-bin-dumps items.json structure."""
import json
import sys
from pathlib import Path

path = Path("/Users/martinabsmeier/develop/trade-calc/data/raw/items.json")
data = json.loads(path.read_text(encoding="utf-8"))

print(f"top-level type: {type(data).__name__}")
print(f"top-level keys: {list(data.keys())}")

inner = data["items"]
print(f"\nitems type: {type(inner).__name__}")
if isinstance(inner, dict):
    print(f"items keys: {list(inner.keys())}")

candidate = None
for k, v in inner.items():
    if isinstance(v, list):
        candidate = (k, v)
        break
if candidate is None:
    for k, v in inner.items():
        if isinstance(v, dict) and "item" in v and isinstance(v["item"], list):
            candidate = ("items.item", v["item"])
            break

if candidate is None:
    print("Could not find list. First 800c of each key:")
    for k, v in inner.items():
        print(f"-- {k} ({type(v).__name__}) --")
        s = json.dumps(v, ensure_ascii=False)[:400]
        print(s)
    sys.exit(1)

key, arr = candidate
print(f"\nFound list under '{key}', len = {len(arr)}")
print("\n--- First item ---")
print(json.dumps(arr[0], indent=2, ensure_ascii=False)[:3500])
print("\n--- Second item ---")
print(json.dumps(arr[1], indent=2, ensure_ascii=False)[:2000])
