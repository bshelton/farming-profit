package com.farmingprofit;

import java.time.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class HerbloreEstimateTest
{
    private final HerbloreEstimate.Choices defaults = new HerbloreEstimate.Choices(true, "");
    private FarmRun sample()
    {
        FarmRun run = new FarmRun(1);
        run.add(ItemID.UNIDENTIFIED_RANARR, "Grimy ranarr weed", 30, 5000, false, "Patch", "Harvest", 1);
        run.add(ItemID.UNIDENTIFIED_SNAPDRAGON, "Grimy snapdragon", 30, 6000, false, "Patch", "Sack harvest", 1);
        return run;
    }
    @Test public void multipleHerbsUseCorrectBaseXpAndCleaning()
    {
        HerbloreEstimate xp = new HerbloreEstimate(sample(), defaults);
        assertEquals(69000, xp.potionTenths); assertEquals(5790, xp.cleaningTenths);
        assertEquals(74790, xp.totalTenths()); assertEquals(60, xp.quantity);
    }
    @Test public void costsSecondariesAndOtherCropsNeverAddXp()
    {
        FarmRun run = sample();
        run.add(ItemID.UNIDENTIFIED_RANARR, "Ranarr cost", 10, 100, true, "Manual", "Cost", 2);
        run.add(ItemID.SNAPE_GRASS, "Snape grass", 100, 100, false, "Patch", "Harvest", 2);
        run.add(ItemID.DRAGONFRUIT, "Dragonfruit", 6, 100, false, "Tree", "Harvest", 2);
        run.add(ItemID.COINS, "GP adjustment", 100, 1, false, "Manual", "Correction", 2);
        assertEquals(74790, new HerbloreEstimate(run, defaults).totalTenths());
    }
    @Test public void correctionsSubtractAndZeroNetHerbsDisappear()
    {
        FarmRun run = sample();
        run.add(ItemID.UNIDENTIFIED_RANARR, "Ranarr", -30, 5000, false, "Manual", "Correct", 2);
        HerbloreEstimate xp = new HerbloreEstimate(run, defaults);
        assertEquals(1, xp.rows.size()); assertEquals(46290, xp.totalTenths());
        run.add(ItemID.UNIDENTIFIED_SNAPDRAGON, "Snapdragon", -31, 6000, false, "Manual", "Correct", 3);
        assertEquals(-1543, new HerbloreEstimate(run, defaults).totalTenths());
    }
    @Test public void recipesRecalculateExistingHistoryWithoutMutatingGp()
    {
        FarmRun run = sample().snapshot(); long profit = run.profit();
        HerbloreEstimate.Choices changed = new HerbloreEstimate.Choices(false,
            ItemID.UNIDENTIFIED_RANARR + ":" + ItemID._3DOSE1DEFENSE);
        assertEquals(65250, new HerbloreEstimate(run, changed).totalTenths());
        assertEquals(profit, run.profit()); assertEquals(2, run.entries.size());
        assertEquals(changed, new HerbloreEstimate.Choices(false, changed.encode()));
    }
    @Test public void invalidAndCrossHerbRecipeSettingsFallBackSafely()
    {
        HerbloreEstimate.Choices invalid = new HerbloreEstimate.Choices(true,
            "bad;123:456;" + ItemID.UNIDENTIFIED_RANARR + ":" + ItemID._3DOSE2RESTORE);
        assertEquals(defaults, invalid); assertEquals(defaults, new HerbloreEstimate.Choices(true, null));
    }
    @Test public void tenthsAccumulateWithoutRoundingEachHerb()
    {
        FarmRun run = new FarmRun(1);
        run.add(ItemID.UNIDENTIFIED_IRIT, "Irit", 3, 1, false, "Patch", "Harvest", 1);
        assertEquals(3264, new HerbloreEstimate(run, defaults).totalTenths());
    }
    @Test public void localCalendarPeriodsUseRunStartAndExcludeFutureDays()
    {
        ZoneId zone = ZoneId.of("America/New_York"); LocalDate today = LocalDate.of(2026, 11, 2);
        FarmRun sunday = new FarmRun(ZonedDateTime.of(2026, 11, 1, 23, 59, 0, 0, zone).toInstant().toEpochMilli());
        sunday.finishedAt = ZonedDateTime.of(2026, 11, 2, 0, 15, 0, 0, zone).toInstant().toEpochMilli();
        assertFalse(HerbloreEstimate.Period.TODAY.includes(sunday, today, zone));
        assertFalse(HerbloreEstimate.Period.WEEK.includes(sunday, today, zone));
        assertTrue(HerbloreEstimate.Period.MONTH.includes(sunday, today, zone));
        FarmRun monday = new FarmRun(today.atStartOfDay(zone).toInstant().toEpochMilli());
        assertTrue(HerbloreEstimate.Period.TODAY.includes(monday, today, zone));
        assertTrue(HerbloreEstimate.Period.WEEK.includes(monday, today, zone));
        FarmRun future = new FarmRun(today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli());
        assertFalse(HerbloreEstimate.Period.MONTH.includes(future, today, zone));
        assertTrue(HerbloreEstimate.Period.ALL.includes(sunday, today, zone));
    }
}
