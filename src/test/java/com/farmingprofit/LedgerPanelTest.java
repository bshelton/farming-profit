package com.farmingprofit;

import java.awt.Component;
import java.awt.Container;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LedgerPanelTest
{
    @Test public void groupingPreservesNetProfitAndSourceLedger()
    {
        FarmRun run = new FarmRun(1);
        for (int i = 0; i < 13; i++) { run.add(1, "Herb", 1, 6000, false, "Patch A", "XP inference", i); }
        run.add(2, "Compost", 1, 238, true, "Patch A", "Compost", 14);
        LedgerPanel.Model model = new LedgerPanel.Model(run);
        assertEquals(2, model.getRowCount()); assertEquals(13L, model.getValueAt(0, 2));
        assertEquals(13, model.rows.get(0).count);
        assertEquals(run.profit(), model.rows.stream().mapToLong(LedgerPanel.Row::impact).sum());
        assertEquals(14, run.entries.size());
        assertTrue(model.rows.get(0).details().contains("XP inference"));
        assertTrue(model.rows.get(0).details().contains("Patch A"));
    }

    @Test public void differentPricesPatchesSourcesAndCorrectionsStaySeparate()
    {
        FarmRun run = new FarmRun(1);
        run.add(1, "Herb", 1, 100, false, "A", "Harvest", 1);
        run.add(1, "Herb", 1, 110, false, "A", "Harvest", 2);
        run.add(1, "Herb", 1, 100, false, "B", "Harvest", 3);
        run.add(1, "Herb", 1, 100, false, "A", "Inference", 4);
        run.add(1, "Herb", -1, 100, false, "A", "Harvest", 5);
        run.add(1, "Herb", -1, 100, true, "A", "Refund", 6);
        LedgerPanel.Model model = new LedgerPanel.Model(run);
        assertEquals(6, model.getRowCount());
        assertEquals(-100L, model.getValueAt(4, 4)); assertEquals(100L, model.getValueAt(5, 4));
        assertEquals("Harvest adj.", model.getValueAt(4, 0));
        assertEquals(run.profit(), model.rows.stream().mapToLong(LedgerPanel.Row::impact).sum());
    }

    @Test public void filteringSortingAndSelectionKeepCorrectDetails() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            FarmRun run = new FarmRun(1);
            run.add(1, "Small harvest", 1, 20, false, "A", "Harvest", 1);
            run.add(2, "Large harvest", 1, 1000, false, "B", "Harvest", 2);
            run.add(3, "Compost", 1, 100, true, "C", "Compost", 3);
            LedgerPanel panel = new LedgerPanel(run);
            JTable table = find(panel, JTable.class);
            table.getRowSorter().toggleSortOrder(4);
            assertEquals(-100L, table.getValueAt(0, 4));
            JComboBox<?> filter = find(panel, JComboBox.class);
            filter.setSelectedIndex(1); assertEquals(2, table.getRowCount());
            table.setRowSelectionInterval(1, 1);
            assertTrue(hasLabel(panel, "Large harvest"));
            assertTrue(hasLabel(panel, "B"));
            filter.setSelectedIndex(2); assertEquals(1, table.getRowCount());
            assertEquals("Compost", table.getValueAt(0, 1));
            filter.setSelectedIndex(0); assertEquals(3, table.getRowCount());
            assertFalse(table.isCellEditable(0, 1));
        });
    }

    @Test public void emptyLedgerRendersWithoutSelection() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            LedgerPanel panel = new LedgerPanel(new FarmRun(1));
            assertEquals(0, find(panel, JTable.class).getRowCount());
            assertTrue(hasLabel(panel, "No entries yet"));
        });
    }

    private static boolean hasLabel(Container parent, String text)
    {
        for (Component child : parent.getComponents())
        {
            if (child instanceof JLabel && text.equals(((JLabel) child).getText())) { return true; }
            if (child instanceof Container && hasLabel((Container) child, text)) { return true; }
        }
        return false;
    }

    private static <T extends Component> T find(Container parent, Class<T> kind)
    {
        for (Component child : parent.getComponents())
        {
            if (kind.isInstance(child)) { return kind.cast(child); }
            if (child instanceof Container)
            { T nested = find((Container) child, kind); if (nested != null) { return nested; } }
        }
        return null;
    }
}
