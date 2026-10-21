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
                panel.render(data, "Preview data · GE estimates before tax", true);
                panel.setSize(225, panel.getPreferredSize().height); layout(panel);
                BufferedImage image = new BufferedImage(225, panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
                try (var stream = output.join("sidebar.png").openOutputStream()) { ImageIO.write(image, "png", stream); }
            }
            catch (Exception ex) { throw new RuntimeException(ex); }
        });
    }
    private static void layout(Container container)
    {
        container.doLayout();
        for (var child : container.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
}
