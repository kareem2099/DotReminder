# Authentication and release review — 2026-10-31

## Authentication changes

Email, Google and Facebook Firebase network tasks, password resets, email
verification, reauthentication and TOTP tasks now have bounded network waits.
Native account pickers remain user-controlled. A timeout becomes a retryable
network error; cancellation by the caller keeps its cancellation semantics.
Server MFA/credential errors retain their type and relevant fields.

Session metadata is recorded in an application-lifetime worker after real
authentication succeeds. A failed preference write cannot crash successful
login or hold up navigation. Writes are serialized and bound to the expected
UID; account switching during a write cannot save the previous session under
the incoming account. Registration records its first authenticated session too.
Remember-email read/write failures are handled, and the login password screen
uses the same screenshot protection as other credential surfaces.

Lock-policy observation now stays subscribed to account changes after a failed
preference read. An incoming signed-in account remains hidden until its policy
loads, and a storage failure keeps the retry message visible across foreground
transitions and fresh sign-in. This closes a failure case where signing out
after a read error could otherwise leave the next account's policy unobserved.

The user has confirmed registration SMS delivery, Google sign-in reaching the
enrolled TOTP challenge, and unlocking a saved session with the physical
fingerprint sensor after leaving and returning to the app. A biometric unlock
does not replace the enrolled server second factor during fresh sign-in.
Facebook's actual provider/account setup has not been exercised with a real
Facebook account in this review.

## Checks

- 301 JVM tests passed, including network timeout/cancellation, session-write
  failures/account changes, SMS resend/confirmation, MFA, alarm timing and
  recovery of lock-policy observation after preference failures.
- All 51 final Home/account/authentication UI tests passed after the reminder
  timing refinement (48 authentication cases and three Home display modes).
  The five app-lock UI cases were repeated successfully after the lock-policy
  error-recovery correction.
- Twelve Auth/Firestore emulator tests passed for owner-only session
  create/read/list/update/delete and denied cross-account/signed-out access.
- Debug APK, isolated test APK and signed, minified Release AAB built
  successfully. Debug and Release lint both completed with zero errors
  (129 Debug warnings / 126 Release warnings).
- The release archive retains the custom alarm tone, both alarm receivers and
  notification/exact-alarm permissions. Java `JarFile` cryptographic validation
  verified all 1,712 non-signature entries and found no duplicate entry names.
  `jarsigner` also reported `jar verified`; it emitted self-signed/no-timestamp
  and ZIP ordering warnings for streaming `JarInputStream` reading. These
  local checks do not replace Play's own internal-track acceptance checks.
- Three device alarm tests passed, with OS-confirmed zero-width scheduling and
  measured notification delays of 57 ms / 82 ms. See `reminder-delivery.md`.
- Candidate GitHub files were scanned for common private-key/API-token patterns
  without printing values; none matched. This is a limited pattern scan, not a
  guarantee that every possible secret format is absent.

The local Firestore file lacked a `/users/{uid}/sessions/{sessionId}` rule. The
owner-only rule was added and tested on demo-project emulators. **It has not
been deployed to production in this review**, and the production rules were
not read back. Deployment must use the intended Firebase account/project.

Re-run the emulator review with:

```sh
firebase emulators:exec --only auth,firestore --project demo-dotreminder-auth --config firebase.auth-review.json 'python3 tools/test_auth_session_rules.py'
```

## GitHub and Play release limits

The Gradle wrapper JAR is no longer ignored, and the tracked machine-specific
JDK path was removed. Keep the wrapper JAR in the source change. Use JDK 21 and
the Android SDK required by the Gradle configuration; provide local Firebase,
API and signing configuration separately. Local properties, keystores, APK/AAB
artifacts and emulator debug logs remain excluded from the candidate source.
That October review did not commit, push, deploy rules or upload to Play. November publication results are recorded below.

## Public app name

On 2026-10-31, the Firebase public-facing name was changed from
`project-12141681876` to `DotReminder`. Firebase displayed the saved value, and
Google Auth Platform's Branding page independently showed the persisted app
name `DotReminder` with no unsaved changes. This updates service branding;
the Firebase project ID, project number, OAuth clients and signing certificates
were not changed.

The user subsequently saved the Branding form with the selected app logo and
DotSuite links: home `https://dotsuite.dev/en`, privacy
`https://dotsuite.dev/en/privacy`, and terms `https://dotsuite.dev/en/terms`.
The existing Firebase domain was retained and `dotsuite.dev` was added to the
branding authorized-domain list. The UI confirmed "Branding changes saved".
The developer contact saved by the user is `support@dotuite.dev`, which differs
from the website domain by a missing `s`; this discrepancy was reported rather
than overwriting the user's concurrent edit.

The live linked legal pages were readable, but their current content is for
DotSuite generally, DotScramble and related developer services. It does not yet
describe DotReminder's Firebase/SMS, location, ads and AI data handling. These
links do not by themselves clear the app-specific privacy-policy requirement.

On 2026-11-04, the source gained a public DotSuite privacy/deletion-request page in
English, Arabic, German, French and Russian, an in-app request explanation/link,
and privacy links on login/registration. Public support is now
`support@dotsuite.dev`, including the website contact-form recipient. Requests are
manual email requests, not automatic Firebase deletion; see `account-deletion.md`.
The user clarified that publication must use GitHub. The site was subsequently
pushed and the production deployment verified, as recorded below.

The Google Branding page was rechecked on 2026-11-04. Developer contact is already
`support@dotsuite.dev`. Its separate **User support email** selector offers only
the existing Google account email and reports no managed Google Groups. No edit was made to
that constrained selector or to project access/group membership.

The release review includes these operational and distribution checks:

1. The privacy/deletion-request page is published and publicly verified.
   The operator must monitor the support inbox and fulfill verified deletion requests.
   The source now exposes an in-app pathway; it is not an automatic deletion backend.
2. Validate the new in-app privacy links against the published page and complete
   the Play privacy/Data safety configuration from actual collection practices.
3. The 0.0.7 source removes the mobile Gemini key field and direct SDK and uses
   the authenticated DotSuite gateway. Production rejects unauthenticated requests,
   the user confirmed a real signed-in reply, and the original key is absent from
   the final AAB. Play accepted that artifact.
   See `protected-ai-gateway.md` for the implemented boundary and tests.
4. Run Google sign-in and SMS through a Play internal-test installation using
   the actual Play App Signing certificate. Debug testing does not validate
   that certificate or every provider's production app-review configuration.
5. Check the latest Play version code before selecting the release version.
   The 0.0.7 source uses version code 8; Play Console currently lists code 6 /
   version 0.0.5 as the newest uploaded bundle before the new upload.

Sources:
- https://support.google.com/googleplay/android-developer/answer/13327111
- https://support.google.com/googleplay/android-developer/answer/10144311
- https://ai.google.dev/gemini-api/docs/api-key
- https://firebase.google.com/docs/auth/android/google-signin

## GitHub publishing and release metadata — 2026-11-04

The user clarified that website deployment must be triggered by pushing GitHub,
not by invoking Vercel deploy. DotSuite privacy/support changes were pushed to
`master` in commit `b34a70e`; the protected AI gateway and updated policy were
pushed in `4c16990`. Other pending website edits were excluded. The production
server secret was configured separately before the second push, with no value
printed or included in source. The runtime compatibility correction was pushed in `79f31f9`, pinning Firebase
Admin 13.10.0. GitHub reported successful deployment. Live missing/invalid-token
checks returned HTTP 401 with JSON and no-store caching. The user confirmed a
normal AI response in the newly installed app after biometric unlock.

Android source metadata is now version name **0.0.7**, code **8**, with
compile/target API **36 (Android 16)** and minimum API **29 (Android 10)**.
Settings displays both build version values. The Play Console overview showed
code 6 / 0.0.5 as the latest bundle, so code 8 is higher than the uploaded versions
observed during this review. README and CHANGELOG describe the actual current
features rather than planned/demo MFA backup-code behavior.

## Signed artifact and Play preparation — 2026-11-04

The signed `DotReminder-0.0.7-8.aab` passed bundletool validation and JAR signature
verification. Its actual manifest reports version 0.0.7 / code 8, minimum API 29
and target API 36. All four 64-bit native libraries have load-segment alignment
of at least 16 KiB. A private byte comparison found no original Gemini key.

SHA-256: `663bb50b4c2a438498ceff590c4eabc3de740e47b24e239685e1bbdfe76e27f8`.

The bundle was uploaded to the new production release, with Arabic and English
release notes. Play displayed target SDK 36 and no blocking validation errors.
It reported one advisory about missing debug symbols for prebuilt native code.
Saving this release prepares Publishing overview; it does not by itself publish
the new version or clear the currently active API-target policy warning.

Final release Lint completed with **zero errors and 130 warnings**. A fresh
`JarFile` verification read all **1,705** payload entries with cryptographic
signature checking and found no unsigned payloads or duplicate entry names.
Play saved production release 8 (0.0.7) in Publishing overview as one change
not yet submitted for review; managed publishing is on. Google quick checks
were running after the save. No production rollout was started.

The Play privacy-policy setting was updated from the old GitHub HTML-source
link to `https://dotsuite.dev/en/privacy#dotreminder`. Publishing overview
confirmed this as a second saved change awaiting review alongside release 8.

The final `lintRelease`, `assembleDebug`, `assembleDebugAndroidTest` and
`testDebugUnitTest` invocation completed successfully. All **309 JVM tests**
passed with no failures or skipped cases. The direct Gemini dependency removal
exposed Natty’s missing SLF4J 1.7 binding during R8; a matching no-op runtime
binding fixes the missing class without suppressing R8 errors or logging reminder
text. The signed AAB was built after that dependency correction.

## Play Data safety update — 2026-11-04

The existing declaration omitted SDK device identifiers, app interactions,
diagnostics, saved text and AI chat. The updated form includes 12 data types:
name, email, user IDs, optional address/phone, approximate/precise location,
other in-app messages, diagnostics, app interactions, other user-generated
content and device/other IDs. It reflects AdMob/Firebase Analytics collection
and sharing, optional AI chat and location features, stored reminder text,
and account/session management. No ephemeral-retention claim was made for
Gemini prompts or SDK data. Account fields no longer claim unsupported
Analytics use; account/security purposes reflect the authentication code.

The form preview showed the new deletion links:

- Account and associated data: `https://dotsuite.dev/en/privacy#delete-dotreminder`
- Specific data requests: `https://dotsuite.dev/en/privacy#dotreminder-data`

The final form Save reported “Change saved. Send for review in Publishing
overview.” This is a saved declaration change, not a completed Google review
or publication. The operator remains responsible for actual support/deletion
fulfillment and any provider-side collection settings outside the source review.

SDK disclosure references:
- https://developers.google.com/admob/android/privacy/play-data-disclosure
- https://support.google.com/analytics/answer/11582702
- https://firebase.google.com/docs/android/play-data-disclosure
- https://support.google.com/googleplay/android-developer/answer/10787469

## Review request submitted — 2026-11-04

After inspecting the final list, the three changes (production 8 / 0.0.7,
privacy-policy URL and Data safety) were sent for Google review. Publishing
overview visibly moved them to **Changes in review**. Quick checks were still
running and the page explained that dispatch completes after those checks
succeed. **Managed publishing remains on**; this submission does not publish
the new version automatically. Unrelated pre-existing ready-to-publish changes
were not published. Android source was pushed to `main` in `a6f6040`.
