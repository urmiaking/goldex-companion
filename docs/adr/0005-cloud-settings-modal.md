# Cloud settings presentation and temporary login

## Context
The first cloud entry was placed above the More Hub and used an isolated dialog style. The owner requested the existing settings/modal visual language and a temporary 123456 server code until SMS is configured.

## Decision
Keep the entry in the existing settings card beside price, financial and lock options. Reuse the bottom-anchored TaxProfitModal geometry, gold border, LuxuryMotion, semantic colors and GoldInputField. CloudSettingsModal contains account settings; CloudSignInForm separates phone and code steps and respects server cooldown. Header cloud and wizard reuse the same feature owner. Read-only cloud data does not force a modal on every render; all mutation guards remain in the data layer.

Server responses explicitly select temporary or SMS mode. Only a temporary response displays its code. The client never accepts or verifies codes itself. Device proof and license enforcement remain server-owned.

## Consequences and revisit
Existing financial data, outbox and formulas do not change. Instrumented tests are available for manual checks of both themes, dismissal and phone/code navigation; they are not part of release CI. Configure SMS on the server when a provider becomes available; no app update is required to remove the temporary-code hint.
