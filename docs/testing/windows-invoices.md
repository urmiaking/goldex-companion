# Windows invoice verification

## Scope

Issue #297: replace the sample archive, remove the duplicate heading, match customer workspace capsules/cards, and port the Android barter invoice editor, category forms, payments, ledger settlement and official output. Work is isolated on `feature/windows-invoices`. The user requested a local merge to `main` and explicitly deferred push; no release tag, CI dispatch or issue closure is performed until that instruction changes.

## Automated behavior coverage

- `DesktopInvoiceStateTest`: real issuance/edit/delete and exactly-once reversal; dated settlement retained on draft cancellation and reversed only on final save; transaction rollback keeps editor and disk consistent; no invented offline quote or empty issuance; all four item/payment categories survive desktop serialization/backup.
- `DesktopInvoiceDocumentTest`: nine rows produce three A5 landscape PDF pages; the bundled Persian font renders; invalid logo input preserves existing exported bytes.
- `DesktopInvoicesScreenTest`: one content heading, issuance beside calendar/status, real Persian-number search and archive filters, preview/deletion, customer shortcut, draft navigation retention, and bounded Android item/payment forms. Screenshots use synthetic data only.
- Existing customer/store/settlement/calculator/UI tests remain part of the full desktop suite. The customer invoice shortcut is now enabled.

## Local gates

Run `gradlew.bat compileDebugKotlin --no-build-cache --no-daemon -q`, `gradlew.bat testDebugUnitTest --no-daemon -q`, and `gradlew.bat :desktop:test --no-daemon -q`. Package using `gradlew.bat :desktop:createDistributable --no-daemon -q`, then run `desktop/build/compose/binaries/main/app/Qirato/Qirato.exe --verify-runtime` hidden. APK assembly/signing remain CI-only.

Review `desktop/build/screenshots/invoices-list-light.png`, `invoices-list-dark.png`, `invoices-editor-light.png`, `invoice-item-form.png`, `invoice-payment-form.png` and `invoice-official-document.png`. Verify clipping, RTL, token consistency, large-number readability and cancel-right/confirm-left placement. Printer dialogs are implemented but automated verification does not send a job to a physical printer. SMS text is copied, not delivered.

## Graph coverage

The task worktree has its own local Graphify structural graph and codebase-memory index. Kotlin is covered; Graphify reports partial parsing in pre-existing workspace/inventory/rates sources. Generic `save`/`update` graph edges can be ambiguous; actual financial types/calls are confirmed from current source, compilation and tests. Graphify's local code-only incremental refresh does not semantically index documentation; the architecture/ADR comparison is inspected directly. Structural refresh uses no LLM extraction tokens. Generated graph files and screenshots are not committed.

The final Graphify code refresh succeeded (5,403 nodes, 17,447 edges); seven existing parser gaps remain. The last codebase-memory refresh returned `Transport closed`, so its final freshness is not asserted. Current source and local verification supply the final integration evidence.

## Release handoff

This change is desktop-only; Android/shared code and versions remain unchanged. Choose the Windows version against the current published artifact when push/release is authorized, accounting for other agents' local merges. Keep #297 open until the Windows CI job and asset publication succeed. Verify publication metadata without downloading a published installer again.

## Recorded local results

- Android `compileDebugKotlin --no-build-cache --no-daemon -q`: passed. `testDebugUnitTest --no-daemon -q`: 237 tests, zero failures. Android/shared sources remain unchanged.
- After integrating local `main` at `3ac5de9`, the combined compile/unit-test/application-image invocation passed on 2026-10-16 in 11m 4s: 177 Windows tests and 237 Android tests, zero failures/errors/skips. The earlier baseline passed 165 Windows tests. The first baseline run exceeded the repository's eight-minute test timeout; a temporary local init script increased only this execution's timeout to sixteen minutes. No tests were excluded and the committed Gradle timeout is unchanged.
- The final focused run passed all 12 invoice state/document/UI and Windows control-contract tests, including cash coin issuance without an invented live gold quote. The application image was rebuilt from those sources. Synthetic archive and form screenshots were reviewed for RTL and desktop control consistency.
- The final packaged `Qirato.exe --verify-runtime` passed with exit code 0 and empty stderr. It exercised bundled fonts/icons/codecs, a real invoice save and customer balance, PDF write/read, and invoice deletion with exact ledger reversal. The application image includes `jdk.unsupported` for PDFBox's Java 17 buffer cleaner; no JVM access-opening flags are needed. Physical printing remains a manual check.

Changed ownership and adapters are documented in ADR 0025. Main implementation files are `desktop/state/DesktopInvoices.kt`, `desktop/ui/DesktopInvoicesPage.kt`, `desktop/ui/DesktopInvoicePreview.kt`, `desktop/data/DesktopInvoiceDocuments.kt`, `desktop/state/DesktopInvoiceCalculator.kt` and the Android-derived `desktop/.../ui/invoices/` editor/state/forms. Shell, customer shortcuts, runtime smoke checks, README and architecture documentation are updated with them. Signing identity, customer persistence shape and shared money policies are unchanged.
