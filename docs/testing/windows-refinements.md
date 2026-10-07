# Windows 0.56.43 interaction verification

Production scope is desktop only. No Android/shared code, persisted schema, signing identity or theme tokens change.

- WindowsUpdateTest / WindowsUpdateScreenTest: immediate and five-minute discovery, no overlapping work, explicit download and ready prompt, retained prepared package, cancellation/hash verification/helper failures.
- WindowsRefinementsScreenTest: workspace starts discovery without manual checking; download box reports 50% while calculator input/navigation and a product draft remain usable; completion stays quiet; notes/restart require a second click. Dense inventory gives selection more width, units render left of numeric text in RTL, and page coordinates move only vertically in both directions.
- DesktopCalculatorRatesTest: actual quotes for every basis, domain conversion for missing basis quotes, manual override preservation, reset/return to market, first startup refresh, no fake price, persisted online quote reopened offline with original observation time and cached status.
- Existing stock journal, backup, formatting, calculator, settings, dashboard, reduced-motion and interrupted-navigation tests remain part of desktop regression gates. Android compile/unit tests run locally; no local APK assembly/signing.

Capture light/dark inventory, unit form and download screenshots from real Compose test rendering. Verify the native app image with --verify-runtime. Release CI owns actual MSI install/upgrade on its disposable Windows host. Confirm release publication using CI result and asset metadata without downloading the released package.
