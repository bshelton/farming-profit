package com.farmingprofit;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("farming-profit")
public interface FarmingProfitConfig extends Config
{
    @ConfigItem(keyName = "automatic", name = "Automatic runs", description = "Start a run on observed farming activity", position = 0)
    default boolean automatic() { return true; }

    @Range(min = 1, max = 60)
    @ConfigItem(keyName = "idleMinutes", name = "Finish after idle minutes", description = "Finish after this many minutes without farming; travel between patches stays in the same run", position = 1)
    default int idleMinutes() { return 10; }

    @ConfigItem(keyName = "compost", name = "Count compost costs", description = "Estimate compost from successful treatment messages; bottomless bucket uses half a bucket's value per application", position = 2)
    default boolean compost() { return true; }

    @ConfigItem(keyName = "diagnostics", name = "Farming diagnostics", description = "Write farming signal details to the debug log for local testing; no chat from other players is logged", position = 3)
    default boolean diagnostics() { return false; }

    @ConfigItem(keyName = "travel", name = "Count teleport items", description = "Count consumed basalt, tablets and inventory teleport scrolls during an active run. Start a run before your first teleport. Runes and jewellery charges need manual costs.", position = 4)
    default boolean travel() { return true; }
}
