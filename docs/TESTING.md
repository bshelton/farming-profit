# In-game acceptance checklist

Status: live testing in progress; herb-sack acceptance is still pending. A successful build is not a live-game test.

## Confirmed live result: Morytania, 2026-10-21

At 23:10 EDT, the user harvested ranarr at (3605,3529) with an open herb sack. The ledger recorded 11 inferred herbs; the user confirmed the sack contained 11 after this first patch. The trace shows ten alternating 30/31 XP picks and a final 57 XP pick with depleted patch state 3. Each produced exactly one ledger herb. Outfit and patch multipliers were both 1.0.

The trace also recorded one ranarr seed (26,646 gp) and one bottomless ultracompost application (215 gp) after harvesting. Those cost events were observed in diagnostics; their quantities/prices have not been independently confirmed by the user. This passes this single Morytania open-sack harvest, not the full crop/location/overflow acceptance matrix. The run was still active.

## Reported failure: 2026-10-21

An open sack received 10 ranarr herbs at the Trollheim patch (ledger location 2848,3934), but the plugin recorded no harvest. The run displayed 371 total Farming XP and one bottomless ultracompost use; total run XP alone is not enough to reconstruct harvest quantities.

The inference gate required the crop name `Herbs`; the reported ledger used `Ranarr weed`. The fix accepts named herb definitions when they match the harvestable patch state, retains the generic name, and logs crop name/state plus sack/XP eligibility when diagnostics are enabled. Regression tests cover ten silent picks and inventory overflow without double counting. This is a likely cause, pending live confirmation; diagnostics were not enabled for the failed harvest.

Retest after restart: enable Farming diagnostics, check the sack count, harvest a ranarr patch with the sack open, then check the sack again. The ledger should match the increase and label inferred entries `Sack harvest — XP inference`. Check that no missing-produce warning appears for successfully inferred picks. Existing missed harvests are not retroactively reconstructed; use a manual correction for the reported ten herbs if needed.

Follow-up: another eight ranarr herbs were missed after the named-crop fix. The existing log had no farming diagnostics, so the cause remains unresolved.

Diagnostic retest at 18:04–18:05 identified two further defects: the herb and open sack were correctly recognized, but regular ranarr XP deltas of 30/31 were rejected because the code used planting XP (27) instead of harvesting XP (30.5). A `CANCEL` menu action then cleared context while harvesting continued. The final XP delta was 57, consistent with one herb plus 26.5 deferred planting XP. The corrected code retains context for menu dismissal/examine/item selection and subtracts completion XP only when the herb patch is depleted. Tests replay the observed sequence `{31,30,31,30,31,30,31,30,31,30,31,57}` as 12 inferred herbs; the actual sack count and corrected live behavior still need user confirmation.

Herb XP references: [Ranarr seed](https://oldschool.runescape.wiki/w/Ranarr_seed), [herb patches](https://oldschool.runescape.wiki/w/Herb_patch), [Huasca plant](https://oldschool.runescape.wiki/w/Huasca_(plant)), and [Torstol seed](https://oldschool.runescape.wiki/w/Torstol_seed). RuneLite's skill calculator values used previously were not per-herb harvesting XP. Harvest and completion XP are now separate fields.

At 18:08 the user confirmed 13 ranarr herbs at Falador (3058,3311). The trace contains twelve alternating 33/34 XP drops and a final 60 XP drop. Opening the Inventory tab (`CC_OP`, option `inventory`) had cleared harvest context before the first drop. Tab navigation is now preserved only for recognized game-frame tab operations; inventory item actions still cancel context. Falador's 10% harvest-XP bonus is applied only in the Falador patch region with the Medium Diary complete; deferred completion XP remains separate. Replaying this confirmed trace produces 13 herbs. Corrected live detection, diary state and final patch-state timing remain to be validated after restart.

## Diagnostic capture

The development launcher now enables diagnostics automatically via a development-only system property. Normal plugin installations retain the optional Farming diagnostics setting. Logging stays at DEBUG and is event-driven, not per frame or idle tick.

After restarting the development client, harvest one patch with an open sack and report the approximate time and actual herb count. The trace includes object/controller/state identification, XP deltas, context cancellation/reset, sack item IDs, rejection reasons, XP reconciliation and ledger writes. It does not log player chat, account identifiers, or the full inventory. Checking the sack is only needed to verify the count, not to trigger capture.

For the local development launch redirected to `/tmp/farming-profit-dev-client.log`, filter with:

```sh
rg 'FP |Farming action=|Harvest eligibility:|Travel consumption:' /tmp/farming-profit-dev-client.log
```

Confirm `FP diagnostic session started: format=2` appears after startup. Preserve the relevant trace before restarting a launch that overwrites that file. No automatic inference fix is considered verified until the user confirms the live count.

## Follow-up detection audit

Live comparison at 18:16–18:18: Hosidius recorded nine inferred ranarr herbs (including the final pick), one seed and one bottomless compost use. At the Varlamore patch (1581,3094), Pick was recognized, but a `LOADING` transition while approaching the patch reset the context. Seven normal XP drops plus the final 57-XP drop were skipped. Ordinary region loading now preserves action context and XP/inventory baselines; logout, reconnect and world-hop transitions still reset them. Reconciliation retains the account, distance and expiry checks. Added lifecycle regression checks and an eight-herb XP trace test. Retest by approaching this patch from the same direction; the corrected live result is pending.

Code inspection found and addressed these additional cases:

- Planting costs depended on same-tick XP, despite deferred planting XP. A planting interaction plus visible seed/sapling/fragment consumption now records the cost without XP.
- Composting, raking and other maintenance were classified as planting, producing misleading seed-cost warnings. They now use a separate maintenance context.
- A real follow-up click could replace context before already-received XP/inventory changes were reconciled. Pending signals are now reconciled before replacing context, as well as on game ticks.
- Tab labels vary across layouts. Passive navigation now uses game-frame tab component IDs. Inspect/Guide and herb-sack Check also preserve context; Fill/Empty and item use still cancel it.
- Gardener-payment context previously survived every widget action. Only dialog continuation/selection now preserves it, avoiding accidental charges for later unrelated inventory actions.
- Outfit bonuses used display-name matching. Both body-type variants now use gameval item IDs, with partial/full-set tests.
- Coral uses Collect rather than Pick/Harvest, and its planting inputs are frags rather than seeds. Both are now included.

Live audit checks: harvest while switching tabs/checking the sack, immediately replant and compost, and confirm the last herb plus exactly one seed cost and one compost cost. Repeat with a partial/full Farmer's outfit and Falador bonus. Check sack overflow into inventory for duplicate counts. For travel, compare consumption with failed teleport attempts, banking and dropping items. Coral collection, gardener dialogs and rapid action timing still require live validation; unit tests are not evidence of RuneLite event order.

Remaining boundaries: 200m Farming XP prevents silent-sack inference; unsupported boosted worlds disable it; rune-pouch spells, jewellery charges, master scroll-book uses and seed-box consumption still require manual entries. Cross-tick delivery of XP versus inventory/patch-state events needs live validation. No historical missed harvest is reconstructed automatically.

Run `./gradlew run`, enable Farming Profit and optionally enable Farming diagnostics. For Jagex Accounts use https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts. Perform all game actions yourself; the project never automates game input.

Record client version, crop, location, outfit, initial/final quantities and review warnings. Do not publish credentials or unrelated logs.

## First priority: herb sack

1. Closed sack: harvest one patch. Compare the plugin count with the inventory increase.
2. Open sack: Check contents before and after harvesting. Compare the actual increase with the ledger. Repeat with game chat filtered.
3. Start with herbs already in the sack. Those herbs must not count.
4. Start near capacity and overflow into inventory. Sack increase plus inventory increase must equal the ledger count.
5. Empty, note, bank, withdraw and clean herbs. None may increase harvest totals.
6. Repeat without the Farmer's outfit, with a partial outfit and with the full outfit. Verify XP-inferred entries against real quantities.
7. Test rapid repeated picking and the final pick. Report unknown patch identity or unexplained XP.
8. Ground herb pickups and Herbiboar loot must not count.

## Other crops and costs

- Start a run before travel. Use one icy basalt, one stony basalt, a house tablet (including Outside), and a loose teleport scroll. Each should add one Travel cost at its GE price, including when the teleport triggers region loading.
- Bank, withdraw and drop teleport items; verify no travel cost. Attempt a blocked teleport; verify no cost without consumption. Finish the run and teleport; verify no new run or charge. Disable Include consumed teleport items and verify no charge.
- First-teleport costs before automatic run start, rune spells, jewellery charges and scroll-book uses remain manual. Travel detection is awaiting live validation.

- Pick an established dragonfruit tree: count fruit without a sapling cost. Return after regrowth and count only new fruit.
- Compare exact cactus/potato cactus, berries and allotment harvests.
- Exercise flowers, hops, mushrooms, seaweed, grapes and special patches separately. Record each verified location.
- Plant a seed/sapling: charge consumed quantities. Seed-box costs currently require correction.
- Apply regular/super/ultracompost and bottomless compost. Count successful applications once; reject already-treated messages.
- Check tree health: record Farming XP. Chopping logs must not create revenue.
- Test gardener payments with noted produce and baskets. Enter unobserved payments manually.
- Add a travel cost and negative harvest correction; verify the net total and audit trail.

## Lifecycle

- Pause an active run: harvest, spend supplies and teleport; totals and Farming XP must remain unchanged, and idle completion must not finish the run.
- Restart while paused: retain entries and paused state. Resume, then interact with a patch again; activity during the pause must not be backfilled.
- Finish a paused run: save it to History. Manual corrections should remain available while paused.
- Cancel Delete run: retain the run. Confirm deletion: remove only the current run without adding it to History. A later farming interaction may auto-start a new run.
- If a deletion dialog outlives its original run, confirming it must not delete a newer run.
- Bank/travel between patches: retain one run.
- Finish manually and inspect History.
- Disable auto-start: only manually started runs should collect entries.
- Temporarily set idle completion to one minute and test it.
- Restart mid-run: retain prior entries without counting starting inventory.
- Switch accounts: keep histories separate.
- Reconnect/hop and confirm resumed counts.
- Open a correction dialog, then let the run finish or change accounts before submitting. It must not change a different run/account. Start/finish/pause/delete actions must reject stale views or disconnected sessions.
- Disable the plugin with a ledger open, then re-enable. The ledger and old sidebar must close without duplicate navigation buttons. Switch accounts with a ledger open: the previous account's ledger must close.
- Verify a successful save after a transient write failure clears the save-error status. Automated storage tests simulate failed/interrupted writes and validate session backups; never corrupt real player history for testing.

## Release gate

Sack counting, repeat dragonfruit harvest, costs, account separation and event-order cases must pass before Plugin Hub submission. Record actual results here. Missing signals must not silently be treated as zero harvest.

## Ledger presentation

- Open an active and a historical run. Confirm harvest minus supply costs matches estimated profit and the sum of signed Net values. Positive net amounts are green, negative amounts are red; signs and entry types also convey the meaning.
- Identical entries at the same patch, price and source combine into one display row. Differing prices, patches, sources and correction signs remain separate. Stored entries are unchanged.
- Sort by quantity, unit price and net amount; numbers must sort numerically. Filter Harvest/Costs and select a row: details must still show the correct patch, source, entry count and time range.
- Expand review notes, resize the window, and test an empty run and negative corrections (including cost refunds). Close with Close or Escape. The window is nonmodal and displays a snapshot taken when opened; reopen to refresh it.
- Automated tests verify grouping totals/provenance, correction signs, filtering/sorting/selection, literal text rendering and empty data. Standalone Swing previews cover 620×520 and 520×480 content sizes. The user approved the ledger design in the dev client on 2026-10-21.

## Farming reminders (awaiting live validation)

- Click a mature herb patch without magic secateurs: show one persistent sidebar reminder. Carry them, then equip them: either location must suppress it. Ordinary secateurs do not qualify.
- Harvest herbs with a closed regular or silk sack: show the reminder. Enable the separate outline setting and check the inventory outline. Open, bank or drop the sack: remove the reminder and outline. No capacity estimate is inferred.
- Pause a run and interact with a patch: show the paused reminder without recording harvest, costs or XP. Resume: clear the paused reminder. Interact with the patch again to re-establish tracking context.
- Repeated clicks and harvest XP must not create duplicate messages. Walk more than 12 tiles away, stop interacting for 30 ticks, log out, hop, switch accounts or disable the plugin: clear stale reminders/outlines. Local scene loading preserves context.
- Disable each reminder separately and verify it clears immediately. The outline must require both its own setting and the sack reminder setting. No reminders are saved to the ledger or History.
- Check no secateurs advice for fruit/calquat trees, cactus/potato cactus, seaweed, crystal trees, belladonna or ordinary flowers. Supported names include herbs, allotments, bushes, hops, grape vines, limpwurt, celastrus and coral; unknown crop names conservatively suppress yield advice.
- Mechanics reference: [Magic secateurs](https://oldschool.runescape.wiki/w/Magic_secateurs), checked 2026-10-21. Inventory and equipment both apply; the affected-crop list is broader than herbs. No quest-completion requirement is assumed.
- Reminder evaluation uses patch clicks and inventory/equipment/config changes. Each active game tick only checks expiration/proximity; Farming XP extends the timeout. The inventory overlay only checks a precomputed flag and item ID before drawing RuneLite's cached item outline.

## Performance review (2026-10-21)

- Removed full-history JSON serialization/deserialization from the client-thread save/refresh paths. Client snapshots copy the current run's collections and share immutable entries; completed history snapshots are cached until a run finishes. JSON encoding and atomic file writes execute on the existing ordered background executor.
- Slow storage retains only the latest queued snapshot per account, plus any write already in progress. Account loads remain ordered behind writes. Swing similarly keeps only the latest pending render.
- History components are reused while harvesting and limited to 25 runs per page. All runs remain stored and accessible. History totals are recalculated only when history changes.
- Paused runs no longer scan inventory every tick. Equipment/XP inference runs only with positive Farming XP, and the outfit multiplier is computed once per reconciliation.
- No scene-wide scans or per-frame handlers. Unpaused inventory reconciliation scans at most the 28 inventory slots per game tick; game API reads stay on the client thread. Diagnostics remain optional and event-driven.
- Regression tests cover snapshot isolation and JSON compatibility, history reuse with 1,000 completed runs, pending-write replacement/account separation, and pagination/component reuse. These are structural performance checks, not live CPU/FPS measurements.
- Remaining scale limits: each save still atomically rewrites the full JSON file in the background; exceptionally large histories increase disk work, startup parsing and the one-time history snapshot/total calculation. The current-run display still rebuilds after ledger changes. Live profiling is required before making numeric CPU or FPS claims.
- Live retest: harvest into an open sack, verify counts/costs, pause/resume, finish a run, browse History, restart and confirm persistence. Observe responsiveness while harvesting and switching sidebar tabs.

## Basalt destination actions (2026-10-21, awaiting live retest)

- Live trace showed a `troll stronghold` inventory action followed by travel, but the old tracker only accepted generic Break/Teleport/Outside actions. No cost was recorded.
- Recognize Stony basalt's Troll Stronghold, Troll Stronghold entrance and Troll Stronghold roof actions, and Icy basalt's Weiss action, restricted to the corresponding item. Still require consumption of exactly one item within 15 seconds during an active, unpaused run.
- References: [Stony basalt](https://oldschool.runescape.wiki/w/Stony_basalt) and [Icy basalt](https://oldschool.runescape.wiki/w/Icy_basalt), checked 2026-10-21.
- Regression tests cover the destination actions, mismatched items, unchanged counts, duplicate events, expiry, cancellation and unexpected quantity changes. Optional diagnostics now report travel clicks and pending inventory reconciliation.
- Live retest after restarting: use each basalt during a run and verify one Travel cost at the GE unit price. Test the explicit roof/entrance options if available. Toggle, dropping, and a blocked teleport must not add a cost; paused runs must not track travel.
