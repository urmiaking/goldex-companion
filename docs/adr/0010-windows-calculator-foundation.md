# ADR 0010: Windows calculator foundation and shared presentation primitives

## Status

Accepted for the first Windows slice (0.56.36). Web is deferred. This is a manual gold calculator, not the full Android financial application.

## Context

The Android/JVM core already shares financial policies, models, ports and Java-backed platform services. UI tokens and reusable Compose components still belonged to the Android host. Rewriting a Windows calculator's formulas or visual system would introduce divergence. Migrating Room, cloud identity and the entire Android shell together would widen data and regression risk unnecessarily.

## Decision

- Retain Kotlin 1.9.23, AGP 8.3.2 and the Android Compose compiler 1.5.11. Use Compose Multiplatform 1.6.10 with the explicit `androidx.compose.compiler:compiler:1.5.11` coordinate for JVM/Android. Modern toolchain upgrades remain separate work. Multiplatform Compose libraries use the Android 1.6.7 runtime/UI; both hosts must pass local gates.
- Add `shared-ui` with Android/JVM targets. Move Color, Shape, Theme, Type, AnimatedPriceTicker, GoldButton, GoldOutlinedTextField, LuxuryCard, LuxurySegmentedControl and the numeric visual transformation while retaining package names, APIs and token values. FontFamily resolution uses expect/actual. Existing `app/src/main/res/font` files remain the single asset source, copied to generated Android library resources and packaged as JVM font resources. Android's existing resource identifiers remain available.
- Add platform-free `core/presentation/calculator/ManualGoldCalculator`: immutable StateFlow, explicit events, strict financial form validation, explicit manual quotes, existing GoldCalculationUseCases and AppSettings defaults. No formula or rounding migration is introduced. Invalid or incomplete inputs clear the result; changing wage units requires a new wage. Price basis changes preserve the normalized quote within the existing domain conversion policy.
- Add `desktop` as a JVM Compose host. Preserve Persian numerals, RTL, LTR decimal inputs, existing Aurum theme/components, keyboard-visible segmented focus, animated values, light/dark themes, responsive form/result panes and user-initiated clipboard export. Theme/form state is session-local; no customer or business data is persisted.
- Keep Android state/navigation, financial Room storage, cloud identity and signing untouched. The missing desktop capabilities (market networking, databases, invoice/customer/inventory workflows, cloud identity, printing and PDF) remain explicit follow-up slices.
- Android CI requires release credentials at `preReleaseBuild`, not configuration of unrelated desktop tasks. Signed-APK checks remain in the release workflow.
- Add Windows x64 portable ZIP packaging to the existing tag workflow, after Android publication. It bundles the JVM/native libraries and verifies the actual packaged font/Skia/core runtime. MSI/EXE installer task configuration is present, but only the portable archive is published in this slice. No new Windows signing identity is created.

## Consequences

The first Windows app can use the same financial core and visual primitives without replacing any Android screen. Android APK assembly/signing remains CI-only. Local tests cover VAT, Persian decimal input, precision, basis/unit changes, invalid/overflow values, reset, actual desktop rendering and theme behavior. No dependency on Windows storage is smuggled into core.

The old calculator's Double-based results and displayed whole-toman truncation are preserved, with tolerances below one toman in compatibility tests. The full application is not yet available on Windows, and Android device validation still requires a connected device.

## Revisit

Revisit when extracting a second feature, selecting desktop database/network adapters, enabling cloud accounts, updating Kotlin/Compose, or publishing a signed installer. Every persisted financial slice must preserve stable IDs, transactional effects, compatibility and rollback. Multiple concurrent writers require a separate protocol decision.
