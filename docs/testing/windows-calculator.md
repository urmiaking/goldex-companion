# Windows workspace validation

The desktop workspace shares the Android/JVM financial core, JSON codecs and Aurum primitives. It provides calculator, dashboard, provider/manual rates, local portfolio and settings/backup. Cloud accounts, invoices and business inventory remain outside this slice.

## Local gates

```powershell
.\gradlew.bat compileDebugKotlin --no-build-cache --no-daemon -q
.\gradlew.bat testDebugUnitTest --no-daemon -q
.\gradlew.bat :core:verifyCoreBoundaries :core:jvmTest --no-daemon -q
.\gradlew.bat :desktop:test --no-daemon -q
.\gradlew.bat :desktop:createDistributable --no-daemon
python -m unittest discover -s scripts/release -p "test_*.py"
```

Desktop tests require a graphical Windows session and render both themes using the actual bundled Vazirmatn font. PNGs are written under `desktop/build/screenshots`. They verify Persian paste, totals, clearing invalid inputs, reset/export state and unchanged results on theme switching. Common tests verify the financial form against the existing calculation policy, including VAT excluding raw gold, 0.001g precision, fineness, basis changes and overflow.

The portable runtime is verified independently:

```powershell
$verification = Start-Process -FilePath .\desktop\build\compose\binaries\main\app\Qirato\Qirato.exe -ArgumentList '--verify-runtime' -WindowStyle Hidden -PassThru -Wait
if ($verification.ExitCode -ne 0) { throw 'Packaged runtime failed' }
```

`--verify-runtime` loads the bundled font/icon with native Skia, shared JSON codecs and package version and checks a known financial result, then exits. It does not write business records or open a window.

## Manual checks

- Start `:desktop:run`; test window resize, Persian/English keyboard layouts, Tab focus, mouse-wheel scrolling, light/dark appearance and clipboard export.
- Enter price 6,000,000; gross weight 2.500; stone deduction 0.500; wage 10%; profit 7%; tax 9%. Expect net 2.000g and total 14,315,160 toman. Tax is 191,160 toman.
- Change the basis to 24k: the displayed quote becomes 8,000,000, with the same result. Changing the wage unit clears the wage/result until a new value is supplied.
- Invalid or incomplete values must not leave a stale amount/export enabled.
- Android: run the retained migration, repository and ViewModel tests. On an available device, additionally check the calculator, font, dark theme and outlined inputs. A successful local JVM/Robolectric run is not a device check.

Local APK assembly/signing is prohibited. The tag workflow selects affected platforms and publishes their artifacts after success. Installer tasks need a Windows packaging toolchain (including WiX); this slice publishes the portable ZIP only.

Windows CI requests only `platform-tools`, `platforms;android-34` and `build-tools;34.0.0`; the SDK's removed legacy `tools` package is not requested. To recover a missing Windows artifact, dispatch `build-and-release.yml` from main with the exact `release_tag` and `target=windows`. The planner verifies the other required artifact already exists. Both host builds check out the immutable tag. Windows-only tags use `windows-vX.Y.Z`, keep Android version/code unchanged and do not become GitHub latest. See ADR 0012 and the platform release routing rule.

## Workspace behavior

- Create Persian-input gold/coin holdings; edit and reopen with the same IDs; search/filter; cancel deletion and confirm another deletion. Verify values against the shared valuation policy. Unavailable quotes and unknown purchase basis must not become fake profit.
- Change pages without losing calculator input or unsaved settings; save defaults and verify they affect the next reset only. Verify RTL confirmation buttons, native backup chooser, Tab focus and Ctrl+1–5/Ctrl+N/Ctrl+R.
- Restart and verify holdings/profile/theme/manual quote. Attempt a second process for the same data directory: it must fail without writing data.
- Storage tests cover corrupt/future/duplicate records, unknown-field retention, atomic-write failure, previous-document backup and non-overwriting export. Rollback instructions are in ADR 0011; tests use temporary synthetic data only.
- Market tests cover rial conversion, real-source fallback, absent quotes, original timestamp on offline failure, two-minute receipt freshness and manual restart. Live-provider availability is a separate read-only smoke check, not a deterministic test prerequisite.
- Inspect actual screenshots for dashboard, holdings, settings, dialogs and rates in both themes and at the compact/wide desktop layouts. UI tests produce screenshots in `desktop/build/screenshots`.
- Verify the packaged EXE has the official icon as well as the in-window icon. Extract ZIP completely; moving the EXE alone is unsupported.
