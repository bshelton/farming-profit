# Local history recovery

History is stored under RuneLite's `farming-profit` plugin-data directory, with a separate filename for each RuneScape profile:

- `<profile>.json`: current saved history.
- `<profile>.pending`: temporary replacement; normally removed by atomic rename.
- `<profile>.backup`: validated history from before the first replacement in a loaded session. On a brand-new profile, this is created when an existing primary is first replaced.
- `<profile>.backup.pending`: temporary backup replacement.

Backups intentionally do not update on every harvest. They may lack the current session's changes. Neither background saves nor backups guarantee preservation of the latest activity if the client/process or machine is abruptly terminated.

If loading fails, tracking stops to protect the file. The plugin does not silently roll back to a backup. A save failure leaves recent changes in memory and the previous successful primary intact; another ledger change attempts a save again. A successful save clears the error.

To recover manually:

1. Close the development/normal client and preserve copies of all files for the affected profile before editing anything.
2. Inspect the primary, backup and any pending JSON. Choose a valid copy with the intended account/run history. Do not mix files from different profiles.
3. Keep the damaged primary under a different filename. Copy the selected valid recovery file to the original `<profile>.json` filename. Keep the recovery files until the restored history is verified.
4. Restart the client and verify the expected run and totals. Re-enter any missing recent harvests using manual corrections with a clear reason.

If no recovery copy is valid, retain the damaged files for diagnosis rather than deleting them. Never share an entire RuneLite directory, credentials or unrelated logs in a bug report.
