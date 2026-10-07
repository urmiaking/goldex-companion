# Windows dashboard and connection parity verification

Issues #255, #256 and #257 were opened before production edits. Production scope is Windows only.

WindowsConnectivityTest covers local-only versus IPv4/IPv6 Internet flags, immediate observation, periodic changes and idempotent start without market calls. Local Windows COM returned flags 66 (IPv4 Internet); this is OS status, not a claim that a market provider is reachable.

DesktopDashboardScreenTest covers saved autoSyncRates versus unsaved drafts, chip changes independent of a manual quote, dollar quote rendering, absence of the inventory shortcut, light/dark and compact views, pointer/keyboard/accessibility selection and unchanged latest-price display. Existing inventory navigation/privacy/stock regression now uses the normal inventory navigation entry. Empty/failed history must not fabricate a chart or price.

Source-only copy normalization removes U+0621/U+0654 in desktop Kotlin UI/error/fixture strings. It does not transform persisted data, storage keys, imported records or user input. Android's remaining orthographic words are outside this Windows change.

Run local Android compile/unit tests, desktop tests, release routing tests and packaged --verify-runtime. Capture actual Compose light/dark/compact dashboard images. Release CI owns disposable MSI installation/upgrade and publication. Verify publication through CI and asset metadata without downloading the released installer or an APK.

Local final gate passed: compileDebugKotlin, 237 Android tests, 89 desktop tests, and createDistributable (zero test failures/errors). Packaged Qirato.exe --verify-runtime exited 0 with version 0.56.45 and verified fonts, icon, shared codecs and financial runtime. Fourteen release routing tests passed. Actual light/dark/compact dashboard and tooltip captures were reviewed. Windows PowerShell 5 NLM read returned flags 66, matching the implementation's built-in shell/COM path. No local APK assembly/signing or published artifact re-download occurred.

Graphify refreshed desktop AST and source-backed Windows documentation facts locally (1027 nodes/2224 edges), preserving parallel edges. DesktopInventory, DesktopWorkspace and WindowsUpdater have partial structural extraction; direct current source, codebase-memory and tests verify touched behavior. The graph does not cover all Android screens; parity was checked directly against DashboardScreen. Precise indexing metadata reports freshness limitations, so source reads and bounded filesystem-backed text search remain authoritative for authored-copy checks.
