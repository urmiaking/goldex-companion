# Windows customer management verification

The Windows customer feature preserves Android customer workflows and financial policies from `7d94001`, with native workspace headers, responsive LuxuryCard directory cards, desktop summaries and existing theme/fonts/motion. See [ADR 0022](../adr/0022-windows-customer-ledger-parity.md).

## Local checks

```powershell
$env:ANDROID_HOME = 'C:/Users/mskho/AppData/Local/Android/Sdk'
.\gradlew.bat compileDebugKotlin --no-build-cache --no-daemon -q
.\gradlew.bat testDebugUnitTest --no-daemon -q
.\gradlew.bat :desktop:test --no-daemon -q
.\gradlew.bat :desktop:createDistributable --no-daemon -q
$verification = Start-Process -FilePath '.\desktop\build\compose\binaries\main\app\Qirato\Qirato.exe' -ArgumentList '--verify-runtime' -WindowStyle Hidden -Wait -PassThru
if ($verification.ExitCode -ne 0) { throw 'Packaged runtime verification failed' }
```

Use your configured Android SDK path. Do not assemble/sign an APK locally. Customer-specific tests can be run with `:desktop:test --tests '*DesktopCustomer*'`. Compose images are generated under desktop/build/screenshots/customers-*.png using synthetic temporary records, never the production workspace.

## Review workflows

- Open مدیریت مشتریان (or Ctrl+7), create a Persian customer, select the retail/market role and enter phone/national ID/note. Search, clear and switch debtor/creditor/settled filters. Check the account totals independently of the selected filter.
- Open the statement and verify customer identity/back/registration actions in the main workspace header. Verify both gold and toman balances and the unfiltered running balance for every dated receipt. Switch statement filters. There is no local page header, floating registration, call or share control.
- Register an independent receipt using آبشده, مصنوعات, سکه or شمش; verify Persian decimal entry and the stored 750-equivalent weight. Exercise transfer, cheque, POS and cash modes. Edit a receipt and verify its old effect is reversed before the new effect. Confirm/cancel deletion and verify reversal happens once.
- For a nonzero account, open settlement. Exercise gold/toman target, gold/cash physical payment, manual/live rate, remaining-amount shortcut and opposing-balance offset. Confirm the shared preview agrees with the committed balance. A rate with offline/unknown freshness must not be presented as live. Allocated saved-invoice payments retain their agreed rate and receipt link.
- Check light/dark mode at 760/1100/1440px widths: the directory adapts to one/two/three columns. On wide content, search and filters occupy one equal-height row. Long names truncate in one line, financial digits fit without truncation, and statement/registration/edit/delete actions stay reachable. The invoice action is disabled. Cancel is on the right and unsettled-contact deletion is disabled. Navigate away/back with a draft and attempt exit/update while saving; drafts remain available and an in-flight write cannot be interrupted by restart.
- Export/reopen a temporary workspace and verify IDs, customer/receipt data, settlement metadata and unknown fields. A corrupted financial collection blocks opening without rewriting the file. Deleting a settled contact retains its archive and ledger history; a nonzero balance prevents deletion with a visible message. Concurrent contact editing and financial writes must retain both the metadata change and latest balance.

## Delivery evidence

The required release is Windows-only while changes remain under desktop plus documentation. Check CI job status and release/asset metadata. Do not re-download the published installer/APK for verification. Full Windows invoice issuance and cloud sync remain separate features; the illustrative invoice list is not a customer account source.
