package com.farmingprofit;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class PerformanceSafetyTest
{
    @Test public void snapshotJsonRoundTripPreservesEntriesAndOmitsCaches()
    {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        FarmingProfitPlugin.Data live = new FarmingProfitPlugin.Data();
        live.active = run(2); live.active.setPaused(true, 3); live.history.add(run(1));
        String json = gson.toJson(live.snapshot());
        assertFalse(json.contains("historySnapshot"));
        FarmingProfitPlugin.Data restored = gson.fromJson(json, FarmingProfitPlugin.Data.class);
        restored.validate();
        assertEquals(1000, restored.active.profit()); assertTrue(restored.active.paused);
        assertEquals(live.active.id, restored.active.id);
        assertEquals(1000, restored.history.get(0).profit());
        restored.active.add(1, "New harvest", 1, 100, false, "Patch", "Test", 4);
        assertEquals(1100, restored.active.profit());
    }

    private static FarmRun run(int time)
    {
        FarmRun run = new FarmRun(time);
        run.add(1, "Test crop", 10, 100, false, "Patch", "Test", time);
        return run;
    }

    @Test public void snapshotsAreIsolatedFromLaterClientThreadChanges()
    {
        FarmingProfitPlugin.Data live = new FarmingProfitPlugin.Data(); live.active = run(1);
        FarmingProfitPlugin.Data copy = live.snapshot();
        live.active.add(1, "Test crop", 1, 100, false, "Patch", "Test", 2);
        live.active.warnings.add("Later warning"); live.active.setPaused(true, 3);
        assertEquals(1000, copy.active.profit());
        assertTrue(copy.active.warnings.isEmpty()); assertFalse(copy.active.paused);
        assertSame(live.active.entries.get(0), copy.active.entries.get(0));
        assertThrows(UnsupportedOperationException.class, () -> copy.active.entries.clear());
    }

    @Test public void harvestUpdatesReuseCompletedHistoryAndFinishInvalidatesCache()
    {
        FarmingProfitPlugin.Data live = new FarmingProfitPlugin.Data();
        for (int i = 0; i < 1000; i++) { live.history.add(run(i)); }
        live.active = run(1001);
        FarmingProfitPlugin.Data first = live.snapshot();
        for (int i = 0; i < 100; i++)
        {
            live.active.farmingXp++;
            assertSame(first.history, live.snapshot().history);
        }
        live.history.add(0, live.active); live.active = null;
        FarmingProfitPlugin.Data finished = live.snapshot();
        assertEquals(1001, finished.history.size()); assertEquals(1000, first.history.size());
        assertNotSame(first.history, finished.history);
        assertThrows(UnsupportedOperationException.class, () -> finished.history.clear());
    }

    @Test public void slowStorageKeepsLatestPendingSnapshotPerAccount()
    {
        PendingWrites writes = new PendingWrites();
        FarmingProfitPlugin.Data live = new FarmingProfitPlugin.Data(); live.active = run(1);
        assertTrue(writes.offer("a", live.snapshot()));
        for (int i = 0; i < 1000; i++)
        {
            live.active.farmingXp = i;
            assertFalse(writes.offer("a", live.snapshot()));
        }
        assertTrue(writes.offer("b", new FarmingProfitPlugin.Data().snapshot()));
        assertEquals(999, writes.take("a").active.farmingXp);
        assertNotNull(writes.take("b")); assertNull(writes.take("a"));
        // Once a worker takes a snapshot, a concurrent update schedules the next write.
        assertTrue(writes.offer("a", live.snapshot()));
        assertNotNull(writes.take("a"));
    }

    @Test public void historyButtonsAreReusedAndPaginationBoundsComponents() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            FarmingProfitPlugin.Data live = new FarmingProfitPlugin.Data();
            for (int i = 0; i < 55; i++) { live.history.add(run(i)); }
            FarmingProfitPanel panel = new FarmingProfitPanel(null);
            panel.render(live.snapshot(), "Saved locally", true);
            List<JButton> before = runButtons(panel);
            assertEquals(25, before.size());
            live.active = run(100);
            panel.render(live.snapshot(), "Saved locally", true);
            assertSame(before.get(0), runButtons(panel).get(0));
            button(panel, "Next").doClick(0);
            assertEquals(25, runButtons(panel).size());
            button(panel, "Next").doClick(0);
            assertEquals(5, runButtons(panel).size());
            assertFalse(button(panel, "Next").isEnabled());
            panel.render(live.snapshot(), "Saved locally", true);
            assertEquals(5, runButtons(panel).size());
            button(panel, "Previous").doClick(0);
            assertEquals(25, runButtons(panel).size());
        });
    }

    private static List<JButton> buttons(Container parent)
    {
        List<JButton> result = new ArrayList<>();
        for (Component child : parent.getComponents())
        {
            if (child instanceof JButton) { result.add((JButton) child); }
            if (child instanceof Container) { result.addAll(buttons((Container) child)); }
        }
        return result;
    }
    private static List<JButton> runButtons(Container parent)
    {
        List<JButton> result = buttons(parent);
        result.removeIf(b -> b.getText() == null || !b.getText().startsWith("<html>"));
        return result;
    }
    private static JButton button(Container parent, String label)
    { return buttons(parent).stream().filter(b -> label.equals(b.getText())).findFirst().orElseThrow(); }
}
