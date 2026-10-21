package com.farmingprofit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.client.util.Filepath;

/** Called only on the ordered IO executor. Never overwrite unreadable history. */
final class HistoryStore
{
    private final Gson gson;
    private final Set<String> backedUp = new HashSet<>();

    @Inject HistoryStore(Gson gson) { this.gson = gson; }

    FarmingProfitPlugin.Data load(Filepath dir, String key) throws IOException
    {
        checkKey(key);
        backedUp.remove(key);
        Filepath file = dir.join(key + ".json");
        if (!file.exists())
        {
            if (dir.join(key + ".pending").exists() || dir.join(key + ".backup").exists())
            { throw new IOException("History is missing but recovery files exist"); }
            return new FarmingProfitPlugin.Data();
        }
        return read(file);
    }

    private FarmingProfitPlugin.Data read(Filepath file) throws IOException
    {
        try (Reader reader = file.openBufferedReader())
        {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json == null || !json.has("version") || !json.has("history"))
            { throw new IOException("Missing history schema"); }
            FarmingProfitPlugin.Data data = gson.fromJson(json, FarmingProfitPlugin.Data.class);
            data.validate(); return data;
        }
        catch (RuntimeException ex) { throw new IOException("Invalid farming history", ex); }
    }

    void save(Filepath dir, String key, FarmingProfitPlugin.Data snapshot) throws IOException
    {
        checkKey(key); snapshot.validate(); dir.createDirectories();
        Filepath primary = dir.join(key + ".json");
        Filepath temporary = dir.join(key + ".pending");
        // One known-good backup per loaded session, not a second full-file write every harvest.
        if (!backedUp.contains(key) && primary.exists())
        {
            read(primary);
            Filepath backup = dir.join(key + ".backup.pending");
            primary.copyTo(backup, StandardCopyOption.REPLACE_EXISTING);
            backup.moveTo(dir.join(key + ".backup"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            backedUp.add(key);
        }
        temporary.write(gson.toJson(snapshot));
        temporary.moveTo(primary, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static void checkKey(String key)
    {
        if (key == null || !key.matches("[A-Za-z0-9._-]+") || key.contains(".."))
        { throw new IllegalArgumentException("Invalid profile key"); }
    }
}
