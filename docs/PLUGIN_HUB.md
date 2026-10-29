# Plugin Hub submission

Draft submission opened on 2026-10-28: https://github.com/runelite/plugin-hub/pull/19639

The draft manifest pins the candidate commit. The pre-rename candidate `0923e658d91bcb449abdcc0714b9f54e8443608a` passed 87 tests and CI; verify CI for the updated candidate separately. Remaining live gates are documented in RELEASE_READINESS.md. The draft is not approval or publication.

Manifest name is `farming-profit-tracker`: the existing `farming-profit` entry belongs to a different, disabled plugin and is left untouched. The display name is now **Farming Run Profit** to distinguish it. The internal identifier matches the manifest (`farming-profit-tracker`); the config group remains `farming-profit`. See RECOVERY.md for preserving history from earlier development builds.

1. Complete and record the acceptance checks in TESTING.md.
2. Run `./gradlew clean check preview` and verify Linux/Windows/macOS CI on the exact candidate commit. Review `RELEASE_READINESS.md`; run `./gradlew performanceCheck` for the synthetic large-history check.
3. Review metadata, README, license and supported/unsupported behaviour.
4. Push the exact release candidate to this public repository.
5. Fork https://github.com/runelite/plugin-hub and create a branch.
6. Add `plugins/farming-profit-tracker`:

```properties
repository=https://github.com/bshelton/farming-profit.git
commit=FULL_40_CHARACTER_TESTED_COMMIT
```

7. Open one PR describing farming-only scope, local storage, sack inference and validation. Update the pinned commit for subsequent fixes in that same PR.
8. Address automated checks and human review. Acceptance/publication belong to RuneLite.
9. Future releases require updating the Hub's pinned commit; pushing here alone does not update users.

Recheck https://github.com/runelite/plugin-hub#submitting-a-plugin before submission. Use a concise feature and validation description without a generated-by footer or name-change narrative.
