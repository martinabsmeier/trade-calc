#!/usr/bin/env python3
"""Count craftable items per bucket and per shopsubcategory1."""
import json
from pathlib import Path
from collections import Counter

path = Path("/Users/martinabsmeier/develop/trade-calc/data/raw/items.json")
data = json.loads(path.read_text(encoding="utf-8"))
items_root = data["items"]

# Collect every item from every bucket into one flat list keyed by @uniquename
flat = {}
for bucket, payload in items_root.items():
    if not (isinstance(payload, list)):
        continue
    for it in payload:
        if not isinstance(it, dict):
            continue
        uid = it.get("@uniquename")
        if uid:
            flat[uid] = it

print(f"Total distinct items across buckets: {len(flat)}")

# Stats
with_recipe = sum(1 for it in flat.values() if "craftingrequirements" in it)
craftable = sum(
    1 for it in flat.values()
    if "craftingrequirements" in it and it.get("@unlockedtocraft") == "true"
)
tradable = sum(1 for it in flat.values() if it.get("@showinmarketplace") == "true")
tradable_and_craftable = sum(
    1 for it in flat.values()
    if it.get("@showinmarketplace") == "true"
    and "craftingrequirements" in it
    and it.get("@unlockedtocraft") == "true"
)

print(f"With craftingrequirements:        {with_recipe}")
print(f"unlockedtocraft=true:             {craftable}")
print(f"showinmarketplace=true:           {tradable}")
print(f"tradable AND craftable:           {tradable_and_craftable}")

# Per shopsubcategory1 distribution for craftable+tradable
sub_counter = Counter(
    it.get("@shopsubcategory1", "?")
    for it in flat.values()
    if it.get("@showinmarketplace") == "true"
    and "craftingrequirements" in it
    and it.get("@unlockedtocraft") == "true"
)
print("\nshopsubcategory1 (top 20):")
for k, v in sub_counter.most_common(20):
    print(f"  {k:30} {v}")

# Per craftingcategory distribution
craft_counter = Counter(
    it.get("@craftingcategory", "?")
    for it in flat.values()
    if it.get("@showinmarketplace") == "true"
    and "craftingrequirements" in it
    and it.get("@unlockedtocraft") == "true"
)
print("\ncraftingcategory:")
for k, v in craft_counter.most_common():
    print(f"  {k:30} {v}")
