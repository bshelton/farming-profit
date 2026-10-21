package com.farmingprofit;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.io.Reader;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(name = "Farming Profit", internalName = "farming-profit",
    description = "Farming-only harvest, supply cost and run history tracker",
    tags = {"farming", "profit", "herbs", "harvest", "fruit", "cactus"})
public class FarmingProfitPlugin extends Plugin
{
    private static final Pattern COMPOST = Pattern.compile("^You treat the .+ with (ultra|super)?compost\\.$", Pattern.CASE_INSENSITIVE);
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ItemManager items;
    @Inject private ClientToolbar toolbar;
    @Inject private ConfigManager configs;
    @Inject private FarmingProfitConfig config;
    @Inject private Gson gson;
    @Inject private ScheduledExecutorService executor;

    private FarmingProfitPanel panel;
    private NavigationButton navigation;
    private volatile boolean enabled;
    private String account;
    private boolean loaded;
    private boolean loadFailed;
    private long accountGeneration;
    private String storageStatus = "Log in to load your farming history.";
    private Data data = new Data();
    private CompletableFuture<Void> io = CompletableFuture.completedFuture(null);
    private Map<Integer, Integer> inventory;
    private final Map<Integer, Integer> sack = new HashMap<>();
    private int farmingXp = -1;
    private int tickXp;
    private int tick;
    private int actionUntil;
    private String action = "";
    private String patch = "";
    private WorldPoint patchPoint;
    private boolean harvestMessage;
    private int compostItem = -1;
    private boolean bottomless;
    private HerbHarvest herbPatch;
    private final TravelCosts travel = new TravelCosts();

    static final class Data
    {
        int version = 1;
        FarmRun active;
        List<FarmRun> history = new ArrayList<>();

        void validate()
        {
            if (version != 1 || history == null) { throw new IllegalStateException("Unsupported history format"); }
            List<FarmRun> runs = new ArrayList<>(history);
            if (active != null) { runs.add(active); }
            for (FarmRun run : runs)
            {
                if (run == null || run.entries == null || run.warnings == null || run.id == null)
                { throw new IllegalStateException("Invalid run data"); }
                for (FarmRun.Entry entry : run.entries)
                {
                    if (entry == null || entry.name == null || entry.patch == null || entry.source == null
                        || entry.quantity == 0 || entry.unitPrice < 0 || entry.unitPrice > 2_000_000_000L)
                    { throw new IllegalStateException("Invalid ledger entry"); }
                }
            }
        }
    }

    @Provides FarmingProfitConfig provideConfig(ConfigManager manager) { return manager.getConfig(FarmingProfitConfig.class); }

    @Override protected void startUp()
    {
        enabled = true;
        if (diagnosticsEnabled()) { log.debug("FP diagnostic session started: format=2 automatic={} travel={}", config.automatic(), config.travel()); }
        SwingUtilities.invokeLater(() -> {
            if (!enabled) { return; }
            panel = new FarmingProfitPanel(this);
            navigation = NavigationButton.builder().tooltip("Farming Profit").icon(BrandAssets.MARK).priority(7).panel(panel).build();
            toolbar.addNavigation(navigation);
        });
        clientThread.invoke(() -> { resetSignals(); refresh(); });
    }

    @Override protected void shutDown()
    {
        enabled = false;
        SwingUtilities.invokeLater(() -> { if (navigation != null) { toolbar.removeNavigation(navigation); } });
        clientThread.invoke(() -> {
            save(); accountGeneration++; account = null; loaded = false; data = new Data(); travel.clear(); resetSignals();
        });
    }

    private boolean diagnosticsEnabled()
    { return config.diagnostics() || Boolean.getBoolean("farmingprofit.devDiagnostics"); }

    private void resetSignals()
    {
        inventory = null; farmingXp = -1; clearAction(); clearTick();
    }
    private void clearAction() { action = ""; patchPoint = null; actionUntil = 0; herbPatch = null; }
    private void clearTick() { tickXp = 0; sack.clear(); harvestMessage = false; compostItem = -1; }

    @Subscribe public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            // Region loading is normal during a teleport; keep pending consumption across it.
            if (event.getGameState() != GameState.LOADING) { travel.clear(); }
            if (diagnosticsEnabled()) { log.debug("FP reset: state={} action={} pendingXp={}", event.getGameState(), action, tickXp); }
            resetSignals();
            if (event.getGameState() == GameState.LOGIN_SCREEN)
            {
                save(); accountGeneration++; account = null; loaded = false; data = new Data();
                storageStatus = "Log in to load your farming history."; refresh();
            }
        }
    }

    private void loadAccount(String key)
    {
        save(); account = key; loaded = false; loadFailed = false; data = new Data(); travel.clear(); resetSignals();
        long generation = ++accountGeneration;
        storageStatus = "Loading local history…"; refresh();
        io = io.handle((v, e) -> null).thenRunAsync(() -> {
            Data read = new Data(); String error = null;
            try
            {
                Filepath file = getPluginDirectory().join(key + ".json");
                if (file.exists())
                {
                    try (Reader reader = file.openBufferedReader()) { read = gson.fromJson(reader, Data.class); }
                    if (read == null) { throw new IllegalStateException("Empty history"); }
                    read.validate();
                }
            }
            catch (Exception ex) { log.warn("Unable to load farming history", ex); error = "History could not be loaded. Tracking paused to protect the saved file."; }
            Data result = read; String failure = error;
            clientThread.invoke(() -> {
                if (!enabled || !key.equals(account) || generation != accountGeneration) { return; }
                loadFailed = failure != null; loaded = !loadFailed;
                data = loadFailed ? new Data() : result;
                storageStatus = failure == null ? "Saved locally · GE estimates before tax" : failure;
                if (data.active != null) { data.active.warnings.add("Resumed after reconnect/restart. Verify any activity while tracking was offline."); }
                inventory = snapshot(); farmingXp = client.getSkillExperience(Skill.FARMING); refresh();
            });
        }, executor);
    }

    private void save()
    {
        if (!loaded || loadFailed || account == null) { return; }
        String key = account;
        String json = gson.toJson(data);
        io = io.handle((v, e) -> null).thenRunAsync(() -> {
            try
            {
                Filepath dir = getPluginDirectory(); dir.createDirectories();
                Filepath temporary = dir.join(key + ".pending");
                temporary.write(json);
                temporary.moveTo(dir.join(key + ".json"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (Exception ex)
            {
                log.warn("Unable to save farming history", ex);
                clientThread.invoke(() -> { if (key.equals(account)) { storageStatus = "Save failed — this run is only in memory. Check disk access."; refresh(); } });
            }
        }, executor);
    }

    private Map<Integer, Integer> snapshot()
    {
        Map<Integer, Integer> result = new HashMap<>();
        ItemContainer container = client.getItemContainer(InventoryID.INV);
        if (container != null)
        {
            for (Item item : container.getItems())
            {
                if (item.getId() < 0 || item.getQuantity() <= 0) { continue; }
                ItemComposition definition = items.getItemComposition(item.getId());
                int id = definition.getNote() != -1 ? definition.getLinkedNoteId() : item.getId();
                result.merge(id, item.getQuantity(), Integer::sum);
            }
        }
        return result;
    }

    @Subscribe public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (!loaded)
        {
            if (diagnosticsEnabled() && ("Pick".equalsIgnoreCase(event.getMenuOption()) || "Harvest".equalsIgnoreCase(event.getMenuOption())))
            { log.debug("FP menu skipped: history not ready loadFailed={}", loadFailed); }
            return;
        }
        String option = Text.removeTags(event.getMenuOption()).toLowerCase(Locale.ENGLISH);
        MenuAction type = event.getMenuAction();
        if (type != MenuAction.WIDGET_CONTINUE)
        {
            travel.clear();
            if (data.active != null && config.travel() && (type == MenuAction.CC_OP || type == MenuAction.CC_OP_LOW_PRIORITY))
            { travel.begin(event.getItemId(), option, inventoryCount(event.getItemId()), System.currentTimeMillis()); }
        }
        // Dialog choices may complete a gardener payment; all other non-patch actions cancel context.
        if (type == MenuAction.WIDGET_CONTINUE || type == MenuAction.CC_OP && "pay".equals(action)) { return; }
        boolean object = type == MenuAction.GAME_OBJECT_FIRST_OPTION || type == MenuAction.GAME_OBJECT_SECOND_OPTION
            || type == MenuAction.GAME_OBJECT_THIRD_OPTION || type == MenuAction.GAME_OBJECT_FOURTH_OPTION
            || type == MenuAction.GAME_OBJECT_FIFTH_OPTION || type == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT;
        if (object)
        {
            ObjectComposition definition = client.getObjectDefinition(event.getId());
            boolean farming = isFarmingVarbit(definition.getVarbitId());
            // Some menu entries identify a transformed crop rather than its patch controller.
            ObjectComposition crop = definition.getImpostorIds() == null ? definition : definition.getImpostor();
            if (crop != null && crop.getActions() != null)
            {
                farming |= java.util.Arrays.stream(crop.getActions()).anyMatch(a ->
                    "Inspect".equalsIgnoreCase(a) || "Check-health".equalsIgnoreCase(a) || "Rake".equalsIgnoreCase(a));
            }
            if (diagnosticsEnabled() && (farming || option.startsWith("pick") || option.equals("harvest")))
            { log.debug("FP object: option={} type={} id={} baseName={} cropName={} varbit={} state={} farming={}", option, type, event.getId(), definition.getName(), crop == null ? null : crop.getName(), definition.getVarbitId(), isFarmingVarbit(definition.getVarbitId()) ? client.getVarbitValue(definition.getVarbitId()) : -1, farming); }
            if (farming)
            {
                patchPoint = WorldPoint.fromScene(client, event.getParam0(), event.getParam1(), client.getPlane());
                patch = Text.removeTags(event.getMenuTarget());
                int arrow = patch.lastIndexOf(" -> ");
                if (arrow >= 0) { patch = patch.substring(arrow + 4); }
                patch += " @ " + patchPoint.getX() + "," + patchPoint.getY();
                action = option.startsWith("pick") || option.equals("harvest") || option.equals("chop-down") || option.equals("cut") ? "harvest" : "plant";
                if (option.equals("check-health")) { action = "health"; }
                int patchState = isFarmingVarbit(definition.getVarbitId()) ? client.getVarbitValue(definition.getVarbitId()) : -1;
                herbPatch = crop == null ? null : HerbHarvest.fromCrop(patchState, crop.getName());
                actionUntil = tick + 30;
                bottomless = Text.removeTags(event.getMenuTarget()).toLowerCase(Locale.ENGLISH).contains("bottomless");
                if (diagnosticsEnabled()) { log.debug("Farming action={} patch={} object={} crop={} controller={} state={} herb={}", action, patch, event.getId(), crop == null ? null : crop.getName(), definition.getVarbitId(), patchState, herbPatch); }
                return;
            }
        }
        if (option.startsWith("pay"))
        {
            NPC npc = event.getMenuEntry().getNpc();
            if (npc != null)
            {
                NPCComposition def = npc.getTransformedComposition();
                if (def != null && def.getActions() != null && java.util.Arrays.stream(def.getActions())
                    .anyMatch(a -> a != null && a.toLowerCase(Locale.ENGLISH).startsWith("pay")))
                {
                    // Only accept payment beside a patch we have interacted with in this run.
                    if (patchPoint != null && patchPoint.distanceTo(npc.getWorldLocation()) <= 15)
                    { action = "pay"; actionUntil = tick + 50; return; }
                }
            }
        }
        if (diagnosticsEnabled() && !action.isEmpty())
        { log.debug("FP context cancelled: previousAction={} menuType={} menuOption={}", action, type, option); }
        clearAction();
    }

    private static boolean isFarmingVarbit(int id)
    {
        return id == VarbitID.FARMING_TRANSMIT_A || id == VarbitID.FARMING_TRANSMIT_B
            || id == VarbitID.FARMING_TRANSMIT_C || id == VarbitID.FARMING_TRANSMIT_D
            || id == VarbitID.FARMING_TRANSMIT_E || id == VarbitID.FARMING_TRANSMIT_F
            || id == VarbitID.FARMING_TRANSMIT_G || id == VarbitID.FARMING_TRANSMIT_H
            || id == VarbitID.FARMING_TRANSMIT_I || id == VarbitID.FARMING_TRANSMIT_J
            || id == VarbitID.FARMING_TRANSMIT_K || id == VarbitID.FARMING_TRANSMIT_L
            || id == VarbitID.FARMING_TRANSMIT_M || id == VarbitID.FARMING_TRANSMIT_N
            || id == VarbitID.FARMING_TRANSMIT_O || id == VarbitID.FARMING_TRANSMIT_P
            || id == VarbitID.FARMING_TRANSMIT_A1 || id == VarbitID.FARMING_TRANSMIT_A2
            || id == VarbitID.FARMING_TRANSMIT_B1 || id == VarbitID.FARMING_TRANSMIT_B2
            || id == VarbitID.FARMING_TRANSMIT_C1 || id == VarbitID.FARMING_TRANSMIT_C2
            || id == VarbitID.FARMING_TRANSMIT_D1 || id == VarbitID.FARMING_TRANSMIT_D2
            || id == VarbitID.FARMING_TRANSMIT_E1 || id == VarbitID.FARMING_TRANSMIT_E2
            || id == VarbitID.FARMING_TRANSMIT_F1 || id == VarbitID.FARMING_TRANSMIT_F2;
    }

    private int inventoryCount(int id)
    {
        ItemContainer container = client.getItemContainer(InventoryID.INV);
        if (container == null || id < 0) { return 0; }
        int count = 0;
        for (Item item : container.getItems()) { if (item.getId() == id) { count += item.getQuantity(); } }
        return count;
    }

    @Subscribe public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() != InventoryID.INV || travel.pendingItem() < 0) { return; }
        if (!enabled || !loaded || data.active == null || !config.travel()) { travel.clear(); return; }
        int id = travel.consumed(inventoryCount(travel.pendingItem()), System.currentTimeMillis());
        if (id < 0) { return; }
        long price = items.getItemPrice(id);
        data.active.add(id, items.getItemComposition(id).getName(), 1, price, true, "Travel", "Teleport item consumed", System.currentTimeMillis());
        if (price == 0) { data.active.warnings.add("Travel item has no GE price; add a manual travel cost if needed."); }
        if (diagnosticsEnabled()) { log.debug("Travel consumption: item={} cost={}", id, price); }
        save(); refresh();
    }

    @Subscribe public void onStatChanged(StatChanged event)
    {
        if (event.getSkill() != Skill.FARMING) { return; }
        int delta = farmingXp < 0 ? 0 : Math.max(0, event.getXp() - farmingXp);
        tickXp += delta;
        if (diagnosticsEnabled() && (delta > 0 || farmingXp < 0))
        { log.debug("FP farming XP: delta={} baselineKnown={} tickXp={} action={} herb={} loaded={} tick={}", delta, farmingXp >= 0, tickXp, action, herbPatch, loaded, tick); }
        farmingXp = event.getXp();
    }

    @Subscribe public void onChatMessage(ChatMessage event)
    {
        if (!loaded || action.isEmpty() || tick > actionUntil) { return; }
        if (event.getType() != ChatMessageType.SPAM && event.getType() != ChatMessageType.GAMEMESSAGE) { return; }
        String text = Text.removeTags(event.getMessage());
        String herb = HarvestSignals.sackHerb(text);
        if (herb != null && action.equals("harvest"))
        {
            Integer id = CropCatalog.herbId(herb);
            if (id != null) { sack.merge(id, 1, Integer::sum); }
        }
        harvestMessage |= text.startsWith("You pick ") || text.startsWith("You harvest ");
        Matcher matcher = COMPOST.matcher(text);
        if (matcher.matches())
        {
            String kind = matcher.group(1);
            compostItem = "ultra".equalsIgnoreCase(kind) ? ItemID.BUCKET_ULTRACOMPOST
                : "super".equalsIgnoreCase(kind) ? ItemID.BUCKET_SUPERCOMPOST : ItemID.BUCKET_COMPOST;
        }
        if (diagnosticsEnabled() && (herb != null || text.startsWith("You pick ") || text.startsWith("You harvest ") || compostItem != -1))
        { log.debug("FP chat evidence: action={} sackHerb={} harvestMessage={} compostItem={}", action, herb, harvestMessage, compostItem); }
        if (text.startsWith("The ") && text.contains("has been treated with") && data.active != null)
        { data.active.warnings.add("Fertile Soil spell supplies need a manual cost adjustment."); }
    }

    @Subscribe public void onGameTick(GameTick event)
    {
        if (!enabled || client.getGameState() != GameState.LOGGED_IN) { return; }
        tick++;
        if (client.getAccountHash() == 0 || configs.getRSProfileKey() == null) { return; }
        String key = configs.getRSProfileKey();
        if (!key.equals(account)) { loadAccount(key); return; }
        if (!loaded) { return; }
        Map<Integer, Integer> now = snapshot();
        if (inventory == null) { if (diagnosticsEnabled()) { log.debug("FP inventory baseline reset: discardedTickXp={}", tickXp); } inventory = now; farmingXp = client.getSkillExperience(Skill.FARMING); clearTick(); return; }
        boolean context = patchPoint != null && tick <= actionUntil && client.getLocalPlayer() != null
            && patchPoint.distanceTo(client.getLocalPlayer().getWorldLocation()) <= 12;
        boolean evidence = tickXp > 0 || harvestMessage || !sack.isEmpty();
        boolean changed = false;
        if (diagnosticsEnabled() && evidence)
        {
            String reason = patchPoint == null ? "no-patch-context" : tick > actionUntil ? "context-expired"
                : client.getLocalPlayer() == null ? "no-local-player"
                : !context ? "too-far-from-patch" : !action.equals("harvest") ? "not-harvesting"
                : herbPatch == null ? "unknown-herb" : !hasOpenSack(now) ? "no-open-sack"
                : !normalXpWorld() ? "unsupported-xp-world" : "eligible";
            log.debug("FP harvest gate: reason={} action={} herb={} tickXp={} tick={} expires={} activeRun={} automatic={} sackIds={}",
                reason, action, herbPatch, tickXp, tick, actionUntil, data.active != null, config.automatic(),
                now.keySet().stream().filter(id -> id == ItemID.SLAYER_HERB_SACK || id == ItemID.SLAYER_HERB_SACK_OPEN
                    || id == ItemID.SLAYER_HERB_SACK_SILK || id == ItemID.SLAYER_HERB_SACK_SILK_OPEN).collect(java.util.stream.Collectors.toList()));
        }
        if (context)
        {
            if (evidence) { actionUntil = tick + 30; }
            Map<Integer, Integer> gains = HarvestSignals.harvest(inventory, now, sack, action.equals("harvest"), evidence);
            for (Map.Entry<Integer, Integer> gain : gains.entrySet())
            {
                if (CropCatalog.CROPS.containsKey(gain.getKey()))
                {
                    changed |= record(gain.getKey(), gain.getValue(), false, "Harvest", -1);
                }
            }
            boolean inferredHarvest = false;
            if (diagnosticsEnabled() && action.equals("harvest") && tickXp > 0)
            { log.debug("Harvest eligibility: herb={} xp={} openSack={} normalXp={}", herbPatch, tickXp, hasOpenSack(now), normalXpWorld()); }
            if (action.equals("harvest") && herbPatch != null && hasOpenSack(now) && normalXpWorld())
            {
                int observed = gains.getOrDefault(herbPatch.itemId, 0);
                int missing = herbPatch.missingCount(tickXp, outfitMultiplier(), observed);
                if (diagnosticsEnabled() && tickXp > 0) { log.debug("FP herb reconciliation: herb={} xp={} multiplier={} inferred={} observed={} missing={} reason={}", herbPatch, tickXp, outfitMultiplier(), herbPatch.inferredCount(tickXp, outfitMultiplier()), observed, missing, herbPatch.inferredCount(tickXp, outfitMultiplier()) == 0 ? "xp-not-matched" : missing == 0 ? "already-observed" : "record-inferred"); }
                if (missing > 0)
                {
                    inferredHarvest = record(herbPatch.itemId, missing, false, "Sack harvest — XP inference", -1);
                    changed |= inferredHarvest;
                    if (data.active != null)
                    { data.active.warnings.add("Sack yield includes XP-inferred herbs. Verify against the sack before/after harvesting; chat messages are not required."); }
                }
            }
            if ((action.equals("plant") && tickXp > 0) || action.equals("pay"))
            {
                for (Map.Entry<Integer, Integer> old : inventory.entrySet())
                {
                    int used = old.getValue() - now.getOrDefault(old.getKey(), 0);
                    if (used > 0 && (action.equals("pay") || CropCatalog.plantingSupply(items.getItemComposition(old.getKey()).getName())))
                    { changed |= record(old.getKey(), used, true, action.equals("pay") ? "Gardener payment" : "Planting", -1); }
                }
            }
            if (compostItem != -1 && config.compost())
            {
                long value = items.getItemPrice(compostItem);
                // A bottomless bucket doubles each bucket added. Treat the per-use price as an estimate.
                changed |= record(compostItem, 1, true, bottomless ? "Bottomless compost (half bucket)" : "Compost", bottomless ? (value + 1) / 2 : value);
            }
            if (tickXp > 0 && ensureRun())
            {
                data.active.farmingXp += tickXp; data.active.lastActivity = System.currentTimeMillis(); changed = true;
                if (action.equals("plant") && !changedPlanting(now))
                { data.active.warnings.add("Some farming XP has no visible seed/sapling cost. Check seed-box and spell costs if used."); }
                if (action.equals("harvest") && gains.isEmpty() && !inferredHarvest)
                { data.active.warnings.add("A harvest had no visible produce signal. Check counts, especially sack harvests."); }
            }
        }
        inventory = now; clearTick();
        if (data.active != null && System.currentTimeMillis() - data.active.lastActivity >= config.idleMinutes() * 60_000L)
        { finish(); return; }
        if (changed) { save(); refresh(); }
    }

    private boolean changedPlanting(Map<Integer, Integer> now)
    {
        return inventory.entrySet().stream().anyMatch(e -> e.getValue() > now.getOrDefault(e.getKey(), 0)
            && CropCatalog.plantingSupply(items.getItemComposition(e.getKey()).getName()));
    }

    private static boolean hasOpenSack(Map<Integer, Integer> inventory)
    {
        return inventory.containsKey(ItemID.SLAYER_HERB_SACK_OPEN) || inventory.containsKey(ItemID.SLAYER_HERB_SACK_SILK_OPEN);
    }
    private boolean normalXpWorld()
    {
        return !client.getWorldType().contains(WorldType.SEASONAL) && !client.getWorldType().contains(WorldType.DEADMAN)
            && !client.getWorldType().contains(WorldType.TOURNAMENT_WORLD);
    }
    private double outfitMultiplier()
    {
        ItemContainer worn = client.getItemContainer(InventoryID.WORN);
        if (worn == null) { return 1; }
        int bonus = 0;
        for (Item item : worn.getItems())
        {
            if (item.getId() <= 0) { continue; }
            String name = items.getItemComposition(item.getId()).getName().toLowerCase(Locale.ENGLISH);
            if (name.startsWith("farmer's strawhat")) { bonus += 4; }
            else if (name.equals("farmer's jacket") || name.equals("farmer's shirt")) { bonus += 8; }
            else if (name.equals("farmer's boro trousers")) { bonus += 6; }
            else if (name.equals("farmer's boots")) { bonus += 2; }
        }
        return 1 + (bonus == 20 ? 25 : bonus) / 1000.0;
    }

    private boolean ensureRun()
    {
        if (!loaded || loadFailed) { return false; }
        if (data.active == null)
        {
            if (!config.automatic()) { return false; }
            data.active = new FarmRun(System.currentTimeMillis());
        }
        return true;
    }

    private boolean record(int id, int count, boolean cost, String source, long override)
    {
        if (!ensureRun())
        {
            if (diagnosticsEnabled()) { log.debug("FP record skipped: item={} count={} loaded={} loadFailed={} automatic={}", id, count, loaded, loadFailed, config.automatic()); }
            return false;
        }
        long price = override >= 0 ? override : id == ItemID.COINS ? 1 : items.getItemPrice(id);
        String name = items.getItemComposition(id).getName();
        data.active.add(id, name, count, price, cost, patch, source, System.currentTimeMillis());
        if (diagnosticsEnabled()) { log.debug("FP ledger recorded: item={} count={} unitPrice={} cost={} source={}", id, count, price, cost, source); }
        if (price == 0) { data.active.warnings.add("No GE price for " + name + "; valued at 0 gp. Add a correction if needed."); }
        return true;
    }

    void startManual() { clientThread.invoke(() -> { if (loaded && data.active == null) { data.active = new FarmRun(System.currentTimeMillis()); save(); refresh(); } }); }
    boolean[] settingsValues() { return new boolean[]{config.automatic(), config.compost(), config.diagnostics(), config.travel()}; }
    int idleMinutes() { return config.idleMinutes(); }
    void updateSettings(boolean automatic, boolean compost, boolean diagnostics, boolean travelCosts, int idle)
    {
        configs.setConfiguration("farming-profit", "automatic", automatic);
        configs.setConfiguration("farming-profit", "compost", compost);
        configs.setConfiguration("farming-profit", "diagnostics", diagnostics);
        configs.setConfiguration("farming-profit", "travel", travelCosts);
        configs.setConfiguration("farming-profit", "idleMinutes", idle);
    }
    void finishManual() { clientThread.invoke(this::finish); }
    private void finish()
    {
        if (!loaded || data.active == null) { return; }
        data.active.finishedAt = System.currentTimeMillis();
        data.history.add(0, data.active); data.active = null; travel.clear(); clearAction(); clearTick(); save(); refresh();
    }

    void adjust(int id, int quantity, long price, boolean cost, String reason)
    {
        clientThread.invoke(() -> {
            if (!loaded) { return; }
            if (data.active == null) { data.active = new FarmRun(System.currentTimeMillis()); }
            String name = id == ItemID.COINS ? "GP adjustment" : items.getItemComposition(id).getName();
            data.active.add(id, name, quantity, price, cost, "Manual", reason, System.currentTimeMillis());
            save(); refresh();
        });
    }

    private void refresh()
    {
        // Deep snapshot before crossing from the client thread to Swing.
        Data copy = gson.fromJson(gson.toJson(data), Data.class);
        String status = storageStatus; boolean ready = loaded;
        SwingUtilities.invokeLater(() -> { if (enabled) { panel.render(copy, status, ready); } });
    }
}
