# Farming Profit

A farming-only RuneLite plugin for harvest value, supply costs and local run history.

**Experimental prototype. Not submitted to or approved for the Plugin Hub. Live game detection requires player validation.**

See [release readiness](docs/RELEASE_READINESS.md) for completed code hardening and the remaining live validation gates.

## Features

- Run and History tabs, settings, automatic/manual runs and idle completion.
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

The prototype combines inventory gains and optional sack messages with an **XP inference fallback**. It identifies the clicked herb patch, observes Farming XP, accounts for the Farmer's outfit and Falador Diary bonus, separates final-pick completion XP, and records only the additional herbs not already observed in inventory or messages.

**Visible chat messages are not required by the fallback.** XP-inferred entries are labelled and produce a review warning. This is not a direct read of sack contents and has not been verified in-game. Unknown crop state, unexpected XP or the 200m XP cap can prevent a reliable count. Inference is disabled on seasonal, Deadman and tournament worlds. Verify sack contents before/after a harvest and use corrections for mismatches.

## Accounting

Estimated run profit = harvest value minus supplies consumed on this trip.

Harvesting an established dragonfruit tree records its fruit without inventing a sapling expense. Replanting charges the new sapling to this trip. Historical planting costs are outside this run ledger; lifetime crop accounting is deferred.

GE values are saved per entry. They are estimates before sale tax, not actual sale proceeds. Unpriced items use zero with a review warning. Woodcutting logs, combat drops and non-farming income are excluded.

## Limitations

- The catalogue covers broad patch categories, but each location and live event sequence still needs testing.
- Consumed icy/stony basalt, supported teleport tablets and loose teleport scrolls count as travel costs during an active run, using GE prices. Start a run manually before the first teleport to include it. This can be disabled in Settings. Only item use followed by consumption counts; banking or dropping items does not.
- Spell runes (including rune-pouch supplies), jewellery charges, master scroll-book uses, Fertile Soil, seed-box consumption and missed gardener payments require manual costs. Payment detection needs recent nearby patch interaction. Unpriced teleport items require a manual value.
- Bottomless compost uses half a bucket's price per application. Successful compost messages must be verified with filtered chat.
- No automatic minigame rewards, Hespori combat loot, farming contracts or birdhouses.
- Lifetime patch accounting, exports, comparisons, automatic custom price overrides and tax estimates are deferred.

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

JSON history is stored in RuneLite's plugin-data directory under `farming-profit`, separated by RuneScape profile. Writes run off the game thread and replace files atomically. A validated session-start backup is retained. Unreadable history pauses tracking rather than overwriting it. See [recovery instructions](docs/RECOVERY.md). Data is not synced to GitHub or other devices.

Optional DEBUG diagnostics record local farming actions and system signals, not player chat. Share only relevant diagnostic excerpts, not entire client logs.

## References

Started from [RuneLite's plugin template](https://github.com/runelite/example-plugin). Patch states and menu actions were checked against RuneLite's `PatchImplementation` and `Produce`. Harvest XP is separate from the planting values in `FarmingAction`; corrections and live trace evidence are documented in [TESTING](docs/TESTING.md). The optional sack message format was checked against GP-Per-Hour's `U_HerbSack`; its presence during farming is not assumed.
