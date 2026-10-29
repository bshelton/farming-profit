# Farming Run Profit

A farming-only RuneLite plugin for harvest value, supply costs and local run history.

Available in the [RuneLite Plugin Hub](https://runelite.net/plugin-hub/).

**[User documentation](https://bshelton.github.io/lumby-labs/docs/farming-run-profit/) · [Help & support](https://bshelton.github.io/lumby-labs/help/) · [Report a bug](https://github.com/bshelton/farming-profit/issues/new?template=bug_report.yml)**

## Features

- Run and History tabs, settings, automatic/manual runs and idle completion.
- History grouped by local start date, with estimated profit, elapsed time and recorded patch locations. Older/newer controls display 25 runs at a time. Select a run for its ledger, location totals, or deletion.
- Friendly area names are derived from stored patch coordinates, including old history. Unknown areas keep their coordinates; travel and manual entries remain separate. Patch counts are distinct recorded tiles, not verified completed patches. Elapsed time includes pauses and time until the run finished.
- Pause/resume preserves the current run across restarts and suspends automatic tracking and idle completion. Manual corrections remain available. Resume starts fresh, excluding activity during the pause.
- Delete the current run with confirmation without saving it to History. A later farming interaction can automatically start a new run.
- Optional contextual sidebar reminders for missing magic secateurs, a closed herb sack and farming while paused. Secateurs in inventory or equipment count. Each reminder has its own setting; a closed-sack inventory outline is separately opt-in.
- Harvest catalogue for herbs, allotments, flowers, hops, bushes, cactus, fruit trees, seaweed, mushrooms, belladonna, grapes, calquat, celastrus, crystal trees and coral.
- Existing/repeat-harvest crops work without a recorded planting event.
- Visible seed/sapling consumption, successful compost treatment messages and contextual gardener payments.
- Farming XP, price snapshots, patch coordinates and an itemized ledger.
- Readable ledger with harvest/cost/profit totals, signed color-coded amounts, sortable numeric columns, harvest/cost filters, grouped matching entries and selectable patch/source details. Corrections stay separate and review notes can be expanded.
- Manual cost/harvest adjustments and negative corrections with a required reason.
- Local per-RuneScape-profile history and unfinished-run recovery; no plugin telemetry.

## Herb sack

The plugin combines inventory gains and optional sack messages with an **XP inference fallback**. It identifies the clicked herb patch, observes Farming XP, accounts for the Farmer's outfit and Falador Diary bonus, separates final-pick completion XP, and records only the additional herbs not already observed in inventory or messages.

**Visible chat messages are not required by the fallback.** XP-inferred entries are labelled and produce a review warning. This is not a direct read of sack contents; a full-run smoke test and a count-confirmed Morytania harvest are documented in [TESTING](docs/TESTING.md). Broader exact-count validation remains pending. Unknown crop state, unexpected XP or the 200m XP cap can prevent a reliable count. Inference is disabled on seasonal, Deadman and tournament worlds. Verify sack contents before/after a harvest and use corrections for mismatches.

## Accounting

Estimated run profit = harvest value minus supplies consumed on this trip.

Harvesting an established dragonfruit tree records its fruit without inventing a sapling expense. Replanting charges the new sapling to this trip. Historical planting costs are outside this run ledger; lifetime crop accounting is deferred.

GE values are saved per entry. They are estimates before sale tax, not actual sale proceeds. Unpriced items use zero with a review warning. Woodcutting logs, combat drops and non-farming income are excluded.

## Limitations

- Automatic travel costs cover supported icy/stony basalt, teleport tablets and loose inventory scrolls consumed during an active, unpaused run. Start manually before the first teleport to include it. Travel tracking can be disabled in Settings; banking or dropping items alone does not count.
- Spell runes (including rune-pouch supplies), jewellery charges, master scroll-book uses, Fertile Soil supplies and seed-box consumption require manual cost entries. Gardener payment detection needs a recent nearby patch interaction; add missed payments manually.
- Compost tracking relies on successful treatment messages and recent patch context. Bottomless compost is estimated at half the relevant bucket's GE price per application, rounded up to a whole gp.
- Herb-sack yields can be inferred from XP rather than read directly. See [Herb sack](#herb-sack) for inference limits and review guidance.
- Profit covers this run's harvest and recorded costs, not the lifetime cost of growing each crop. GE prices are estimates before sale tax. Items without a GE price are valued at zero and flagged for correction.
- Location names use known farming regions. Unknown areas retain their coordinates; travel and manual entries are not allocated to individual patches. Patch counts represent distinct recorded tiles, and elapsed time includes pauses and time until the run finished.
- Minigame rewards, Hespori combat loot, farming-contract rewards and birdhouses are not tracked automatically.
- Exports, run comparisons, automatic custom-price rules and sale-tax calculations are not implemented. Manual entries and corrections can use a supplied price.

### Testing coverage

A completed nine-patch ranarr run and History were checked in-game. Its trace recorded harvests at all nine stops, both basalt teleports, a tablet, seeds and bottomless compost. One Morytania sack harvest was separately confirmed against the player's count. This does not establish exact-count accuracy for every crop, patch or event sequence; broader coverage and filtered-chat compost behavior remain unverified. See the [test record](docs/TESTING.md) for evidence and outstanding checks.

## Development

Tested with Java 21; plugin bytecode targets Java 11.

```sh
./gradlew test
./gradlew run
```

For Jagex Accounts, follow [RuneLite's development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

`./gradlew preview` renders a synthetic Swing sidebar in `build/preview/sidebar.png` without launching or interacting with the game. Automated tests exercise ledger math, storage validation, reconciliation and XP inference, not live event delivery.

Follow [the in-game acceptance checklist](docs/TESTING.md) before release, then [the Plugin Hub checklist](docs/PLUGIN_HUB.md).

## Local data

JSON history is stored in RuneLite's plugin-data directory under `farming-profit-tracker`, separated by RuneScape profile. Writes run off the game thread and replace files atomically. A validated session-start backup is retained. Unreadable history pauses tracking rather than overwriting it. See [recovery instructions](docs/RECOVERY.md). Data is not synced to GitHub or other devices.

Optional DEBUG diagnostics record local farming actions and system signals, not player chat. Share only relevant diagnostic excerpts, not entire client logs.

## References

Started from [RuneLite's plugin template](https://github.com/runelite/example-plugin). Patch states and menu actions were checked against RuneLite's `PatchImplementation` and `Produce`. Harvest XP is separate from the planting values in `FarmingAction`; corrections and live trace evidence are documented in [TESTING](docs/TESTING.md). The optional sack message format was checked against GP-Per-Hour's `U_HerbSack`; its presence during farming is not assumed.
