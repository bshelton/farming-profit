package com.farmingprofit;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import net.runelite.client.util.Filepath;
import static org.junit.Assert.*;

public class HistoryStoreTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final HistoryStore store = new HistoryStore(new Gson());
    private Filepath root() { return Filepath.Unchecked.getRooted(temporary.getRoot().toPath()); }
    private static FarmingProfitPlugin.Data data(int quantity)
    {
        FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data(); data.active = new FarmRun(1);
        data.active.add(1, "Crop", quantity, 100, false, "Patch", "Harvest", 2); return data;
    }

    @Test public void atomicWritesRoundTripAndAccountsRemainSeparate() throws Exception
    {
        store.save(root(), "profile-a", data(13).snapshot());
        store.save(root(), "profile-b", data(8).snapshot());
        assertEquals(1300, store.load(root(), "profile-a").active.profit());
        assertEquals(800, store.load(root(), "profile-b").active.profit());
        assertFalse(root().join("profile-a.pending").exists());
    }

    @Test public void deletedHistoryStaysDeletedWithoutChangingActiveOrOtherRuns() throws Exception
    {
        FarmingProfitPlugin.Data live = data(13);
        FarmRun removed = new FarmRun(10), kept = new FarmRun(20);
        live.history.add(removed); live.history.add(kept);
        FarmingProfitPlugin.Data before = live.snapshot();
        store.save(root(), "a", before);
        assertFalse(live.deleteHistory(1, 2, removed.id));
        assertFalse(live.deleteHistory(2, 2, live.active.id));
        assertEquals(2, live.history.size());
        assertTrue(live.deleteHistory(2, 2, removed.id));
        assertFalse(live.deleteHistory(2, 2, removed.id));
        assertEquals(2, before.history.size());
        assertEquals(1, live.snapshot().history.size());
        store.save(root(), "a", live.snapshot());
        FarmingProfitPlugin.Data restored = store.load(root(), "a");
        assertEquals(1300, restored.active.profit());
        assertEquals(1, restored.history.size());
        assertEquals(kept.id, restored.history.get(0).id);
        assertTrue(restored.deleteHistory(2, 2, kept.id));
        store.save(root(), "a", restored.snapshot());
        assertTrue(store.load(root(), "a").history.isEmpty());
    }

    @Test public void backupRetainsSessionStartAndRotatesAfterNextLoad() throws Exception
    {
        store.save(root(), "a", data(1).snapshot());
        store.load(root(), "a");
        store.save(root(), "a", data(2).snapshot());
        store.save(root(), "a", data(3).snapshot());
        assertEquals(100, readBackup().active.profit());
        store.load(root(), "a"); store.save(root(), "a", data(4).snapshot());
        assertEquals(300, readBackup().active.profit());
    }

    private FarmingProfitPlugin.Data readBackup() throws Exception
    {
        try (Reader reader = root().join("a.backup").openBufferedReader())
        { return new Gson().fromJson(reader, FarmingProfitPlugin.Data.class); }
    }

    @Test public void interruptedPendingWriteDoesNotReplacePrimary() throws Exception
    {
        store.save(root(), "a", data(13).snapshot());
        root().join("a.pending").write("{interrupted");
        assertEquals(1300, store.load(root(), "a").active.profit());
        store.save(root(), "a", data(14).snapshot());
        assertEquals(1400, store.load(root(), "a").active.profit());
    }

    @Test public void failedWritePreservesPrimaryAndNextSaveCanRecover() throws Exception
    {
        store.save(root(), "a", data(13).snapshot());
        root().join("a.pending").createDirectory(); // deterministic write failure, even as root
        assertThrows(IOException.class, () -> store.save(root(), "a", data(14).snapshot()));
        assertEquals(1300, store.load(root(), "a").active.profit());
        root().join("a.pending").deleteIfExists();
        store.save(root(), "a", data(14).snapshot());
        assertEquals(1400, store.load(root(), "a").active.profit());
    }

    @Test public void corruptPrimaryIsNeverOverwrittenOrCopiedOverBackup() throws Exception
    {
        store.save(root(), "a", data(1).snapshot()); store.save(root(), "a", data(2).snapshot());
        root().join("a.json").write("{broken");
        assertThrows(IOException.class, () -> store.load(root(), "a"));
        assertThrows(IOException.class, () -> store.save(root(), "a", data(3).snapshot()));
        assertEquals(100, readBackup().active.profit());
    }

    @Test public void missingPrimaryWithRecoveryDataDoesNotStartBlankHistory() throws Exception
    {
        root().join("a.pending").write(new Gson().toJson(data(13)));
        assertThrows(IOException.class, () -> store.load(root(), "a"));
        assertTrue(root().join("a.pending").exists());
    }

    @Test public void invalidSchemaAndNullWarningsAreRejected() throws Exception
    {
        for (String json : new String[]{"null", "{}", "{\"version\":2,\"history\":[]}", "{\"version\":1,\"history\":null}", "[]"})
        {
            root().join("a.json").write(json);
            assertThrows(IOException.class, () -> store.load(root(), "a"));
        }
        FarmingProfitPlugin.Data invalid = data(1); invalid.active.warnings.add(null);
        root().join("a.json").write(new Gson().toJson(invalid));
        assertThrows(IOException.class, () -> store.load(root(), "a"));
    }

    @Test public void unsafeProfileNamesCannotEscapeDirectory()
    {
        assertThrows(IllegalArgumentException.class, () -> store.load(root(), "../a"));
        assertThrows(IllegalArgumentException.class, () -> store.save(root(), "/tmp/a", data(1)));
    }
}
