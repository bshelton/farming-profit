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
                panel.render(new FarmingProfitPlugin.Data(), "Saved locally | GE estimates", true);
                capture(panel, output, "empty.png");
                panel.render(new FarmingProfitPlugin.Data(), "Log in to load your farming history.", false);
                capture(panel, output, "logged-out.png");
            }
            catch (Exception ex) { throw new RuntimeException(ex); }
        });
    }
    private static void capture(FarmingProfitPanel panel, Filepath output, String name) throws Exception
    {
        panel.setSize(225, panel.getPreferredSize().height); layout(panel);
        BufferedImage image = new BufferedImage(450, panel.getHeight() * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics(); graphics.scale(2, 2); panel.printAll(graphics); graphics.dispose();
        try (var stream = output.join(name).openOutputStream()) { ImageIO.write(image, "png", stream); }
    }
    private static void layout(Container container)
    {
        container.doLayout();
        for (var child : container.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
}
