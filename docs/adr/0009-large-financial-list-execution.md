# 0009: Bounded rendering and background financial list operations

## Context

Invoice, customer, statement and inventory screens could compose every card inside a single scroll/animated item. A 2,000-record invoice snapshot also queried ledger records separately per invoice and repeatedly rewrote the whole card list for transfers. Synchronous repository calls in feature constructors and events ran on the UI thread. These paths could block rendering despite Room already being available.

## Decision

- Expose each record directly as a keyed lazy item; retain the existing cards and Persian design.
- Give each animated destination its own finite viewport and scroll policy.
- Keep synchronous repository contracts, but invoke feature reads, writes and projections through a serialized coroutine queue using IO by default and an injectable dispatcher for tests.
- Run each existing financial unit of work in one synchronous worker invocation. Preserve transaction, outbox, rounding and financial policies.
- Publish success only after persistence; retain prior snapshots and show retryable failures. Reject duplicate submissions and obsolete customer/request results.
- Add a backward-compatible bulk ledger port and a Room query in batches of 500 IDs. Use snapshot caches and an ID-position map to project transfers in the original order.
- Move historical statement balance reconstruction to the shared customer domain; calculate snapshot totals while loading.
- Defer report snapshots until the reporting feature opens, and load the classic invoice archive in the background.

## Consequences

Only visible/prefetched cards are composed. Invoice list reads scale by query batches rather than one query per invoice; transfer projection scales with invoices plus transfers. All records still occupy a snapshot in memory. Existing IDs, JSON backups, database version, signing identity, RTL and money semantics remain compatible.

Room's global main-thread compatibility option stays enabled because settings/onboarding/portfolio still have synchronous host paths. Removing it belongs to a separate verified migration. No paging library or new DI/network framework is introduced.

## Validation and revisit conditions

Local tests cover 2,000 records on all four Compose screens, animated tab changes, real Room bulk reads, worker-thread repository access, stale customer results/errors, duplicate-submit prevention, transfer reconstruction and historical balances. Existing financial/persistence tests remain required.

Robolectric does not establish real-device FPS, memory use or ANR rates. Profile the signed release on a low-memory Android device using the procedure in docs/testing/large-record-lists.md. Revisit database-backed filtering/paging when larger snapshots or measured device allocation/query latency justify it.
