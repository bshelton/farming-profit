package com.farmingprofit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure reconciliation logic shared by live events and regression tests. */
final class HarvestSignals
{
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
