package com.farmingprofit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A price-snapshotted ledger; moving an item is never a ledger transaction. */
final class FarmRun
{
    String id;
    long startedAt;
    long lastActivity;
    long finishedAt;
    long farmingXp;
    boolean paused;
    List<Entry> entries = new ArrayList<>();
    Set<String> warnings = new LinkedHashSet<>();

    FarmRun(long now) { id = UUID.randomUUID().toString(); startedAt = lastActivity = now; }

    private FarmRun(FarmRun original)
    {
        id = original.id; startedAt = original.startedAt; lastActivity = original.lastActivity;
        finishedAt = original.finishedAt; farmingXp = original.farmingXp; paused = original.paused;
        entries = java.util.Collections.unmodifiableList(new ArrayList<>(original.entries));
        warnings = java.util.Collections.unmodifiableSet(new LinkedHashSet<>(original.warnings));
    }

    // Entries are immutable; only the collections and run metadata need copying.
    FarmRun snapshot() { return new FarmRun(this); }

    void setPaused(boolean value, long now) { paused = value; lastActivity = now; }
    boolean idleExpired(long now, int minutes)
    { return !paused && now - lastActivity >= minutes * 60_000L; }

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
        final int itemId;
        final String name;
        final int quantity;
        final long unitPrice;
        final boolean cost;
        final String patch;
        final String source;
        final long at;
        Entry(int itemId, String name, int quantity, long unitPrice, boolean cost,
              String patch, String source, long at)
        {
            this.itemId = itemId; this.name = name; this.quantity = quantity;
            this.unitPrice = unitPrice; this.cost = cost; this.patch = patch;
            this.source = source; this.at = at;
        }
    }
}
