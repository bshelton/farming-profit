# Plugin Hub submission

The plugin has not been submitted.

1. Complete and record the acceptance checks in TESTING.md.
2. Run `./gradlew clean check preview` and verify Linux/Windows/macOS CI on the exact candidate commit. Review `RELEASE_READINESS.md`; run `./gradlew performanceCheck` for the synthetic large-history check.
3. Review metadata, README, license and supported/unsupported behaviour.
4. Push the exact release candidate to this public repository.
5. Fork https://github.com/runelite/plugin-hub and create a branch.
6. Add `plugins/farming-profit`:

```properties
repository=https://github.com/bshelton/farming-profit.git
commit=FULL_40_CHARACTER_TESTED_COMMIT
```

7. Open one PR describing farming-only scope, local storage, sack inference and validation. Update the pinned commit for subsequent fixes in that same PR.
8. Address automated checks and human review. Acceptance/publication belong to RuneLite.
9. Future releases require updating the Hub's pinned commit; pushing here alone does not update users.

Recheck https://github.com/runelite/plugin-hub#submitting-a-plugin before submission. Accurately attribute generated code to Codex in the PR.
