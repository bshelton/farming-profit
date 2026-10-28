# Release readiness

Code hardening completed locally on 2026-10-21. This is not a declaration of production readiness or Plugin Hub approval. The user approved the ledger UI in the development client; counting and lifecycle acceptance remain incomplete.

## Code changes

- Profile identity and session/run tokens guard manual actions, including correction/deletion dialogs that outlive their original run. Gameplay events cannot write into another profile before the next game tick loads it.
- Finish settles pending observed signals before saving the completed run. Fertile Soil review warnings now persist immediately.
- History loading rejects missing schema, malformed data, duplicate run IDs, null warnings, invalid dates/XP and overflowing totals. Unsupported versions stop tracking rather than replacing data.
- Atomic writes preserve the previous primary file on failure. One validated session-start backup is retained per profile; backups are not rewritten on every herb. Orphan recovery files block creation of blank history when the primary is missing. Successful retries clear the save-error status; callbacks from an old account session cannot change the current status.
- Ledger labels render user-provided text literally rather than interpreting HTML.
- Disabling the plugin closes its open ledger windows; switching profile sessions closes old account ledgers. Deferred startup callbacks cannot register a stale sidebar after the plugin has been disabled.
- CI now exercises Linux, Windows and macOS, headless Swing previews, storage regression tests and the release JAR. The JAR check verifies Java 11 bytecode and bundled artwork, and rejects development launchers/tests/service registration.

## Reproduce the local checks

```sh
./gradlew clean check preview performanceCheck --console=plain
```

The performance task generates synthetic data under `build/performance-fixture`, never reads player history, and never launches the game. A local Java 21 run with 1,000 completed runs / 100,000 historical entries and 100 active entries measured:

| Operation | Observed time |
| --- | ---: |
| First history snapshot | 1.720 ms |
| Cached snapshot, p95 of 1,000 samples | 0.001 ms |
| Background JSON save, 13.3 MB file | 136.436 ms |
| Background load and validation | 178.164 ms |

These are single-process synthetic timings, not a rigorous benchmark or live FPS/CPU claim. Full-file disk writes still scale with history size; save/UI queues coalesce pending updates. CI outcomes for the final commit must be checked separately from local success.

The final local clean check passed 85 tests, release-JAR checks and headless previews. No live game actions were automated.

## Remaining release gates

1. Complete the live checks below and resolve count mismatches, especially silent-sack final picks, overflow and the Varlamore map boundary.
2. Confirm account switching/reconnect/pause/delete behavior and saved totals in a real client.
3. Profile live CPU, memory and game-thread responsiveness with diagnostics off, including large histories and the optional sack outline.
4. Get independent beta feedback; describe only validated crop/cost coverage in release metadata.
5. Verify the exact candidate commit's CI, then follow `PLUGIN_HUB.md`. Do not create a public release tag or submit to the Hub until these gates pass.

## Short live test record

Record actual before/after quantities, ledger totals, location and time. A successful startup or green unit test is not evidence for these rows.

| Check | Expected result | Status |
| --- | --- | --- |
| Full run, 2026-10-28 | User reports run seems good; trace records 95 ranarr across nine patches and both basalts plus one tablet | Smoke test passed; exact counts and lifecycle checks pending |
| Morytania open-sack ranarr, 2026-10-21 23:10 EDT | Ledger and actual sack count both 11, including final pick | Passed for this harvest |
| Open sack: Hosidius, Farming Guild, Falador and north of Hunter Guild | Exact sack delta, including final pick; no missing/double herbs | Pending |
| Closed sack and near-full sack overflowing into inventory | Exact inventory plus sack delta | Pending |
| Harvest, immediately replant and compost | Last herb plus one seed and one treatment cost | Pending |
| Basalt/tablet use versus bank/drop/failed teleport | Exactly one charge only for consumed travel items during active run | Pending |
| Established dragonfruit/cactus and advertised non-herb crops | New produce counted; no invented old planting cost | Pending |
| Pause/resume, finish, cancel/confirm delete | No paused backfill; correct history and deletion scope | Pending |
| Logout/reconnect, account switch, old correction dialog | Correct profile and run only; saved totals preserved | Pending |
| Secateurs, regular/silk sack, paused reminder toggles | Correct contextual reminders; stale outlines clear | Pending |
| Current/history ledger styling | User approved in dev client | Passed |

See `TESTING.md` for the fuller acceptance matrix and previously observed detection failures.
