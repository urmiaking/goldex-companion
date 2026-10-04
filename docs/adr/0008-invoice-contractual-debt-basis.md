# ADR 0008: Explicit invoice debt basis and dated settlement

## Context

Customer role does not specify the contract: a retail customer can owe grams, while a colleague can owe fixed tomans. Reopening a saved invoice's original payment form also used the invoice rate for later cash receipts. Conversion-bearing generated receipts were incorrectly treated as independent dated settlements when resaving, duplicating their effects.

## Decision

- New invoices explicitly select GOLD (default in the editor) or CASH independently of customer role. GOLD requires a customer, ledger connection and positive agreed issue rate. The net agreed invoice value (including existing wage/profit/VAT and received-item valuations) is rounded HALF_UP to whole tomans and divided by the issue rate once. It is a financial 750-equivalent obligation, not just the physical weight of the jewelry. Existing tax and price formulas are unchanged.
- Shared `InvoiceDebtPolicy` owns the principal and initial payment allocation. Weight retains fractional financial equivalents; physical gold keeps 0.001g input precision. Whole-toman arithmetic uses the checked decimal adapter. A deferral to the ledger is not a receipt. Excess belongs to the customer's physical payment unit and cannot reopen the invoice.
- Saved invoice basis is immutable. Missing basis means the previous role-dependent policy; no old invoice, customer balance or ledger entry is converted automatically. A financial amendment after dated settlement requires reversing those settlements first. Customer ownership cannot change while invoice-linked entries exist.
- Both saved-invoice settlement entry points open `CustomerSettlementViewModel` for that invoice. Each stage uses its own explicit quote snapshot/custom rate. Cash, POS, bank transfer and agreed coin value settle gold by that rate; scrap and bullion use net physical weight and fineness. Coin value is explicitly entered by agreement; type/count belong in the receipt note. Mixed payments are multiple dated stages, each with its own preview and receipt.
- The ledger remains authoritative for saved invoice status and account balances. The invoice's dated payment display carries the same immutable `LedgerSettlement` snapshot, date and receipt ID. It cannot be edited/deleted as an original invoice payment. Deleting the ledger receipt atomically removes its display row and reverses its original effects. Existing implicit issue payments are materialized before appending the first dated row, so the fallback fields never disappear from accounting.
- Resaving reverses/rebuilds generated entries, including generated conversion entries. Only non-generated settlement entries are retained. Their receipt IDs exclude the display rows from regeneration. Filtering the last payment also clears legacy fallback fields for regeneration, preventing a second implicit charge.

## Persistence, compatibility and rollback

Room 3→4 adds only `barter_invoices.debtBasis TEXT NOT NULL DEFAULT ''`; the empty value preserves the old contract. Previous migrations remain chained. JSON backups, Room payment converters and cloud numeric-string payloads round-trip the nullable basis and nested payment settlement snapshot. Unknown settlement fields survive. Unknown contract values/malformed settlement metadata fail import instead of silently converting or dropping records. Sync remains on protocol 1; all active writer devices must run this version before editing these invoices, since older clients do not understand explicit debt basis.

No bulk rewrite, historical repricing, signing change or removal of retained import backups occurs. Before rollback/export, retain the version-4 database and financial backup. Rollback means restoring a compatible pre-upgrade backup with the old app, or using a version-4-compatible fix; never downgrade by dropping the new column or recomputing balances. Existing cash contracts require a separately planned, explicit conversion operation if the user wishes to change them.

## Consequences and verification

At 20 million toman/g, a 200 million invoice with 100 million paid leaves 5g. A later 22 million quote proposes 110 million; paying 100 million leaves 0.454545…g. The cash contract still requires 100 million. Local tests cover these flows, mixed payments, excess, fineness, repeated saves, deletion, stale previews, persistence/cloud round-trips and Room migration without changing financial rows or pending sync.

Receipt posting does not automatically create inventory movements. PDF states the contractual basis while retaining the original sale values. The ledger and dated receipt are the source for subsequent settlement details.

## Revisit conditions

Revisit for explicit conversion of historical contracts, fixed-point gram storage, per-receipt stock movements, richer coin inventory metadata, or cloud capability negotiation.
