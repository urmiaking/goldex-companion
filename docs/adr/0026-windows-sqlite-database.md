# ADR 0026 — Windows Relational SQLite Database Architecture and Zero-Data-Loss Migration

## Context

The Windows desktop workspace previously relied on a single monolithic `workspace.json` document with in-memory JSON cloning and atomic file replacement. While acceptable for initial desktop prototyping, scaling business operations with thousands of inventory items, customer contacts, barter invoices, and ledger transactions causes high memory allocation overhead and lacks database indexing.

Android previously completed its migration from SharedPreferences JSON to a relational Room SQLite database (ADR 0001). On Windows Desktop (JVM), `org.xerial:sqlite-jdbc` embeds the native Windows SQLite engine directly within the packaged MSI application runtime, providing robust ACID transactions, Write-Ahead Logging (WAL), foreign key integrity, and zero external runtime dependencies.

## Decision

- **SQLite Database Engine (`DesktopSqliteDatabase`)**:
  - Implements the shared repository contracts (`CustomerStore`, `InventoryStore`, `PortfolioStore`, `InvoiceStore`, `SettingsStore`, `OnboardingCheckpoint`, `SyncUnitOfWork`).
  - Configures production SQLite pragmas on connection initialization:
    - `PRAGMA journal_mode = WAL;` (high concurrency; readers and writers do not block each other)
    - `PRAGMA synchronous = NORMAL;` (optimal performance with WAL safety)
    - `PRAGMA foreign_keys = ON;` (strict referential integrity)
    - `PRAGMA busy_timeout = 5000;` (resilient lock retry)
  - Tables created with comprehensive indexes:
    - `customers`, `deleted_customers` (indexed on `phone`, `account_code`)
    - `ledger_transactions` (indexed on `customer_id`, `invoice_id`, `timestamp`)
    - `inventory_items` (indexed on unique `code`, `category`)
    - `stock_adjustments` (indexed on `item_id`)
    - `inventory_options` (indexed on `type`)
    - `portfolio_items`, `invoices`, `barter_invoices`, `system_settings`, `market_cache`, `metadata`
  - Preserves forward compatibility and unknown fields by persisting payload JSON alongside queryable columns.

- **Zero-Data-Loss Migration (`DesktopDataMigrationManager`)**:
  - Automatically checks on startup if legacy `workspace.json` exists and whether the migration marker is set.
  - Validates legacy data with strict financial checks before import.
  - Migrates all customers, ledger transactions, inventory items, adjustments, options, portfolio items, invoices, and settings into `qirato.db` inside a single ACID transaction.
  - Leaves the original `workspace.json` file intact for safety and rollback.

- **Storage Layer Integration (`DesktopDataStore`)**:
  - Delegates persistence and transactional boundaries (`financialTransaction`) to `DesktopSqliteDatabase.transaction`.
  - Maintains `workspace.lock` file locking to guarantee single-instance workspace access.
  - Maintains atomic backup snapshots on disk for portable export and verification.
  - Both in-memory and database states roll back atomically on any financial transaction failure.

## Persistence and Compatibility

- Zero changes to Android Room schema, Android preferences, or cloud ownership.
- Financial formulas, rounding policies, Persian number formatting, and RTL presentation remain 100% identical.
- Existing Windows workspaces seamlessly migrate their data on first launch with zero data loss.

## Consequences

- Database lookups (transactions by customer, inventory by code, invoices by number) are indexed and instantaneous.
- Memory consumption is significantly reduced by eliminating full-document JSON re-serialization on every keystroke/mutation.
- ACID transaction guarantees protect financial ledger and invoice linkages against crashes.

## Revisit Conditions

Revisit if Room KMP reaches full maturity across Kotlin 1.9/Compose Multiplatform desktop targets without requiring major toolchain migrations, or when cloud synchronization requirements dictate an unified offline sync engine across both mobile and desktop.
