# Windows inventory verification — 0.56.41

| Android workflow | Windows coverage |
| --- | --- |
| Categories and title/code/RFID/location search | All eight categories, localized digits/Arabic-letter normalization, workshop search |
| Gross/stone/net weight and fineness | 0.001 g precision, presets 750/875/999, custom 100…1000 |
| Location/workshop/RFID/code/title/quantity | Complete validated form, unique code, stable edit identity/date |
| Percentage/per-gram wage, profit and tax | Existing whole-toman formula, category defaults 7/20/0, configurable percentages |
| Vault, weight, pieces, trays/safes | Metal-only vault and full summary, persistent privacy, no invented quote |
| Charge/deduct | Atomic journal+quantity, exact resulting-stock preview, zero stock and insufficient-stock guard |
| Print tag | Actual printable Code 128/QR, decode round-trip test |
| Photo | Actual import/removal, bounded embedded JPEG in backups |
| Delete | Confirmation, retained audit history, previous-document backup |
| Transfer to invoice | Deferred to Windows invoice feature, no placeholder action |

UI includes desktop master/detail, compact details, a wide two-column form, scrollbars, empty/filter states, dirty-form confirmation, preserved navigation drafts and shared RTL/theme/motion primitives. Updater restart cannot interrupt an inventory form/write.

Core tests verify Android price parity across categories/fineness/wage modes, VAT/rounding, localized input, malformed precision and stock bounds. Desktop tests verify reopening, unknown fields, strict malformed-record rejection, failed atomic writes, idempotency, retained history, photo/backup restoration and event-to-state validation. Compose tests cover save → invalid deduction → zero stock → history, search, both themes and 940×700/1400×980 layouts.

```powershell
.\gradlew compileDebugKotlin --no-build-cache --no-daemon -q
.\gradlew testDebugUnitTest :core:verifyCoreBoundaries :core:jvmTest :desktop:test --no-daemon -q
.\gradlew :desktop:createDistributable --no-daemon -q
```

Screenshots: desktop/build/screenshots/inventory-*.png. Native --verify-runtime exercises inventory math and actual label rendering, including the bundled font/barcode dependency. Physical printer output remains a manual check. MSI installation/upgrade/updater-helper verification runs on the disposable CI runner, never over the developer installation. Android assembly/signing remains CI-only.
