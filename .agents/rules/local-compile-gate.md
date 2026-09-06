---
trigger: model_decision
description: Run a local Gradle compile check before triggering GitHub Actions CI to catch syntax errors early
---

# Local Compile Gate Before CI

Before pushing commits or triggering a GitHub Actions workflow run:

1. **Run the fastest local compile task** to verify code compiles without errors:
   ```powershell
   .\gradlew compileDebugKotlin --no-build-cache --no-daemon -q
   ```
   - Use `compileDebugKotlin` (not `assembleDebug` or `build`) — it is the fastest task that validates Kotlin syntax and type resolution.
   - Add `--no-build-cache` to ensure a clean check, and `-q` for quiet output.
   - Run from the repository root where `gradlew.bat` / `gradlew` is located.

2. **If compilation fails**: Fix all errors before pushing. Do NOT proceed to create or trigger a GitHub Actions run with known compile errors.

3. **If compilation succeeds**: Proceed to commit, push, and trigger the CI workflow as normal.

4. **What this does NOT replace**: Full builds, unit tests, instrumentation tests, APK assembly, signing, and release verification still run exclusively in GitHub Actions CI.
