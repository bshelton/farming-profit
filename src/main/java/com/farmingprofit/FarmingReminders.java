package com.farmingprofit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.runelite.api.gameval.ItemID;

/** Pure reminder rules. Unknown crops do not produce yield advice. */
final class FarmingReminders
{
    static final String PAUSED_MESSAGE = "Farming while paused. Resume your run to track it.";
    private static final String[] CROP_SUFFIXES = {" plant", " plants", " bush", " bushes", " hops", " vine", " vines", " coral"};
    static boolean benefitsFromSecateurs(String cropName, boolean herb)
    {
        if (herb) { return true; }
        if (cropName == null) { return false; }
        String name = cropName.toLowerCase(Locale.ENGLISH);
        for (CropCatalog.Crop crop : CropCatalog.CROPS.values())
        {
            if (!(crop.category.equals("Allotment") || crop.category.equals("Hops")
                || crop.category.equals("Bush") || crop.category.equals("Coral")
                || crop.id == ItemID.LIMPWURT_ROOT || crop.id == ItemID.GRAPES)) { continue; }
            String base = crop.name.toLowerCase(Locale.ENGLISH);
            if (name.equals(base) || name.equals(base + "s")) { return true; }
            for (String suffix : CROP_SUFFIXES)
            { if (name.equals(base + suffix)) { return true; } }
        }
        return name.equals("celastrus tree");
    }

    static boolean closedSack(int id)
    { return id == ItemID.SLAYER_HERB_SACK || id == ItemID.SLAYER_HERB_SACK_SILK; }

    static List<String> messages(boolean harvest, boolean herb, boolean benefits, boolean paused,
        boolean secateurs, boolean closedSack, boolean warnSecateurs, boolean warnSack, boolean warnPaused)
    {
        List<String> result = new ArrayList<>(3);
        if (paused && warnPaused) { result.add(PAUSED_MESSAGE); }
        if (harvest && benefits && !secateurs && warnSecateurs)
        { result.add("Magic secateurs missing. Carry or equip a pair for this harvest."); }
        if (harvest && herb && closedSack && warnSack)
        { result.add("Herb sack closed. Open it to store picked herbs."); }
        return List.copyOf(result);
    }
}
