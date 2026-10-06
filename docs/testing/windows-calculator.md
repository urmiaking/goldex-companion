# Windows calculator validation

The first desktop slice shares the Android/JVM financial core and Aurum primitives. Quotes are manually entered; it has no market requests, financial storage or cloud synchronization.

## Local gates

```powershell
.\gradlew.bat compileDebugKotlin --no-build-cache --no-daemon -q
.\gradlew.bat testDebugUnitTest --no-daemon -q
.\gradlew.bat :core:verifyCoreBoundaries :core:jvmTest --no-daemon -q
.\gradlew.bat :desktop:test --no-daemon -q
.\gradlew.bat :desktop:createDistributable --no-daemon
```

Desktop tests require a graphical Windows session and render both themes using the actual bundled Vazirmatn font. PNGs are written under `desktop/build/screenshots`. They verify Persian paste, totals, clearing invalid inputs, reset/export state and unchanged results on theme switching. Common tests verify the financial form against the existing calculation policy, including VAT excluding raw gold, 0.001g precision, fineness, basis changes and overflow.

The portable runtime is verified independently:

```powershell
$verification = Start-Process -FilePath .\desktop\build\compose\binaries\main\app\Qirato\Qirato.exe -ArgumentList '--verify-runtime' -WindowStyle Hidden -PassThru -Wait
if ($verification.ExitCode -ne 0) { throw 'Packaged runtime failed' }
```

`--verify-runtime` loads the bundled font with native Skia and checks a known financial result, then exits. It does not write business records or open a window.

## Manual checks

- Start `:desktop:run`; test window resize, Persian/English keyboard layouts, Tab focus, mouse-wheel scrolling, light/dark appearance and clipboard export.
- Enter price 6,000,000; gross weight 2.500; stone deduction 0.500; wage 10%; profit 7%; tax 9%. Expect net 2.000g and total 14,315,160 toman. Tax is 191,160 toman.
- Change the basis to 24k: the displayed quote becomes 8,000,000, with the same result. Changing the wage unit clears the wage/result until a new value is supplied.
- Invalid or incomplete values must not leave a stale amount/export enabled.
- Android: run the retained migration, repository and ViewModel tests. On an available device, additionally check the calculator, font, dark theme and outlined inputs. A successful local JVM/Robolectric run is not a device check.

Local APK assembly/signing is prohibited. The tag workflow builds/signs Android and then publishes the Windows portable ZIP. Installer tasks need a Windows packaging toolchain (including WiX); this slice does not promise installer publication.

Windows CI requests only `platform-tools`, `platforms;android-34` and `build-tools;34.0.0`; the SDK's removed legacy `tools` package is not requested. If Android publication succeeds but Windows packaging fails, dispatch `build-and-release.yml` from main with `release_tag` set to that existing release (for example `v0.56.36`). The recovery run skips Android, checks out the exact release tag, builds/verifies Windows and uploads its ZIP to the same release. The input is validated as a version tag and passed through environment variables.
