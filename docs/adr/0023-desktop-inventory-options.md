# 0023 — Windows inventory options and automatic product identifiers

Status: Accepted

## Context

Windows product entry needs reusable user-defined categories and storage locations, inline menu creation and automatic numeric product codes. Shared inventory categories also select financial policies, including the coin exemption. Existing codes are referenced by labels, search and financial records and must remain stable.

## Decision

Keep the extension desktop-owned. Optional `inventoryCategories` and `inventoryLocations` arrays in workspace schema 1 contain stable UUID `id` and user-entered `name` fields. Names are trimmed only on creation, nonblank, unique per catalog and at most 50 characters. Existing records without these arrays load empty catalogs. Location names continue to use the existing item location field.

An optional item `customCategoryId` references the category catalog. New custom selections use the shared MISC category and its existing financial policy; native categories continue to use their existing policies. The stored native category remains authoritative for finances, including native category edits by older clients that retain the unknown custom reference. Custom-name filtering follows the reference independently of that native policy. Missing or malformed catalog entries and dangling references fail validation without rewriting the file. Unknown fields are retained by the current document writer and shared codec merging.

`DesktopInventoryOptions` reads and validates extensions. `DesktopInventory` owns selection and option-creation events. Category references and product records commit together through the locked atomic `DesktopDataStore` writer. New product codes are selected from the available seven-digit numeric range starting at 1000000 under the same storage lock. A bounded search of at most 10001 candidates skips occupied codes under the existing 10000-item limit. An optional durable inventoryCodeSequence prevents recycling codes after deletion; exhausting the seven-digit range fails without writing data. The allocation checks normalized existing codes and occurs at commit, so a competing local event cannot claim the same code. Existing product codes are never renumbered.

## Migration and rollback

This is additive: no persisted records are rewritten on startup and there is no schema version bump. Existing catalog-free files remain valid. Exported backups include catalogs and references. Atomic writes retain the preceding document in `workspace.previous.json`; failed validation or writes publish neither item nor assignment.

Older Windows releases ignore the optional catalogs and show custom products under MISC. Their merging writer preserves unknown root/item fields. Reinstalling this release restores custom names. Preserve or export the complete workspace before rollback; no deletion/import is required. Android files, shared persistence contracts, signing identity and cloud authority are unchanged. Windows catalogs are local and do not imply synchronization.

## Consequences and revisit conditions

Table/details/labels and custom filters resolve display names through desktop inventory state. Adding an option saves it immediately as a reusable choice even if product entry is subsequently cancelled. Product cancellation is direct by user request; critical stock movement and app exit still protect drafts.

Move catalog contracts into shared core only when Android or explicit cross-host sync also needs them. Define category financial policy mapping and migration before allowing custom category edits/deletions or synchronization.
