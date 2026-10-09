#!/usr/bin/env python3
"""Probe actual structure of each bucket."""
import json
from pathlib import Path
path = Path("/Users/martinabsmeier/develop/trade-calc/data/raw/items.json")
data = json.loads(path.read_text(encoding="utf-8"))
items_root = data["items"]
for k, v in items_root.items():
    if isinstance(v, list):
        print(f"{k:30} list({len(v)})")
    elif isinstance(v, dict):
        # Show nested keys
        nested_keys = list(v.keys())[:5]
        sizes = [(nk, type(v[nk]).__name__, len(v[nk]) if hasattr(v[nk], '__len__') else '?') for nk in v.keys() if nk != '@xmlns:xsi' and nk != '@xsi:noNamespaceSchemaLocation']
        print(f"{k:30} dict  sizes: {sizes}")
    else:
        print(f"{k:30} {type(v).__name__}")
