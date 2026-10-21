package com.farmingprofit;

import org.junit.Test;
import static org.junit.Assert.*;

public class HerbHarvestTest
{
    @Test public void partialAndFullOutfitsUseBothBodyTypeItemIds()
    {
        assertEquals(1, HerbHarvest.outfitMultiplier(new int[]{}), .00001);
        assertEquals(1.008, HerbHarvest.outfitMultiplier(new int[]{net.runelite.api.gameval.ItemID.TITHE_REWARD_TORSO_FEMALE}), .00001);
        assertEquals(1.025, HerbHarvest.outfitMultiplier(new int[]{
            net.runelite.api.gameval.ItemID.TITHE_REWARD_HAT_MALE,
            net.runelite.api.gameval.ItemID.TITHE_REWARD_TORSO_MALE,
            net.runelite.api.gameval.ItemID.TITHE_REWARD_LEGS_MALE,
            net.runelite.api.gameval.ItemID.TITHE_REWARD_FEET_MALE}), .00001);
        assertEquals(1.025, HerbHarvest.outfitMultiplier(new int[]{
            net.runelite.api.gameval.ItemID.TITHE_REWARD_HAT_FEMALE,
            net.runelite.api.gameval.ItemID.TITHE_REWARD_TORSO_FEMALE,
            net.runelite.api.gameval.ItemID.TITHE_REWARD_LEGS_FEMALE,
            net.runelite.api.gameval.ItemID.TITHE_REWARD_FEET_FEMALE}), .00001);
    }
    @Test public void boostedDoublePickAndFinalPickRoundCorrectly()
    {
        assertEquals(2, HerbHarvest.RANARR.inferredCount(69, 1.025, false, 1.1));
        assertEquals(1, HerbHarvest.RANARR.inferredCount(62, 1.025, true, 1.1));
        assertEquals(1, HerbHarvest.SNAPDRAGON.inferredCount(201, 1.025, true, 1.1));
        assertEquals(0, HerbHarvest.RANARR.inferredCount(0, 1.025, false, 1.1));
    }
    @Test public void allLayoutsKeepTabNavigationRegardlessOfLabel()
    {
        for (int widget : new int[]{net.runelite.api.gameval.InterfaceID.Toplevel.STONE2,
            net.runelite.api.gameval.InterfaceID.ToplevelOsrsStretch.ICON4,
            net.runelite.api.gameval.InterfaceID.ToplevelPreEoc.STONE10,
            net.runelite.api.gameval.InterfaceID.ToplevelOsm.STONE3})
        { assertTrue(FarmingProfitPlugin.preservesFarmingContext(net.runelite.api.MenuAction.CC_OP, widget, "different tab label")); }
        assertTrue(FarmingProfitPlugin.preservesFarmingContext(net.runelite.api.MenuAction.GAME_OBJECT_SECOND_OPTION, 0, "inspect"));
        assertFalse(FarmingProfitPlugin.preservesFarmingContext(net.runelite.api.MenuAction.CC_OP,
            net.runelite.api.gameval.InterfaceID.ToplevelOsrsStretch.MINIMAP, "inventory"));
        assertFalse(FarmingProfitPlugin.preservesFarmingContext(net.runelite.api.MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, 0, "use"));
    }
    @Test public void replaysConfirmedThirteenHerbFaladorHarvest()
    {
        // 2026-10-21 live trace, verified by the user's sack increase of 13.
        int[] deltas = {33, 34, 33, 34, 33, 34, 33, 34, 33, 34, 33, 34, 60};
        double bonus = HerbHarvest.patchMultiplier(new net.runelite.api.coords.WorldPoint(3058, 3311, 0), true);
        int total = 0;
        for (int i = 0; i < deltas.length; i++)
        {
            total += HerbHarvest.RANARR.missingCount(deltas[i], 1, 0, i == deltas.length - 1, bonus);
        }
        assertEquals(13, total);
        // The completion bonus is not multiplied by the Falador harvest bonus.
        assertEquals(1, HerbHarvest.RANARR.inferredCount(60, 1, true, bonus));
        assertEquals(0, HerbHarvest.RANARR.missingCount(60, 1, 1, true, bonus));
        assertEquals(2, HerbHarvest.RANARR.inferredCount(67, 1, false, bonus));
    }
    @Test public void faladorBonusRequiresBothLocationAndDiary()
    {
        net.runelite.api.coords.WorldPoint falador = new net.runelite.api.coords.WorldPoint(3058, 3311, 0);
        assertEquals(1, HerbHarvest.patchMultiplier(falador, false), 0);
        assertEquals(1, HerbHarvest.patchMultiplier(new net.runelite.api.coords.WorldPoint(2670, 3374, 0), true), 0);
        assertEquals(1, HerbHarvest.patchMultiplier(null, true), 0);
        assertEquals(0, HerbHarvest.RANARR.inferredCount(34, 1, false, 1));
    }
    @Test public void inventoryTabKeepsContextButItemActionsDoNot()
    {
        assertTrue(FarmingProfitPlugin.preservesFarmingContext(net.runelite.api.MenuAction.CC_OP,
            net.runelite.api.gameval.InterfaceID.Toplevel.STONE3, "inventory"));
        assertTrue(FarmingProfitPlugin.preservesFarmingContext(net.runelite.api.MenuAction.CC_OP,
            net.runelite.api.gameval.InterfaceID.ToplevelOsrsStretch.STONE3, "inventory"));
        assertFalse(FarmingProfitPlugin.preservesFarmingContext(net.runelite.api.MenuAction.CC_OP,
            net.runelite.api.gameval.InterfaceID.Inventory.ITEMS, "teleport"));
        assertFalse(FarmingProfitPlugin.preservesFarmingContext(net.runelite.api.MenuAction.CC_OP,
            net.runelite.api.gameval.InterfaceID.Inventory.ITEMS, "inventory"));
    }

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
