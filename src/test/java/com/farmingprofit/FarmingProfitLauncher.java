package com.farmingprofit;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FarmingProfitLauncher
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(FarmingProfitPlugin.class);
        RuneLite.main(args);
    }
}
