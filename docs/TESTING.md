# In-game acceptance checklist

Status: live testing in progress; herb-sack acceptance is still pending. A successful build is not a live-game test.

## Reported failure: 2026-10-21

An open sack received 10 ranarr herbs at the Trollheim patch (ledger location 2848,3934), but the plugin recorded no harvest. The run displayed 371 total Farming XP and one bottomless ultracompost use; total run XP alone is not enough to reconstruct harvest quantities.

The inference gate required the crop name `Herbs`; the reported ledger used `Ranarr weed`. The fix accepts named herb definitions when they match the harvestable patch state, retains the generic name, and logs crop name/state plus sack/XP eligibility when diagnostics are enabled. Regression tests cover ten silent picks and inventory overflow without double counting. This is a likely cause, pending live confirmation; diagnostics were not enabled for the failed harvest.

Retest after restart: enable Farming diagnostics, check the sack count, harvest a ranarr patch with the sack open, then check the sack again. The ledger should match the increase and label inferred entries `Sack harvest — XP inference`. Check that no missing-produce warning appears for successfully inferred picks. Existing missed harvests are not retroactively reconstructed; use a manual correction for the reported ten herbs if needed.

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

- Bank/travel between patches: retain one run.
- Finish manually and inspect History.
- Disable auto-start: only manually started runs should collect entries.
- Temporarily set idle completion to one minute and test it.
- Restart mid-run: retain prior entries without counting starting inventory.
- Switch accounts: keep histories separate.
- Reconnect/hop and confirm resumed counts.

## Release gate

Sack counting, repeat dragonfruit harvest, costs, account separation and event-order cases must pass before Plugin Hub submission. Record actual results here. Missing signals must not silently be treated as zero harvest.
