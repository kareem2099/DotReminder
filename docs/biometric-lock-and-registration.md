# Biometric app lock and phone registration

## App lock

`AppLockController` keeps foreground authorization in memory. A saved protected
Firebase session starts locked, remains locked when a biometric attempt is
cancelled or fails, and locks again on `ON_STOP`. Attempts are bound to the
current Firebase UID and a foreground generation. A late callback cannot
authorize a different account or a backgrounded session.

The root gate loads its policy before composing account screens. While locked,
the protected navigation subtree is removed, including its native dialogs and
accessibility nodes. The NavController, entry ViewModels and saveable state
holder preserve pending work and ordinary drafts. Account-security controllers
stay in memory when hidden by the gate, so returning from Google/Facebook
reauthentication does not silently cancel an authorized MFA flow. Sensitive
window flags are reference counted across nested security surfaces.

Enabling protection requires a successful system `BIOMETRIC_STRONG` prompt.
The preference is stored with its account UID. Old enabled preferences without
an owner remain protected rather than silently becoming disabled. Policy read
failures keep the UI closed. A cancelled setup does not disable existing
protection. App-open ads are suppressed on the lock screen and signed-out
authentication screens.

The explicit "Sign in again" fallback ends the saved Firebase session and
opens normal sign-in. A new successful Firebase sign-in, including any required
MFA, is valid authorization for the new foreground session. Biometrics alone
never create a new Firebase identity. No account password is stored for
biometric sign-in.

## Registration SMS

`SignUpController` manages details, code delivery, phone confirmation, account
creation and profile saving as separate states. ViewModel methods delegate to
this controller; the Firebase gateway implements the network boundary.

Phone verification uses a secondary FirebaseApp/Auth instance with the same
Firebase project options. Sending or verifying a registration SMS cannot
replace the main app's signed-in user. The main instance signs in only after
the verified phone account has its email/password linked. Existing accounts
with email or federated providers are directed to normal sign-in instead of
being changed by the registration form. Linking retries are accepted only for
the account/email being created by that registration attempt.

- `onCodeSent` moves to manual code entry and keeps listening for subsequent
  automatic verification. It does not mark the phone or account verified.
- Automatic verification confirms the phone but still requires the explicit
  "Create account" action.
- Resending has a monotonic 60-second minimum cooldown, retains the SDK resend
  token for its recipient, and replaces the verification ID only after a new
  code is sent. Editing details does not reset the cooldown.
- Failed resends leave the previous code available for server verification.
  Late callbacks after cancellation or a replacement request are ignored.
- An unfinished Activity-bound request can reattach its callbacks on return
  within its active retrieval window. This does not use a force-resend token.
  No automatic restart is attempted after that window or after code delivery.
- Automatic-retrieval timeout is not reported as SMS code expiration. Invalid
  codes, expired sessions, network failures, rate limits, quotas and app
  configuration errors have distinct safe messages. SDK error text, OTPs and
  setup credentials are not logged.
- Arabic/Persian code digits are normalized before server verification.
  Locally valid six-digit input is never proof of phone ownership.
- Request cancellation is explicit, and profile saving has a bounded timeout
  and a retry that does not create a second account. Passwords are cleared
  from UI state after account creation and on leaving the form.

## Firebase app verification

The Firebase configuration read on 2026-10-30 confirmed that phone sign-in was
enabled. The Android app was already registered with this debug certificate's
SHA-1, but lacked its SHA-256. Its SHA-256 was added to the existing Android app
and verified by reading the certificate list back; every previously registered
certificate was preserved. Release signing SHA-1/SHA-256 were already present.

Debug certificate SHA-256:
`1D:27:FD:36:4E:02:72:A4:9F:50:7A:61:B1:A3:8B:11:4D:62:2E:02:F8:11:47:75:7D:91:4E:DD:FA:37:68:93`

No SMS was sent by the configuration audit. Delivery still depends on Firebase
app verification, configured region policy, quota and the recipient network.
Production must keep app verification enabled; test-only bypass settings and
hardcoded test numbers are not used in the application.

References:
- https://developer.android.com/reference/androidx/biometric/BiometricPrompt
- https://firebase.google.com/docs/auth/android/phone-auth

## Acceptance review

Initial validation on 2026-10-31: debug APK and isolated test APK built
successfully; all 278 unit tests and all 48 account/authentication device UI
tests passed. The subsequent full review passed 301 unit tests, 51 Home/auth UI
cases, three alarm-device tests and 12 emulator-rule tests; the signed Release
AAB and both Debug/Release lint passed. See `auth-release-review.md` for the
remaining public-release requirements.
The device review includes the registration code form with the real keyboard,
large text and RTL layout, using the same edge-to-edge and IME resize policy as
MainActivity. Android lint completed with zero errors (128 warnings).

Controller tests cover lock/cancel/background/account-change cases, complete
sign-in fallback, resend throttling, delayed automatic verification, invalid
codes, recipient mismatch and idempotent profile retries. Fake device tests
also exercise removal of native dialogs while locked, preservation of
navigation ViewModels and saveable drafts, and large text/RTL/keyboard layouts.

A real biometric acceptance check requires the device owner to touch the
sensor. A real SMS delivery check requires the user to enter their own number
and request a code in the registration UI. Component tests do not substitute
for these live checks and do not sign out the developer, create accounts or
send SMS automatically.

User acceptance on 2026-10-31: the user confirmed that the registration SMS
arrived and the flow worked. The reported authenticator challenge occurs
after a fresh Google sign-in, which requires the enrolled Firebase second
factor. This is separate from biometric unlocking of an existing session.
The user subsequently confirmed that returning from the background displayed
the biometric prompt and successfully opened the account after touching the
sensor.
