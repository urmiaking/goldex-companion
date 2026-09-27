# Atomic diffs, receipts and precision
Status: implemented, protocol version 1.

## Context
Repeated financial effects, device-clock ordering and JavaScript number rounding are unacceptable.

## Decision
Room mutations and durable outbox entries share one transaction. Nested mutations coalesce by type/ID into one group; invoice + actual ledger + balance and stock count + stock movement commit together. UUIDs and existing financial formulas are unchanged. Incoming records are applied directly through DAOs; invoice/ledger generation is never called during restore or pull.

Create sends a complete payload, patch changed top-level fields, delete a tombstone. Missing keys mean no patch; JSON null is explicit null; remove deletes a field. Arrays are replaced as one field. Whole-toman Long values use integer strings; existing historical Double fields retain decimal strings (no conversion to Long or rounding). BigDecimal codecs preserve the existing representable local value. Unknown top-level fields remain in metadata and survive a local edit. Local outbox also stores full local versions for conscious conflict resolution; localVersion/localPayload never enter the wire request.

A short UPDLOCK/HOLDLOCK workspace transaction rechecks membership, license and writer, allocates revision, applies all changes and writes changelog/receipt. The same operation ID/hash returns the same receipt; a different hash is rejected. Sequence is per device; versions are per record, including deleted records. ACK advances only sent versions and removes only the sent operation. Pull captures upperRevision and returns intact groups, normally 200 changes. Push normally allows 100 changes/512 KiB; larger groups stage base64 chunks and commit up to 5 MiB. HTTP compression is enabled. No full dataset enumeration occurs in normal sync; full enumeration is reserved for bootstrap, restore and explicit backup.

## Consequences
Version conflicts stop the whole group. Review loads every affected remote record. Explicit local choice rebases the whole group and later local versions with fresh operation IDs, without recomputing money; a second concurrent change stops again. Cloud choice uses confirmed complete restore, preserving a local backup. Deleted IDs cannot be resurrected automatically. SQL Server stores validated versioned JSON text in NVarChar(Max), with indexed ownership/version/revision columns, rather than Prisma Json.

## Revisit
Fine-grained array patches, multi-writer merging, SQL Int revision exhaustion or changing money representation require a new protocol.
