package com.farmingprofit;

import org.junit.Test;
import static org.junit.Assert.*;

public class HerbHarvestTest
{
    @Test public void replaysObservedArdougneRanarrXpIncludingFinalPick()
    {
        // Actual 2026-10-21 trace: eleven regular picks and one 57-XP final pick.
        int[] deltas = {31, 30, 31, 30, 31, 30, 31, 30, 31, 30, 31, 57};
        int total = 0;
        for (int i = 0; i < deltas.length; i++)
        {
            total += HerbHarvest.RANARR.missingCount(deltas[i], 1, 0, i == deltas.length - 1);
        }
        assertEquals(12, total);
    }
    @Test public void finalPickBonusIsNotAnExtraHerb()
    {
        assertEquals(1, HerbHarvest.RANARR.inferredCount(57, 1, true));
        assertEquals(2, HerbHarvest.RANARR.inferredCount(88, 1, true));
        assertEquals(0, HerbHarvest.RANARR.inferredCount(57, 1, false));
        assertEquals(0, HerbHarvest.RANARR.inferredCount(27, 1, true));
        assertEquals(0, HerbHarvest.RANARR.missingCount(57, 1, 1, true));
        assertEquals(1, HerbHarvest.TORSTOL.inferredCount(435, 1.025, true));
    }
    @Test public void menuDismissalAndItemSelectionKeepHarvestContext()
    {
        assertTrue(FarmingProfitPlugin.preservesFarmingContext(net.runelite.api.MenuAction.CANCEL));
        assertTrue(FarmingProfitPlugin.preservesFarmingContext(net.runelite.api.MenuAction.EXAMINE_OBJECT));
        assertTrue(FarmingProfitPlugin.preservesFarmingContext(net.runelite.api.MenuAction.WIDGET_TARGET));
        assertFalse(FarmingProfitPlugin.preservesFarmingContext(net.runelite.api.MenuAction.WALK));
        assertFalse(FarmingProfitPlugin.preservesFarmingContext(net.runelite.api.MenuAction.CC_OP));
        assertFalse(FarmingProfitPlugin.preservesFarmingContext(net.runelite.api.MenuAction.WIDGET_TARGET_ON_GAME_OBJECT));
    }

    @Test public void namedRanarrHarvestIntoSilentSackCountsTenHerbs()
    {
        HerbHarvest herb = HerbHarvest.fromCrop(36, "Ranarr weed");
        assertEquals(HerbHarvest.RANARR, herb);
        int total = 0;
        // Ten picks with no inventory gains and no sack chat messages.
        for (int pick = 0; pick < 10; pick++) { total += herb.missingCount(pick % 2 == 0 ? 31 : 30, 1, 0); }
        assertEquals(10, total);
    }
    @Test public void supportsGenericAndNamedHerbDefinitions()
    {
        assertEquals(HerbHarvest.RANARR, HerbHarvest.fromCrop(38, "Herbs"));
        assertEquals(HerbHarvest.GUAM, HerbHarvest.fromCrop(8, "Guam"));
        assertEquals(HerbHarvest.GUAM, HerbHarvest.fromCrop(8, "Guam leaf"));
        for (HerbHarvest herb : HerbHarvest.values())
        {
            String cropName = CropCatalog.CROPS.get(herb.itemId).name.substring(6);
            assertEquals(herb, HerbHarvest.fromCrop(herb.start, cropName));
        }
    }
    @Test public void rejectsOtherCropsMismatchedNamesAndNonHarvestableStates()
    {
        assertNull(HerbHarvest.fromCrop(36, "Tomato"));
        assertNull(HerbHarvest.fromCrop(36, "Snapdragon"));
        assertNull(HerbHarvest.fromCrop(35, "Ranarr weed"));
        assertNull(HerbHarvest.fromCrop(-1, "Ranarr weed"));
        assertNull(HerbHarvest.fromCrop(36, null));
    }
    @Test public void sackOverflowAndVisibleSignalsAreNotCountedTwice()
    {
        assertEquals(1, HerbHarvest.RANARR.missingCount(61, 1, 1));
        assertEquals(0, HerbHarvest.RANARR.missingCount(61, 1, 2));
        assertEquals(0, HerbHarvest.RANARR.missingCount(31, 1, 2));
    }
    @Test public void silentSackCanBeEstimatedFromHarvestXp()
    { assertEquals(1, HerbHarvest.RANARR.inferredCount(31, 1)); }
    @Test public void multiplePicksInOneTick()
    { assertEquals(2, HerbHarvest.RANARR.inferredCount(61, 1)); }
    @Test public void fractionalXpIsTolerated()
    { assertEquals(1, HerbHarvest.SNAPDRAGON.inferredCount(98, 1)); assertEquals(1, HerbHarvest.SNAPDRAGON.inferredCount(99, 1)); }
    @Test public void fullFarmersOutfit()
    { assertEquals(2, HerbHarvest.TORSTOL.inferredCount(460, 1.025)); }
    @Test public void partialFarmersOutfit()
    { assertEquals(1, HerbHarvest.TORSTOL.inferredCount(226, 1.008)); }
    @Test public void rejectUnexplainedXpAndXpCap()
    { assertEquals(0, HerbHarvest.RANARR.inferredCount(0, 1)); assertEquals(0, HerbHarvest.RANARR.inferredCount(41, 1)); }
    @Test public void unknownBonusIsNotGuessed()
    { assertEquals(0, HerbHarvest.RANARR.inferredCount(61, 2)); }
    @Test public void identifiesHarvestablePatchOnly()
    { assertEquals(HerbHarvest.RANARR, HerbHarvest.fromPatchState(36)); assertEquals(HerbHarvest.RANARR, HerbHarvest.fromPatchState(38)); assertNull(HerbHarvest.fromPatchState(35)); assertNull(HerbHarvest.fromPatchState(142)); }
}
