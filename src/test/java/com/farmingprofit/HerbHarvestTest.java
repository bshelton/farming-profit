package com.farmingprofit;

import org.junit.Test;
import static org.junit.Assert.*;

public class HerbHarvestTest
{
    @Test public void namedRanarrHarvestIntoSilentSackCountsTenHerbs()
    {
        HerbHarvest herb = HerbHarvest.fromCrop(36, "Ranarr weed");
        assertEquals(HerbHarvest.RANARR, herb);
        int total = 0;
        // Ten picks with no inventory gains and no sack chat messages.
        for (int pick = 0; pick < 10; pick++) { total += herb.missingCount(27, 1, 0); }
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
        assertEquals(1, HerbHarvest.RANARR.missingCount(54, 1, 1));
        assertEquals(0, HerbHarvest.RANARR.missingCount(54, 1, 2));
        assertEquals(0, HerbHarvest.RANARR.missingCount(27, 1, 2));
    }
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
