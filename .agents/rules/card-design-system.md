# Card Design System Invariant (`card-design-system.md`)

## 1. Core Principle
All elevated cards, containers, and modules across the entire application MUST strictly adhere to the unified card design language established on the Home screen (`DashboardScreen.kt` / `LuxuryCard.kt`). No arbitrary corner radiuses, border thicknesses, or inconsistent shadows are permitted.

## 2. Standard Card Specifications (Light & Dark)

| Attribute | Standard Value | Description / Token |
|---|---|---|
| **Corner Radius** | `16.dp` (`RoundedCornerShape(16.dp)`) | Universal card corner radius for all cards across all screens |
| **Border Stroke** | `0.6.dp` | Gilded hairline border: `BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f))` |
| **Shadow Elevation** | `2.dp` (Light) / `0.dp` (Dark) | `shadowElevation = if (colors.isDark) 0.dp else 2.dp` |
| **Background Color** | `colors.surface` | Light alabaster `#FFFFFF` / Dark slate `#161C28` |
| **Top Hairline** | `2.dp` (Optional) | `colors.specularHairlineBrush` for premier luxury cards |
| **Inner Padding** | `14.dp` to `16.dp` | Standardized padding for internal card content |

## 3. Hero Dark Card Specifications (مانیتور مشکی ابسیدین)
For high-impact valuation monitor cards (e.g. Asset Vault in Home, Total Payable Monitor in Calculator):
- **Shape**: `RoundedCornerShape(16.dp)` or `RoundedCornerShape(18.dp)`
- **Background**: `colors.heroCardGradient` (Linear gradient: `#141B2B` -> `#1D263B` -> `#111622`)
- **Border**: `BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.6f))`
- **Typography & Contrast**: Pure white (`Color.White`), gold currency (`colors.goldSecondary` / `#FFE088`), and emerald accents (`#10B981`).
