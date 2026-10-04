# Large record list regression checks

Issue: https://github.com/urmiaking/goldex-companion/issues/231

## Local coverage

Run from the task checkout with a configured Android SDK:

```powershell
.\gradlew compileDebugKotlin --no-build-cache --no-daemon -q
.\gradlew testDebugUnitTest --no-daemon -q
.\gradlew :core:verifyCoreBoundaries :core:jvmTest --no-daemon -q
```

Focused slice:

```powershell
.\gradlew :app:testDebugUnitTest --tests '*LargeRecordList*Test' --tests '*LargeLedgerBulkReadTest' --no-daemon -q
```

- LargeRecordListComposeTest: each of invoices, customers, statement transactions and inventory has 2,000 synthetic records. The final row is absent from initial composition and becomes visible after scrolling to it. The actual main viewport also survives invoice/home animated transitions.
- LargeRecordListStateTest: repository reads/writes use a real worker executor; 2,000 invoice projections use the bulk ledger port; 2,000 transfers preserve order and do not accumulate on reload; obsolete statement reads/failures cannot replace the newly selected customer's 2,000 transactions; repeated inventory submissions write once; failed reads retain the old snapshot and recover on retry. A failed customer save never selects an unsaved customer or signals success; report snapshots are deferred until opening and run on the worker.
- LargeLedgerBulkReadTest: a real in-memory Room database returns the exact 2,000 linked transactions across multiple query batches, handles duplicate IDs/empty input and excludes unrelated records.
- CustomerStatementBalancesTest: 2,000 historical balances and tied timestamp ordering retain the existing ledger effect rules.
- Existing calculation, settlement, deletion, navigation, serialization and migration tests remain part of the full suite.

Tests create isolated in-memory data only. No production records or signing material are used.

Local verification on 2026-10-04 for 0.56.35: compile passed; 237 Android-host unit/Compose tests and 70 core JVM tests passed with zero failures/errors; core boundary verification passed. Graphify's scoped core structural graph was refreshed (460 nodes, 1,041 edges); Android UI/Room call paths were reindexed and checked with codebase-memory MCP/current source. The graph does not cover Android UI or documentation. The index's partial inventory expression range was verified directly and by compilation/tests.

## Device profiling

No Android device/emulator was connected during implementation. Robolectric validates behavior and layout constraints; it cannot certify smoothness or absence of ANRs on physical hardware.

Use a test installation of the CI-signed APK on a representative low-memory Android device, with an isolated test account/database containing synthetic invoices, customers, 2,000 statement transactions for one customer and inventory records. Preserve any existing device data through the supported backup/restore workflow before replacing a test dataset.

1. Open each list, scroll repeatedly to the last record, change filters and search.
2. Switch invoice/home tabs during scrolling; rapidly alternate customer statements.
3. Save a record twice quickly, then test load failure/retry. Verify one financial write and correct balances.
4. Observe frame timing and allocations in Android Studio Profiler, including cold open, repeated navigation and background/foreground transitions. Inspect logcat for crashes, ANRs and unbounded-scroll measurement exceptions without logging customer payloads.
5. Repeat with cloud synchronization enabled and during a slow read. Record device model, Android version, dataset sizes, frame timing and peak memory; compare the previous release under the same conditions.

If whole-snapshot allocation/query time becomes material at higher volumes, add Room-backed filtering and paging in a separate compatible feature slice.
