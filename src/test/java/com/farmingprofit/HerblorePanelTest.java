package com.farmingprofit;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class HerblorePanelTest
{
    @Test public void changingRecipeAndCleaningRecalculatesRunAndHistory() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data();
            FarmRun run = new FarmRun(System.currentTimeMillis());
            run.add(ItemID.UNIDENTIFIED_RANARR, "Grimy ranarr weed", 10, 5000, false, "Patch", "Harvest", 1);
            data.active = run; data.history.add(run.snapshot());
            Set<Integer> icons = new HashSet<>();
            FarmingProfitPanel panel = new FarmingProfitPanel(null, (label, id) -> icons.add(id));
            panel.render(data, "Saved locally", true); panel.selectXpView(true);
            assertTrue(labels(panel).contains("950 XP"));
            assertTrue(icons.contains(ItemID.UNIDENTIFIED_RANARR)); assertTrue(icons.contains(ItemID._3DOSEPRAYERRESTORE));
            panel.openRecipeEditor();
            JComboBox<?> ranarr = descendants(panel, JComboBox.class).stream()
                .filter(c -> "Ranarr potion choice".equals(c.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow();
            ranarr.setSelectedIndex(1);
            descendants(panel, JCheckBox.class).get(0).setSelected(false);
            click(panel, "Done");
            assertTrue(labels(panel).contains("750 XP")); assertFalse(labels(panel).contains("950 XP"));
            assertTrue(icons.contains(ItemID._3DOSE1DEFENSE));
            assertEquals(50000, data.active.profit()); assertEquals(50000, data.history.get(0).profit());
            panel.selectXpView(false); assertTrue(labels(panel).contains("+50,000 gp"));
        });
    }
    @Test public void cancellingRecipeEditsDoesNotChangeEstimatesAndAccountSwitchClosesEditor() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data();
            data.active = new FarmRun(1); data.active.add(ItemID.UNIDENTIFIED_RANARR, "Ranarr", 10, 1, false, "Patch", "Harvest", 1);
            FarmingProfitPanel panel = new FarmingProfitPanel(null);
            panel.render(data, "Saved locally", true, 1); panel.selectXpView(true); panel.openRecipeEditor();
            descendants(panel, JCheckBox.class).get(0).setSelected(false); click(panel, "Back");
            assertTrue(labels(panel).contains("950 XP"));
            panel.openRecipeEditor(); panel.render(new FarmingProfitPlugin.Data(), "Log in", false, 2);
            assertTrue(descendants(panel, PotionChoicesPanel.class).isEmpty());
            assertFalse(labels(panel).contains("950 XP"));
        });
    }
    @Test public void periodFiltersCompletedRunsAndNeverIncludesActiveRun() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data();
            for (int days : new int[]{0, 1, 40})
            {
                FarmRun run = new FarmRun(java.time.LocalDate.now().minusDays(days).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                run.add(ItemID.UNIDENTIFIED_RANARR, "Ranarr", 10, 1, false, "Patch", "Harvest", run.startedAt);
                data.history.add(run);
            }
            data.active = new FarmRun(System.currentTimeMillis());
            data.active.add(ItemID.UNIDENTIFIED_RANARR, "Ranarr", 100, 1, false, "Patch", "Harvest", 1);
            FarmingProfitPanel panel = new FarmingProfitPanel(null); panel.render(data, "Saved locally", true); panel.selectXpView(true);
            assertEquals(3, runButtons(panel)); click(panel, "Today"); assertEquals(1, runButtons(panel));
            assertTrue(labels(panel).contains("950 XP")); click(panel, "All"); assertEquals(3, runButtons(panel));
            assertTrue(labels(panel).contains("2,850 XP"));
        });
    }
    @Test public void reopeningHistoryAfterMidnightRefreshesPeriod() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> day =
                new java.util.concurrent.atomic.AtomicReference<>(java.time.LocalDate.of(2026, 10, 30));
            FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data();
            FarmRun run = new FarmRun(day.get().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            run.add(ItemID.UNIDENTIFIED_RANARR, "Ranarr", 10, 1, false, "Patch", "Harvest", run.startedAt);
            data.history.add(run);
            FarmingProfitPanel panel = new FarmingProfitPanel(null, (label, id) -> {}, day::get);
            panel.render(data, "Saved locally", true); panel.selectXpView(true);
            JTabbedPane tabs = descendants(panel, JTabbedPane.class).get(0);
            tabs.setSelectedIndex(1); click(panel, "Today"); assertEquals(1, runButtons(panel));
            tabs.setSelectedIndex(0); day.set(day.get().plusDays(1)); tabs.setSelectedIndex(1);
            assertEquals(0, runButtons(panel)); click(panel, "All"); assertEquals(1, runButtons(panel));
            assertTrue(labels(panel).contains("Yesterday"));
        });
    }
    @Test public void routineFooterIsHiddenButSaveFailuresRemainVisible() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data(); data.active = new FarmRun(1);
            FarmingProfitPanel panel = new FarmingProfitPanel(null); panel.render(data, "Saved locally | GE estimates", true);
            panel.selectXpView(true);
            assertFalse(descendants(panel, JLabel.class).stream().anyMatch(l -> l.isVisible() && l.getText() != null && l.getText().contains("Saved locally")));
            panel.render(data, "Save failed — recent changes are only in memory.", true);
            assertTrue(descendants(panel, JLabel.class).stream().anyMatch(l -> l.isVisible() && l.getText() != null && l.getText().contains("Save failed")));
        });
    }
    private static int runButtons(Container parent)
    { return (int) descendants(parent, JButton.class).stream().filter(b -> b.getAccessibleContext().getAccessibleName().startsWith("Open run ")).count(); }
    private static Set<String> labels(Container parent)
    {
        Set<String> labels = new HashSet<>(); descendants(parent, JLabel.class).forEach(l -> labels.add(l.getText())); return labels;
    }
    private static void click(Container parent, String label)
    { descendants(parent, JButton.class).stream().filter(b -> label.equals(b.getText())).findFirst().orElseThrow().doClick(0); }
    private static <T> java.util.List<T> descendants(Container parent, Class<T> type)
    {
        java.util.List<T> result = new ArrayList<>();
        for (Component child : parent.getComponents())
        {
            if (type.isInstance(child)) { result.add(type.cast(child)); }
            if (child instanceof Container) { result.addAll(descendants((Container) child, type)); }
        }
        return result;
    }
}
