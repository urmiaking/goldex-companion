# Input Field & Numeric Typography Invariant (input-field-design-system.md)

## 1. Core Principle
In Persian financial and gold market interfaces, typing numbers with decimal points and unit adornments in native RTL often introduces severe UX bugs (such as misplaced decimal dots, reversed digit entry, and inverted unit badges). All input fields in GoldEx Companion must adhere to a unified layout and typography standard.

## 2. Inviolable Rules

### Rule 1: Unit Adornments Anchored on the Visual LEFT
- All unit indicators, badges, and adornments (e.g. گرم, تومان, تومان / گرم, ٪) MUST be visually positioned on the **LEFT** side of the input field container.
- **In Row layouts (Compose RTL)**:
  - The BasicTextField (with Modifier.weight(1f)) MUST be the **first child** (anchored to the Start / Right side).
  - The adornment Text or badge MUST be the **second child** (anchored to the End / Left side).
- **In OutlinedTextField (GoldInputField)**:
  - The adornment MUST be passed to 	railingIcon (which maps to End / visual Left in RTL).

### Rule 2: Numbers Always Typed Left-to-Right (LTR)
- For all numeric inputs (weight, price, wage, profit, tax, karat, discount):
  `kotlin
  textStyle = TextStyle(
      fontFamily = VazirmatnFamily,
      fontFeatureSettings = VazirmatnFeatureSettings, // ss01 for Persian digits
      textDirection = TextDirection.Ltr,
      ...
  )
  `
- 	extDirection = TextDirection.Ltr prevents the Android text engine from flipping decimal points (e.g. ensuring 12.5 renders as ۱۲.۵ instead of .۱۲.۵).

### Rule 3: Typography & Persian Digits
- All input fields must explicitly set ontFamily = VazirmatnFamily and ontFeatureSettings = VazirmatnFeatureSettings (which enables OpenType ss01).
- ASCII digits typed into inputs are automatically rendered as authentic Persian numerals while preserving LTR keyboard flow.
