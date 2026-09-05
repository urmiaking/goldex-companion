# Button Design System Invariant (`button-design-system.md`)

## 1. Core Principle
All primary action buttons across the application MUST strictly adhere to the warm amber-gold visual identity approved by the user (derived directly from `media_1788630349237.png`). Dark brown or arbitrary button colors are strictly prohibited for primary call-to-action buttons.

## 2. Standard Primary Action Button Specifications

| Token / Attribute | Standard Value | Description |
|---|---|---|
| **Background Gradient Start** | `#FAC24B` | Warm radiant sunshine gold |
| **Background Gradient End** | `#E7B342` | Deep rich honey gold |
| **Solid Average Color** | `#EBB644` | Fallback solid container color |
| **Content Color (Text & Icon)** | `#554300` (`RGB: 85, 67, 0`) | High-contrast antique dark bronze |
| **Button Shape** | `RoundedCornerShape(24.dp)` | Pill-shaped luxury container (کپسولی) |
| **Typography** | `VazirmatnFamily` (`fontFeatureSettings = "ss01"`) | Bold 14sp Persian numerals |
| **Height** | `48.dp` | Standardized ergonomic touch target |
| **Shadow / Elevation** | `1.5.dp` to `2.dp` | Soft golden ambient shadow |

## 3. Standard Secondary Action Button Specifications
- **Background**: `colors.surfaceElevated` with `colors.hairlineBorder` (0.6.dp)
- **Text & Icon**: `colors.textMain` and `colors.goldPrimary`
- **Shape**: `RoundedCornerShape(14.dp)` or `RoundedCornerShape(24.dp)`
