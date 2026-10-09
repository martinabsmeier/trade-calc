#!/usr/bin/env python3
"""Final coverage with the correct filter: has_recipe disregards @unlockedtocraft
(player-fame gate, not a recipe existence flag)."""
import json
from pathlib import Path
from collections import Counter

path = Path("/Users/martinabsmeier/develop/trade-calc/data/raw/items.json")
data = json.loads(path.read_text(encoding="utf-8"))

flat = {}
for bucket, payload in data["items"].items():
    if isinstance(payload, list):
        for it in payload:
            if isinstance(it, dict) and "@uniquename" in it:
                flat[it["@uniquename"]] = it


def has_recipe(it):
    cr = it.get("craftingrequirements")
    if not cr or isinstance(cr, list):
        return False
    res = cr.get("craftresource")
    if not res:
        return False
    if isinstance(res, dict):
        return res.get("@uniquename") is not None
    return all(r.get("@uniquename") for r in res)


def show(uid):
    it = flat[uid]
    cr = it["craftingrequirements"]
    res = cr["craftresource"]
    ing = (f"{res['@count']}x{res['@uniquename']}"
           if isinstance(res, dict)
           else ", ".join(f"{r['@count']}x{r['@uniquename']}" for r in res))
    print(f"  {uid:35} cat={it.get('@craftingcategory'):18} -> {ing}")


total = len(flat)
craft = sum(1 for it in flat.values() if has_recipe(it))
craft_unlocked = sum(
    1 for it in flat.values() if has_recipe(it) and it.get("@unlockedtocraft") == "true"
)
tradable = sum(1 for it in flat.values() if it.get("@showinmarketplace") == "true")
tradable_craft = sum(
    1 for it in flat.values() if has_recipe(it) and it.get("@showinmarketplace") == "true"
)

print(f"Total items:                                  {total}")
print(f"Items with a recipe:                          {craft}")
print(f"  of which @unlockedtocraft=true:             {craft_unlocked}")
print(f"Items showinmarketplace=true:                 {tradable}")
print(f"Items tradable AND with recipe:               {tradable_craft}")

print("\n=== craft + tradable per @craftingcategory (top 30) ===")
sub = Counter(
    it.get("@craftingcategory", "<none>")
    for it in flat.values()
    if has_recipe(it) and it.get("@showinmarketplace") == "true"
)
for k, v in sub.most_common(30):
    print(f"  {k:25} {v}")

print("\n=== Sample recipes across categories ===")
for uid in (
    "T4_2H_LONGBOW", "T4_2H_CROSSBOW", "T4_MAIN_SWORD",
    "T4_PLATE_ARMOR_UNDEAD", "T4_LEATHER_JACKET", "T4_CLOTH_ARMOR",
    "T4_MAIN_FIRE_STAFF", "T4_PLANKS", "T4_CLOTH", "T4_LEATHER",
    "T4_METALBAR", "T4_STONEBLOCK", "T4_POTION_HEAL", "T4_BREAD",
    "T4_OFF_TORCH",
):
    show(uid)
