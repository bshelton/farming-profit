package com.farmingprofit;

import org.junit.Test;
import static org.junit.Assert.*;

public class SessionActionTest
{
    @Test public void dialogCannotApplyAfterRelogOrAccountSwitch()
    {
        FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data(); data.active = new FarmRun(1);
        assertTrue(data.matchesAction(4, 4, data.active.id));
        assertFalse(data.matchesAction(4, 5, data.active.id));
    }

    @Test public void oldRunControlsCannotModifyReplacementRun()
    {
        FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data();
        String old = new FarmRun(1).id; data.active = new FarmRun(2);
        assertFalse(data.matchesAction(4, 4, old));
        assertFalse(data.matchesAction(4, 4, null));
        data.active = null;
        assertFalse(data.matchesAction(4, 4, old));
        assertTrue(data.matchesAction(4, 4, null));
    }

    @Test public void corruptTotalsAndDuplicateRunsAreRejected()
    {
        FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data(); data.active = new FarmRun(1);
        data.history.add(data.active);
        assertThrows(IllegalStateException.class, data::validate);
        data.history.clear();
        for (int i = 0; i < 3; i++) { data.active.add(1, "Huge", Integer.MAX_VALUE, 2_000_000_000L, false, "Patch", "Test", 2); }
        assertThrows(ArithmeticException.class, data::validate);
    }

    @Test public void invalidTimestampsAndXpAreRejected()
    {
        FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data(); data.active = new FarmRun(Long.MAX_VALUE);
        assertThrows(IllegalStateException.class, data::validate);
        data.active = new FarmRun(1); data.active.farmingXp = -1;
        assertThrows(IllegalStateException.class, data::validate);
    }
}
