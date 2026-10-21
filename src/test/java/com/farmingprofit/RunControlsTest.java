package com.farmingprofit;

import com.google.gson.Gson;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunControlsTest
{
    @Test public void pausePreservesTotalsAndSuspendsIdleCompletion()
    {
        FarmRun run = new FarmRun(0);
        run.add(ItemID.UNIDENTIFIED_RANARR, "Grimy ranarr weed", 13, 5000, false, "Patch", "Harvest", 1);
        run.setPaused(true, 2);
        assertEquals(65000, run.profit());
        assertFalse(run.idleExpired(3_600_000, 10));
        run.setPaused(false, 3_600_000);
        assertFalse(run.idleExpired(3_600_001, 10));
        assertTrue(run.idleExpired(4_200_000, 10));
    }

    @Test public void pausedRunSurvivesStorageRoundTripAndOldSavesDefaultToRunning()
    {
        Gson gson = new Gson();
        FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data();
        data.active = new FarmRun(1); data.active.setPaused(true, 2);
        FarmingProfitPlugin.Data copy = gson.fromJson(gson.toJson(data), FarmingProfitPlugin.Data.class);
        copy.validate();
        assertTrue(copy.trackingPaused());
        FarmRun legacy = gson.fromJson("{\"id\":\"old\",\"startedAt\":1,\"entries\":[],\"warnings\":[]}", FarmRun.class);
        assertFalse(legacy.paused);
    }

    @Test public void deletingCurrentRunDoesNotSaveItToHistory()
    {
        FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data();
        FarmRun completed = new FarmRun(1); data.history.add(completed);
        data.active = new FarmRun(2); data.active.setPaused(true, 3);
        assertTrue(data.deleteActive(data.active.id));
        assertNull(data.active);
        assertFalse(data.trackingPaused());
        assertEquals(1, data.history.size());
        assertSame(completed, data.history.get(0));
        assertFalse(data.deleteActive("already-deleted"));
    }

    @Test public void staleConfirmationCannotDeleteANewerRun()
    {
        FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data();
        String oldId = new FarmRun(1).id;
        FarmRun newer = new FarmRun(2); data.active = newer;
        assertFalse(data.deleteActive(oldId));
        assertSame(newer, data.active);
    }
}
