# 0014: Windows dashboard projection and page motion

## Context

Windows needs the Android dashboard's hierarchy and motion while its available adapters still cover personal holdings, rates and settings. Android business inventory and invoice storage remain Android-owned. Copying Android's assumed-live badges or fallback chart prices would misrepresent desktop data.

## Decision

Keep this feature desktop-owned. Extract dashboard rendering from the shell and introduce `DesktopDashboard` for immutable history state/events. Fetch genuine TGJU intraday and dated daily-close records with the existing HTTP/JSON stack. No historical series is inferred from a current/manual quote. Use calendar-aware Tehran timestamps, explicit rial-to-toman integer division, real temporal spacing, source/receipt labels, errors and retry. The current-process cache lasts five minutes before a requested refresh; cached data remains visible with its receipt time on failure, and yesterday's intraday snapshot cannot appear as today's chart. No persistent history cache or schema migration is introduced.

Use adaptive columns with existing Aurum tokens, Vazirmatn, RTL, financial ticker and segmented control. Keep the vault explicitly about personal holdings, distinct from mobile business inventory. Show the unavailable Windows invoice capability honestly; no fake invoice count, disabled pseudo-actions, phone access or speculative invoice persistence contract.

Animate destination keys with bounded fade/slide transitions. Each page has a retained saveable scope and finite size. Suppress pointer/accessibility interaction with outgoing pages and clear stale focus. Drafts and calculator state retain their existing owners. Add an optional `reduceMotion` boolean to the desktop version-1 document for page/sidebar/chart transitions; older documents default false, unknown fields and financial records survive atomic updates and previous-document backup. Older builds already preserve this unknown root field, so rollback needs no import. Shared financial ticker/selector motion stays unchanged.

## Consequences

The chart uses an independent historical source and may differ from manual/provider quotes used to value holdings. Network failure never invents data. History is unavailable offline after process restart, and a future history cache would need explicit cache compatibility and expiry policy. The invoice section remains a capability state until Windows invoice creation/import is implemented. Windows-only release routing applies; Android versions/contracts remain untouched.

## Revisit

When Windows invoice/transaction adapters exist, wire an immutable latest-invoice projection into the dashboard. Revisit historical-provider fallback and durable cache when market availability warrants it. A broader reduced-motion mode should be implemented in shared UI as a separate change affecting both platforms, rather than changing shared primitives incidentally here.
