# 0016 — Windows inventory and atomic stock journal

## Context

Windows inventory precedes business invoices. Android's InventoryItem describes per-piece weights; RoomInventoryRepository changes quantity and inserts StockAdjustment in one transaction. PortfolioStore and InventoryStore have incompatible getItems return types. Windows already owns one locked atomic workspace.json writer.

## Decision

- Reuse core models, InventoryStore and existing JSON codecs unchanged. Core presentation/inventory/InventoryForm owns platform-free draft validation, localized input and search. Core domain/inventory/InventoryPolicies owns stock bounds and price breakdown, following InventoryItem's exact calculation order/whole-toman truncation, zero coin profit and VAT on wage + profit. Android runtime ownership stays unchanged.
- DesktopInventory owns immutable StateFlow, forms, selection, privacy, errors and IO writes. DesktopWorkspace supplies lifetime/market quotes. Compose renders and emits events. The vault shows metal-only value, while product pricing is per piece. Unavailable quotes have no fake fallback.
- DesktopDataStore.inventory is an owned adapter to the existing synchronized, locked writer. Optional inventory and stockAdjustments arrays are additive to schemaVersion 1; older documents default empty. Preserve portfolio/settings and unknown root/record fields. Validate persisted records strictly before lenient codecs: malformed numbers, discarded records, unknown enums and duplicate ID/code block opening and preserve bytes.
- Commit quantity and history atomically. Identical movement-ID retries are idempotent; conflicting retries fail. Stock cannot go below zero or exceed one million pieces per product. Metadata editing cannot change quantity; movement weight is documentary and never changes per-piece gross weight. Deletion retains immutable journal and historical product title.
- Embed bounded JPEG photos in imageUrl for self-contained backups. Import PNG/JPEG <=20 MB and <=16 million pixels; scale to <=640 on either axis, output <=220 KB, record <=300,000 characters. Raise workspace bound from 10 MB to 100 MB; inventory <=10,000, journal <=100,000. Export remains CREATE_NEW and includes photos/history. No remote image fetch.
- Print real 60×42 mm labels with java.desktop PrinterJob, bundled Vazirmatn and desktop-only [ZXing core 3.5.3](https://github.com/zxing/zxing/releases/tag/zxing-3.5.3). ASCII codes <=40 characters use Code 128; localized/long codes use UTF-8 QR. Native printer dialog runs on EDT and print I/O off the UI thread; cancellation is not reported as successful printing.
- Root forms survive navigation and protect dirty dismissal/application exit. Updater restart is blocked during inventory forms/writes. Existing shortcuts remain; Ctrl+6 opens inventory, Ctrl+N creates a product there and Ctrl+S saves an inventory form.

## Consequences and rollback

No Room schema, Android data, signing identity or financial policy changes. The desktop backup is a self-contained JSON document. For rollback, close the app, preserve the current file, and restore a verified export/previous document to workspace.json. MSI upgrade/uninstall preserves the separate data directory. No legacy ZIP migration is introduced.

Inventory remains local to this computer. Customers, invoices, ledger, onboarding and cloud remain later slices. Inventory-to-invoice transfer depends on the invoice slice; no inactive action is exposed. Physical printer output needs a manual device check; barcode/font/rendering are automated.

## Revisit

Introduce transactional database storage when journal/query volume, relations, multiple writers or sync justify it; import preserving IDs/images with rollback before retiring JSON. Define identity, ownership, deletion markers, stock authority and conflict policy before sync. Revisit embedded images near the 100 MB limit.
