# Windows rates page verification

Issue #259 preceded source changes. The user requested an isolated worktree, no version bump/release, and merge after successful tests. Scope is Windows rates; the concurrently edited dashboard screen is unchanged.

The two supplied screenshots are the design source. Stitch hosted downloads redirected to Google login; they were not treated as images or code. Android LiveRatesScreen was inspected for navigation, gold basis, categories and details. Demo/fallback prices and synthetic statistics were not copied.

DesktopRatesBoardTest covers provider units and ounce decimals, malformed ranges/directions, ordered real history, cache reopening, unknown fields, absent instruments, malformed cache retention, concurrent/failed refresh preserving original time, destination-scoped global refresh, manual/provider isolation and shared-policy bubbles.

DesktopRatesPageTest captures wide light/dark, table continuation, compact light/dark and detail. It checks grouped rows, filter retention, K24 transfer without clearing calculator weight, disabled unavailable rates, honest manual history and retained manual entry.

Run focused desktop tests, then compileDebugKotlin, testDebugUnitTest, desktop tests, createDistributable and packaged --verify-runtime. Review captured images. No local APK assembly/signing, published artifact re-download, version bump or tag. Delivery is merge only per explicit user instruction.

Verified locally on 2026-10-07:

- compileDebugKotlin and testDebugUnitTest succeeded: 237 Android tests, zero failures/errors.
- Initial desktop/Android gates and packaged runtime succeeded. Rebased onto beaa311 (the separate dashboard change) without conflicts, then verified desktop integration.
- Final :desktop:test and :desktop:createDistributable succeeded after the global/periodic refresh refinement: 99 desktop tests, zero failures/errors.
- Packaged Qirato.exe --verify-runtime exited 0, validating version 0.56.45, font, icon, codecs, calculator/inventory calculations and label printing. No installation or customer-data access was performed by this verification mode.
- Six Compose captures were reviewed: wide/continuation light, continuation dark, compact light/dark and gold detail. Captures use deterministic test fixtures, not current live market quotes.

The five-minute reference subscription runs in the workspace coroutine scope and is cancelled on leaving the rates page. It forces each periodic check while preserving single-flight requests. Page, shell and Ctrl+R refresh routes update references on the rates destination; other destinations avoid these extra requests. No timer is scheduled on Compose's test/frame clock.

Graph coverage: Graphify AST refresh covers the 13 changed desktop source/test inputs since c941efc, including the separate merged dashboard. DesktopRatesBoard and DesktopWorkspace have partial AST extraction; their current source and behavior were verified directly and through codebase-memory MCP. Documentation semantic extraction covers ARCHITECTURE.md, ADR 0020 and this verification note. Graph coverage does not establish absence or full call direction.

GitHub release and installer workflows are outside this merge-only delivery: packaging/updater configuration is unchanged and the user explicitly deferred a version/release. No CI publication or installer-installation result is claimed.
