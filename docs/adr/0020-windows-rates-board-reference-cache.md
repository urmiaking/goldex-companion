# Windows rates board reference cache

## Context

Windows follows Stitch screen de1138f22c2a4069af9bd7c248a65a36 (project 9122211023780147846), supplied as two continuous screenshots, and Android market navigation. Shared MarketRates does not carry daily statistics, AED/USDT, bid prices or spreads. The selected/manual financial quote must remain authoritative.

## Decision

DesktopRatesBoard owns immutable reference state, bounded refresh and offline fallback. DesktopRatesBoardRepository reads TGJU current quotes and intraday tables with three concurrent history requests, IO dispatch, explicit headers and byte/time limits. Refresh occurs on entering the rates page and every five minutes while it is open and automatic rates are enabled. Explicit refresh updates selected rates and references. Dashboard history and shared source are unchanged.

Existing instruments use the selected snapshot. AED and USDT are independent TGJU references with their own source/time. Daily statistics and history for existing instruments require matching TGJU price, non-manual snapshot and today's Tehran data. Missing values remain unavailable; history fallback retains its original timestamp and is limited to the same day. Bid prices, spread and demand are unavailable rather than inferred. Coin bubbles reuse GoldCalculationUseCases.calculateCoinBubble including pure weights and mint fees. They are calculated metrics, not recommendations.

Optional rates-board-v1.json uses schema 1 and stable enum identifiers. It never changes workspace.json or financial/customer records. Writes use atomic replacement where supported. Unknown root/record fields and instruments survive; known instruments absent from a new response are removed so an old price cannot gain a new timestamp. Malformed/unknown-version files remain untouched and produce a visible error. An older app ignores this optional public cache; rollback needs no financial migration.

Feed currency parses via decimal arithmetic, truncating sub-toman rials consistently with the existing adapter. Bounded whole-toman prices are exactly representable in the display Double; only ounces retain fractions. Existing coin-policy display rounds explicitly to whole toman. Financial formulas remain unchanged.

## Consequences and revisit conditions

Rates filters retain navigation state. Gold detail applies K18/K24/mesghal to the existing calculator without clearing weights. Tables collapse into readable rows on narrow windows. Both themes reuse Sovereign Aurum tokens and Vazirmatn.

Revisit for cross-host statistics, verified provider bid/ask/demand, or a larger shared detail feature. Public reference cache does not participate in financial cloud sync.
