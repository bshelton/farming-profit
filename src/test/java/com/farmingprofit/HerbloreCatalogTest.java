package com.farmingprofit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.client.plugins.skillcalculator.skills.HerbloreAction;
import org.junit.Test;
import static org.junit.Assert.*;

/** Test-only independent oracle: shipped RuneLite calculator, not a copy of our rates. */
public class HerbloreCatalogTest
{
    @Test public void everyRecipeMatchesRuneLiteCalculator()
    {
        Map<Integer, Integer> expected = new HashMap<>();
        for (HerbloreAction action : HerbloreAction.values())
        { expected.put(action.getItemId(), Math.round(action.getXp() * 10)); }
        Set<Integer> herbs = new HashSet<>();
        for (HerbloreEstimate.Herb herb : HerbloreEstimate.HERBS)
        {
            assertTrue("Duplicate herb " + herb.name, herbs.add(herb.id));
            Set<Integer> recipes = new HashSet<>();
            for (HerbloreEstimate.Recipe recipe : herb.recipes)
            {
                assertTrue("Duplicate choice " + recipe.name, recipes.add(recipe.id));
                assertEquals("Base XP for " + herb.name + " / " + recipe.name,
                    expected.get(recipe.id), Integer.valueOf(recipe.xpTenths));
            }
        }
    }
    @Test public void everyCleaningRateMatchesRuneLiteCalculator()
    {
        Map<String, HerbloreAction> actions = new HashMap<>();
        for (HerbloreAction action : new HerbloreAction[]{HerbloreAction.GUAM_LEAF, HerbloreAction.MARRENTILL,
            HerbloreAction.TARROMIN, HerbloreAction.HARRALANDER, HerbloreAction.RANARR_WEED,
            HerbloreAction.TOADFLAX, HerbloreAction.IRIT_LEAF, HerbloreAction.AVANTOE, HerbloreAction.KWUARM,
            HerbloreAction.HUASCA, HerbloreAction.SNAPDRAGON, HerbloreAction.CADANTINE,
            HerbloreAction.LANTADYME, HerbloreAction.DWARF_WEED, HerbloreAction.TORSTOL})
        { actions.put(action.name().replace("_LEAF", "").replace("RANARR_WEED", "RANARR").replace('_', ' '), action); }
        assertEquals(actions.size(), HerbloreEstimate.HERBS.size());
        for (HerbloreEstimate.Herb herb : HerbloreEstimate.HERBS)
        {
            HerbloreAction action = actions.get(herb.name.toUpperCase(java.util.Locale.ROOT));
            assertNotNull(herb.name, action);
            assertEquals(herb.name, Math.round(action.getXp() * 10), herb.cleaningTenths);
        }
    }
}
