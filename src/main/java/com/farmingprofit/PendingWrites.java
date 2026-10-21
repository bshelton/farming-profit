package com.farmingprofit;

import java.util.HashMap;
import java.util.Map;

/** One queued snapshot per account; an in-flight write still completes in order. */
final class PendingWrites
{
    private final Map<String, FarmingProfitPlugin.Data> pending = new HashMap<>();

    synchronized boolean offer(String key, FarmingProfitPlugin.Data snapshot)
    {
        return pending.put(key, snapshot) == null;
    }

    synchronized FarmingProfitPlugin.Data take(String key) { return pending.remove(key); }
}
