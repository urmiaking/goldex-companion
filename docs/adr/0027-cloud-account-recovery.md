# Cloud account recovery across Android and Windows

Status: implemented.

## Context

Android's platform `JSONObject.quote` escapes `/`, unlike the server's `JSON.stringify`. Thus valid snapshots containing Persian dates or document titles with slashes failed hash verification on Android. A Robolectric test reproduces this using a synthetic ledger date. JVM-only JSON tests did not catch the platform difference.

Windows had no existing-account entry at the beginning of onboarding. Its coordinator silently activated downloaded snapshots during login, omitted hash checks and ignored a failed sync result. Completing the ordinary wizard after recovery could overwrite the recovered profile or seed opening inventory.

## Decision

- Keep protocol version 1 and the server's existing hashes. `SyncJson.canonical` uses explicit JSON.stringify-compatible quoting for keys and strings on both hosts, including control characters and unpaired UTF-16 surrogates. Persisted records and monetary representations are unchanged.
- Authentication and replacement consent remain separate. Windows reports `RESTORE_REQUIRED` for an existing account; background sync cannot replace local data. The shared cloud dialog offers explicit replacement confirmation.
- Windows recovery first writes an independent SQLite backup in the private data directory's `cloud-backups/`, including records, checkpoints and pending operations. A fresh server snapshot is staged, each page hash checked, then activated atomically. A changed local operation sequence invalidates consent and rolls back activation. The coordinator serializes authentication, sync, restore and logout with one mutex; logout preserves queued operations.
- The wizard's existing-account action opens the same cloud UI. Successful activation increments `restoredGeneration`. The wizard uses `CompleteOnboardingUseCase` with `restoredFromCloud=true`; recovered settings and balances remain authoritative, and no opening stock is seeded again. Activation also records the opening-inventory checkpoint for later wizard retries.
- Failures preserve active records and queued changes, end the busy state, and display an actionable error. Authentication success alone is never a successful sync timestamp.

## Consequences

Existing Android and Windows data and release identity remain compatible. No server schema or account migration is needed. Update the client to repair Android hash checks; redeploying the server alone cannot change an installed client's serializer. A SQLite recovery copy can be restored while the app is closed by retaining the current data folder and replacing its database with the selected backup; never overwrite the only copy. Automatic in-app recovery-copy import remains future work.

The production deployment also pins the new-activation trial policy to seven days without changing expiry dates already stored for existing devices. Landing deployment must publish the matching seven-day text.

## Revisit

Revisit for a versioned protocol migration, resumable snapshots, dedicated backup import UI, or replacing per-host cloud coordinators. Keep cross-runtime golden tests when changing serialization.
