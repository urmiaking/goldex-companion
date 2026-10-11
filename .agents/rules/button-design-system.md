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
| **Button Shape** | `ButtonShape` / `RoundedCornerShape(12.dp)` | Normal in-page curvature; Windows header/navigation capsules are the explicit contextual exception |
| **Typography** | `VazirmatnFamily` (`fontFeatureSettings = "ss01"`) | Bold 14sp Persian numerals |
| **Height** | `48.dp` | Standardized ergonomic touch target |
| **Shadow / Elevation** | `1.5.dp` to `2.dp` | Soft golden ambient shadow |

## 3. Standard Secondary Action Button Specifications
- **Background**: `colors.surfaceElevated` with `colors.hairlineBorder` (0.6.dp)
- **Text & Icon**: `colors.textMain` and `colors.goldPrimary`
- **Shape**: `ButtonShape` / `RoundedCornerShape(12.dp)`; do not choose an arbitrary radius per screen.

## 4. Windows context and required icons

Windows MUST follow [windows-design-system.md](windows-design-system.md) and [the detailed contract](../../docs/design/windows-controls.md): header/nav capsules, 12dp body/dialog actions, 14sp labels and mandatory 20dp meaningful icons. Use `DesktopActionButton` for body actions and `WorkspaceControl` only in header/nav. Filters and dashboard quick-access tiles have their own roles; they are not variants of a primary action button. Android's current shared `GoldButton`/`ButtonShape` implementation remains the source of truth for that host.
