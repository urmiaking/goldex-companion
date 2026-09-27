# Cloud identity and writer ownership
Status: implemented behind a server release gate.

## Context
Financial data must survive network loss and device replacement. Store contact details are not an authentication identity. A device fingerprint is not a credential.

## Decision
Canonical Iranian mobile + single-use OTP owns one workspace. OTP expires after 120 seconds, resend cooldown is 60 seconds, five verification attempts; phone/IP hourly limits apply. A P-256 key in Android Keystore proves device registration, refresh and transfer. Registration also presents a signed license token bound to the fingerprint; expired users may authenticate, but every business endpoint independently checks live SQL license/trial status. Cloud HS256 audience/key is separate from admin and license signing, with 15-minute access and rotating 30-day refresh sessions. Refresh replay with a valid device proof revokes device sessions. Encrypted refresh material lives in noBackupFilesDir; opt-in preferences are excluded from Android cloud backup and device transfer.

One active writer is fenced by writerEpoch. Normal release drains the local queue and freezes further local writes before signing the final server revision/target. Emergency takeover requires OTP verified within five minutes and explicit loss warning. A replaced device key revokes sessions and increments writer epoch. Known displaced devices block financial writes and require restore or explicit local detach; fully offline old devices cannot know a transfer occurred, but their old epoch is rejected by the server.

## Consequences
No phone number, OTP, token or financial payload enters telemetry. License expiry never deletes data or opt-in. Production SMS and protected secrets are release prerequisites; fake SMS exists only through dependency injection in isolated tests. Switching accounts requires backup and explicit detach, with active records retained locally and old queue archived in the backup.

## Revisit
Multiple writers, key recovery beyond OTP, or multi-workspace accounts require a new protocol and conflict policy.
