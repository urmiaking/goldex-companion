# Windows hover, cloud modal and invoice print preview

Related issue: #321. Scope: desktop presentation only; Android/shared modules, persisted records and signing files unchanged.

## Behavior and focused checks

- Rounded cloud header, body actions, quick-access tile and shared custom-fineness input retain visible hover feedback with unchanged outer corner pixels in both themes.
- Explicit modifier clips match their owning Surface shapes across inventory, customer and third-party pickers, wizard, calculator, reporting date selection and invoice/payment controls. Reporting uses clickable Surface to preserve shadow geometry.
- Header/settings open the same root cloud modal without triggering sync. Login cancellation returns to the account summary; reopening does not retain the phone/code subpage. Destination remains unchanged.
- Real pending count, last success, status/error text and account state render in RTL. Keyboard Enter invokes explicit sync only. Busy disables close, toggle, sync and logout. Disabled synchronization cannot invoke sync.
- Archive print opens the existing PDF preview; native printing remains an explicit action within it with existing license/busy guards.

## Verification

Initial focused run: 7 UI tests passed (3 cloud/hover + 4 invoice workflow tests). Expanded focused run: 11 UI tests passed, including the Windows control contract. Final shared-input hover check: 3 tests passed after adding DesktopInlineChipField to the corner-pixel check. Final local gate passed with exit 0:

```powershell
./gradlew.bat compileDebugKotlin testDebugUnitTest :desktop:test :desktop:createDistributable --no-build-cache --no-daemon -q '-Pkotlin.compiler.execution.strategy=in-process' --max-workers=2
```

XML reports: 222 desktop tests (41 suites), 249 Android-host tests (40 suites), 93 core Android tests (16 suites); zero failures, errors or skipped tests. Release planner tests: 14 passed. No core/shared code changed, so core JVM-specific gates were not required.

Packaged Qirato.exe --verify-runtime reported version 0.56.84 and passed font, icon, shared codecs, customer/invoice storage, financial and official PDF checks; exit 0. Existing SDK XML, image-decoder fixture and SLF4J diagnostics did not fail the gates. Local full APK assembly/signing was not run. Light/dark cloud screenshots were visually inspected at the minimum 940x700 window. No physical printer or live SMS/account connection was exercised.

Delivery target: [windows-v0.56.84](https://github.com/urmiaking/goldex-companion/releases/tag/windows-v0.56.84). [Issue #321](https://github.com/urmiaking/goldex-companion/issues/321) records the final CI run and publication/asset metadata before closure. Android stays at 0.56.83 (172). Screenshots are generated in desktop/build/screenshots; no production records are used.

## Graph scope and limits

Graphify starts from 16 scoped source/test inputs (496 nodes, 1117 edges); Android CloudSettingsModal is a read-only presentation reference. Its initial orientation query was budget-truncated and is not exhaustive dependency proof. The installed Graphify 0.9.73 reports a newer skill version and partial syntax extraction for DesktopWorkspace; direct source/MCP and tests establish behavior. MCP's initial fast index reports four unrelated parse gaps, excludes docs/scripts by design, and reports metadata changes on edited inputs. Non-code docs are inspected directly; no remote semantic extraction or full-repository rebuild was needed. After focused validation, incremental refresh re-extracted 21 changed/new inputs: the graph now covers 30 inputs, 760 nodes and 2045 edges, including affected pickers, payment presets and the new cloud/hover tests. Both orientation queries were budget-limited. MCP was fast-reindexed after source edits; it still reports metadata_changed immediately after reindex. Its name-based call graph also misattributes generic launch/logout and omits callable-reference inbound edges; direct source and event tests are authoritative for those callbacks.

UI/UX skill outcome search supported visible keyboard focus in modals. Compose-specific searches returned no database match; implementation follows the repository's existing Windows control/dialog contract and Material primitives.
