package com.farmingprofit;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FarmingProfitLauncher
{
    public static void main(String[] args) throws Exception
    {
        // Development only: capture failed detection paths without changing saved user settings.
        System.setProperty("farmingprofit.devDiagnostics", "true");
        ExternalPluginManager.loadBuiltin(FarmingProfitPlugin.class);
        RuneLite.main(args);
    }
}
