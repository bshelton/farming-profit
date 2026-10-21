package com.farmingprofit;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.coords.WorldPoint;

/** Patch states from RuneLite's farming tracker; harvest/completion XP from OSRS herb data.
 * XP provides an explicitly labelled estimate, never a claim to read sack contents. */
enum HerbHarvest
{
    GUAM(8, ItemID.UNIDENTIFIED_GUAM, 12.5, 11),
    MARRENTILL(15, ItemID.UNIDENTIFIED_MARENTILL, 15, 13.5),
    TARROMIN(22, ItemID.UNIDENTIFIED_TARROMIN, 18, 16),
    HARRALANDER(29, ItemID.UNIDENTIFIED_HARRALANDER, 24, 21.5),
    RANARR(36, ItemID.UNIDENTIFIED_RANARR, 30.5, 26.5),
    TOADFLAX(43, ItemID.UNIDENTIFIED_TOADFLAX, 38.5, 34),
    IRIT(50, ItemID.UNIDENTIFIED_IRIT, 48.5, 43),
    AVANTOE(57, ItemID.UNIDENTIFIED_AVANTOE, 61.5, 54.5),
    HUASCA(64, ItemID.UNIDENTIFIED_HUASCA, 110, 86.5),
    KWUARM(72, ItemID.UNIDENTIFIED_KWUARM, 78, 69),
    SNAPDRAGON(79, ItemID.UNIDENTIFIED_SNAPDRAGON, 98.5, 87.5),
    CADANTINE(86, ItemID.UNIDENTIFIED_CADANTINE, 120, 106.5),
    LANTADYME(93, ItemID.UNIDENTIFIED_LANTADYME, 151.5, 134.5),
    DWARF_WEED(100, ItemID.UNIDENTIFIED_DWARF_WEED, 192, 170.5),
    TORSTOL(107, ItemID.UNIDENTIFIED_TORSTOL, 224.5, 199.5);

    // FarmingWorld identifies the Falador allotment/herb area in region 12083, north of Port Sarim.
    static double patchMultiplier(WorldPoint patch, boolean faladorMediumComplete)
    {
        return faladorMediumComplete && patch != null && patch.getRegionID() == 12083
            && patch.getY() >= 3272 ? 1.1 : 1;
    }

    static double outfitMultiplier(int[] equipment)
    {
        int pieces = 0;
        for (int id : equipment)
        {
            if (id == ItemID.TITHE_REWARD_HAT_MALE || id == ItemID.TITHE_REWARD_HAT_FEMALE) { pieces |= 1; }
            if (id == ItemID.TITHE_REWARD_TORSO_MALE || id == ItemID.TITHE_REWARD_TORSO_FEMALE) { pieces |= 2; }
            if (id == ItemID.TITHE_REWARD_LEGS_MALE || id == ItemID.TITHE_REWARD_LEGS_FEMALE) { pieces |= 4; }
            if (id == ItemID.TITHE_REWARD_FEET_MALE || id == ItemID.TITHE_REWARD_FEET_FEMALE) { pieces |= 8; }
        }
        if (pieces == 15) { return 1.025; }
        return 1 + ((pieces & 1) != 0 ? .004 : 0) + ((pieces & 2) != 0 ? .008 : 0)
            + ((pieces & 4) != 0 ? .006 : 0) + ((pieces & 8) != 0 ? .002 : 0);
    }

    final int start;
    final int itemId;
    final double xp;
    final double completionXp;
    HerbHarvest(int start, int itemId, double xp, double completionXp)
    { this.start = start; this.itemId = itemId; this.xp = xp; this.completionXp = completionXp; }
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
        return missingCount(delta, outfitMultiplier, observed, false);
    }
    int missingCount(int delta, double outfitMultiplier, int observed, boolean depleted)
    {
        return missingCount(delta, outfitMultiplier, observed, depleted, 1);
    }
    int missingCount(int delta, double outfitMultiplier, int observed, boolean depleted, double patchMultiplier)
    {
        return Math.max(0, inferredCount(delta, outfitMultiplier, depleted, patchMultiplier) - observed);
    }
    int inferredCount(int delta, double outfitMultiplier)
    {
        return inferredCount(delta, outfitMultiplier, false);
    }
    int inferredCount(int delta, double outfitMultiplier, boolean depleted)
    {
        return inferredCount(delta, outfitMultiplier, depleted, 1);
    }
    int inferredCount(int delta, double outfitMultiplier, boolean depleted, double patchMultiplier)
    {
        if (patchMultiplier != 1 && patchMultiplier != 1.1) { return 0; }
        if (delta <= 0 || outfitMultiplier < 1 || outfitMultiplier > 1.0251) { return 0; }
        double perHerb = xp * outfitMultiplier * patchMultiplier;
        // The final pick also awards deferred planting XP. Only subtract it when the patch is empty.
        double harvestXp = delta - (depleted ? completionXp * outfitMultiplier : 0);
        int count = (int) Math.round(harvestXp / perHerb);
        // Account for fractional XP and per-action rounding. Reject unexplained XP.
        return count >= 1 && count <= 8 && Math.abs(harvestXp - count * perHerb) <= 1.0 + count * 0.1 ? count : 0;
    }
}
