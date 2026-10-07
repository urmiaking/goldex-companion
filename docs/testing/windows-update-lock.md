# Windows 0.56.44 lock handoff verification

The initial 0.56.43 release job failed after a native launcher was stopped while its JVM still held workspace.lock. That tag remains immutable and unpublished. Before-merge installer CI had passed, confirming the failure was timing-dependent.

The disposable fixture now stops only the matching Qirato descendants of its explicitly launched process, then its launcher. The shipped helper waits up to 30 seconds for the exclusive lock after process exit; it postpones without installation if another process keeps the store open. It does not kill user processes or read/delete financial data.

WindowsUpdateHelperTest uses two isolated PowerShell processes and a temporary sentinel lock file. A held lock causes a bounded timeout without changed contents; a waiter acquires only after the other process releases it. Focused helper tests passed (4 tests, zero failures). Both shipped/fixture scripts passed PowerShell parser checks.

Full local Android compilation and 237 Android unit tests passed, along with 84 desktop tests and native image creation. The packaged Qirato.exe --verify-runtime reported version 0.56.44 and verified fonts, icon, shared codecs and financial runtime behavior. Disposable CI installation/upgrade and publication remain the release gates. No local APK assembly/signing, installed customer data changes, or published artifact re-download occurred.
