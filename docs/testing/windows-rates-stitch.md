# Windows rates page verification

Issue #259 preceded source changes. The user requested an isolated worktree, no version bump/release, and merge after successful tests. Scope is Windows rates; the concurrently edited dashboard screen is unchanged.

The two supplied screenshots are the design source. Stitch hosted downloads redirected to Google login; they were not treated as images or code. Android LiveRatesScreen was inspected for navigation, gold basis, categories and details. Demo/fallback prices and synthetic statistics were not copied.

DesktopRatesBoardTest covers provider units and ounce decimals, malformed ranges/directions, ordered real history, cache reopening, unknown fields, absent instruments, malformed cache retention, concurrent/failed refresh preserving original time, manual/provider isolation and shared-policy bubbles.

DesktopRatesPageTest captures wide light/dark, table continuation, compact light/dark and detail. It checks grouped rows, filter retention, K24 transfer without clearing calculator weight, disabled unavailable rates, honest manual history and retained manual entry.

Run focused desktop tests, then compileDebugKotlin, testDebugUnitTest, desktop tests, createDistributable and packaged --verify-runtime. Review captured images. No local APK assembly/signing, published artifact re-download, version bump or tag. Delivery is merge only per explicit user instruction.
