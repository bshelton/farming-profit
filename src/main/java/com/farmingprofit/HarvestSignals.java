package com.farmingprofit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.MenuAction;
import net.runelite.api.gameval.ItemID;

/** Pure reconciliation logic shared by live events and regression tests. */
final class HarvestSignals
{
    static String patchAction(String option, String target)
    {
        if (option.startsWith("pick") || option.equals("harvest") || option.equals("collect")
            || option.equals("chop-down") || option.equals("cut"))
        { return "harvest"; }
        if (option.equals("check-health")) { return "health"; }
        int arrow = target.indexOf(" -> ");
        if (option.startsWith("plant") || option.equals("use") && arrow >= 0
            && CropCatalog.plantingSupply(target.substring(0, arrow))) { return "plant"; }
        return "tend";
    }

    static boolean isSupplyCost(String action, String itemName)
    {
        // Planting consumes supplies now, even when its XP is deferred until harvest/check-health.
        return action.equals("pay") || action.equals("plant") && CropCatalog.plantingSupply(itemName);
    }

    static boolean isSackCheck(MenuAction type, int item, String option)
    {
        return (type == MenuAction.CC_OP || type == MenuAction.CC_OP_LOW_PRIORITY) && option.equals("check")
            && (item == ItemID.SLAYER_HERB_SACK || item == ItemID.SLAYER_HERB_SACK_OPEN
                || item == ItemID.SLAYER_HERB_SACK_SILK || item == ItemID.SLAYER_HERB_SACK_SILK_OPEN);
    }

    private static final Pattern SACK = Pattern.compile(
        "^You put the (Grimy [A-Za-z ]+) herb into your herb sack\\.$", Pattern.CASE_INSENSITIVE);

    static String sackHerb(String text)
    {
        Matcher matcher = SACK.matcher(text);
        return matcher.matches() ? matcher.group(1) : null;
    }

    static Map<Integer, Integer> harvest(Map<Integer, Integer> before, Map<Integer, Integer> after,
        Map<Integer, Integer> sack, boolean harvesting, boolean farmingEvidence)
    {
        if (!harvesting || !farmingEvidence) { return Collections.emptyMap(); }
        Map<Integer, Integer> result = new HashMap<>(sack);
        after.forEach((id, count) -> {
            int gained = count - before.getOrDefault(id, 0);
            if (gained > 0) { result.merge(id, gained, Integer::sum); }
        });
        return result;
    }
}
