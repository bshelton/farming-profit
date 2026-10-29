package com.farmingprofit;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class FarmingProfitPanelLayoutTest
{
    @Test public void hiddenHistoryDoesNotPushWelcomeActionsDown() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            net.runelite.client.ui.laf.RuneLiteLAF.setup();
            FarmingProfitPanel panel = new FarmingProfitPanel(null);
            panel.render(new FarmingProfitPlugin.Data(), "Saved locally | GE estimates", true);
            int emptyHeight = panel.getPreferredSize().height;
            FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data();
            for (int i = 0; i < 25; i++) { data.history.add(new FarmRun(1000000L + i)); }
            panel.render(data, "Saved locally | GE estimates", true);
            assertEquals(emptyHeight, panel.getPreferredSize().height);
            JTabbedPane tabs = tabs(panel);
            assertNotNull(tabs);
            tabs.setSelectedIndex(1);
            assertTrue(panel.getPreferredSize().height > emptyHeight);
            tabs.setSelectedIndex(0);
            assertEquals(emptyHeight, panel.getPreferredSize().height);
        });
    }

    private static JTabbedPane tabs(Container parent)
    {
        for (Component child : parent.getComponents())
        {
            if (child instanceof JTabbedPane) { return (JTabbedPane) child; }
            if (child instanceof Container)
            {
                JTabbedPane found = tabs((Container) child);
                if (found != null) { return found; }
            }
        }
        return null;
    }
}
