# Windows calculator and update cleanup verification

Issue: #328. Windows-only release: windows-v0.56.89. No Android/shared implementation or signing changes.

## Behavior checks

- Custom jewelry fineness accepts successive digits through a preset, Persian replacements and preset switching; the same control is exercised in melt.
- Melt, direct conversion and assay input layouts assert LTR text and a unit visually left of the input center.
- Existing calculator tests cover formulas, VAT, reset, copy/export and navigation across all five modes.
- Coin ratio tests cover positive premium, negative bubble, missing rates and non-finite data. Screenshots cover all five views at 940/1440 in light/dark.
- Shipped PowerShell cleanup is exercised under an isolated fake LOCALAPPDATA: failure retains all sources; success removes only strictly older successful recognized stages; current repair source, failed/incomplete, unknown files, subdirectories, same/newer and mismatched results remain. Cleanup-only startup recovers a previous-helper cache and is idempotent. No real installation is modified by local tests.
- Disposable installer CI extends the existing upgrade test with an old successful cache fixture, asserts its removal, preserves the current MSI source and reinstalls from that source. User-data sentinel, shortcuts, downgrade rejection and uninstall checks remain.

## Validation evidence

Local verification passed on the delivered source:

- `compileDebugKotlin --no-build-cache --no-daemon -q`.
- `testDebugUnitTest :desktop:test :desktop:createDistributable --no-daemon -q`: 254 app tests, 93 core Android-target tests and 239 desktop tests; zero failures/errors/skips.
- Packaged `Qirato.exe --verify-runtime`: exit 0, version 0.56.89; Persian font, icon, codecs, financial/customer/invoice storage and PDF runtime checks passed.
- Release planner Python suite: 14 tests passed.
- PowerShell parser checks passed for shipped helper and installer fixture; `git diff --check` passed.
- Twenty screenshots reviewed across five calculator views, two sizes and both themes. Test fixtures supply the background explicitly so transparent previous frames cannot masquerade as rendering defects.

Disposable installation/upgrade CI and final Windows asset publication are recorded on the delivery PR/release. No local APK assembly/signing, real-user installation mutation or published artifact download is used.

## Changed ownership surfaces

- `DesktopCalculatorScreen.kt`, `DesktopCalculatorTabs.kt`, `DesktopKaratSelector.kt`: navigation, custom editing and summary appearance.
- `DesktopMeltCalculatorPane.kt`, `DesktopKaratConvertCalculatorPane.kt`: numeric direction, units, shared fineness selection and scrolling.
- `DesktopCoinBubbleCalculatorPane.kt`, `CoinValueComposition.kt`, `DesktopStandardFormulasPane.kt`: standard cards and coin composition.
- `WindowsUpdateInstaller.kt`, `install-msi.ps1`, `verify-installer.ps1`: cache lifecycle and disposable installation verification.
- `DesktopCalculatorPolishTest.kt`, `WindowsUpdateCleanupTest.kt`, one scroll-aware reset step in the existing calculator test; Windows version/notes and architecture/design/ADR documentation.

## Graph limits

A deliberately scoped worktree-local Graphify graph covers calculator/updater sources and their design/ADR references. It guides ownership only; Kotlin partial parsing, unresolved external references and collapsed relation pairs mean current source, codebase-memory inspection and executable tests remain authoritative. The final scoped graph covers 30 safe files (365 nodes, 770 directed edges); a repeat hash check reported zero changed/deleted inputs. Codebase-memory provided initial precise discovery and call sites; its snapshot does not track the newly added files, so their behavior is established by current source and the executed tests. Generated graphs are not committed.
