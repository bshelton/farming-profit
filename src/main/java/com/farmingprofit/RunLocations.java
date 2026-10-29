package com.farmingprofit;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Derived summaries work with old history without changing the storage format. */
final class RunLocations
{
    final Map<String, long[]> totals = new LinkedHashMap<>();
    final Set<String> tiles = new LinkedHashSet<>();
    final Set<String> places = new LinkedHashSet<>();
    RunLocations(FarmRun run)
    {
        for (FarmRun.Entry entry : run.entries)
        {
            String location = PatchLocations.location(entry.patch);
            long[] values = totals.computeIfAbsent(location, key -> new long[2]);
            values[entry.cost ? 1 : 0] += entry.unitPrice * entry.quantity;
            String tile = PatchLocations.tile(entry.patch);
            if (!tile.isEmpty()) { tiles.add(tile); places.add(location); }
        }
    }
    String shortPlaces()
    {
        if (places.isEmpty()) { return "No recorded patch locations"; }
        String first = places.iterator().next();
        return first + (places.size() > 1 ? " +" + (places.size() - 1) + " more" : "");
    }
    static String elapsed(FarmRun run)
    {
        long end = run.finishedAt > 0 ? run.finishedAt : run.lastActivity;
        long minutes = Math.max(0, end - run.startedAt) / 60000;
        return minutes < 1 ? "<1 min elapsed" : minutes + " min elapsed";
    }
}
