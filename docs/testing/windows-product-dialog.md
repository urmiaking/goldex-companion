# Windows product entry

The add/edit dialog has a maximum width of 1040dp and bounded height. Name/category share a row; location, optional RFID and quantity share the next. Weight/fineness and wage/photo occupy adjacent columns with equal section and field alignment. The body remains scrollable when constrained; header and RTL save/cancel actions stay fixed.

Category and location dropdowns include inline creation, required-name feedback, duplicate selection and immediate selection of a saved option. The calculator's custom fineness chip accepts Persian digits directly and retains the input when a typed value matches a preset. Product cancel/close dismisses directly. Navigation retains open drafts, while stock movement and application-exit guards still apply.

New product identifiers are generated during the atomic write, numeric and seven digits. A durable sequence prevents reuse after deletion. Edits retain legacy codes and financial metadata. Category/location catalogs and custom category references are additive desktop extensions with strict validation, unknown-field retention, atomic writes and backup compatibility. Shared models, financial formulas, design tokens and Android behavior are unchanged. See [ADR 0023](../adr/0023-desktop-inventory-options.md).

`DesktopInventoryOptionsTest` covers catalog/reference round trips, backups, legacy editing, duplicate names, occupied/deleted codes, failed writes, corrupt extensions, custom filtering and product/movement dismissal. Existing inventory store tests cover stock journal atomicity and data retention. `DesktopInventoryScreenTest` covers custom input, both themes, compact sizing, dropdown creation, alignment, numeric identifiers, category filtering, stock movements and photos. `WindowsRefinementsScreenTest` checks visual-left units.

Required delivery gates: Android compile-only and local unit tests; desktop tests, `createDistributable` and packaged launcher `--verify-runtime`; release-planner tests; Windows CI publication and asset metadata. No local APK assembly/signing or redundant release downloads.

Discovery used a current task-scoped Graphify graph for inventory code and architecture documentation, followed by exact codebase-memory snippets/call sites. The local Graphify Kotlin parser partially extracts `DesktopInventory.kt`; current source, codebase-memory and executed tests supply exact behavior evidence. Graph artifacts remain local and are not committed.
