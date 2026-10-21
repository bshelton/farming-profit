package com.farmingprofit;

import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class HarvestSignalsTest
{
    private static final int HERB = ItemID.UNIDENTIFIED_RANARR;
    @Test public void plantingCostDoesNotRequireImmediateXp()
    {
        String action = HarvestSignals.patchAction("use", "Ranarr seed -> Herb patch");
        assertEquals("plant", action);
        assertTrue(HarvestSignals.isSupplyCost(action, "Ranarr seed"));
        assertTrue(HarvestSignals.isSupplyCost(HarvestSignals.patchAction("use", "Dragonfruit sapling -> Tree patch"), "Dragonfruit sapling"));
        assertFalse(HarvestSignals.isSupplyCost(action, "Grimy ranarr weed"));
    }
    @Test public void compostAndMaintenanceAreNotPlanting()
    {
        for (String target : new String[]{"Bottomless compost bucket -> Herb patch", "Ultracompost -> Herb patch", "Spade -> Ranarr weed"})
        {
            assertEquals("tend", HarvestSignals.patchAction("use", target));
            assertFalse(HarvestSignals.isSupplyCost(HarvestSignals.patchAction("use", target), "Ranarr seed"));
        }
        assertEquals("tend", HarvestSignals.patchAction("rake", "Herb patch"));
        assertEquals("health", HarvestSignals.patchAction("check-health", "Magic tree"));
        assertEquals("harvest", HarvestSignals.patchAction("pick", "Dragonfruit tree"));
    }
    @Test public void checkingSackIsPassiveButEmptyingAndFillingAreNot()
    {
        for (int id : new int[]{ItemID.SLAYER_HERB_SACK, ItemID.SLAYER_HERB_SACK_OPEN,
            ItemID.SLAYER_HERB_SACK_SILK, ItemID.SLAYER_HERB_SACK_SILK_OPEN})
        {
            assertTrue(HarvestSignals.isSackCheck(net.runelite.api.MenuAction.CC_OP, id, "check"));
            assertFalse(HarvestSignals.isSackCheck(net.runelite.api.MenuAction.CC_OP, id, "empty"));
            assertFalse(HarvestSignals.isSackCheck(net.runelite.api.MenuAction.CC_OP, id, "fill"));
        }
        assertFalse(HarvestSignals.isSackCheck(net.runelite.api.MenuAction.CC_OP, ItemID.SEED_BOX, "check"));
    }
    @Test public void coralCollectionAndFragmentPlantingAreIncluded()
    {
        assertEquals("harvest", HarvestSignals.patchAction("collect", "Elkhorn coral"));
        assertEquals("plant", HarvestSignals.patchAction("use", "Elkhorn frag -> Coral nursery"));
        assertTrue(HarvestSignals.isSupplyCost("plant", "Elkhorn frag"));
        assertTrue(HarvestSignals.isSupplyCost("plant", "Pillar frag"));
        assertTrue(HarvestSignals.isSupplyCost("plant", "Umbral frag"));
        assertFalse(HarvestSignals.isSupplyCost("plant", "Elkhorn coral"));
    }
    @Test public void directToSackDoesNotNeedInventoryChange()
    {
        assertEquals(Integer.valueOf(2), HarvestSignals.harvest(Map.of(), Map.of(), Map.of(HERB, 2), true, true).get(HERB));
    }
    @Test public void fullSackCanSpillIntoInventoryInSameTick()
    {
        assertEquals(Integer.valueOf(3), HarvestSignals.harvest(Map.of(HERB, 10), Map.of(HERB, 11), Map.of(HERB, 2), true, true).get(HERB));
    }
    @Test public void existingInventoryIsNotAHarvest()
    {
        assertTrue(HarvestSignals.harvest(Map.of(HERB, 20), Map.of(HERB, 20), Map.of(), true, true).isEmpty());
    }
    @Test public void emptyingSackAndBankWithdrawalsDoNotCountOutsideHarvest()
    {
        assertTrue(HarvestSignals.harvest(Map.of(), Map.of(HERB, 30), Map.of(), false, true).isEmpty());
    }
    @Test public void inventoryGainWithoutFarmingEvidenceIsIgnored()
    {
        assertTrue(HarvestSignals.harvest(Map.of(), Map.of(HERB, 30), Map.of(), true, false).isEmpty());
    }
    @Test public void normalizedNotingTransferHasNoNetGain()
    {
        assertTrue(HarvestSignals.harvest(Map.of(HERB, 20), Map.of(HERB, 20), Map.of(), false, false).isEmpty());
    }
    @Test public void harvestingExistingDragonfruitDoesNotRequirePlanting()
    {
        assertEquals(Integer.valueOf(6), HarvestSignals.harvest(Map.of(), Map.of(ItemID.DRAGONFRUIT, 6), Map.of(), true, true).get(ItemID.DRAGONFRUIT));
    }
    @Test public void bankingProduceIsNotNegativeHarvest()
    {
        assertTrue(HarvestSignals.harvest(Map.of(HERB, 30), Map.of(), Map.of(), true, true).isEmpty());
    }
    @Test public void parsesObservedSackMessageIncludingMultiwordHerb()
    {
        assertEquals("Grimy dwarf weed", HarvestSignals.sackHerb("You put the Grimy dwarf weed herb into your herb sack."));
        assertEquals(Integer.valueOf(ItemID.UNIDENTIFIED_DWARF_WEED), CropCatalog.herbId("Grimy dwarf weed"));
        assertEquals(Integer.valueOf(ItemID.UNIDENTIFIED_RANARR), CropCatalog.herbId("Grimy ranarr weed"));
        assertEquals(Integer.valueOf(ItemID.UNIDENTIFIED_GUAM), CropCatalog.herbId("Grimy guam leaf"));
        assertEquals(Integer.valueOf(ItemID.UNIDENTIFIED_IRIT), CropCatalog.herbId("Grimy irit leaf"));
    }
    @Test public void sackCheckAndFullMessagesAreNotHarvests()
    {
        assertNull(HarvestSignals.sackHerb("30 x Grimy ranarr weed"));
        assertNull(HarvestSignals.sackHerb("Your herb sack is full."));
        assertNull(HarvestSignals.sackHerb("You look in your herb sack and see:"));
    }
    @Test public void nonFarmingSackPickupDoesNotCount()
    {
        assertTrue(HarvestSignals.harvest(Map.of(), Map.of(), Map.of(HERB, 1), false, true).isEmpty());
    }
}
