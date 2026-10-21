package com.farmingprofit;

import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import net.runelite.client.util.ImageUtil;

/** Small bundled assets; no image downloads or resizing on the client thread. */
final class BrandAssets
{
    static final BufferedImage MARK = ImageUtil.loadImageResource(BrandAssets.class, "brand/mark-24.png");
    static final ImageIcon HARVEST = new ImageIcon(ImageUtil.loadImageResource(BrandAssets.class, "brand/harvest-144.png"));

    private BrandAssets() {}
}
