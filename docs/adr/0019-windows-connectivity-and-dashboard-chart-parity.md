# ADR 0019: Windows connection status and mobile dashboard chart parity

## Context

The shell chip used rate freshness, so a cached/manual quote could look disconnected even when Windows had Internet access. The dashboard chart used a grid and a separate slider, differing from Android's compact price/delta, smooth gold area, dashed marker and floating tooltip.

## Decision

Reuse the core ConnectivityObserver port with a desktop-owned WindowsConnectivityObserver. It reads Windows Network List Manager IPv4/IPv6 Internet flags at startup and every 30 seconds, independently of providers, quotes and autoSyncRates. A hidden built-in PowerShell process reads COM status with a five-second timeout and no external request; only that owned process may be terminated on timeout. Workspace owns the observation lifetime. Unsupported/read-failure states conservatively show offline. The chip renders only online/offline; quote source/freshness stays in rate captions. The shell refresh icon follows the saved autoSyncRates preference; unsaved changes do not hide/show it. The future cloud control remains unchanged.

DesktopDashboardTrend owns rendering of the Android card structure, 32dp segmented horizon control, independently animated latest price/delta, 130dp smooth area chart, live-end marker, dashed selection guide, floating price/date tooltip and matching fade/scale curves. It consumes existing DesktopDashboard/GoldHistorySnapshot state. Use the shared semantic palette and hero gradient for the tooltip, without introducing color tokens or dependencies on Android classes. Keyboard arrows/Home/End and an accessibility progress action supplement click/drag without a visible slider. Draw bounded real extrema as before; selection resolves against all original records. The mobile synthetic fallback price and unsupported union provenance are intentionally not copied; unavailable prices stay unavailable and actual TGJU history receipt/error state remains visible.

## Consequences

Windows Internet status does not promise any market provider is available; source captions and cached/error states remain authoritative for rates. History remains the existing in-memory cache with no storage migration. No financial formula, signing identity, Android/shared source, or customer-authored text changes. Remove only extra hamza marks from program-authored Windows copy and record the writing rule. Shared validation messages are normalized only at Windows error rendering through programErrorText; domain errors and editable/stored customer text remain unchanged.

## Revisit conditions

Replace the shell chip when cloud identity/sync becomes implemented. Consider direct native event observation if connection latency or the bounded PowerShell poll becomes material. Move chart rendering to shared-ui only as an explicit cross-platform change with both releases and Android visual regression checks.

## Source

[Microsoft NLM connectivity flags](https://learn.microsoft.com/en-us/windows/win32/api/netlistmgr/ne-netlistmgr-nlm_connectivity).
