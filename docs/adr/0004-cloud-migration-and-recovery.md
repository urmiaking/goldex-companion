# Migration, bootstrap and recovery
Status: implemented; production release remains gated.

## Context
Existing Room version 1, legacy JSON backups, IDs and signing identity must survive adoption of cloud sync.

## Decision
Explicit Room migration 1 -> 2 adds metadata/outbox/checkpoint/conflict/business settings/assets/staging only. Schema exports are checked in; destructive migration is removed. Legacy import checks counts, unique IDs and unchanged IDs, commits a database marker and retains original preferences. Malformed imports roll back and require recovery instead of silently accepting a partial dataset. Business settings move once to Room; device security, theme, onboarding and cloud opt-in remain local. The onboarding completion marker joins the initial financial transaction.

An empty account uploads a consistent staged local snapshot; writes after that cut remain queued. An initialized account requires explicit replacement consent. Restore backs up records, metadata, queue, assets and conflict versions; downloads into staging, checks page hashes/record codecs and activates all records in one transaction. An edit during download invalidates consent. Failed pages remain staged; retrying restore requires confirmation and a fresh consistent server snapshot. Expired upload staging is reset for the next bootstrap attempt without deleting local records. No automatic merge or cross-account queue reuse occurs.

Coordinator owns StateFlow and a shared mutex for foreground/worker/restore/auth. One-second debounce, foreground and connectivity triggers drain the queue. WorkManager persists one-time retries and 15-minute compensation work; exponential backoff has jitter and 429 honors Retry-After. Auth/license/conflict/protocol errors stop retry loops. Files are private, content-hashed, <=5 MiB PNG/JPEG/WebP; references publish after upload completes. Green status requires all ACKs/pull pages/assets and zero queued operations.

## Consequences
Rollback disables cloud, retaining schema and data. Server migrations are additive; existing databases must baseline their old schema before migrate deploy. Changelog retention is 180 days; old cursors require snapshot. Tombstones/receipts are retained. Daily SQL + immutable file backups retain 30 days, with restore rehearsal before activation. SMS, secrets, operator backup schedule and real-device migration/Compose tests are release gates.

## Revisit
Resumable long downloads without fresh snapshot, backup import tooling, database background-only UI migration, or dataset sizes exceeding snapshot transaction limits warrant separate work.
