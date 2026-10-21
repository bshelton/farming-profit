package com.farmingprofit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A price-snapshotted ledger; moving an item is never a ledger transaction. */
final class FarmRun
{
    String id = UUID.randomUUID().toString();
    long startedAt;
    long lastActivity;
    long finishedAt;
    long farmingXp;
    List<Entry> entries = new ArrayList<>();
    Set<String> warnings = new LinkedHashSet<>();

    FarmRun(long now) { startedAt = lastActivity = now; }

    void add(int itemId, String name, int quantity, long unitPrice, boolean cost,
             String patch, String source, long now)
    {
        if (quantity == 0 || unitPrice < 0) { throw new IllegalArgumentException("Invalid ledger entry"); }
        entries.add(new Entry(itemId, name, quantity, unitPrice, cost, patch, source, now));
        lastActivity = now;
    }

    long harvestValue() { return value(false); }
    long costs() { return value(true); }
    long profit() { return harvestValue() - costs(); }
    private long value(boolean costs)
    {
        return entries.stream().filter(e -> e.cost == costs).mapToLong(e -> e.unitPrice * e.quantity).sum();
    }

    static final class Entry
    {
        int itemId;
        String name;
        int quantity;
        long unitPrice;
        boolean cost;
        String patch;
        String source;
        long at;
        Entry(int itemId, String name, int quantity, long unitPrice, boolean cost,
              String patch, String source, long at)
        {
            this.itemId = itemId; this.name = name; this.quantity = quantity;
            this.unitPrice = unitPrice; this.cost = cost; this.patch = patch;
            this.source = source; this.at = at;
        }
    }
}
