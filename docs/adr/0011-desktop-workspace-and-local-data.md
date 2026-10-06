# ADR 0011: Desktop workspace, portfolios, preferences and quotes

## Status

Accepted for 0.56.37. The user selected dashboard, market rates, personal holdings and settings as the next Windows slice.

## Context

The first Windows app was a session-only manual calculator. Shared repository contracts, financial valuation and models already existed, but Android-owned JSON codecs, preference storage and market requests prevented reuse by the JVM host. The personal portfolio is distinct from business inventory/customer balances; presenting it as the entire business vault would be misleading.

## Decision

- Extract `PersistenceJsonCodecs` unchanged from `app/data` to `core/jvmSharedMain/data`, make the object public, and add a settings codec. Android retains its existing persisted shapes, Room version/transactions, import paths and callers. The JVM target supplies `org.json`; Android uses platform JSON. No Java/JSON import enters commonMain.
- `DesktopDataStore` implements shared `PortfolioStore` and `SettingsStore`. A version-1 document at `%LOCALAPPDATA%/Qirato/Desktop/workspace.json` holds stable portfolio IDs, profile/default settings, theme and the last quote. Version 0.56.36 had no financial desktop store, so there is no existing desktop dataset to migrate. Android storage is neither opened nor implicitly imported.
- The local store holds an exclusive channel lock for the entire app session. A second writer cannot open it. Writes force a temporary file and atomically replace the document; there is no non-atomic fallback. The immediately preceding document is separately preserved as `workspace.previous.json`. State is published only after a successful write.
- Before decoding, validate supported schema, all record IDs/enums/numbers and record count. Malformed/future records block startup without rewriting the file. Unknown root, settings and portfolio fields survive edits. Do not silently discard records through the retained tolerant Android codec.
- User-requested backup export uses `CREATE_NEW`, preserving existing backup names. Recovery/rollback: close the app; preserve the original and previous document under distinct names; validate a saved version-1 export; restore that copy to `workspace.json`; reopen and verify counts/IDs/settings. Restore the preserved original to roll back. There is no automatic restore or cross-device import in this slice.
- `DesktopMarketRepository` implements existing market ports with desktop HTTP adapters for the three existing providers, using explicit headers, bounded responses, timeouts, IO dispatch and `finally` disconnect. Parsers retain provider units; missing quotes remain zero/unavailable. No guessed coin/USD/ounce defaults are supplied. Preferred-provider failure tries the other providers and retains the actual successful source.
- `MarketSnapshot` records local observation time and ONLINE/CACHED/MANUAL provenance. A receipt is fresh for at most two minutes; cached startup/failure states retain the original timestamp. This is receipt freshness, not a promise of exchange-level price timestamps. Manual mode suspends automatic refresh until explicit online refresh. No unavailable quote can be exported to the calculator or counted in a complete portfolio value.
- `DesktopPortfolioPolicy` validates Persian input, whole-toman purchase values, positive 0.001g weights, positive integer coin counts, stable IDs and overflow boundaries. It delegates valuation to the existing `PortfolioItem`/`PortfolioValuation`. Zero cost means unknown purchase basis in this UI and is not shown as profit. Missing quotes/overflow make the complete value unavailable.
- `DesktopWorkspace` owns immutable StateFlow, navigation, persistent drafts/events, data error states and calculator state. Settings changes update only the calculator's next reset; an in-progress quote is unchanged. Search normalizes Persian/Arabic letter variants. Cancelled forms/deletions do not write storage.
- Desktop UI has a Persian right-side navigation rail/sidebar, responsive content, virtualized holdings, keyboard-visible navigation focus, retained forms, source/time states, explicit delete confirmation, native backup chooser and Ctrl+1–5 / Ctrl+N / Ctrl+R shortcuts. Existing Aurum tokens, Vazirmatn, RTL and shared components remain authoritative. Window and packaged launcher use the approved Android icon; the ICO container preserves the original PNG pixel data.

## Consequences

The desktop app now has real local holdings/settings and provider-backed rates without migrating Android's database or duplicating its formulas. JSON is sufficient for this bounded portfolio slice; invoice/ledger/inventory transactions still require a dedicated database/transaction migration decision. Cloud accounts, customer ledgers, invoices, business stock, PDF/printing, license activation and advanced calculator screens remain future slices. Rates are publicly sourced and can be unavailable; explicit manual entry remains usable offline.

## Revisit

Revisit before adding financial relationships/queries, introducing a desktop database, restoring/importing Android data, shared cloud writers, upstream quote freshness contracts, or a signed installer. Preserve IDs and backup/rollback in every migration.
