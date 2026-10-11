# Windows product entry

The add/edit dialog uses one column and paired fields, with shared Persian typography, RTL layout, theme tokens and animated segmented controls. Title and save/cancel stay outside the vertical scroll; cancel is on the right and save on the left. Categories are all visible without horizontal scrolling. The custom fineness field keeps focus when its value matches a preset. Location can be selected or typed directly.

Removed from entry: duplicated sale-price arithmetic, monetary preview, wage conversion preview, profit/tax banner, oversized weight banner and wage steppers. Wage remains item metadata. Storage schemas, category profit defaults, tax values, stock journals, financial policies and Android are unchanged. Editing quantity still requires a stock movement.

`DesktopInventoryScreenTest` covers Persian decimal entry and custom fineness, add/save/stock movement, compact-window scroll reachability and fixed RTL actions, dirty dismissal across navigation, photo retention, and dark-theme metadata edits preserving stored profit/tax/wage/quantity. It writes screenshots under `desktop/build/screenshots/` for visual inspection.

Required delivery gates: Android compile-only and local unit tests; desktop tests, local `createDistributable` and the packaged launcher's `--verify-runtime`; Windows CI publication and release metadata inspection. Do not assemble/sign an APK locally or re-download release assets for verification.

Precise discovery used the worktree's codebase-memory index and current source. This localized UI change does not bootstrap or query the main checkout's Graphify graph; no Graphify graph exists in this task worktree.

Local verification for 0.56.60: `compileDebugKotlin --no-build-cache --no-daemon -q` passed; `testDebugUnitTest :desktop:test :desktop:createDistributable --no-daemon -q` passed (237 app, 86 core and 125 desktop tests; zero failures/errors). The packaged `Qirato.exe --verify-runtime` exited successfully, reporting version 0.56.60 and verified fonts, icon, codecs and financial runtime. Release-planner tests passed (14 tests). Final light/dark and 940×700 dialog captures were visually inspected. No APK assembly/signing or release asset re-download was performed.

The precise index was refreshed after extraction of the dialog. Its coverage metadata continued to flag file metadata freshness changes, so current source reads and executed tests supply the verification evidence. An older main-checkout Graphify graph is not evidence for this change.
