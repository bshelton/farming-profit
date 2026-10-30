package com.farmingprofit;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import net.runelite.api.gameval.ItemID;

/** Derived estimates only: never changes the price ledger or records actual skill XP.
 * Base XP agrees with RuneLite's Herblore skill calculator. Recipes consume one herb;
 * subsequent upgrades and ingredient XP are deliberately not chained together.
 */
final class HerbloreEstimate
{
    static final List<Herb> HERBS = List.of(
        herb(ItemID.UNIDENTIFIED_GUAM, "Guam", 25,
            recipe(ItemID._3DOSE1ATTACK, "Attack potion", 250), recipe(ItemID.SALAMANDER_TAR_GREEN, "Guam tar", 300)),
        herb(ItemID.UNIDENTIFIED_MARENTILL, "Marrentill", 38,
            recipe(ItemID._3DOSEANTIPOISON, "Antipoison", 375), recipe(ItemID.SALAMANDER_TAR_ORANGE, "Marrentill tar", 425)),
        herb(ItemID.UNIDENTIFIED_TARROMIN, "Tarromin", 50,
            recipe(ItemID._3DOSE1STRENGTH, "Strength potion", 500), recipe(ItemID.MORT_SERUM3, "Serum 207", 500), recipe(ItemID.SALAMANDER_TAR_RED, "Tarromin tar", 550)),
        herb(ItemID.UNIDENTIFIED_HARRALANDER, "Harralander", 63,
            recipe(ItemID._3DOSE1ENERGY, "Energy potion", 675), recipe(ItemID._3DOSESTATRESTORE, "Restore potion", 625),
            recipe(ItemID.SUPERCOMPOST_POTION_3, "Compost potion", 600), recipe(ItemID._3DOSECOMBAT, "Combat potion", 840), recipe(ItemID._3DOSEGOADING, "Goading potion", 1320), recipe(ItemID.SALAMANDER_TAR_BLACK, "Harralander tar", 725)),
        herb(ItemID.UNIDENTIFIED_RANARR, "Ranarr", 75,
            recipe(ItemID._3DOSEPRAYERRESTORE, "Prayer potion", 875), recipe(ItemID._3DOSE1DEFENSE, "Defence potion", 750)),
        herb(ItemID.UNIDENTIFIED_TOADFLAX, "Toadflax", 80,
            recipe(ItemID._3DOSEPOTIONOFSARADOMIN, "Saradomin brew", 1800), recipe(ItemID._3DOSE1AGILITY, "Agility potion", 800), recipe(ItemID.ANTIDOTE_4, "Antidote+", 1550)),
        herb(ItemID.UNIDENTIFIED_IRIT, "Irit", 88,
            recipe(ItemID._3DOSE2ATTACK, "Super attack", 1000), recipe(ItemID._3DOSE2ANTIPOISON, "Superantipoison", 1063), recipe(ItemID.SALAMANDER_TAR_MOUNTAIN, "Irit tar", 850), recipe(ItemID.ANTIDOTE__4, "Antidote++", 1775)),
        herb(ItemID.UNIDENTIFIED_AVANTOE, "Avantoe", 100,
            recipe(ItemID._3DOSE2ENERGY, "Super energy", 1175), recipe(ItemID._3DOSEFISHERSPOTION, "Fishing potion", 1125), recipe(ItemID._3DOSEHUNTING, "Hunter potion", 1200)),
        herb(ItemID.UNIDENTIFIED_KWUARM, "Kwuarm", 113,
            recipe(ItemID._3DOSE2STRENGTH, "Super strength", 1250), recipe(ItemID.WEAPON_POISON, "Weapon poison", 1375)),
        herb(ItemID.UNIDENTIFIED_HUASCA, "Huasca", 118,
            recipe(ItemID._3DOSE1PRAYER_REGENERATION, "Prayer regeneration", 1320)),
        herb(ItemID.UNIDENTIFIED_SNAPDRAGON, "Snapdragon", 118,
            recipe(ItemID._3DOSE2RESTORE, "Super restore", 1425)),
        herb(ItemID.UNIDENTIFIED_CADANTINE, "Cadantine", 125,
            recipe(ItemID._3DOSE2DEFENSE, "Super defence", 1500), recipe(ItemID._3DOSEBASTION, "Bastion potion", 1550)),
        herb(ItemID.UNIDENTIFIED_LANTADYME, "Lantadyme", 131,
            recipe(ItemID._3DOSE1ANTIDRAGON, "Antifire potion", 1575), recipe(ItemID._3DOSE1MAGIC, "Magic potion", 1725)),
        herb(ItemID.UNIDENTIFIED_DWARF_WEED, "Dwarf weed", 138,
            recipe(ItemID._3DOSERANGERSPOTION, "Ranging potion", 1625), recipe(ItemID._3DOSESTATRENEWAL, "Menaphite remedy", 2000)),
        herb(ItemID.UNIDENTIFIED_TORSTOL, "Torstol", 150,
            recipe(ItemID._4DOSE2COMBAT, "Super combat potion", 1500), recipe(ItemID._3DOSEPOTIONOFZAMORAK, "Zamorak brew", 1750),
            recipe(ItemID._3DOSESURGE, "Surge potion", 1850))
    );
    private static final Map<Integer, Herb> BY_ID = new HashMap<>();
    static { for (Herb herb : HERBS) { BY_ID.put(herb.id, herb); } }
    static Herb herb(int id) { return BY_ID.get(id); }
    private static Herb herb(int id, String name, int cleaning, Recipe... recipes)
    { return new Herb(id, name, cleaning, List.of(recipes)); }
    private static Recipe recipe(int id, String name, int xp) { return new Recipe(id, name, xp); }

    static final class Herb
    {
        final int id, cleaningTenths;
        final String name;
        final List<Recipe> recipes;
        Herb(int id, String name, int cleaning, List<Recipe> recipes)
        { this.id = id; this.name = name; cleaningTenths = cleaning; this.recipes = recipes; }
    }
    static final class Recipe
    {
        final int id, xpTenths;
        final String name;
        Recipe(int id, String name, int xp) { this.id = id; this.name = name; xpTenths = xp; }
        public String toString() { return name; }
    }
    static final class Choices
    {
        final boolean cleaning;
        private final Map<Integer, Integer> recipes;
        Choices(boolean cleaning, String encoded)
        {
            this.cleaning = cleaning;
            Map<Integer, Integer> parsed = new HashMap<>();
            if (encoded != null && encoded.length() <= 4096)
            {
                for (String pair : encoded.split(";"))
                {
                    String[] parts = pair.split(":");
                    if (parts.length != 2) { continue; }
                    try
                    {
                        int herbId = Integer.parseInt(parts[0]), recipeId = Integer.parseInt(parts[1]);
                        Herb herb = herb(herbId);
                        if (herb != null && herb.recipes.stream().anyMatch(r -> r.id == recipeId))
                        { parsed.put(herbId, recipeId); }
                    }
                    catch (NumberFormatException ignored) { /* Invalid old settings use defaults. */ }
                }
            }
            recipes = Collections.unmodifiableMap(parsed);
        }
        Recipe recipe(Herb herb)
        {
            int selected = recipes.getOrDefault(herb.id, herb.recipes.get(0).id);
            return herb.recipes.stream().filter(r -> r.id == selected).findFirst().orElse(herb.recipes.get(0));
        }
        String encode()
        {
            StringJoiner result = new StringJoiner(";");
            for (Herb herb : HERBS) { result.add(herb.id + ":" + recipe(herb).id); }
            return result.toString();
        }
        @Override public boolean equals(Object other)
        { return other instanceof Choices && cleaning == ((Choices) other).cleaning && encode().equals(((Choices) other).encode()); }
        @Override public int hashCode() { return Objects.hash(cleaning, encode()); }
    }
    static final class Row
    {
        final Herb herb;
        final Recipe recipe;
        final long quantity, potionTenths, cleaningTenths;
        Row(Herb herb, Recipe recipe, long quantity, boolean cleaning)
        {
            this.herb = herb; this.recipe = recipe; this.quantity = quantity;
            potionTenths = quantity * recipe.xpTenths;
            cleaningTenths = cleaning ? quantity * herb.cleaningTenths : 0;
        }
        long totalTenths() { return potionTenths + cleaningTenths; }
    }
    final List<Row> rows;
    final long potionTenths, cleaningTenths, quantity;
    HerbloreEstimate(FarmRun run, Choices choices)
    {
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (FarmRun.Entry entry : run.entries)
        {
            if (!entry.cost && herb(entry.itemId) != null)
            { counts.merge(entry.itemId, (long) entry.quantity, Long::sum); }
        }
        List<Row> result = new ArrayList<>();
        counts.forEach((id, count) -> { if (count != 0) { Herb herb = herb(id); result.add(new Row(herb, choices.recipe(herb), count, choices.cleaning)); } });
        rows = Collections.unmodifiableList(result);
        potionTenths = rows.stream().mapToLong(r -> r.potionTenths).sum();
        cleaningTenths = rows.stream().mapToLong(r -> r.cleaningTenths).sum();
        quantity = rows.stream().mapToLong(r -> r.quantity).sum();
    }
    long totalTenths() { return potionTenths + cleaningTenths; }
    static String format(long tenths)
    {
        java.text.NumberFormat format = java.text.NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(1);
        return format.format(tenths / 10.0) + " XP";
    }
    enum Period
    {
        TODAY("Today"), WEEK("Week"), MONTH("Month"), ALL("All");
        final String label;
        Period(String label) { this.label = label; }
        boolean includes(FarmRun run, LocalDate today, ZoneId zone)
        {
            if (this == ALL) { return true; }
            LocalDate start = this == TODAY ? today : this == WEEK
                ? today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : today.withDayOfMonth(1);
            LocalDate day = Instant.ofEpochMilli(run.startedAt).atZone(zone).toLocalDate();
            return !day.isBefore(start) && !day.isAfter(today);
        }
    }
}
