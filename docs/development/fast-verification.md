# Qirato: fast verification workflow

The default development loop is now **incremental**. Never generate or inspect PNG screenshots as an automatic verification step. Preserve tests for financial calculations, persistent data, synchronization, document integrity and other business-critical behavior.

## Run only the affected platform (PowerShell)

- Windows only: `.\scripts\dev\verify.ps1 -Target windows`
- Android only: `.\scripts\dev\verify.ps1 -Target android`
- Shared core: `.\scripts\dev\verify.ps1 -Target shared`
- Pure core: `.\scripts\dev\verify.ps1 -Target core`
- Both hosts: `.\scripts\dev\verify.ps1 -Target all`

These commands use a **persistent local Gradle daemon**, incremental compilation, local build cache and configuration cache. Never pass `--no-daemon`, `--no-build-cache` or `clean` in routine verification.

The default `:desktop:test` runs only non-UI JVM tests. The older Compose UI suite has been moved to `desktop/src/uiTest/kotlin` and does **not** run in the normal test/check lifecycle or release runner. To run expensive on-screen interaction tests **only when specifically necessary**, invoke:

```powershell
.\scripts\dev\verify.ps1 -Target windows -Ui
# or a single class:
.\gradlew.bat :desktop:uiTest --tests 'com.goldex.companion.desktop.DesktopInventoryScreenTest'
```

Visual inspection is manual, when the feature actually requires it. The old automatic PNG screenshot writers were removed.

## Scope-specific policy

1. Pure visual/layout changes: affected-host compile and focused JVM tests; no full UI suite.
2. Changes in financial calculations, invoice issuance, sync, migrations or persistent storage: relevant automated business regression tests **must pass**. Add/update focused unit tests for changed rules.
3. Changes to the installer/updater packaging: run the Windows installer-validation PR workflow. Routine UI changes should not build an installer before the release.
4. Release tags: the existing GitHub Actions pipeline builds, signs and verifies only changed platforms, with full packaged-runtime and installer verification as applicable. Do **not** assemble an Android release APK locally.
5. For troubleshooting configuration-cache compatibility, use `--no-configuration-cache` on the specific command, document why, and fix the incompatibility rather than permanently disabling performance features.
6. Avoid polling, generated screenshot uploads, unnecessary artifact downloads and repeated full test runs. One passing relevant verification after a code change is sufficient until code changes again.

Gradle configuration deliberately limits parallel workers to four to balance performance with multiple active worktrees. The daemon and shared Gradle user-home cache are reusable between worktrees; individual working directories keep separate local `.gradle` state.