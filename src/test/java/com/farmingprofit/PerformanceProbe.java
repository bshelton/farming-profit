package com.farmingprofit;

import com.google.gson.Gson;
import java.nio.file.Paths;
import java.util.Arrays;
import net.runelite.client.util.Filepath;

/** Synthetic data only; does not start or interact with RuneScape. Timings are not FPS measurements. */
public final class PerformanceProbe
{
    public static void main(String[] args) throws Exception
    {
        FarmingProfitPlugin.Data data = new FarmingProfitPlugin.Data();
        for (int r = 0; r < 1000; r++)
        {
            FarmRun run = new FarmRun(r);
            for (int e = 0; e < 100; e++) { run.add(1, "Synthetic crop", 1, 5000, false, "Synthetic patch", "Test", r + e); }
            data.history.add(run);
        }
        data.active = new FarmRun(1001);
        for (int i = 0; i < 100; i++) { data.active.add(1, "Synthetic crop", 1, 5000, false, "Synthetic patch", "Test", 1001 + i); }
        long start = System.nanoTime();
        FarmingProfitPlugin.Data first = data.snapshot();
        System.out.printf("First history snapshot (100,000 completed entries): %.3f ms%n", elapsed(start));
        long[] times = new long[1000];
        for (int i = 0; i < 1000; i++)
        {
            start = System.nanoTime(); FarmingProfitPlugin.Data copy = data.snapshot(); times[i] = System.nanoTime() - start;
            if (copy.history != first.history) { throw new AssertionError("History cache was not reused"); }
        }
        Arrays.sort(times);
        System.out.printf("Cached snapshot (100 active entries): median %.3f ms, p95 %.3f ms%n", times[500] / 1e6, times[950] / 1e6);
        Filepath dir = Filepath.Unchecked.getRooted(Paths.get(args[0])); dir.createDirectories();
        HistoryStore store = new HistoryStore(new Gson());
        start = System.nanoTime(); store.save(dir, "synthetic", first);
        System.out.printf("Background save: %.3f ms; file size: %d bytes%n", elapsed(start), dir.join("synthetic.json").size());
        start = System.nanoTime(); store.load(dir, "synthetic");
        System.out.printf("Background load and validation: %.3f ms%n", elapsed(start));
    }
    private static double elapsed(long start) { return (System.nanoTime() - start) / 1e6; }
}
