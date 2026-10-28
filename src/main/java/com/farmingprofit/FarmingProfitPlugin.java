package com.farmingprofit;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(name = "Farming Run Profit", internalName = "farming-profit",
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
    @Inject private HistoryStore historyStore;
    @Inject private ScheduledExecutorService executor;
    @Inject private OverlayManager overlays;
    @Inject private FarmingReminderOverlay reminderOverlay;

    private FarmingProfitPanel panel;
    private NavigationButton navigation;
    private volatile boolean enabled;
    private volatile long lifecycleGeneration;
    private String account;
    private boolean loaded;
    private boolean loadFailed;
    private long accountGeneration;
    private String storageStatus = "Log in to load your farming history.";
    private Data data = new Data();
    private CompletableFuture<Void> io = CompletableFuture.completedFuture(null);
    private final PendingWrites pendingWrites = new PendingWrites();
    private final AtomicReference<Runnable> pendingRender = new AtomicReference<>();
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
    private int herbVarbit = -1;
    private final TravelCosts travel = new TravelCosts();
    private WorldPoint reminderPoint;
    private int reminderUntil;
    private boolean reminderHarvest;
    private boolean reminderHerb;
    private boolean reminderBenefits;
    private List<String> reminders = List.of();
    private volatile boolean highlightClosedSack;

    static final class Data
    {
        int version = 1;
        FarmRun active;
        List<FarmRun> history = new ArrayList<>();
        private transient List<FarmRun> historySnapshot;

        Data snapshot()
        {
            // Completed runs are immutable; invalidate the cache when history membership changes.
            if (historySnapshot == null || historySnapshot.size() != history.size())
            {
                List<FarmRun> copy = new ArrayList<>(history.size());
                for (FarmRun run : history) { copy.add(run.snapshot()); }
                historySnapshot = java.util.Collections.unmodifiableList(copy);
            }
            Data copy = new Data(); copy.version = version;
            copy.active = active == null ? null : active.snapshot();
            copy.history = historySnapshot;
            return copy;
        }

        boolean trackingPaused() { return active != null && active.paused; }

        boolean matchesAction(long expectedGeneration, long currentGeneration, String expectedId)
        {
            return expectedGeneration == currentGeneration
                && java.util.Objects.equals(expectedId, active == null ? null : active.id);
        }

        boolean deleteActive(String expectedId)
        {
            if (active == null || !active.id.equals(expectedId)) { return false; }
            active = null;
            return true;
        }

        boolean deleteHistory(long expectedGeneration, long currentGeneration, String expectedId)
        {
            if (expectedGeneration != currentGeneration || expectedId == null) { return false; }
            boolean removed = history.removeIf(run -> expectedId.equals(run.id));
            if (removed) { historySnapshot = null; }
            return removed;
        }

        void validate()
        {
            if (version != 1 || history == null) { throw new IllegalStateException("Unsupported history format"); }
            List<FarmRun> runs = new ArrayList<>(history);
            java.util.Set<String> ids = new java.util.HashSet<>();
            if (active != null) { runs.add(active); }
            for (FarmRun run : runs)
            {
                if (run == null || run.entries == null || run.warnings == null || run.id == null || run.id.isEmpty()
                    || !ids.add(run.id) || run.warnings.contains(null) || run.farmingXp < 0
                    || !validTime(run.startedAt) || !validTime(run.finishedAt) || !validTime(run.lastActivity))
                { throw new IllegalStateException("Invalid run data"); }
                long harvest = 0; long costs = 0;
                for (FarmRun.Entry entry : run.entries)
                {
                    if (entry == null || entry.name == null || entry.patch == null || entry.source == null
                        || entry.quantity == 0 || entry.unitPrice < 0 || entry.unitPrice > 2_000_000_000L || !validTime(entry.at))
                    { throw new IllegalStateException("Invalid ledger entry"); }
                    long value = Math.multiplyExact(entry.unitPrice, entry.quantity);
                    if (entry.cost) { costs = Math.addExact(costs, value); } else { harvest = Math.addExact(harvest, value); }
                }
                Math.subtractExact(harvest, costs);
            }
        }

        private static boolean validTime(long value) { return value >= 0 && value <= 253402300799999L; }
    }

    @Provides FarmingProfitConfig provideConfig(ConfigManager manager) { return manager.getConfig(FarmingProfitConfig.class); }

    @Override protected void startUp()
    {
        long lifecycle = ++lifecycleGeneration;
        enabled = true;
        overlays.add(reminderOverlay);
        if (diagnosticsEnabled()) { log.debug("FP diagnostic session started: format=2 automatic={} travel={}", config.automatic(), config.travel()); }
        SwingUtilities.invokeLater(() -> {
            if (!enabled || lifecycle != lifecycleGeneration) { return; }
            panel = new FarmingProfitPanel(this);
            navigation = NavigationButton.builder().tooltip("Farming Run Profit").icon(BrandAssets.MARK).priority(7).panel(panel).build();
            toolbar.addNavigation(navigation);
        });
        clientThread.invoke(() -> { if (enabled && lifecycle == lifecycleGeneration) { resetSignals(); refresh(); } });
    }

    @Override protected void shutDown()
    {
        enabled = false;
        lifecycleGeneration++;
        highlightClosedSack = false;
        overlays.remove(reminderOverlay);
        NavigationButton oldNavigation = navigation;
        FarmingProfitPanel oldPanel = panel;
        SwingUtilities.invokeLater(() -> {
            if (oldNavigation != null) { toolbar.removeNavigation(oldNavigation); }
            if (oldPanel != null) { oldPanel.closeLedgers(); }
        });
        clientThread.invoke(() -> {
            save(); accountGeneration++; account = null; loaded = false; data = new Data(); travel.clear(); resetSignals();
        });
    }

    private boolean diagnosticsEnabled()
    { return config.diagnostics() || Boolean.getBoolean("farmingprofit.devDiagnostics"); }

    private boolean trackingPaused() { return data.trackingPaused(); }

    private void freshTrackingBaseline()
    {
        travel.clear(); clearAction(); clearTick();
        inventory = snapshot(); farmingXp = client.getSkillExperience(Skill.FARMING);
    }

    private void resetSignals()
    {
        clearReminders();
        inventory = null; farmingXp = -1; clearAction(); clearTick();
    }
    private void clearAction() { action = ""; patchPoint = null; actionUntil = 0; herbPatch = null; herbVarbit = -1; }
    private void clearTick() { tickXp = 0; sack.clear(); harvestMessage = false; compostItem = -1; }

    @Subscribe public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOADING)
        {
            // Crossing a map boundary is not a logout. Keep the clicked world point and
            // XP/inventory baselines so an in-progress harvest survives the scene rebuild.
            // Reconciliation still requires the same account, proximity and unexpired context.
            if (diagnosticsEnabled()) { log.debug("FP region loading: preserving action={} herb={} pendingXp={}", action, herbPatch, tickXp); }
        }
        if (resetsTracking(event.getGameState()))
        {
            travel.clear();
            if (diagnosticsEnabled()) { log.debug("FP reset: state={} action={} pendingXp={}", event.getGameState(), action, tickXp); }
            resetSignals();
            if (event.getGameState() == GameState.LOGIN_SCREEN)
            {
                save(); accountGeneration++; account = null; loaded = false; data = new Data();
                storageStatus = "Log in to load your farming history."; refresh();
            }
        }
        if (event.getGameState() != GameState.LOADING) { refresh(); }
    }

    static boolean resetsTracking(GameState state)
    {
        return state != GameState.LOGGED_IN && state != GameState.LOADING;
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
                read = historyStore.load(getPluginDirectory(), key);
            }
            catch (Exception ex) { log.warn("Unable to load farming history", ex); error = "History could not be loaded. Tracking paused to protect the saved file."; }
            Data result = read; String failure = error;
            clientThread.invoke(() -> {
                if (!enabled || !key.equals(account) || generation != accountGeneration) { return; }
                loadFailed = failure != null; loaded = !loadFailed;
                data = loadFailed ? new Data() : result;
                storageStatus = failure == null ? "Saved locally · GE estimates before tax" : failure;
                if (data.active != null && !data.active.paused) { data.active.warnings.add("Resumed after reconnect/restart. Verify any activity while tracking was offline."); }
                inventory = snapshot(); farmingXp = client.getSkillExperience(Skill.FARMING); refresh();
            });
        }, executor);
    }

    private void save()
    {
        if (!loaded || loadFailed || account == null) { return; }
        String key = account;
        long generation = accountGeneration;
        if (!pendingWrites.offer(key, data.snapshot())) { return; }
        io = io.handle((v, e) -> null).thenRunAsync(() -> {
            Data snapshot = pendingWrites.take(key);
            try
            {
                historyStore.save(getPluginDirectory(), key, snapshot);
                clientThread.invoke(() -> {
                    if (enabled && key.equals(account) && generation == accountGeneration && storageStatus.startsWith("Save failed"))
                    { storageStatus = "Saved locally · GE estimates before tax"; refresh(); }
                });
            }
            catch (Exception ex)
            {
                log.warn("Unable to save farming history", ex);
                clientThread.invoke(() -> { if (enabled && key.equals(account) && generation == accountGeneration) { storageStatus = "Save failed — recent changes are only in memory. Check disk access."; refresh(); } });
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
        if (!currentProfile())
        {
            if (diagnosticsEnabled() && ("Pick".equalsIgnoreCase(event.getMenuOption()) || "Harvest".equalsIgnoreCase(event.getMenuOption())))
            { log.debug("FP menu skipped: history not ready loadFailed={}", loadFailed); }
            return;
        }
        inspectReminderInteraction(event);
        if (trackingPaused()) { return; }
        String option = Text.removeTags(event.getMenuOption()).toLowerCase(Locale.ENGLISH);
        MenuAction type = event.getMenuAction();
        // Dismissing a menu, examining, or selecting an item does not stop server-side harvesting.
        if (preservesFarmingContext(type, event.getParam1(), option)
            || HarvestSignals.isSackCheck(type, event.getItemId(), option)) { return; }
        // Settle signals already received for the previous action before this click replaces its context.
        if (!action.isEmpty() && client.getGameState() == GameState.LOGGED_IN) { reconcileSignals(); }
        if (type != MenuAction.WIDGET_CONTINUE)
        {
            travel.clear();
            if (data.active != null && config.travel() && (type == MenuAction.CC_OP || type == MenuAction.CC_OP_LOW_PRIORITY))
            { travel.begin(event.getItemId(), option, inventoryCount(event.getItemId()), System.currentTimeMillis()); }
            if (diagnosticsEnabled() && TravelCosts.supports(event.getItemId()))
            {
                log.debug("FP travel click: item={} option={} type={} count={} active={} enabled={} armed={}",
                    event.getItemId(), option, type, inventoryCount(event.getItemId()),
                    data.active != null, config.travel(), travel.pendingItem() >= 0);
            }
        }
        // Dialog choices may complete a gardener payment; all other non-patch actions cancel context.
        if (type == MenuAction.WIDGET_CONTINUE || type == MenuAction.CC_OP && "pay".equals(action)
            && (event.getParam1() >>> 16) == InterfaceID.CHATMENU) { return; }
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
                action = HarvestSignals.patchAction(option, Text.removeTags(event.getMenuTarget()));
                int patchState = isFarmingVarbit(definition.getVarbitId()) ? client.getVarbitValue(definition.getVarbitId()) : -1;
                herbPatch = crop == null ? null : HerbHarvest.fromCrop(patchState, crop.getName());
                herbVarbit = herbPatch == null ? -1 : definition.getVarbitId();
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
        { log.debug("FP context cancelled: previousAction={} menuType={} menuOption={} widget={}", action, type, option, event.getParam1()); }
        clearAction();
    }

    static boolean preservesFarmingContext(MenuAction type, int widgetId, String option)
    {
        if (preservesFarmingContext(type)) { return true; }
        if ((type == MenuAction.GAME_OBJECT_FIRST_OPTION || type == MenuAction.GAME_OBJECT_SECOND_OPTION
            || type == MenuAction.GAME_OBJECT_THIRD_OPTION || type == MenuAction.GAME_OBJECT_FOURTH_OPTION
            || type == MenuAction.GAME_OBJECT_FIFTH_OPTION) && (option.equals("inspect") || option.equals("guide")))
        { return true; }
        if (type != MenuAction.CC_OP && type != MenuAction.CC_OP_LOW_PRIORITY) { return false; }
        // Match tab components, not their labels (which vary by layout and selected tab).
        return between(widgetId, InterfaceID.Toplevel.STONE7, InterfaceID.Toplevel.ICON13)
            || between(widgetId, InterfaceID.Toplevel.STONE0, InterfaceID.Toplevel.ICON6)
            || between(widgetId, InterfaceID.ToplevelOsrsStretch.STONE7, InterfaceID.ToplevelOsrsStretch.ICON13)
            || between(widgetId, InterfaceID.ToplevelOsrsStretch.STONE0, InterfaceID.ToplevelOsrsStretch.ICON6)
            || between(widgetId, InterfaceID.ToplevelPreEoc.STONE10, InterfaceID.ToplevelPreEoc.ICON10)
            || between(widgetId, InterfaceID.ToplevelPreEoc.STONE7, InterfaceID.ToplevelPreEoc.ICON13)
            || between(widgetId, InterfaceID.ToplevelPreEoc.STONE0, InterfaceID.ToplevelPreEoc.ICON6)
            || between(widgetId, InterfaceID.ToplevelOsm.STONE10, InterfaceID.ToplevelOsm.ICON10)
            || between(widgetId, InterfaceID.ToplevelOsm.STONE1, InterfaceID.ToplevelOsm.ICON11)
            || between(widgetId, InterfaceID.ToplevelOsm.STONE3, InterfaceID.ToplevelOsm.STONE2)
            || between(widgetId, InterfaceID.ToplevelOsm.ICON3, InterfaceID.ToplevelOsm.ICON2);
    }

    private static boolean between(int id, int first, int last) { return id >= first && id <= last; }

    static boolean preservesFarmingContext(MenuAction type)
    {
        // Client-only menus (for example player Lookup) do not interrupt server-side harvesting.
        if (type == MenuAction.RUNELITE || type == MenuAction.RUNELITE_PLAYER
            || type == MenuAction.RUNELITE_WIDGET || type == MenuAction.RUNELITE_HIGH_PRIORITY
            || type == MenuAction.RUNELITE_LOW_PRIORITY || type == MenuAction.RUNELITE_OVERLAY
            || type == MenuAction.RUNELITE_OVERLAY_CONFIG || type == MenuAction.RUNELITE_INFOBOX)
        { return true; }
        return type == MenuAction.CANCEL || type == MenuAction.WIDGET_TARGET
            || type == MenuAction.EXAMINE_OBJECT || type == MenuAction.EXAMINE_NPC
            || type == MenuAction.EXAMINE_ITEM || type == MenuAction.EXAMINE_ITEM_GROUND;
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
        if (!currentProfile()) { return; }
        if (reminderPoint != null && (event.getContainerId() == InventoryID.INV || event.getContainerId() == InventoryID.WORN))
        { updateReminders(); }
        if (event.getContainerId() != InventoryID.INV || travel.pendingItem() < 0) { return; }
        if (!enabled || !loaded || data.active == null || trackingPaused() || !config.travel()) { travel.clear(); return; }
        int pending = travel.pendingItem();
        int remaining = inventoryCount(pending);
        int id = travel.consumed(remaining, System.currentTimeMillis());
        if (diagnosticsEnabled())
        { log.debug("FP travel inventory: item={} remaining={} consumed={} pending={}", pending, remaining, id >= 0, travel.pendingItem() >= 0); }
        if (id < 0) { return; }
        long price = items.getItemPrice(id);
        data.active.add(id, items.getItemComposition(id).getName(), 1, price, true, "Travel", "Teleport item consumed", System.currentTimeMillis());
        if (price == 0) { data.active.warnings.add("Travel item has no GE price; add a manual travel cost if needed."); }
        if (diagnosticsEnabled()) { log.debug("Travel consumption: item={} cost={}", id, price); }
        save(); refresh();
    }

    @Subscribe public void onStatChanged(StatChanged event)
    {
        if (event.getSkill() != Skill.FARMING || !currentProfile()) { return; }
        if (reminderPoint != null && event.getXp() > farmingXp) { reminderUntil = tick + 30; }
        if (trackingPaused()) { farmingXp = event.getXp(); tickXp = 0; return; }
        int delta = farmingXp < 0 ? 0 : Math.max(0, event.getXp() - farmingXp);
        tickXp += delta;
        if (diagnosticsEnabled() && (delta > 0 || farmingXp < 0))
        { log.debug("FP farming XP: delta={} baselineKnown={} tickXp={} action={} herb={} loaded={} tick={}", delta, farmingXp >= 0, tickXp, action, herbPatch, loaded, tick); }
        farmingXp = event.getXp();
    }

    @Subscribe public void onChatMessage(ChatMessage event)
    {
        if (!currentProfile() || trackingPaused() || action.isEmpty() || tick > actionUntil) { return; }
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
        {
            if (data.active.warnings.add("Fertile Soil spell supplies need a manual cost adjustment.")) { save(); refresh(); }
        }
    }

    @Subscribe public void onGameTick(GameTick event)
    {
        if (!enabled || client.getGameState() != GameState.LOGGED_IN) { return; }
        tick++;
        if (client.getAccountHash() == 0 || configs.getRSProfileKey() == null) { return; }
        String key = configs.getRSProfileKey();
        if (!key.equals(account)) { loadAccount(key); return; }
        if (!loaded) { return; }
        if (reminderPoint != null && (tick > reminderUntil || client.getLocalPlayer() == null
            || reminderPoint.distanceTo(client.getLocalPlayer().getWorldLocation()) > 12)) { clearReminders(); }
        reconcileSignals();
    }

    private void inspectReminderInteraction(MenuOptionClicked event)
    {
        if (!config.remindSecateurs() && !config.remindSack() && !config.remindPaused()) { return; }
        MenuAction type = event.getMenuAction();
        String option = Text.removeTags(event.getMenuOption()).toLowerCase(Locale.ENGLISH);
        if (preservesFarmingContext(type, event.getParam1(), option)) { return; }
        if (type != MenuAction.GAME_OBJECT_FIRST_OPTION && type != MenuAction.GAME_OBJECT_SECOND_OPTION
            && type != MenuAction.GAME_OBJECT_THIRD_OPTION && type != MenuAction.GAME_OBJECT_FOURTH_OPTION
            && type != MenuAction.GAME_OBJECT_FIFTH_OPTION && type != MenuAction.WIDGET_TARGET_ON_GAME_OBJECT) { return; }
        ObjectComposition definition = client.getObjectDefinition(event.getId());
        ObjectComposition crop = definition.getImpostorIds() == null ? definition : definition.getImpostor();
        boolean farming = isFarmingVarbit(definition.getVarbitId());
        if (crop != null && crop.getActions() != null)
        {
            for (String action : crop.getActions())
            { farming |= "Inspect".equalsIgnoreCase(action) || "Check-health".equalsIgnoreCase(action) || "Rake".equalsIgnoreCase(action); }
        }
        if (!farming) { clearReminders(); return; }
        String name = crop == null ? "" : crop.getName();
        int state = isFarmingVarbit(definition.getVarbitId()) ? client.getVarbitValue(definition.getVarbitId()) : -1;
        reminderPoint = WorldPoint.fromScene(client, event.getParam0(), event.getParam1(), client.getPlane());
        reminderUntil = tick + 30;
        reminderHarvest = HarvestSignals.patchAction(option, Text.removeTags(event.getMenuTarget())).equals("harvest");
        reminderHerb = "Herbs".equalsIgnoreCase(name) || HerbHarvest.fromCrop(state, name) != null;
        reminderBenefits = FarmingReminders.benefitsFromSecateurs(name, reminderHerb);
        updateReminders();
    }

    private void updateReminders()
    {
        if (reminderPoint == null) { return; }
        boolean secateurs = false; boolean closedSack = false;
        ItemContainer carried = client.getItemContainer(InventoryID.INV);
        if (carried != null)
        {
            for (Item item : carried.getItems())
            {
                secateurs |= item.getId() == ItemID.FAIRY_ENCHANTED_SECATEURS;
                closedSack |= FarmingReminders.closedSack(item.getId());
            }
        }
        ItemContainer worn = client.getItemContainer(InventoryID.WORN);
        if (worn != null)
        {
            for (Item item : worn.getItems()) { secateurs |= item.getId() == ItemID.FAIRY_ENCHANTED_SECATEURS; }
        }
        List<String> next = FarmingReminders.messages(reminderHarvest, reminderHerb, reminderBenefits,
            trackingPaused(), secateurs, closedSack, config.remindSecateurs(), config.remindSack(), config.remindPaused());
        highlightClosedSack = enabled && reminderHarvest && reminderHerb && closedSack && config.remindSack() && config.reminderOutline();
        if (!next.equals(reminders)) { reminders = next; refresh(); }
    }

    private void clearReminders()
    {
        reminderPoint = null; highlightClosedSack = false;
        if (!reminders.isEmpty()) { reminders = List.of(); refresh(); }
    }

    boolean highlightClosedSack() { return highlightClosedSack; }

    @Subscribe public void onConfigChanged(ConfigChanged event)
    {
        if ("farming-profit".equals(event.getGroup()))
        { clientThread.invoke(() -> { if (enabled) { updateReminders(); } }); }
    }

    private void reconcileSignals()
    {
        if (!currentProfile()) { return; }
        // Pause clears context once; resume takes fresh baselines. No inventory work while paused.
        if (trackingPaused()) { return; }
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
            int herbState = herbVarbit < 0 ? -1 : client.getVarbitValue(herbVarbit);
            boolean depleted = herbPatch != null && herbState >= 0 && herbState <= 3;
            if (diagnosticsEnabled() && action.equals("harvest") && tickXp > 0)
            { log.debug("Harvest eligibility: herb={} xp={} openSack={} normalXp={}", herbPatch, tickXp, hasOpenSack(now), normalXpWorld()); }
            if (tickXp > 0 && action.equals("harvest") && herbPatch != null && hasOpenSack(now) && normalXpWorld())
            {
                int observed = gains.getOrDefault(herbPatch.itemId, 0);
                boolean faladorDiary = client.getVarbitValue(VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE) > 0;
                double patchMultiplier = HerbHarvest.patchMultiplier(patchPoint, faladorDiary);
                double outfit = outfitMultiplier();
                int inferred = herbPatch.inferredCount(tickXp, outfit, depleted, patchMultiplier);
                int missing = Math.max(0, inferred - observed);
                if (diagnosticsEnabled()) { log.debug("FP herb reconciliation: herb={} xp={} multiplier={} patchMultiplier={} faladorDiary={} state={} depleted={} inferred={} observed={} missing={} reason={}", herbPatch, tickXp, outfit, patchMultiplier, faladorDiary, herbState, depleted, inferred, observed, missing, inferred == 0 ? "xp-not-matched" : missing == 0 ? "already-observed" : "record-inferred"); }
                if (missing > 0)
                {
                    inferredHarvest = record(herbPatch.itemId, missing, false, "Sack harvest — XP inference", -1);
                    changed |= inferredHarvest;
                    if (data.active != null)
                    { data.active.warnings.add("Sack yield includes XP-inferred herbs. Verify against the sack before/after harvesting; chat messages are not required."); }
                }
            }
            if (action.equals("plant") || action.equals("pay"))
            {
                for (Map.Entry<Integer, Integer> old : inventory.entrySet())
                {
                    int used = old.getValue() - now.getOrDefault(old.getKey(), 0);
                    if (used > 0 && HarvestSignals.isSupplyCost(action, items.getItemComposition(old.getKey()).getName()))
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
                if (action.equals("harvest") && gains.keySet().stream().noneMatch(CropCatalog.CROPS::containsKey) && !inferredHarvest)
                { data.active.warnings.add("A harvest had no visible produce signal. Check counts, especially sack harvests."); }
            }
            if (depleted && tickXp > 0) { clearAction(); }
        }
        inventory = now; clearTick();
        if (data.active != null && data.active.idleExpired(System.currentTimeMillis(), config.idleMinutes()))
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
        return HerbHarvest.outfitMultiplier(java.util.Arrays.stream(worn.getItems()).mapToInt(Item::getId).toArray());
    }

    private boolean ensureRun()
    {
        if (!loaded || loadFailed || trackingPaused()) { return false; }
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

    void startManual(long generation) { clientThread.invoke(() -> { if (canEdit(generation, null) && data.active == null) { data.active = new FarmRun(System.currentTimeMillis()); save(); refresh(); } }); }
    void pauseManual(long generation, String expectedId, boolean paused)
    {
        clientThread.invoke(() -> {
            if (!canEdit(generation, expectedId) || data.active == null) { return; }
            if (!data.active.paused && paused) { reconcileSignals(); }
            if (data.active == null || !data.active.id.equals(expectedId)) { return; }
            data.active.setPaused(paused, System.currentTimeMillis());
            updateReminders();
            freshTrackingBaseline(); save(); refresh();
        });
    }

    void deleteManual(long generation, String expectedId)
    {
        clientThread.invoke(() -> {
            if (!canEdit(generation, expectedId) || !data.deleteActive(expectedId)) { return; }
            clearReminders();
            freshTrackingBaseline(); save(); refresh();
        });
    }

    void deleteHistoryManual(long generation, String expectedId)
    {
        clientThread.invoke(() -> {
            if (!currentProfile() || client.getGameState() != GameState.LOGGED_IN
                || !data.deleteHistory(generation, accountGeneration, expectedId)) { return; }
            save(); refresh();
        });
    }
    boolean[] settingsValues() { return new boolean[]{config.automatic(), config.compost(), config.diagnostics(), config.travel(),
        config.remindSecateurs(), config.remindSack(), config.remindPaused(), config.reminderOutline()}; }
    int idleMinutes() { return config.idleMinutes(); }
    void updateSettings(boolean automatic, boolean compost, boolean diagnostics, boolean travelCosts, int idle)
    {
        configs.setConfiguration("farming-profit", "automatic", automatic);
        configs.setConfiguration("farming-profit", "compost", compost);
        configs.setConfiguration("farming-profit", "diagnostics", diagnostics);
        configs.setConfiguration("farming-profit", "travel", travelCosts);
        configs.setConfiguration("farming-profit", "idleMinutes", idle);
    }
    void finishManual(long generation, String expectedId)
    {
        clientThread.invoke(() -> {
            if (!canEdit(generation, expectedId) || data.active == null) { return; }
            reconcileSignals();
            if (canEdit(generation, expectedId)) { finish(); }
        });
    }
    void updateReminderSettings(boolean secateurs, boolean sack, boolean paused, boolean outline)
    {
        configs.setConfiguration("farming-profit", "remindSecateurs", secateurs);
        configs.setConfiguration("farming-profit", "remindSack", sack);
        configs.setConfiguration("farming-profit", "remindPaused", paused);
        configs.setConfiguration("farming-profit", "reminderOutline", outline);
    }
    private void finish()
    {
        if (!loaded || data.active == null) { return; }
        data.active.finishedAt = System.currentTimeMillis();
        data.active.paused = false;
        clearReminders();
        data.history.add(0, data.active); data.active = null; travel.clear(); clearAction(); clearTick(); save(); refresh();
    }

    void adjust(long generation, String expectedId, int id, int quantity, long price, boolean cost, String reason)
    {
        clientThread.invoke(() -> {
            if (!canEdit(generation, expectedId)) { return; }
            if (quantity == 0 || Math.abs((long) quantity) > 1_000_000 || price < 0 || price > 2_000_000_000L
                || reason == null || reason.trim().isEmpty() || reason.length() > 1000) { return; }
            if (data.active == null) { data.active = new FarmRun(System.currentTimeMillis()); }
            String name = id == ItemID.COINS ? "GP adjustment" : items.getItemComposition(id).getName();
            data.active.add(id, name, quantity, price, cost, "Manual", reason, System.currentTimeMillis());
            save(); refresh();
        });
    }

    private boolean currentProfile()
    { return enabled && loaded && account != null && account.equals(configs.getRSProfileKey()) && client.getAccountHash() != 0; }

    private boolean canEdit(long generation, String expectedId)
    {
        return currentProfile() && client.getGameState() == GameState.LOGGED_IN
            && data.matchesAction(generation, accountGeneration, expectedId);
    }

    private void refresh()
    {
        Data copy = data.snapshot();
        String status = storageStatus; boolean ready = currentProfile() && client.getGameState() == GameState.LOGGED_IN;
        long generation = accountGeneration;
        List<String> notices = reminders;
        Runnable render = () -> { if (enabled && panel != null) { panel.renderReminders(notices); panel.render(copy, status, ready, generation); } };
        if (pendingRender.getAndSet(render) == null)
        {
            SwingUtilities.invokeLater(() -> {
                Runnable latest = pendingRender.getAndSet(null);
                if (latest != null) { latest.run(); }
            });
        }
    }
}
