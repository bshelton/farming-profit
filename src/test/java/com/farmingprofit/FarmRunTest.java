package com.farmingprofit;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class FarmRunTest
{
    @Test public void runProfitChargesOnlyCurrentTripSupplies()
    {
        FarmRun run = new FarmRun(1);
        run.add(ItemID.UNIDENTIFIED_RANARR, "Grimy ranarr weed", 72, 3000, false, "Catherby", "Harvest", 2);
        run.add(ItemID.COINS, "Seeds", 6, 8000, true, "Catherby", "Planting", 3);
        run.add(ItemID.BUCKET_ULTRACOMPOST, "Ultracompost", 6, 800, true, "Catherby", "Compost", 4);
        assertEquals(163200, run.profit());
        assertEquals(52800, run.costs());
    }
    @Test public void repeatedFruitHarvestHasNoInventedSaplingCost()
    {
        FarmRun run = new FarmRun(1);
        run.add(ItemID.DRAGONFRUIT, "Dragonfruit", 6, 2000, false, "Tree", "Harvest", 2);
        run.add(ItemID.DRAGONFRUIT, "Dragonfruit", 6, 2100, false, "Tree", "Harvest", 3);
        assertEquals(24600, run.profit()); assertEquals(0, run.costs());
    }
    @Test public void entriesKeepTheirOwnPriceSnapshots()
    {
        FarmRun run = new FarmRun(1);
        run.add(ItemID.CACTUS_SPINE, "Cactus spine", 3, 1000, false, "Cactus", "Harvest", 2);
        run.add(ItemID.CACTUS_SPINE, "Cactus spine", 3, 2000, false, "Cactus", "Harvest", 3);
        assertEquals(9000, run.harvestValue());
    }
    @Test public void correctionIsAuditableAndCanReverseAnOvercount()
    {
        FarmRun run = new FarmRun(1);
        run.add(ItemID.DRAGONFRUIT, "Dragonfruit", 7, 2000, false, "Tree", "Harvest", 2);
        run.add(ItemID.DRAGONFRUIT, "Dragonfruit", -1, 2000, false, "Manual", "Correct observed count", 3);
        assertEquals(12000, run.profit()); assertEquals(2, run.entries.size());
    }
    @Test public void supportsLossMakingTreeRuns()
    {
        FarmRun run = new FarmRun(1);
        run.add(ItemID.COINS, "Sapling", 1, 100000, true, "Tree", "Planting", 2);
        run.farmingXp = 10000;
        assertEquals(-100000, run.profit());
    }
    @Test public void totalsUseLongArithmetic()
    {
        FarmRun run = new FarmRun(1);
        run.add(ItemID.COINS, "Adjustment", 1000000, 2000000000L, false, "Manual", "Test", 2);
        assertEquals(2000000000000000L, run.profit());
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsNegativePrice()
    {
        new FarmRun(1).add(ItemID.COINS, "Bad", 1, -1, false, "Manual", "Bad", 2);
    }
    @Test public void catalogCoversRepeatHarvestsAndExcludesWoodcutting()
    {
        assertTrue(CropCatalog.CROPS.containsKey(ItemID.DRAGONFRUIT));
        assertTrue(CropCatalog.CROPS.containsKey(ItemID.CACTUS_SPINE));
        assertTrue(CropCatalog.CROPS.containsKey(ItemID.CACTUS_POTATO));
        assertTrue(CropCatalog.CROPS.containsKey(ItemID.CELASTRUS_WOOD));
        assertFalse(CropCatalog.CROPS.containsKey(ItemID.MAGIC_LOGS));
        assertFalse(CropCatalog.CROPS.containsKey(ItemID.REDWOOD_LOGS));
    }
    @Test(expected = IllegalStateException.class) public void refusesUnsupportedHistoryVersion()
    {
        FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data(); data.version = 99; data.validate();
    }
    @Test(expected = IllegalStateException.class) public void refusesCorruptHistoryBeforeOverwritingIt()
    {
        FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data(); data.history.add(null); data.validate();
    }
    @Test public void acceptsEmptyHistoryAndAnUnfinishedRun()
    {
        FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data(); data.validate();
        data.active = new FarmRun(1); data.validate();
    }
}
