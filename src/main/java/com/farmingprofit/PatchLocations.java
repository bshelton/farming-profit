package com.farmingprofit;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Presentation-only lookup; raw saved patch strings remain unchanged. */
final class PatchLocations
{
    private static final Pattern TILE = Pattern.compile(" @ (\\d{1,5}),(\\d{1,5})$");
    private static final Map<Integer, String> REGIONS = regions();
    private static Map<Integer, String> regions()
    {
        Map<Integer, String> regions = new HashMap<>();
        // Primary farming-region names/IDs from RuneLite FarmingWorld (not varbit visibility aliases).
        // https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/timetracking/farming/FarmingWorld.java
        regions.put(13106, "Al Kharid");
        regions.put(5421, "Aldarin");
        regions.put(9770, "Anglers' Retreat");
        regions.put(10290, "Ardougne");
        regions.put(10548, "Ardougne");
        regions.put(5427, "Auburnvale");
        regions.put(6702, "Avium Savannah");
        regions.put(11058, "Brimhaven");
        regions.put(11062, "Catherby");
        regions.put(11317, "Catherby");
        regions.put(6192, "Civitas illa Fortis");
        regions.put(12596, "Champions' Guild");
        regions.put(12340, "Draynor Manor");
        regions.put(11060, "Entrana");
        regions.put(10300, "Etceteria");
        regions.put(11828, "Falador");
        regions.put(12083, "Falador");
        regions.put(14651, "Fossil Island");
        regions.put(15008, "Fossil Island (underwater)");
        regions.put(9781, "Gnome Stronghold");
        regions.put(12581, "Great Conch");
        regions.put(15148, "Harmony Island");
        regions.put(5423, "Kastori");
        regions.put(6967, "Hosidius");
        regions.put(7223, "Hosidius");
        regions.put(9265, "Lletya");
        regions.put(12851, "Lumbridge");
        regions.put(12594, "Lumbridge");
        regions.put(13622, "Morytania");
        regions.put(14391, "Morytania");
        regions.put(12082, "Port Sarim");
        regions.put(11570, "Rimmington");
        regions.put(10551, "Seers' Village");
        regions.put(11056, "Tai Bwo Wannai");
        regions.put(11573, "Taverley");
        regions.put(9777, "Tree Gnome Village");
        regions.put(11321, "Troll Stronghold");
        regions.put(12854, "Varrock");
        regions.put(10288, "Yanille");
        regions.put(11325, "Weiss");
        regions.put(5021, "Farming Guild");
        regions.put(4922, "Farming Guild");
        regions.put(13151, "Prifddinas");
        return java.util.Collections.unmodifiableMap(regions);
    }
    static String tile(String patch)
    {
        Matcher matcher = TILE.matcher(patch);
        return matcher.find() ? matcher.group(1) + "," + matcher.group(2) : "";
    }
    static String location(String patch)
    {
        Matcher matcher = TILE.matcher(patch);
        if (!matcher.find()) { return "Travel".equals(patch) ? "Travel (run-wide)" : "Unassigned / manual"; }
        int x = Integer.parseInt(matcher.group(1)), y = Integer.parseInt(matcher.group(2));
        String name = REGIONS.get(((x >> 6) << 8) | (y >> 6));
        return name == null ? "Unknown area · " + x + "," + y : name;
    }
    static String display(String patch)
    {
        Matcher matcher = TILE.matcher(patch);
        if (!matcher.find()) { return patch.isEmpty() ? "Unassigned" : patch; }
        return location(patch) + " · " + patch.substring(0, matcher.start()) + " · Tile " + tile(patch);
    }
    private PatchLocations() {}
}
