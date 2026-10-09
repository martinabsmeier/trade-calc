#!/usr/bin/env python3
"""For each Royal-City category, count craftable tradable items."""
import json
from pathlib import Path
from collections import Counter

# Royal-City bonus categories exactly as AGENTS.md documents them
ROYAL_CITY_CATEGORIES = [
    "Häute, Lederwaren",
    "Holz",
    "Bögen, Stoff, Möbel",
    "Rüstungen, Waffen",
    "Stein, Erz",
]

# Sample mapping shopsubcategory1 -> human category (subset, to be refined)
# Wiki: Lymhurst = Bögen/Stoff/Möbel, Martlock = Rüstungen/Waffen,
#       Thetford = Häute/Lederwaren, Bridgewatch = Holz, Fort Sterling = Stein/Erz
path = Path("/Users/martinabsmeier/develop/trade-calc/data/raw/items.json")
data = json.loads(path.read_text(encoding="utf-8"))
items_root = data["items"]

flat = {}
for bucket, payload in items_root.items():
    if isinstance(payload, list):
        for it in payload:
            if isinstance(it, dict) and "@uniquename" in it:
                flat[it["@uniquename"]] = it

# How many of those 357 belong to each shopsubcategory1 (to map to Royal Cities)
cities = {
    "Lymhurst (Bögen, Stoff, Möbel)": ["bow", "warbow", "longbow", "crossbow", "crossbow_large",
                                       "quarterstaff", "spear", "dagger", "fire_staff",
                                       "frost_staff", "arcane_staff", "holy_staff", "cursed_staff",
                                       "nature_staff", "cloth_armor", "cloth_helmet", "cloth_shoes",
                                       "furniture", "decorations"],
    "Martlock (Rüstungen, Waffen)":   ["sword", "axe", "mace", "hammer", "knuckles",
                                       "plate_armor", "plate_helmet", "plate_shoes"],
    "Thetford (Häute, Lederwaren)":   ["leather_armor", "leather_helmet", "leather_shoes",
                                       "horse", "ox", "bag"],
    "Bridgewatch (Holz)":             ["planks", "bow_yew", "spear_yew"],
    "Fort Sterling (Stein, Erz)":     ["ore", "bars", "stone", "block"],
}

city_sub_count = {city: Counter() for city in cities}
city_total = Counter()

for uid, it in flat.items():
    if it.get("@showinmarketplace") != "true":
        continue
    if "craftingrequirements" not in it:
        continue
    if it.get("@unlockedtocraft") != "true":
        continue
    s1 = it.get("@shopsubcategory1", "")
    sc = it.get("@shopcategory", "")
    sub2 = it.get("@shopsubcategory2", "")
    matched = False
    for city, keys in cities.items():
        if s1 in keys or any(k in s1 for k in keys):
            city_sub_count[city][s1] += 1
            city_total[city] += 1
            matched = True
            break
    if not matched:
        city_total["_unmatched_"] += 1

for city, ct in city_total.items():
    print(f"\n=== {city}  total craftable+tradable: {ct}")
    if city in city_sub_count:
        for sub, n in city_sub_count[city].most_common(10):
            print(f"   {sub:30} {n}")
