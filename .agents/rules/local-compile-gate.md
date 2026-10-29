---
trigger: model_decision
description: Fast, platform-scoped local Gradle verification; avoid expensive PNG/Compose UI runs by default
---

# Fast local verification before merge or release

Follow [Fast Verification](../../docs/development/fast-verification.md).

Use the reusable Gradle daemon, shared build cache and configuration cache. **Do not** routinely run \`clean\`, \`--no-daemon\`, or \`--no-build-cache\`.

Run only affected platform verification:

- Windows: \`.\scripts\dev\verify.ps1 -Target windows\`
- Android: \`.\scripts\dev\verify.ps1 -Target android\`
- Core only: \`.\scripts\dev\verify.ps1 -Target core\`
- Changes shared by both apps: \`.\scripts\dev\verify.ps1 -Target shared\`

All platform-relevant JVM business tests must pass before merge. \`:desktop:test\` contains fast JVM tests; legacy Compose interaction tests are now optional via \`:desktop:uiTest\` and \`-Ui\` in the script. **Never generate PNG screenshots by default or inspect them as a required step.** Only run optional Compose UI tests for complex interaction regressions when appropriate.

The final release workflow still owns signed Android APK assembly, Windows packaging and packaged runtime/installer verification. Do not build a local Android release APK. For release workflow or routing changes, also run \`python -m unittest discover -s scripts/release -p "test_*.py"\` and \`actionlint\` if available. Never claim tests passed unless their exit output is available.
