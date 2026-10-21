package com.farmingprofit;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/** Explicit harvest allowlist, including repeat-harvest crops. Logs are excluded. */
final class CropCatalog
{
    static final Map<Integer, Crop> CROPS = new LinkedHashMap<>();
    static
    {
        add(ItemID.POTATO, "Potato", "Allotment");
        add(ItemID.ONION, "Onion", "Allotment");
        add(ItemID.CABBAGE, "Cabbage", "Allotment");
        add(ItemID.TOMATO, "Tomato", "Allotment");
        add(ItemID.SWEETCORN, "Sweetcorn", "Allotment");
        add(ItemID.STRAWBERRY, "Strawberry", "Allotment");
        add(ItemID.WATERMELON, "Watermelon", "Allotment");
        add(ItemID.SNAPE_GRASS, "Snape grass", "Allotment");
        add(ItemID.MARIGOLD, "Marigold", "Flower");
        add(ItemID.ROSEMARY, "Rosemary", "Flower");
        add(ItemID.NASTURTIUM, "Nasturtium", "Flower");
        add(ItemID.WOADLEAF, "Woad", "Flower");
        add(ItemID.LIMPWURT_ROOT, "Limpwurt", "Flower");
        add(ItemID.WHITELILLY, "White lily", "Flower");
        add(ItemID.REDBERRIES, "Redberry", "Bush");
        add(ItemID.CADAVABERRIES, "Cadavaberry", "Bush");
        add(ItemID.DWELLBERRIES, "Dwellberry", "Bush");
        add(ItemID.JANGERBERRIES, "Jangerberry", "Bush");
        add(ItemID.WHITE_BERRIES, "Whiteberry", "Bush");
        add(ItemID.POISONIVY_BERRIES, "Poison ivy", "Bush");
        add(ItemID.BARLEY, "Barley", "Hops");
        add(ItemID.HAMMERSTONE_HOPS, "Hammerstone", "Hops");
        add(ItemID.ASGARNIAN_HOPS, "Asgarnian", "Hops");
        add(ItemID.JUTE_FIBRE, "Jute", "Hops");
        add(ItemID.YANILLIAN_HOPS, "Yanillian", "Hops");
        add(ItemID.FLAX, "Flax", "Hops");
        add(ItemID.KRANDORIAN_HOPS, "Krandorian", "Hops");
        add(ItemID.WILDBLOOD_HOPS, "Wildblood", "Hops");
        add(ItemID.HEMP, "Hemp", "Hops");
        add(ItemID.COTTON_BOLL, "Cotton", "Hops");
        add(ItemID.UNIDENTIFIED_GUAM, "Grimy guam leaf", "Herb");
        add(ItemID.UNIDENTIFIED_MARENTILL, "Grimy marrentill", "Herb");
        add(ItemID.UNIDENTIFIED_TARROMIN, "Grimy tarromin", "Herb");
        add(ItemID.UNIDENTIFIED_HARRALANDER, "Grimy harralander", "Herb");
        add(ItemID.UNIDENTIFIED_RANARR, "Grimy ranarr weed", "Herb");
        add(ItemID.UNIDENTIFIED_TOADFLAX, "Grimy toadflax", "Herb");
        add(ItemID.UNIDENTIFIED_IRIT, "Grimy irit leaf", "Herb");
        add(ItemID.UNIDENTIFIED_AVANTOE, "Grimy avantoe", "Herb");
        add(ItemID.UNIDENTIFIED_KWUARM, "Grimy kwuarm", "Herb");
        add(ItemID.UNIDENTIFIED_HUASCA, "Grimy huasca", "Herb");
        add(ItemID.UNIDENTIFIED_SNAPDRAGON, "Grimy snapdragon", "Herb");
        add(ItemID.UNIDENTIFIED_CADANTINE, "Grimy cadantine", "Herb");
        add(ItemID.UNIDENTIFIED_LANTADYME, "Grimy lantadyme", "Herb");
        add(ItemID.UNIDENTIFIED_DWARF_WEED, "Grimy dwarf weed", "Herb");
        add(ItemID.UNIDENTIFIED_TORSTOL, "Grimy torstol", "Herb");
        add(ItemID.EADGAR_GOUTWEED_HERB, "Goutweed", "Herb");
        add(ItemID.COOKING_APPLE, "Apple", "Fruit Tree");
        add(ItemID.BANANA, "Banana", "Fruit Tree");
        add(ItemID.ORANGE, "Orange", "Fruit Tree");
        add(ItemID.CURRY_LEAF, "Curry", "Fruit Tree");
        add(ItemID.PINEAPPLE, "Pineapple", "Fruit Tree");
        add(ItemID.PAPAYA, "Papaya", "Fruit Tree");
        add(ItemID.COCONUT, "Palm", "Fruit Tree");
        add(ItemID.DRAGONFRUIT, "Dragonfruit", "Fruit Tree");
        add(ItemID.CACTUS_SPINE, "Cactus", "Cactus");
        add(ItemID.CACTUS_POTATO, "Potato cactus", "Cactus");
        add(ItemID.CORAL_ELKHORN, "Elkhorn", "Coral");
        add(ItemID.CORAL_PILLAR, "Pillar", "Coral");
        add(ItemID.CORAL_UMBRAL, "Umbral", "Coral");
        add(ItemID.GIANT_SEAWEED, "Seaweed", "Seaweed");
        add(ItemID.GRAPES, "Grape", "Grapes");
        add(ItemID.ZAMORAK_GRAPES, "Zamorak's grapes", "Grapes");
        add(ItemID.BITTERCAP_MUSHROOM, "Mushroom", "Mushroom");
        add(ItemID.NIGHTSHADE, "Belladonna", "Belladonna");
        add(ItemID.CALQUAT_FRUIT, "Calquat", "Calquat");
        add(ItemID.CELASTRUS_WOOD, "Celastrus bark", "Celastrus");
        add(ItemID.PRIF_CRYSTAL_SHARD, "Crystal tree", "Crystal Tree");
    }
    private static void add(int id, String name, String category) { CROPS.put(id, new Crop(id, name, category)); }
    static Integer herbId(String name)
    {
        return CROPS.values().stream().filter(c -> c.name.equalsIgnoreCase(name)).map(c -> c.id).findFirst().orElse(null);
    }
    static boolean plantingSupply(String name)
    {
        String n = name.toLowerCase(Locale.ENGLISH);
        return n.endsWith(" seed") || n.endsWith(" seeds") || n.endsWith(" sapling") || n.equals("gout tuber") || n.equals("mushroom spore") || n.equals("seaweed spore") || n.equals("crystal acorn")
            || n.equals("elkhorn frag") || n.equals("pillar frag") || n.equals("umbral frag");
    }
    static final class Crop
    {
        final int id;
        final String name;
        final String category;
        Crop(int id, String name, String category) { this.id=id; this.name=name; this.category=category; }
        public String toString() { return name + " (" + category + ")"; }
    }
}
