package com.farmingprofit;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class TravelCostsTest
{
    @Test public void basaltConsumptionIsChargedOnce()
    {
        for (int id : new int[]{ItemID.WEISS_TELEPORT_BASALT, ItemID.STRONGHOLD_TELEPORT_BASALT})
        {
            TravelCosts costs = new TravelCosts();
            costs.begin(id, "teleport", 10, 0);
            assertEquals(-1, costs.consumed(10, 500));
            assertEquals(id, costs.consumed(9, 1000));
            assertEquals(-1, costs.consumed(9, 1500));
        }
    }

    @Test public void tabletsAndScrollsCountActualConsumption()
    {
        TravelCosts costs = new TravelCosts();
        costs.begin(ItemID.POH_TABLET_TELEPORTTOHOUSE, "outside", 1, 0);
        assertEquals(ItemID.POH_TABLET_TELEPORTTOHOUSE, costs.consumed(0, 1000));
        costs.begin(ItemID.POH_TABLET_CAMELOTTELEPORT, "break", 3, 0);
        assertEquals(ItemID.POH_TABLET_CAMELOTTELEPORT, costs.consumed(2, 1000));
        costs.begin(ItemID.TELEPORTSCROLL_PISCATORIS, "teleport", 2, 0);
        assertEquals(ItemID.TELEPORTSCROLL_PISCATORIS, costs.consumed(1, 1000));
    }

    @Test public void depositingDroppingAndUnrelatedItemsDoNotCount()
    {
        TravelCosts costs = new TravelCosts();
        for (String option : new String[]{"deposit-1", "drop", "use", "examine"})
        {
            costs.begin(ItemID.WEISS_TELEPORT_BASALT, option, 10, 0);
            assertEquals(-1, costs.consumed(9, 1000));
        }
        costs.begin(ItemID.ECTOPHIAL, "teleport", 1, 0);
        assertEquals(-1, costs.consumed(0, 1000));
    }

    @Test public void cancelledExpiredAndUnexpectedInventoryChangesDoNotCount()
    {
        TravelCosts costs = new TravelCosts();
        costs.begin(ItemID.WEISS_TELEPORT_BASALT, "teleport", 10, 0);
        costs.clear();
        assertEquals(-1, costs.consumed(9, 1000));
        costs.begin(ItemID.WEISS_TELEPORT_BASALT, "teleport", 10, 0);
        assertEquals(-1, costs.consumed(9, 15001));
        costs.begin(ItemID.WEISS_TELEPORT_BASALT, "teleport", 10, 0);
        assertEquals(-1, costs.consumed(0, 1000));
        costs.begin(ItemID.WEISS_TELEPORT_BASALT, "teleport", 10, 0);
        assertEquals(-1, costs.consumed(11, 1000));
        assertEquals(-1, costs.consumed(9, 2000));
    }
}
