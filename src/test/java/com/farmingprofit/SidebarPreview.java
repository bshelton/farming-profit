package com.farmingprofit;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;

/** Renders Swing only. Does not launch or interact with the game. */
public class SidebarPreview
{
    public static void main(String[] args) throws Exception
    {
        Filepath output = Filepath.Unchecked.getRooted(Paths.get(args[0]));
        output.createDirectories();
        SwingUtilities.invokeAndWait(() -> {
            try
            {
                net.runelite.client.ui.laf.RuneLiteLAF.setup();
                FarmingProfitPanel panel = new FarmingProfitPanel(null);
                FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data();
                data.active = new FarmRun(System.currentTimeMillis());
                data.active.add(ItemID.UNIDENTIFIED_RANARR, "Grimy ranarr weed", 72, 3000, false, "Catherby", "Harvest", 1);
                data.active.add(ItemID.COINS, "Planting supplies", 1, 54000, true, "Catherby", "Manual test fixture", 2);
                data.active.farmingXp = 1944;
                panel.render(data, "Saved locally | GE estimates", true);
                capture(panel, output, "sidebar.png");
                data.active.setPaused(true, System.currentTimeMillis());
                panel.render(data, "Saved locally | GE estimates", true);
                capture(panel, output, "paused.png");
                panel.renderReminders(FarmingReminders.messages(true, true, true, true, false, true, true, true, true));
                panel.render(data, "Saved locally | GE estimates", true);
                capture(panel, output, "reminders.png");
                panel.renderReminders(java.util.List.of());
                panel.render(new FarmingProfitPlugin.Data(), "Saved locally | GE estimates", true);
                capture(panel, output, "empty.png");
                panel.render(new FarmingProfitPlugin.Data(), "Log in to load your farming history.", false);
                capture(panel, output, "logged-out.png");
                FarmingProfitPlugin.Data history = new FarmingProfitPlugin.Data();
                for (int i = 0; i < 3; i++)
                {
                    long start = System.currentTimeMillis() - (i == 2 ? 86400000L : i * 7200000L);
                    FarmRun saved = new FarmRun(start);
                    saved.add(ItemID.UNIDENTIFIED_RANARR, "Grimy ranarr weed", 11, 6100, false, "Ranarr weed @ 3605,3529", "Harvest", start);
                    saved.add(ItemID.BUCKET_ULTRACOMPOST, "Ultracompost", 1, 238, true, "Herb patch @ 3605,3529", "Compost", start);
                    saved.add(ItemID.UNIDENTIFIED_RANARR, "Grimy ranarr weed", 10, 6100, false, "Ranarr weed @ 2813,3463", "Harvest", start);
                    saved.finishedAt = start + 720000;
                    history.history.add(saved.snapshot());
                }
                panel.render(history, "Saved locally | GE estimates", true);
                selectHistory(panel);
                capture(panel, output, "history.png");
                LedgerPanel locationsPreview = new LedgerPanel(history.history.get(0));
                selectHistory(locationsPreview);
                capture(locationsPreview, output, "locations.png", 620, 520);
                FarmRun ledger = new FarmRun(System.currentTimeMillis());
                for (int i = 0; i < 13; i++) { ledger.add(ItemID.UNIDENTIFIED_RANARR, "Grimy ranarr weed", 1, 6100, false, "Ranarr weed @ 1234,5678", "Sack harvest — XP inference", ledger.startedAt + i * 600L); }
                ledger.add(ItemID.BUCKET_ULTRACOMPOST, "Ultracompost", 1, 238, true, "Ranarr weed @ 1234,5678", "Bottomless compost (half bucket)", ledger.startedAt + 9000);
                ledger.add(ItemID.COINS, "Planting supplies", 1, 26000, true, "Manual", "Seed cost", ledger.startedAt + 10000);
                ledger.add(ItemID.UNIDENTIFIED_RANARR, "Grimy ranarr weed", -1, 6100, false, "Manual", "Correct observed count", ledger.startedAt + 11000);
                ledger.warnings.add("Sack yield includes XP-inferred herbs. Verify against the sack before/after harvesting.");
                LedgerPanel ledgerPanel = new LedgerPanel(ledger);
                capture(ledgerPanel, output, "ledger.png", 620, 520);
                capture(ledgerPanel, output, "ledger-compact.png", 520, 480);
                selectFirstRow(ledgerPanel);
                capture(ledgerPanel, output, "ledger-selected.png", 620, 520);
            }
            catch (Exception ex) { throw new RuntimeException(ex); }
        });
    }
    private static void capture(FarmingProfitPanel panel, Filepath output, String name) throws Exception
    {
        capture(panel, output, name, 225, panel.getPreferredSize().height);
    }
    private static void capture(javax.swing.JComponent panel, Filepath output, String name, int width, int height) throws Exception
    {
        panel.setSize(width, height); layout(panel);
        BufferedImage image = new BufferedImage(width * 2, height * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics(); graphics.scale(2, 2); panel.paint(graphics); graphics.dispose();
        try (var stream = output.join(name).openOutputStream()) { ImageIO.write(image, "png", stream); }
    }
    private static void selectFirstRow(Container container)
    {
        for (var child : container.getComponents())
        {
            if (child instanceof javax.swing.JTable)
            { ((javax.swing.JTable) child).setRowSelectionInterval(0, 0); }
            else if (child instanceof Container) { selectFirstRow((Container) child); }
        }
    }
    private static void selectHistory(Container container)
    {
        for (var child : container.getComponents())
        {
            if (child instanceof javax.swing.JTabbedPane) { ((javax.swing.JTabbedPane) child).setSelectedIndex(1); }
            else if (child instanceof Container) { selectHistory((Container) child); }
        }
    }
    private static void layout(Container container)
    {
        container.doLayout();
        for (var child : container.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
}
