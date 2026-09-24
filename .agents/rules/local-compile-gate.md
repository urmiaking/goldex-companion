---
trigger: model_decision
description: Run a local Gradle compile check before triggering GitHub Actions CI to catch syntax errors early
---

# Local Verification Gate Before Merge & Release

Before merging a feature branch to `main`, pushing commits, or triggering a GitHub Actions release:

1. **Run the local compile task** to verify code compiles without errors:
   ```powershell
   .\gradlew compileDebugKotlin --no-build-cache --no-daemon -q
   ```
   - Validates Kotlin syntax and type resolution quickly.

2. **Run local unit tests** to verify financial calculations, formatting, and domain logic:
   ```powershell
   .\gradlew testDebugUnitTest --no-daemon -q
   ```
   - All unit tests must pass locally before any merge to `main` or release tag creation.
   - Unit tests have been offloaded from the cloud CI runner to optimize build speed and runner quota.

3. **If compilation or tests fail**: Fix all errors locally. Do NOT proceed to merge to `main` or trigger a release with failing tests.

4. **Cloud CI Scope**: GitHub Actions CI (`build-and-release.yml`) is now exclusively dedicated to:
   - Keystore decoding and signing configuration
   - Release APK assembly (`assembleRelease`)
   - Failure reporting
   - GitHub Release publishing and asset uploading
