# ADR 0018: Windows update discovery and calculator market binding

## Context

The user requires an update offer at startup, a user-started background download with visible progress, and a separate user-started notes/restart dialog after verification. The calculator already shares financial policies, while desktop market snapshots are persisted by DesktopDataStore. Fetching rates did not populate its manual form. Dense stock browsing should leave more room for the selected product without changing inventory ownership or financial data.

## Decision

- WindowsUpdater.start is invoked by the workspace UI once per updater instance. It checks on startup and every five minutes after each attempt. AVAILABLE is metadata only; download is an explicit event. DOWNLOADING/VERIFYING keep the global progress box active across destinations, without modal presentation. READY exposes installation; only clicking that action opens notes and restart consent. Retry/cancel remain serialized and incomplete packages never enable restart. This supersedes automatic download in ADRs 0013/0015; MSI verification, helper, rollback and installation identity remain unchanged.
- DesktopCalculatorRates is the desktop presentation owner binding the shared ManualGoldCalculator to the existing market stream. Cached rates populate the form immediately; automatic updates use actual 18k/24k/mesghal quotes, falling back to existing domain conversion only when a basis quote is absent. Editing SPOT enters manual mode; incoming quotes cannot replace that price. Returning to market mode or reset is explicit. Weight, fineness and fees remain owned by the shared form and unchanged by market events. Clipboard summaries retain source/freshness/time.
- Cached snapshots always carry isLive=false and retain observation time. Existing provider fallback, disk cache and manual snapshots remain compatible. No new persistence schema, signing or Android changes.
- Page transitions use vertical motion and existing durations/reduced-motion handling. Stock rows use existing neutral surface tokens, a subtle gold border/selection rail and compact typography; the selected panel receives the larger share of desktop width. Units are trailing adornments on the visual left, with LTR numeric editing and unchanged validation.

## Consequences and verification

The workspace remains usable while an update downloads or completes; no automatic dialog steals focus from a draft. Unsaved financial forms and pending saves block restart. Prepared updates remain process-local. An installed MSI is still required for installation; development images never apply updates.

Local tests cover startup/five-minute checks, discovery without download, background progress while entering data/navigation, explicit ready consent, cancellation/retry, actual quote basis selection, manual-rate preservation, disk cache across offline restart, rendered unit placement and vertical navigation. Local native runtime and release CI installation/upgrade remain required. Released MSI/APK bytes are not downloaded again for verification.

## Revisit

Revisit for durable prepared-update recovery, enterprise installation or a new quote synchronization preference. Such changes require explicit persistence/compatibility policies; this decision introduces none.

Release verification found a launcher/JVM lifetime race in the disposable fixture: stopping only its launcher could leave a child JVM holding workspace.lock. The fixture now stops only its explicit process tree. The shipped helper waits up to 30 seconds for exclusive lock acquisition after the target process exits, postpones if the lock remains held and never forces another process to exit or installs while storage is open. windows-v0.56.43 remains immutable and unpublished; the verified delivery advances to 0.56.44.

## 0.56.48 refinement

Update notification and progress now live in the persistent sidebar directly above the theme control. Background discovery uses a separate checking flag and retains AVAILABLE without rendering a checking spinner; selecting download cancels pending discovery and the serialized result cannot overwrite the download phase. Financial dialog and dirty-settings restart guards are retained.

Desktop market fetching is always enabled at startup and whenever the quote expires. The legacy autoSyncRates property remains serialized with its original value for compatibility and Android remains unchanged; desktop no longer reads it to schedule fetching or show manual refresh controls. Offline failures preserve original cached timestamps and calculator manual SPOT overrides. The shared shell displays today's Persian date independently of the quote observation time, refreshing across Tehran midnight through the existing workspace clock tick.
