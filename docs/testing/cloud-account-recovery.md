# Cloud account recovery verification

## Reproduction and ownership

`CloudCanonicalAndroidTest` ran under Robolectric Android API 28 and failed before the fix: the canonical snapshot containing a Persian date escaped `/`, unlike the existing server protocol. It passes after using platform-independent quoting in shared `SyncJson`. JVM golden tests include slashes in keys, HTML closing tags, Persian text, control characters, Unicode line separators, emoji and unpaired surrogates. No customer records are fixtures.

`DesktopCloudRecoveryTest` uses a fake protocol adapter and a real temporary SQLite database. It checks authentication does not replace existing records, explicit consent, a complete pre-restore SQLite backup including queued changes, hash rejection, concurrent-edit rollback, device sequence continuation, restored-generation notification and propagation of bootstrap failure.

`DesktopOnboardingWizardTest` checks the new intro entry opens login before profile entry, cancelling preserves the wizard, and recovered completion preserves the remote profile without opening-stock seeding. Existing wizard/navigation, persistence and financial tests remain in the standard local suites.

## Delivery gates

Run local Android compilation and unit tests, core boundary checks/JVM tests, the full desktop suite, release-planning tests and desktop `createDistributable`; execute the packaged launcher with `--verify-runtime`. APK construction/signing remains GitHub Actions only. Both hosts are selected because the serializer is shared. Do not re-download release artifacts for verification.

## Production evidence and limitations

Backend deployment uses the existing protected runner. The active trial override was 14 even though the API default was 7; deployment now pins 7 for new activations, preserving existing expiry dates. The public landing was an older image; its current seven-day source was published through the established Docker path because that repository has no active landing workflow.

Production investigation read health, deployment metadata and aggregate request/database counts only. It did not log in as a customer or download customer snapshots. Existing request URLs are redacted in backend logs, so those logs alone cannot identify the failing client stage. The Android reproduction and repaired cross-runtime golden test establish the serializer defect independently. Physical Android-device smoke testing remains distinct from local Robolectric and CI packaging.

Scoped Graphify covers the selected account-recovery source/docs inputs; current source and codebase-memory calls verified concrete owners. Kotlin AST recovery is partial in several coordinator/workspace files; graph diagnostics include external/dangling endpoints, self-loops and undirected edge collapses. These graph links are orientation evidence, not execution-order proof.


## Recorded local results

The full local gate completed with exit 0: Android compilation, 253 Android-host tests, 93 core Android tests, 229 desktop tests, core boundaries/JVM tests and desktop packaging. Final additional tests then passed: 7 desktop tests (including three minimum/wide/consent UI checks and logout queue preservation), 10 Android tests (including a successful atomic Room recovery with a Persian date, exact 9,007,199,254,740,993-toman balance and profile), and the complete 95-test JVM suite. The JVM-only golden test initially needed a forced test recompilation after moving source sets; the forced run and subsequent normal 95-test run passed.

Four new wizard screenshots at 940x700 and 1400x980 in light/dark were inspected: account recovery remains readable and separated from new-gallery setup. Packaged Qirato 0.56.88 `--verify-runtime` exited 0 and verified fonts/icons, shared codecs, financial behavior, customer/invoice storage and official PDF rendering. Release-planning tests: 14 passed. Existing SDK XML, SLF4J and intentionally incomplete image fixture diagnostics did not fail the gates. No local full APK assembly/signing or published-artifact re-download was performed.
