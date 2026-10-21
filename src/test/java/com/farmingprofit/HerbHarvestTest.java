package com.farmingprofit;

import org.junit.Test;
import static org.junit.Assert.*;

public class HerbHarvestTest
{
    @Test public void silentSackCanBeEstimatedFromHarvestXp()
    { assertEquals(1, HerbHarvest.RANARR.inferredCount(27, 1)); }
    @Test public void multiplePicksInOneTick()
    { assertEquals(2, HerbHarvest.RANARR.inferredCount(54, 1)); }
    @Test public void fractionalXpIsTolerated()
    { assertEquals(1, HerbHarvest.SNAPDRAGON.inferredCount(87, 1)); assertEquals(1, HerbHarvest.SNAPDRAGON.inferredCount(88, 1)); }
    @Test public void fullFarmersOutfit()
    { assertEquals(2, HerbHarvest.TORSTOL.inferredCount(409, 1.025)); }
    @Test public void partialFarmersOutfit()
    { assertEquals(1, HerbHarvest.TORSTOL.inferredCount(201, 1.008)); }
    @Test public void rejectUnexplainedXpAndXpCap()
    { assertEquals(0, HerbHarvest.RANARR.inferredCount(0, 1)); assertEquals(0, HerbHarvest.RANARR.inferredCount(41, 1)); }
    @Test public void unknownBonusIsNotGuessed()
    { assertEquals(0, HerbHarvest.RANARR.inferredCount(54, 2)); }
    @Test public void identifiesHarvestablePatchOnly()
    { assertEquals(HerbHarvest.RANARR, HerbHarvest.fromPatchState(36)); assertEquals(HerbHarvest.RANARR, HerbHarvest.fromPatchState(38)); assertNull(HerbHarvest.fromPatchState(35)); assertNull(HerbHarvest.fromPatchState(142)); }
}
