package com.farmingprofit;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.Icon;

/** Fits RuneLite's real item sprite into the sidebar without copying or reloading it. */
final class ItemSpriteIcon implements Icon
{
    private final BufferedImage image;
    private final int size;
    ItemSpriteIcon(BufferedImage image, int size) { this.image = image; this.size = size; }
    public int getIconWidth() { return size; }
    public int getIconHeight() { return size; }
    public void paintIcon(Component component, Graphics graphics, int x, int y)
    {
        double scale = Math.min((double) size / image.getWidth(), (double) size / image.getHeight());
        int width = (int) (image.getWidth() * scale), height = (int) (image.getHeight() * scale);
        graphics.drawImage(image, x + (size - width) / 2, y + (size - height) / 2, width, height, component);
    }
}
