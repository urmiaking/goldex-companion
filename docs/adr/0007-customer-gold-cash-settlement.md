# ADR 0007: Customer gold/cash settlement with a recorded rate

## Context

Customer accounts contain independent gold (750-equivalent grams) and whole-toman balances. A standalone cash receipt cannot infer whether it pays a gold debt. The old one-unit receipt path could leave both a gold debt and a cash credit. Delayed settlements also need their own rate, not the original invoice rate or a changing market quote.

## Decision

- Add a dedicated settlement form owned by `CustomerSettlementViewModel` and a pure shared `CustomerSettlementPolicy`. Select overall account or a ledger-connected invoice, target debt unit, physical payment unit, and an explicit live quote snapshot or custom rate. Preview both customer balances before confirmation. Independent manual receipts remain separate.
- Both receiving from a debtor and paying a creditor use the same signed policy. Opposing existing balances can be offset only through an explicit operation; this records no new physical receipt. No historical account is silently netted or revalued.
- Append one immutable ledger settlement with the physical payment and `LedgerSettlement` metadata: target unit, agreed rate, source/time, offset flag, and signed effects on each balance. `balanceEffect()` is the shared source of truth for apply/reversal/running balances. Metadata stays with the entry through Room, JSON backups and cloud diffs. Editing a settlement in the generic receipt editor is disabled; deletion reverses the recorded effects, then a corrected settlement can be entered.
- Persist using Room's additive 2→3 migration: `settlementJson TEXT NOT NULL DEFAULT ''`. Old records and IDs are unchanged; an empty value means the legacy one-unit effect. Unknown metadata fields survive serialization. Malformed metadata must fail rather than become an ordinary receipt. Legacy import backups remain untouched.
- Whole-toman valuation uses decimal multiplication with HALF_UP and checked Long overflow. Cash-to-gold division uses DECIMAL128 before adapting to the existing Double gram contract. Financial equivalents retain sub-milligram fractions so rounding a displayed 0.001g value does not create phantom cash credit. Physical gold input and full-payment suggestions use 0.001g; a suggestion rounds upward and any resulting excess is shown explicitly. Extremely small nonzero gram remainders remain on the account rather than being silently written off. Floating cancellation below 1e-10g is normalized to zero.
- Cross-unit payment applies to the selected debt first. Only actual excess becomes credit in the physical payment unit. Offsetting consumes only the portion needed from the existing opposite balance. Invoice balances sum stored allocated effects; excess stays on the overall customer account. An offset also reduces the selected invoice's own opposing credit up to the amount consumed, without allocating another invoice's credit to it.
- Use the existing mandatory `SyncUnitOfWork` for one atomic ledger/customer/outbox operation. A form request ID makes retries idempotent. Confirm rechecks customer and invoice balances against the preview; stale previews require reopening. Existing sync writer permissions remain enforced.
- Delayed receipts are invoice-linked ledger entries, not a rewrite of the original invoice's payment rows. Invoice list status uses recorded ledger balances. Saving an edited invoice preserves dated settlements and cannot remove/change their customer or ledger connection. Deleting the invoice reverses all recorded invoice-linked effects, including settlements, once.

## Consequences

- A 500,000,000 toman receipt against a 10g debt at 100,000,000 toman/g leaves 5g and zero additional cash credit. A receipt against a fixed cash debt changes the required gold weight when the rate changes, not the original sale amount.
- This does not change VAT, wage/profit formulas, invoice identity, signing, inventory valuation, or warehouse movements. The ledger settlement is not a general-ledger posting or an automatic stock receipt. Classic invoices without a customer-ledger connection retain their current workflow. Creating a new invoice still uses the existing retail/wholesaler debt policy; delayed settlement applies to the recorded obligation.
- A market snapshot requires a positive rate, live status and a known update time. Offline/placeholder values require a custom agreed rate. Later changes do not affect saved settlements.
- Database downgrade is unsupported: the additive column preserves old rows, but an older app must not open a version-3 database. Cloud protocol stays at 1 with additional metadata. Older clients do not understand cross-unit effects and must not edit/delete these entries. They should be upgraded before sharing this feature across devices; no remote capability negotiation exists yet.
- Rollback is restore-from-backup with a compatible app; it is not dropping the column or recomputing old customer balances. Before rolling back deployment, retain the version-3 database and export/backup financial records through the existing backup flow. Never discard settlement metadata to imitate legacy records.

## Follow-up

ADR 0008 implements explicit invoice debt basis and rate-preserving dated payment display rows. Its resave and rollback rules supersede the initial role-only creation restriction above.

## Revisit conditions

Revisit when invoice creation needs an explicit contractual debt basis independent of customer role, formal general-ledger postings/stock receipts are added, common fixed-point gold replaces legacy Double balances, or sync gains client capability negotiation for financial record types.
