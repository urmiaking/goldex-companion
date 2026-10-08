# Windows 0.56.48: shell and automatic rate polish

Issue: #267. Desktop-owned UI/state changes; Android, shared UI/core, persisted record shapes and financial formulas are unchanged. Legacy autoSyncRates stays serialized but does not control desktop fetching.

## Behavioral verification

- Today is formatted from the workspace clock in Asia/Tehran, separately from quote receipt time. The date test covers the Persian calendar and midnight rollover.
- Every destination has exactly one connection/date header. Greeting remains dashboard-only; manual refresh controls and the automatic-rate preference are removed from desktop.
- A legacy disabled preference does not block startup or periodic market fetching. Offline failures preserve cached quote time, weight and unsaved settings; recovery persists the new quote. Explicit calculator SPOT overrides retain their existing tests.
- Update discovery never changes AVAILABLE to CHECKING in the background and never opens a dialog. The global sidebar control retains download percentage while the calculator and inventory drafts remain editable; ready consent and financial restart guards remain tested.
- Header/date/update/theme controls use the same height. Compact-window coverage verifies update/theme visibility at the 940×700 production minimum.
- Settings identifier fields retain raw phone/license/union input; percentages have left-side units. Backup and motion cards share row/height. The calculator total and تومان are checked against their actual text baselines.
- Rounded summary-card hover is clipped within the Surface. Light/dark settings and hovered rates screenshots were visually reviewed; all prices in screenshots are deterministic test fixtures.

## Local gates

- Focused desktop: 48 tests passed before the final compact-sidebar addition.
- Android compileDebugKotlin with no build cache: passed.
- Android testDebugUnitTest: 237 tests, no failures.
- Full desktop :desktop:test: 108 tests, no failures.
- :desktop:createDistributable and packaged Qirato.exe --verify-runtime: passed; output confirms version 0.56.48, font, icon, codecs and financial runtime.
- Release planner: 14 tests passed.
- No local APK assembly/signing or published-artifact re-download was performed.

## Exploration coverage

A local allowlisted Graphify graph mapped desktop shell, workspace, calculator market binding, updater and architecture documentation, then received an incremental code/semantic refresh (341 nodes, 668 edges). Kotlin structural extraction reports partial parsing for DesktopWorkspace, DesktopRatesBoard and WindowsUpdater; inferred graph links were not treated as call-order proof. Exact current source and compilation/tests establish behavior. Codebase-memory was reindexed; touched paths have no recorded parse gaps, but its filesystem freshness still reports metadata_changed immediately after indexing. Direct source reads and local tests were used for final verification.

## Delivery

Windows-only version/tag: 0.56.48 / windows-v0.56.48. The issue stays open until cloud packaging, runtime/installer checks and MSI publication succeed. Release verification uses CI output and asset metadata; it does not download MSI/APK bytes again.
