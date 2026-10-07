# Windows settings and inventory parity — 0.56.42

Android reference: MainScreen's InventoryScreen route and DashboardScreen's totalInventoryWeight18k/totalInventoryValuationTomans. PortfolioTab remains source-compatible but is not a current Android navigation entry.

DesktopMobileParityTest covers:

- Actual rendered phone, union and license identifiers contain no price separators and retain leading zeros, Persian digits and symbols; gallery names, fractional percentages and multiline addresses survive saving and reopening. Light/dark settings screenshots are captured; monetary manual-rate fields still group prices.
- Five visible destinations include one inventory entry and no separate portfolio entry. A retained portfolio record cannot inflate stock. Three 4-gram net pieces of purity 875 show 14 grams at purity 750 and 84,000,000 toman at a 6,000,000 quote.
- Dashboard and inventory share persisted privacy. A deduction updates both current weight and metal value immediately; the old portfolio remains available through settings without changing IDs.
- Missing gold quotes keep the real stock weight visible and show unavailable value without a fallback price.
- Compact dashboard quick-add writes a complete inventory item, preserves 0.001g input and quantity, and creates no new portfolio record.

Existing dashboard large-value, layout, interrupted-transition, reduced-motion and inventory form/journal tests remain applicable. Monetary manual-rate and purchase-cost fields retain price grouping. Missing quotes remain unavailable, and no purchase-profit estimate is presented as current stock value.

Run local compile/unit gates, `:desktop:test`, `:desktop:createDistributable` and packaged `--verify-runtime`; release routing is Windows-only. CI verifies real MSI installation/upgrade. Verify publication from CI and asset metadata without downloading the released MSI/APK again.

Screenshots live at desktop/build/screenshots/settings-identifiers-*.png, unified-dashboard.png and unified-inventory-compact.png.
