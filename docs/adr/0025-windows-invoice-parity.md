# ADR 0025 — Windows invoice workflows and official document output

## Context

The Windows invoice archive was an illustrative page with fixed financial values and inactive controls. Windows already persisted real invoice links for customer settlements. Android owns mature barter editor state and forms; the money policies, invoice document projection, JSON codecs and repository/transaction contracts are shared.

## Decision

- Add a desktop composition root, `DesktopInvoices`, backed by the existing customer/invoice stores and `DesktopDataStore.financialTransaction`. Save, dated settlement and delete use the existing shared policies without repricing historical entries or changing VAT/rounding.
- Retain a documented Android UI/state snapshot at `94bc39f` in the desktop host, adapting ViewModel lifetime to the injected coroutine scope, Persian input parsing, bounded desktop modals and platform callbacks. Shared domain calculations remain in `core`; Android sources and shared tokens are unchanged.
- Replace the demonstration archive with state-driven summaries, keyed responsive cards, Persian search and filters. Keep one workspace heading and a 48dp capsule issuance action beside calendar/status, consistent with customers.
- Render body actions, selectors, inputs and modal lifetimes through the Windows control adapters from ADR 0024, retaining the latest customer layout and dialog motion.
- Validate positive item weight/amount and valid category inputs before desktop issuance; invalid or empty forms cannot write financial records. Preserve existing financial formulas and persisted records.
- Reuse the customer settlement owner and selected-invoice allocation. Refresh both projections after settlement. An editor can remove a dated settlement only through explicit final save; cancellation and page navigation cannot mutate the stored ledger.
- Calculator transfer preserves its validated result and selected price basis. A draft survives page navigation. Financial saves, modal state and document export block updater restart/unsafe exit. Explicit close can discard an unsaved draft after the existing confirmation.
- Project official documents through `OfficialInvoiceDocumentFactory`. Desktop AWT renders bundled Persian fonts and local logo/stamp/signature assets into paginated A5 landscape pages; PDFBox 3.0.7 packages these pages and supplies native printing. The preview renders the same pages. PDF output is rasterized at 144 DPI, so text selection is not offered. File output uses a sibling temporary file and atomic replacement where supported; overwriting an export requires a user decision.
- Include `jdk.unsupported` in the packaged runtime so PDFBox can use the Java 17 mapped-buffer cleaner without falling back to inaccessible Java 8 reflection.
- Windows has no SMS service. Copy a meaningful invoice message to the clipboard and report that it was copied. Do not claim delivery or add credentials/network sending.

## Persistence and compatibility

No persisted shape changes. Continue the optional schema-1 `barterInvoices`/`invoices`/customer/ledger collections and Android-compatible codecs established in ADR 0022. Existing IDs, unknown fields, strict financial validation, previous-document backup and transactional rollback are retained. No Android data is implicitly imported, and no signing material is accessed or copied.

## Consequences

Full Windows invoice workflows reuse the financial source of truth. Host UI/state duplication is a deliberate migration compromise: future changes must compare both snapshots. Document pages preserve Persian shaping and work offline, at the cost of raster PDF text and a desktop-only PDF dependency. Invalid brand assets surface an output failure and preserve an existing export instead of silently omitting identity.

## Revisit conditions

Extract platform-free invoice presentation state to `core` when both hosts next change together. Revisit searchable vector PDF output if accessibility, selectable text or commercial print requirements justify a shaping-capable renderer. Implement SMS only when an authenticated service, delivery/error contract and explicit user sending flow exist.
