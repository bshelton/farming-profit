package com.farmingprofit;

import net.runelite.api.GameState;
import org.junit.Test;
import static org.junit.Assert.*;

public class RegionLoadingTest
{
    @Test public void ordinarySceneRebuildDoesNotResetHarvestOrBaselines()
    {
        assertFalse(FarmingProfitPlugin.resetsTracking(GameState.LOADING));
        assertFalse(FarmingProfitPlugin.resetsTracking(GameState.LOGGED_IN));
    }

    @Test public void logoutReconnectAndWorldHopStillResetTracking()
    {
        assertTrue(FarmingProfitPlugin.resetsTracking(GameState.LOGIN_SCREEN));
        assertTrue(FarmingProfitPlugin.resetsTracking(GameState.LOGGING_IN));
        assertTrue(FarmingProfitPlugin.resetsTracking(GameState.CONNECTION_LOST));
        assertTrue(FarmingProfitPlugin.resetsTracking(GameState.HOPPING));
    }

    @Test public void varlamoreTraceAfterLoadingContainsEightHerbs()
    {
        // 18:17–18:18 trace: region loading happened after Pick, before the first XP drop.
        int[] xp = {30, 31, 30, 31, 30, 31, 30, 57};
        int herbs = 0;
        for (int i = 0; i < xp.length; i++)
        { herbs += HerbHarvest.RANARR.missingCount(xp[i], 1, 0, i == xp.length - 1); }
        assertEquals(8, herbs);
    }
}
