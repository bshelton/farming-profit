package com.farmingprofit;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

final class FarmingReminderOverlay extends WidgetItemOverlay
{
    private static final Color GOLD = new Color(242, 190, 101);
    private final FarmingProfitPlugin plugin;
    private final ItemManager items;

    @Inject FarmingReminderOverlay(FarmingProfitPlugin plugin, ItemManager items)
    {
        this.plugin = plugin; this.items = items;
        showOnInventory();
    }

    @Override public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        if (!plugin.highlightClosedSack() || !FarmingReminders.closedSack(itemId)) { return; }
        Rectangle bounds = widgetItem.getCanvasBounds();
        graphics.drawImage(items.getItemOutline(itemId, widgetItem.getQuantity(), GOLD), bounds.x, bounds.y, null);
    }
}
