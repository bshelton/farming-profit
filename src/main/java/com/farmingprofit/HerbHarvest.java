package com.farmingprofit;

import net.runelite.api.gameval.ItemID;

/** Harvestable patch states and base XP from RuneLite's farming tracker/calculator.
 * XP provides an explicitly labelled estimate, never a claim to read sack contents. */
enum HerbHarvest
{
    GUAM(8, ItemID.UNIDENTIFIED_GUAM, 11),
    MARRENTILL(15, ItemID.UNIDENTIFIED_MARENTILL, 13.5),
    TARROMIN(22, ItemID.UNIDENTIFIED_TARROMIN, 16),
    HARRALANDER(29, ItemID.UNIDENTIFIED_HARRALANDER, 21.5),
    RANARR(36, ItemID.UNIDENTIFIED_RANARR, 27),
    TOADFLAX(43, ItemID.UNIDENTIFIED_TOADFLAX, 34),
    IRIT(50, ItemID.UNIDENTIFIED_IRIT, 43),
    AVANTOE(57, ItemID.UNIDENTIFIED_AVANTOE, 54.5),
    HUASCA(64, ItemID.UNIDENTIFIED_HUASCA, 86.5),
    KWUARM(72, ItemID.UNIDENTIFIED_KWUARM, 69),
    SNAPDRAGON(79, ItemID.UNIDENTIFIED_SNAPDRAGON, 87.5),
    CADANTINE(86, ItemID.UNIDENTIFIED_CADANTINE, 106.5),
    LANTADYME(93, ItemID.UNIDENTIFIED_LANTADYME, 134.5),
    DWARF_WEED(100, ItemID.UNIDENTIFIED_DWARF_WEED, 170.5),
    TORSTOL(107, ItemID.UNIDENTIFIED_TORSTOL, 199.5);

    final int start;
    final int itemId;
    final double xp;
    HerbHarvest(int start, int itemId, double xp) { this.start = start; this.itemId = itemId; this.xp = xp; }
    static HerbHarvest fromPatchState(int value)
    {
        for (HerbHarvest herb : values()) { if (value >= herb.start && value <= herb.start + 2) { return herb; } }
        return null;
    }
    static HerbHarvest fromCrop(int value, String cropName)
    {
        HerbHarvest herb = fromPatchState(value);
        if (herb == null || cropName == null) { return null; }
        // Modern crop definitions can use the herb's name instead of the generic "Herbs".
        // Require both a harvestable state and a matching name: other patch types reuse these states.
        String name = cropName.trim();
        String itemName = CropCatalog.CROPS.get(herb.itemId).name.substring("Grimy ".length());
        return name.equalsIgnoreCase("Herbs") || name.equalsIgnoreCase(itemName)
            || name.equalsIgnoreCase(herb.name().replace('_', ' ')) ? herb : null;
    }

    int missingCount(int delta, double outfitMultiplier, int observed)
    {
        return Math.max(0, inferredCount(delta, outfitMultiplier) - observed);
    }
    int inferredCount(int delta, double outfitMultiplier)
    {
        if (delta <= 0 || outfitMultiplier < 1 || outfitMultiplier > 1.0251) { return 0; }
        double perHerb = xp * outfitMultiplier;
        int count = (int) Math.round(delta / perHerb);
        // Account for fractional XP and per-action rounding. Reject unexplained XP.
        return count >= 1 && count <= 8 && Math.abs(delta - count * perHerb) <= 1.0 + count * 0.1 ? count : 0;
    }
}
