# 0017 — Desktop inventory navigation parity

## Context

Android MainScreen opens InventoryScreen for business stock; its dashboard displays inventory weight and metal value. The retained PortfolioTab source is not a current navigation destination. Windows exposed both earlier portfolio holdings and new business inventory as primary destinations, while its dashboard still showed the former. This made the same notion of current assets refer to different records. DesktopField also treated every numeric input as a price, grouping phone numbers and identifiers.

## Decision

- Use DesktopInventory's existing immutable state for dashboard weight, metal-only valuation, piece count and shared privacy. Both dashboard links and new-product actions open the full inventory feature. Stock changes flow directly back to the dashboard, without a second projection/store or invented missing quotes.
- Expose one inventory destination in the five-item sidebar. Preserve the internal PORTFOLIO route and its data, editing/deletion and backups; open it from settings only when retained records exist. Do not convert portfolio records into inventory: their purchase-cost/coin-type semantics cannot be losslessly mapped to inventory fineness and per-piece stock. Current totals never sum these distinct datasets. No persisted migration is needed; rollback remains compatible with schema 1.
- Keep Ctrl+1/2/3/5, route Ctrl+4 to inventory, retain Ctrl+6 as an inventory alias, and route Ctrl+N to new inventory products. Derive motion direction from main navigation order. Preserve root-owned drafts and updater restart protection.
- Separate numeric input direction from monetary formatting. Identifiers and phones keep leading zeros and symbols, use LTR with appropriate keyboard semantics and no separators; percentages, quantities and weights also remain ungrouped. Amounts alone opt into the existing shared price transformation. Gallery names remain text; addresses support line breaks.

## Consequences

The dashboard and inventory now report the same current stock, including purity, stone deductions and quantities. Historical portfolio data stays accessible but has no effect on this balance. The change belongs entirely to desktop; Android/core/shared-ui remain unchanged, and only Windows receives a new artifact.

## Revisit

Revisit retained holdings when an explicit import/conversion flow with field mapping, preview, duplicate detection and rollback is requested. Inventory-to-invoice transfer remains part of the future Windows invoice slice.
