# Two-step verification

DotReminder uses Firebase Authentication's TOTP MFA. A valid first factor alone
does not complete a sign-in for an enrolled user. The Firebase resolver must
accept a current authenticator code before the app publishes login success or
records a new device session.

## Server prerequisite

The project is `remindme-56f27`. During implementation its Firebase console was
observed upgraded to Authentication with Identity Platform. The existing project
is on Blaze. The upgrade review states that the change is irreversible and that
Authentication usage above the free MAU tier is billable. The Android client
never performs project or billing upgrades.

Server-side TOTP was enabled and read back successfully on 2026-10-30 through
an authorized Cloud Shell session. The returned configuration had
`totpProviderConfig.adjacentIntervals = 1` and `state = ENABLED`; existing
`PHONE_SMS` and the project-wide MFA state were preserved. No user factors were
created or removed during this configuration change.

For future inspection or setup, an authorized administrator can run the following in Google
Cloud Shell with this script available:

```sh
python3 tools/firebase_totp_config.py --project remindme-56f27
python3 tools/firebase_totp_config.py --project remindme-56f27 --enable
```

The first command only reads the project configuration. The second enables the
TOTP provider, preserves existing non-TOTP providers, allows one adjacent
30-second interval on each side, and reads the configuration back. It never
enrolls a user, resets their factors or prints credentials.

Official references:
- https://firebase.google.com/docs/auth/android/totp-mfa
- https://firebase.google.com/docs/reference/android/com/google/firebase/auth/TotpSecret

## User flows

- Account security loads the actual enrolled factors from Firebase.
- Setup requires a verified email and a supported primary provider: password,
  Google or Facebook. The user reauthenticates before setup. Existing MFA is
  also verified before adding a backup or removing a factor.
- A server-generated setup key and QR code remain in memory. The user must
  enter an accepted current authenticator code to finish enrollment. Secrets,
  passwords, OTPs and pending resolvers are not saved in preferences or saved
  instance state. Credential/setup routes prevent screen capture. Explicitly
  copied setup keys are marked sensitive and cleared when the setup closes if
  the clipboard still contains that key.
- Email, Google and Facebook sign-in all preserve the Firebase MFA exception
  and display the factor challenge. Cancelling or entering invalid codes cannot
  publish login success. Unsupported factor types do not bypass verification.
- Password changes that require MFA complete only after the server accepts the
  second factor. Removing the last authenticator explains that protection ends.
  If Firebase expires a session during removal, the app requests sign-in again
  rather than claiming that the account is still protected.
- Old local demo preferences and setup secrets are removed when account
  security initializes; they never determine the actual protection status.

## Recovery

This implementation does not provide the old demo's recovery-code feature. No locally
generated recovery code is accepted by this implementation. Users can enroll
an additional authenticator on a separate backup device and select it during
sign-in. Losing every authenticator requires an administrator-run identity
recovery process. An administrator must establish identity through a trusted
procedure before removing a factor. Password reset is not an MFA bypass.
Do not implement a client-side recovery endpoint that accepts just a user ID,
email address, local flag or password as authorization to remove MFA.

## Validation

Before production rollout, use a dedicated test account after the server
prerequisite is enabled. Verify enrollment, an incorrect six-digit code,
sign-out/sign-in, backup enrollment and selection, password change, factor
removal, last-factor removal, and expired/offline requests. Also verify provider
reauthentication against the exact Google/Facebook account already signed in.
Fake component and controller tests do not replace this live server check.
Do not enroll or remove factors on the developer's personal account as an
automated test, and do not store authenticator setup secrets in logs or test
artifacts from live accounts.

The final local review passed 245 unit tests (21 MFA controller/rule tests) and
39 on-device account/authentication UI tests, including nine MFA component
cases. The debug APK and test APK built successfully and lintDebug passed with
no errors. Live enrollment/sign-in with a real authenticator remains a separate
user-driven acceptance check; automated reviews did not modify any real user's
factors.
