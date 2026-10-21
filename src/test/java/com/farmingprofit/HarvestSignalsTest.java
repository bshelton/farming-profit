package com.farmingprofit;

import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class HarvestSignalsTest
{
    private static final int HERB = ItemID.UNIDENTIFIED_RANARR;
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
