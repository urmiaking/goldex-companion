# Inventory linked invoice sales

Use the shared InventorySalePolicy in core to create invoice line snapshots and reconcile stock deltas inside SyncUnitOfWork. Never deduct stock during draft selection or map a bank coin into CraftedGoldItem; bank coins use BankCoinItem with exact stored coinUnitPrice and coinType.

InventoryItem stores optional coin metadata (null for legacy); Android Room GoldexDatabase v5 adds nullable columns with COIN_INVENTORY_MIGRATION_4_5, desktop SQLite uses JSON payload with no schema change, and the JVM shared persistence codec/SyncJson must both preserve the fields. Cloud API validates price numeric strings without redoing financial calculations. Extend core tests for retries, stock shortages, deletion reversals and codec roundtrip; run local fast JVM/Android tests and selective CI releases. The issue and release lifecycle is in .agents/rules/task-issue-lifecycle.md.

The shared invoice stock picker is in `shared-ui/src/commonMain/kotlin/com/goldex/companion/ui/components/InventorySalePickerContent.kt`. Android hosts it in Material 3 `ModalBottomSheet` (not a full-height Dialog); Windows uses `DesktopDialogHost` and `DesktopDialog`. Keep picker height bounded, header/actions visible and only result list scrollable. Preserve RTL, quantity validation, test tags and non-mutating selection.
