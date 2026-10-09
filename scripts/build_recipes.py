#!/usr/bin/env python3
"""Generate src/main/resources/data/recipes.json: recipe book for craftable items.

Filter: an item is included iff it has BOTH
  - `@craftingcategory` (the game uses this to mark craftable items, drops don't have it)
  - `craftingrequirements.craftresource` (a real recipe with ingredients)
Enchantment variants (.0/.1/.2/.3) get their own entries.

Schema per recipe:
  { id, tier, name (DE), category, shopCategory, shopSub1, shopSub2,
    enchantmentLevel, refiningCategory, craftingTime, craftingFocus, silver,
    ingredients: [{item, count}, ...] }

`refiningCategory` is derived from the dominant refined ingredient
(ponytail simplification; the real Albion carries a per-ingredient category).
"""
import json
import re
from collections import Counter
from pathlib import Path

RAW_ITEMS = Path("/Users/martinabsmeier/develop/trade-calc/data/raw/items.json")
LOCALIZATION = Path("/Users/martinabsmeier/develop/trade-calc/data/raw/localization.json")
OUT = Path("/Users/martinabsmeier/develop/trade-calc/src/main/resources/data/recipes.json")

# Heuristic: refined ingredient id -> Royal-City bonus category (AGENTS.md style)
INGREDIENT_TO_REFINING = {
    "PLANKS": "Wood", "WOOD": "Wood",
    "CLOTH": "Fiber", "FIBER": "Fiber",
    "LEATHER": "Hide", "HIDE": "Hide",
    "METALBAR": "Ore", "ORE": "Ore",
    "STONEBLOCK": "Stone", "ROCK": "Stone",
}

TIER_RE = re.compile(r"^T(\d+)_")


def flat_items():
    data = json.loads(RAW_ITEMS.read_text(encoding="utf-8"))
    out = {}
    for payload in data["items"].values():
        if isinstance(payload, list):
            for it in payload:
                if isinstance(it, dict) and "@uniquename" in it:
                    out[it["@uniquename"]] = it
    return out


def localization_lookup():
    """Reads localization.json (40k TU entries) and returns {ITEMS_xxx: name}.
    German first, fallback English."""
    out = {}
    if not LOCALIZATION.exists():
        return out
    data = json.loads(LOCALIZATION.read_text(encoding="utf-8"))
    tu = data.get("tmx", {}).get("body", {}).get("tu", [])
    for entry in tu:
        tag = entry.get("@tuid", "")
        # TMX keys carry an "@" prefix ("@ITEMS_T2_BOW") — strip it before matching.
        if tag.startswith("@"):
            tag = tag[1:]
        if not tag.startswith("ITEMS_"):
            continue
        if tag in out:
            continue
        translations = entry.get("tuv", [])
        if isinstance(translations, dict):
            translations = [translations]
        translations = [t for t in translations if isinstance(t, dict)]
        de = next((t["seg"] for t in translations if t.get("@xml:lang") == "DE-DE"), None)
        if de:
            out[tag] = de
            continue
        en = next((t["seg"] for t in translations if t.get("@xml:lang") == "EN-US"), None)
        if en:
            out[tag] = en
    return out


def refining_category(ingredients_raw):
    """ponytail: dominant refined resource picks the Royal-City bonus category.
    Upgrade path: thread the per-ingredient category into RecipeIngredient."""
    counts = Counter()
    for r in ingredients_raw:
        name = r["@uniquename"] if isinstance(r, dict) else r
        for prefix, cat in INGREDIENT_TO_REFINING.items():
            if prefix in name:
                counts[cat] += 1
                break
    return counts.most_common(1)[0][0] if counts else None


# ponytail: Handgepflegte deutsche Shop-Kategorie-Labels — der ao-bin-dump liefert
# keine Lokalisierung für @shopsubcategory1/@shopsubcategory2. Upgrade path: SBI-Tags
# für Marktkategorien im Dump ergänzen und Tabelle entfernen.
SUB1_DE = {
    "accessoires_capes_capes": "Umhänge",
    "arcanestaff": "Arkanstäbe",
    "axe": "Äxte",
    "bags": "Taschen",
    "booktype": "Bücher",
    "bow": "Bögen",
    "cloth_armor": "Stoffrüstungen",
    "cloth_helmet": "Stoffhelme",
    "cloth_shoes": "Stoffschuhe",
    "crossbow": "Armbrüste",
    "cursestaff": "Fluchstäbe",
    "dagger": "Dolche",
    "farmingproducts": "Landwirtschaftsprodukte",
    "fiber": "Faser",
    "firestaff": "Feuerstäbe",
    "fish": "Fisch",
    "food": "Essen",
    "froststaff": "Froststäbe",
    "guilds": "Gildenwerkzeuge",
    "hammer": "Hämmer",
    "hide": "Häute",
    "holystaff": "Heiligenstäbe",
    "knuckles": "Schlägerhandschuhe",
    "leather_armor": "Lederrüstungen",
    "leather_helmet": "Lederhelme",
    "leather_shoes": "Lederschuhe",
    "mace": "Keulen",
    "naturestaff": "Naturstäbe",
    "ore": "Erze",
    "other": "Sonstige",
    "plate_armor": "Plattenrüstungen",
    "plate_helmet": "Plattenhelme",
    "plate_shoes": "Plattenschuhe",
    "potions": "Tränke",
    "quarterstaff": "Kampfstäbe",
    "refinedresources": "Verarbeitete Ressourcen",
    "rock": "Steine",
    "satchels": "Tornister",
    "shieldtype": "Schilde",
    "shapeshifterstaff": "Verwandlungsstäbe",
    "spear": "Speere",
    "sword": "Schwerter",
    "torchtype": "Fackeln",
    "tracking": "Fährtensuche",
    "wood": "Holz",
}

# Schlüssel ist shopSub2; für Buckets, deren Label pro Rohstoff variiert
# (gathering_*/Werkzeuge), gewinnt der kombinierte "sub1|sub2"-Schlüssel.
SUB2_DE = {
    "arcanestaff_main_arcanestaff": "Arkanstäbe",
    "arcanestaff_2h_arcanestaff": "Große Arkanstäbe",
    "arcanestaff_enigmaticstaff": "Mysteriöse Stäbe",
    "arcanestaff_crystal": "Kristallstäbe",
    "axe_main_axe": "Streitäxte",
    "axe_2h_axe": "Große Äxte",
    "axe_halberd": "Hellebarden",
    "axe_crystal": "Kristalläxte",
    "booktype_book": "Bücher",
    "booktype_crystal": "Kristallbücher",
    "bow_bow": "Bögen",
    "bow_longbow": "Langbögen",
    "bow_warbow": "Kriegsbögen",
    "bow_crystal": "Kristallbögen",
    "armors_cloth_set1": "Gelehrtenroben",
    "armors_cloth_set2": "Klerikerroben",
    "armors_cloth_set3": "Magierroben",
    "armors_cloth_fey": "Feenroben",
    "head_cloth_set1": "Gelehrtengugeln",
    "head_cloth_set2": "Klerikergugeln",
    "head_cloth_set3": "Magiergugeln",
    "head_cloth_fey": "Feenhüte",
    "shoes_cloth_set1": "Gelehrtensandalen",
    "shoes_cloth_set2": "Klerikersandalen",
    "shoes_cloth_set3": "Magiersandalen",
    "shoes_cloth_fey": "Feensandalen",
    "crossbow_1hcrossbow": "Leichte Armbrüste",
    "crossbow_crossbow": "Armbrüste",
    "crossbow_crossbowlarge": "Schwere Armbrüste",
    "crossbow_crystal": "Kristallarmbrüste",
    "cursestaff_main_cursedstaff": "Verfluchte Stäbe",
    "cursestaff_2h_cursedstaff": "Große verfluchte Stäbe",
    "cursestaff_demonicstaff": "Dämonenstäbe",
    "cursestaff_crystal": "Kristallfluchstäbe",
    "dagger_dagger": "Dolche",
    "dagger_daggerpair": "Dolchpaare",
    "dagger_clawpair": "Klauen",
    "dagger_crystal": "Kristalldolche",
    "farmingproducts|alcohol": "Schnaps",
    "farmingproducts|bread": "Brot",
    "farmingproducts|butter": "Butter",
    "farmingproducts|flour": "Mehl",
    "farmingproducts|meat": "Rohes Fleisch",
    "fiber|sickle": "Sicheln",
    "fish|fishingrods": "Angeln",
    "food|grilledfish": "Gegrillter Fisch",
    "food|omelettes": "Omeletts",
    "food|other": "Sonstiges",
    "food|pies": "Pasteten",
    "food|roasts": "Braten",
    "food|salads": "Salate",
    "food|sandwiches": "Sandwiches",
    "food|soups": "Suppen",
    "food|stews": "Eintöpfe",
    "firestaff_main_firestaff": "Feuerstäbe",
    "firestaff_2h_firestaff": "Große Feuerstäbe",
    "firestaff_infernostaff": "Höllenstäbe",
    "firestaff_crystal": "Kristallfeuerstäbe",
    "froststaff_main_froststaff": "Froststäbe",
    "froststaff_2h_froststaff": "Große Froststäbe",
    "froststaff_glacialstaff": "Gletscherstäbe",
    "froststaff_crystal": "Kristallfroststäbe",
    "guilds|siegehammer": "Belagerungshämmer",
    "hammer_main_hammer": "Hämmer",
    "hammer_2h_hammer": "Großhämmer",
    "hammer_polehammer": "Rabenschnäbel",
    "hammer_crystal": "Kristallhämmer",
    "hide|knifes": "Abhäutemesser",
    "holystaff_main_holystaff": "Heiligenstäbe",
    "holystaff_2h_holystaff": "Große Heiligenstäbe",
    "holystaff_divinestaff": "Gottesstäbe",
    "holystaff_crystal": "Kristallheiligenstäbe",
    "knuckles_set1": "Schlägerhandschuhe",
    "knuckles_set2": "Kampfarmschützer",
    "knuckles_set3": "Nagelhandschuhe",
    "knuckles_crystal": "Kristallarmschienen",
    "armors_leather_set1": "Söldnerjacken",
    "armors_leather_set2": "Jägerjacken",
    "armors_leather_set3": "Attentäterjacken",
    "armors_leather_dragon": "Drachentöterjacken",
    "armors_leather_fey": "Nebelläuferjacken",
    "head_leather_set1": "Söldnerkapuzen",
    "head_leather_set2": "Jägerkapuzen",
    "head_leather_set3": "Attentäterkapuzen",
    "head_leather_dragon": "Drachentöterkapuzen",
    "head_leather_fey": "Nebelläuferkapuzen",
    "shoes_leather_set1": "Söldnerschuhe",
    "shoes_leather_set2": "Jägerschuhe",
    "shoes_leather_set3": "Attentäterschuhe",
    "shoes_leather_dragon": "Drachentöterschuhe",
    "shoes_leather_fey": "Nebelläuferschuhe",
    "mace_main_mace": "Keulen",
    "mace_2h_mace": "Schwere Keulen",
    "mace_flail": "Morgensterne",
    "mace_crystal": "Kristallkeulen",
    "naturestaff_main_naturestaff": "Naturstäbe",
    "naturestaff_2h_naturestaff": "Große Naturstäbe",
    "naturestaff_wildstaff": "Wildstäbe",
    "naturestaff_crystal": "Kristallnaturstäbe",
    "ore|picks": "Spitzhacken",
    "armors_plate_set1": "Soldatenrüstungen",
    "armors_plate_set2": "Ritterrüstungen",
    "armors_plate_set3": "Beschützerrüstungen",
    "armors_plate_fey": "Dunkelweberrüstungen",
    "head_plate_set1": "Soldatenhelme",
    "head_plate_set2": "Ritterhelme",
    "head_plate_set3": "Beschützerhelme",
    "head_plate_fey": "Dunkelweberhelme",
    "shoes_plate_set1": "Soldatenstiefel",
    "shoes_plate_set2": "Ritterstiefel",
    "shoes_plate_set3": "Beschützerstiefel",
    "shoes_plate_fey": "Dunkelweberstiefel",
    "potions|acid": "Säuretränke",
    "potions|berserk": "Berserkertränke",
    "potions|calming": "Beruhigungstränke",
    "potions|cleanse": "Reinigungstränke",
    "potions|energy": "Energietränke",
    "potions|gather": "Sammeltränke",
    "potions|gigantify": "Riesenwuchstränke",
    "potions|heal": "Lebenstränke",
    "potions|invisibility": "Tränke der Unsichtbarkeit",
    "potions|lava": "Höllenfeuertränke",
    "potions|lifeward": "Lebensschutztränke",
    "potions|other": "Sonstige Tränke",
    "potions|poison": "Gifttränke",
    "potions|resistance": "Resistenztränke",
    "potions|slowfield": "Klebrige Tränke",
    "potions|tornado": "Tornados in der Flasche",
    "quarterstaff_quarterstaff": "Kampfstäbe",
    "quarterstaff_doublebladedstaff": "Doppelklingenstäbe",
    "quarterstaff_ironcladedstaff": "Eisenpanzerstäbe",
    "quarterstaff_crystal": "Kristallkampfstäbe",
    "refinedresources|cloth": "Stoffe",
    "refinedresources|leather": "Leder",
    "refinedresources|metalbars": "Barren",
    "refinedresources|planks": "Planken",
    "refinedresources|stoneblock": "Steinblöcke",
    "rock|hammer": "Steinhämmer",
    "shieldtype_shield": "Schilde",
    "shieldtype_crystal": "Kristallschilde",
    "shapeshifterstaff_avalon": "Lichtrufer",
    "shapeshifterstaff_hell": "Höllische Stäbe",
    "shapeshifterstaff_keeper": "Erdrunenstäbe",
    "shapeshifterstaff_morgana": "Blutmondstäbe",
    "shapeshifterstaff_set1": "Pirschstäbe",
    "shapeshifterstaff_set2": "Wurzelbinderstäbe",
    "shapeshifterstaff_set3": "Urstäbe",
    "shapeshifterstaff_crystal": "Starreblickstäbe",
    "spear_main_spear": "Speere",
    "spear_2h_spear": "Piken",
    "spear_glaive": "Glefen",
    "spear_crystal": "Kristallspeere",
    "sword_sword": "Breitschwerter",
    "sword_dualsword": "Zweischwerter",
    "sword_claymore": "Claymores",
    "sword_crystal": "Kristallschwerter",
    "torchtype_torch": "Fackeln",
    "torchtype_crystal": "Kristallfackeln",
    "tracking|toolkit": "Fährtensucher-Ausrüstungen",
    "wood|axes": "Holzfälleräxte",
}

# gathering_*-Buckets: Label variiert je Rohstoff → kombinierte Schlüssel.
_GATHER_LABELS = {
    ("fiber", "armor"): "Erntehelferkleidung",
    ("fiber", "capes"): "Erntehelferrucksäcke",
    ("fiber", "head"): "Erntehelfermützen",
    ("fiber", "shoes"): "Erntehelfer-Arbeitsschuhe",
    ("fish", "armor"): "Anglerkleidung",
    ("fish", "capes"): "Anglerrucksäcke",
    ("fish", "head"): "Anglermützen",
    ("fish", "shoes"): "Anglerstiefel",
    ("hide", "armor"): "Kürschnerkleidung",
    ("hide", "capes"): "Kürschnerrucksäcke",
    ("hide", "head"): "Kürschnermützen",
    ("hide", "shoes"): "Kürschner-Arbeitsschuhe",
    ("ore", "armor"): "Bergarbeiterkleidung",
    ("ore", "capes"): "Bergarbeiterrucksäcke",
    ("ore", "head"): "Bergarbeitermützen",
    ("ore", "shoes"): "Bergarbeiter-Arbeitsschuhe",
    ("rock", "armor"): "Steinbrucharbeiter-Kleidung",
    ("rock", "capes"): "Steinbrucharbeiter-Rucksäcke",
    ("rock", "head"): "Steinbrucharbeitermützen",
    ("rock", "shoes"): "Steinbrucharbeiter-Arbeitsschuhe",
    ("wood", "armor"): "Holzfällerkleidung",
    ("wood", "capes"): "Holzfällerrucksäcke",
    ("wood", "head"): "Holzfällermützen",
    ("wood", "shoes"): "Holzfäller-Arbeitsschuhe",
}
for (_sub1, _slot), _label in _GATHER_LABELS.items():
    SUB2_DE[f"{_sub1}|gathering_{_slot}"] = _label

# Faction/transmog variants (Avalonian, Hellgate, Keeper, Morgana, Undead …) have no
# shopsubcategory2 localization either — compose "Faction-Base" labels from the base
# variant's German label, e.g. bow_avalon → "Avalon-Bögen".
FACTION_DE = {
    "avalon": "Avalon",
    "hell": "Höllen",
    "keeper": "Erdrunen",
    "morgana": "Blutmond",
    "undead": "Untoten",
    "hellsskull": "Höllenschädel",
    "keeperhorn": "Erdrunen-Hörner",
}
STRIP_LABELS = {
    "armors_cloth": "Stoffrüstungen", "armors_leather": "Lederrüstungen",
    "armors_plate": "Plattenrüstungen",
    "head_cloth": "Stoffhelme", "head_leather": "Lederhelme", "head_plate": "Plattenhelme",
    "shoes_cloth": "Stoffschuhe", "shoes_leather": "Lederschuhe", "shoes_plate": "Plattenschuhe",
    "naturestaff_main": "Naturstäbe", "naturestaff_2h": "Große Naturstäbe",
    "booktype": "Bücher", "shieldtype": "Schilde", "knuckles": "Schlägerhandschuhe",
    "torchtype": "Fackeln", "satchels": "Tornister", "bags": "Taschen",
}


def _faction_suffix(sub2):
    """Finds a trailing faction token, e.g. 'bow_avalon' → 'avalon',
    'naturestaff_2h_keeper' → 'keeper'; None if the value has no faction suffix."""
    if "_" not in sub2:
        return None
    suffix = sub2.rsplit("_", 1)[1]
    return suffix if suffix in FACTION_DE else None


def _base_label(stripped):
    """German label of the base (non-faction) variant for a stripped sub2 prefix.
    Order: exact plain key ("bow_bow"), the _main_ variant ("cursestaff_main_cursedstaff"),
    the STRIP_LABELS table (armor types etc.)."""
    plain = SUB2_DE.get(f"{stripped}_{stripped}")
    if plain:
        return plain
    prefix = stripped + "_main_"
    for key in SUB2_DE:
        if key.startswith(prefix):
            return SUB2_DE[key]
    return STRIP_LABELS.get(stripped)


def german_label(sub1, sub2):
    """Display label for shopSub1/shopSub2: hand-curated table first, then faction variants
    composed as "Faction-Base" (e.g. bow_avalon → "Avalon-Bögen"). Falls back to the raw
    dump token; known gaps should be added to the tables, not patched ad hoc."""
    sub1_de = SUB1_DE.get(sub1, sub1) if sub1 else sub1
    if sub2 is None:
        return None, sub1_de
    # Composite keys ("ore|picks") must be looked up with the RAW sub1 — the German label of sub1
    # serves display only.
    combo = SUB2_DE.get(f"{sub1}|{sub2}")
    if combo:
        return combo, sub1_de
    label = SUB2_DE.get(sub2)
    if label:
        return label, sub1_de
    suffix = _faction_suffix(sub2)
    if suffix:
        stripped = sub2[: -(len(suffix) + 1)]
        base = _base_label(stripped)
        if base:
            return f"{FACTION_DE[suffix]}-{base}", sub1_de
    return sub2, sub1_de  # ponytail: unknown tokens stay raw; fix by extending the tables


def nice_name(uid, loc):
    tag = "ITEMS_" + uid
    if tag in loc:
        return loc[tag]
    no_tier = uid.split("_", 1)[1] if "_" in uid else uid
    return no_tier.replace("_", " ").title()


def tier_of(uid):
    m = TIER_RE.match(uid)
    return int(m.group(1)) if m else None


def _is_faction_recipe(cr):
    """Faction variants pay with a FACTION_* token instead of extra resources —
    not the recipe the profit calc should model."""
    res = cr.get("craftresource")
    if isinstance(res, dict):
        res = [res]
    return any("FACTION_" in r.get("@uniquename", "") for r in res or [])


def pick_crafting_requirements(cr):
    """craftingrequirements is a dict for plain recipes but a list when the item
    has a faction variant (e.g. T5_PLANKS). Prefer the non-faction entry."""
    if not cr:
        return None
    if isinstance(cr, list):
        cr = next((c for c in cr if isinstance(c, dict) and not _is_faction_recipe(c)), None)
        if cr is None:
            return None
    res = cr.get("craftresource")
    if not res:
        return None
    if isinstance(res, dict):
        return cr if res.get("@uniquename") is not None else None
    return cr if all(r.get("@uniquename") for r in res) else None


def has_real_recipe(it):
    return pick_crafting_requirements(it.get("craftingrequirements")) is not None


def is_market_tradable(it):
    """`@showinmarketplace` is missing on the bulk of equipables — the game treats
    None as tradable. Explicit "false" is the only blocker."""
    return it.get("@showinmarketplace") != "false"


def build_recipe_entry(items, loc, uid, it, cr, ench_level):
    res = cr.get("craftresource")
    if isinstance(res, dict):
        ing_list = [res]
    else:
        ing_list = list(res)
    if not ing_list:
        return None
    sub1 = it.get("@shopsubcategory1")
    sub2 = it.get("@shopsubcategory2")
    sub2_de, sub1_de = german_label(sub1, sub2)
    return {
        "id": uid,
        "tier": tier_of(uid),
        "name": nice_name(uid, loc),
        "category": it.get("@craftingcategory"),
        "shopCategory": it.get("@shopcategory"),
        "shopSub1": sub1_de,
        # Kombinierter Schlüssel schlägt den generischen (z. B. "ore|picks" vor "picks").
        "shopSub2": sub2_de,
        "enchantmentLevel": ench_level,
        "refiningCategory": refining_category(ing_list),
        "craftingTime": float(cr.get("@time", 0)),
        "craftingFocus": int(cr.get("@craftingfocus", 0)),
        "silver": int(cr.get("@silver", 0)),
        "ingredients": [
            {"item": r["@uniquename"], "count": int(r["@count"])}
            for r in ing_list
        ],
    }


def main():
    items = flat_items()
    loc = localization_lookup()

    recipes = []
    enchant_count = 0

    for uid, it in items.items():
        if not it.get("@craftingcategory"):
            continue
        if not is_market_tradable(it):
            continue
        cr = it.get("craftingrequirements")
        if has_real_recipe(it):
            entry = build_recipe_entry(items, loc, uid, it, pick_crafting_requirements(cr), None)
            if entry:
                recipes.append(entry)

        ench = it.get("enchantments", {}).get("enchantment")
        if not ench:
            continue
        if isinstance(ench, dict):
            ench = [ench]
        for e in ench:
            level = int(e.get("@enchantmentlevel", 0))
            ecr = e.get("craftingrequirements")
            ecr = pick_crafting_requirements(ecr)
            if ecr is None:
                continue
            entry = build_recipe_entry(items, loc, uid, it, ecr, level)
            if entry:
                recipes.append(entry)
                enchant_count += 1

    # Sort by id for stable diffs
    recipes.sort(key=lambda r: (r["id"], r["enchantmentLevel"] or 0))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(
        # Kompakt: 3,8 MB → ~1,3 MB; das Diff-Argument zieht bei 7k Rezepten ohnehin nicht.
        json.dumps(recipes, separators=(",", ":"), ensure_ascii=False),
        encoding="utf-8",
    )

    print(f"Wrote {len(recipes)} recipes to {OUT}")
    print(f"  base recipes: {len(recipes) - enchant_count}")
    print(f"  enchantment variants: {enchant_count}")

    # Summary
    by_cat = Counter(r["category"] for r in recipes)
    print("\nBy category (top 20):")
    for k, v in by_cat.most_common(20):
        print(f"  {(k or '<none>'):30} {v}")

    by_tier = Counter(r["tier"] for r in recipes)
    print("\nBy tier:")
    for k, v in sorted(by_tier.items(), key=lambda kv: (kv[0] is None, kv[0] or 0)):
        print(f"  T{k}  {v}")

    # Localization hit rate
    named = sum(1 for r in recipes if not r["name"].startswith("Artefact")
                and " " in r["name"])
    print(f"\nRecipes with friendly German name: {named} / {len(recipes)}")

    # Spot-check Longbow T4 (the spec example)
    print("\n--- Spec example: T4_2H_LONGBOW ---")
    for r in recipes:
        if r["id"] == "T4_2H_LONGBOW" and r["enchantmentLevel"] is None:
            print(json.dumps(r, indent=2, ensure_ascii=False))
            break


if __name__ == "__main__":
    main()
