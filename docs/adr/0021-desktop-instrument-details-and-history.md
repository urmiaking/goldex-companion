# Dedicated Windows instrument details and historical data

## Context

Stitch screen b400a9bd531a410785d17f8465300988 and the user's two screenshots replace the local rates dialog with a dedicated analysis route. The current global header must remain intact. Existing dashboard history covers 18k only; using it for coins/FX would misrepresent another instrument. DesktopInvoicesPage currently contains sample rows and no real invoice persistence.

## Decision

RATE_DETAIL is an internal desktop destination, highlighted under RATES. DesktopWorkspace owns route events; DesktopRateDetail owns immutable selected instrument/horizon/history state and generation-guarded requests. Separate saved page keys preserve board scroll. Today/week/month/year use each BoardInstrument's stable TGJU indicator; TETHER remains hidden by the user's earlier request. Refresh starts on entry and repeats every five minutes while observed. Cancellation never applies a late result to another instrument/timeframe. Chart and annual statistics have separate loading/error fields.

Optional public caches live in rate-history-v1, keyed by stable instrument and timeframe names, with schemaVersion 1. Atomic replacement preserves unknown root fields and protects malformed/newer schemas. Older records missing low/high default to price. This new directory does not migrate or alter workspace.json, settings, customers, inventory, invoices, signing identity or financial formulas. Rollback ignores the optional cache and retains all financial records. Offline/error display retains original receipt and point timestamps and labels stored data; prior-day intraday data is explicitly identified.

History prices are Long whole toman, truncating incoming rials consistently with the existing market adapter. Ounce prices are Long US cents with exact decimal parsing. Reject malformed, negative, future, inconsistent OHLC and insufficient rows. Daily statistics use actual OHLC and a simple close average, never fictitious volume weighting, support/resistance or trading risk ratings. 30-day metrics require coverage reaching the start of that calendar window. Persian-year return requires a prior-year close adjacent to the first recorded current-year date. Nominal gold premium uses the same snapshot's ounce/USD values, existing toSpotPrice18k normalization, 31.1035 grams per ounce, 750 purity and explicit HALF_UP whole-toman intrinsic value; it is a comparison per gram of 18k, not a transaction price. Coin bubbles reuse the existing domain policy.

The page retains the global header and vertical transitions, uses Sovereign Aurum/Vazirmatn, shared segmented selectors, RTL chronological chart with keyboard/pointer readout, honest missing states and responsive adjacent/stacked bottom cards. Gold shortcuts explicitly apply only available K18/K24/mesghal prices and preserve weight/other inputs. Recent documents show an unavailable capability state; no sample financial records or inert invoice creation button is copied from the reference.

## Consequences and revisit conditions

No shared or Android changes are required. A cached chart remains useful after restart, but is not claimed live. TGJU can omit an instrument/timeframe or reject requests, so retries retain the last valid snapshot. Intraday endpoints expose time-only rows interpreted in Tehran's provider-day context; empty/future rows are rejected. Native Windows invoice ownership and stable rate associations must exist before filling the documents panel. Revisit when verified provider volumes, bid/ask, cross-host history, or real Windows documents become available.
