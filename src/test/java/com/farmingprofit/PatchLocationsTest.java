package com.farmingprofit;
import org.junit.Test;
import static org.junit.Assert.*;

public class PatchLocationsTest
{
    @Test public void namesKnownPatchesAndPreservesUnknownCoordinates()
    {
        assertEquals("Morytania", PatchLocations.location("Ranarr weed @ 3605,3529"));
        assertEquals("Catherby", PatchLocations.location("Herb patch @ 2813,3463"));
        assertEquals("Civitas illa Fortis", PatchLocations.location("Herb patch @ 1581,3094"));
        assertTrue(PatchLocations.display("Tree @ 1,2").contains("Unknown area · 1,2"));
        assertEquals("Travel (run-wide)", PatchLocations.location("Travel"));
        assertEquals("Unassigned / manual", PatchLocations.location("Manual"));
    }
    @Test public void correctionsAndUnassignedCostsReconcileWithRunProfit()
    {
        FarmRun run = new FarmRun(0);
        run.add(1,"Herb",10,100,false,"Ranarr weed @ 3605,3529","Harvest",1);
        run.add(1,"Herb",-1,100,false,"Ranarr weed @ 3605,3529","Correction",2);
        run.add(2,"Compost",1,20,true,"Herb patch @ 3605,3529","Compost",3);
        run.add(3,"Teleport",1,50,true,"Travel","Travel",4);
        run.add(4,"Cost",1,30,true,"Manual","Manual",5);
        RunLocations result = new RunLocations(run);
        assertEquals(1,result.tiles.size());
        assertEquals(1,result.places.size());
        assertEquals(880,result.totals.get("Morytania")[0]-result.totals.get("Morytania")[1]);
        assertEquals(run.profit(),result.totals.values().stream().mapToLong(v -> v[0]-v[1]).sum());
    }
}
