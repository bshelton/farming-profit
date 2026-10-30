# Plugin Hub submission

The plugin is already in the Hub. The [original submission](https://github.com/runelite/plugin-hub/pull/19639) and [UI/history update](https://github.com/runelite/plugin-hub/pull/19678) are merged.

The Herblore XP feature is being tested in [draft PR #2](https://github.com/bshelton/farming-profit/pull/2). It has not been submitted to the Hub yet.

For an update, use the existing manifest **`plugins/farming-profit-tracker`**. The display name is **Farming Run Profit**; the config group remains `farming-profit`. See [recovery](RECOVERY.md) for older development histories.

1. Complete and record the acceptance checks in TESTING.md.
2. Run `./gradlew clean check preview` and verify Linux/Windows/macOS CI on the exact candidate commit. Review `RELEASE_READINESS.md`; run `./gradlew performanceCheck` for the synthetic large-history check.
3. Review metadata, README, license and supported/unsupported behaviour.
4. Push the exact release candidate to this public repository.
5. Fork https://github.com/runelite/plugin-hub and create a branch.
6. Update the commit in the existing `plugins/farming-profit-tracker` manifest:

```properties
repository=https://github.com/bshelton/farming-profit.git
commit=FULL_40_CHARACTER_TESTED_COMMIT
```

7. Open one PR describing farming-only scope, local storage, sack inference and validation. Update the pinned commit for subsequent fixes in that same PR.
8. Address automated checks and human review. Acceptance/publication belong to RuneLite.
9. Pushing this repository alone does not update Plugin Hub users; the manifest update must be accepted.

Recheck https://github.com/runelite/plugin-hub#submitting-a-plugin before submission. Use a concise feature and validation description without a generated-by footer or name-change narrative.
