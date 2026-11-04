# DotReminder account and associated data deletion

The app exposes **Account security → Privacy and your data → Request account and data deletion**.
The explanation opens `https://dotsuite.dev/en/privacy#delete-dotreminder`; Arabic, German, French and Russian use `/ar/…`, `/de/…`, `/fr/…` and `/ru/…`.
Login and registration also expose the privacy policy. No account information is sent in those URLs.
The website page is fully localized in its five supported languages. Native action labels follow
the app's existing English UI; the link selects the matching website language from the device locale.
This feature does not declare a partially translated locale for the rest of the Android app.

## User request, not immediate deletion

The public page is accessible without signing into DotSuite or reinstalling DotReminder.
It prepares a mail draft to the user-selected DotSuite support address `support@dotsuite.dev`.
The user must send it. Neither opening the page nor opening the mail draft submits a request.
There is no automatic deletion job, no public Firebase Admin endpoint, and no claim of instant deletion.
The operator must monitor that inbox and fulfill verified requests within **30 days after ownership verification**.
That is an operating commitment, not a fixed deadline imposed by Google Play.

## Fulfillment procedure — authorized operator only

Do not test this procedure against a real user's account. It is destructive.

1. Acknowledge the email, identify the Firebase Auth UID in project `remindme-56f27`, and verify account ownership using an authenticated channel. An unverified email address or a supplied UID alone is not proof. Never ask for passwords, SMS/TOTP codes, biometric data or an identity-document attachment. Provide support for a user who lost access to their inbox.
2. Confirm the permanent scope with the requester. The request covers DotReminder only, not their separate Google, Facebook or DotSuite account.
3. Disable the verified Firebase Auth account and revoke refresh tokens using an authorized administrator session. Previously issued ID tokens may remain valid for up to one hour; allow them to expire before the final purge. Do not treat deleting the Auth user as cascading deletion of Firestore data.
4. In the default Firestore database, delete every document where `userId == UID` in **reminders**, **categories**, and **reminder_templates**. Paginate through all matches; do not assume one batch covers all records. Delete any nested subcollections of those documents too.
5. Recursively delete **users/UID**, including **users/UID/sessions/** and any other child collections. Deleting a parent document alone does not delete its children. Verify the three owned collection queries and the profile/subcollections are empty after outstanding credentials have expired; account/session workers must not repopulate them.
6. Delete the disabled Firebase Authentication user (including enrolled authentication factors). Confirm absence in Auth and repeat the Firestore checks. On a partial failure, keep the account disabled, record the incomplete step, retry and verify; do not send a completion notice prematurely. Do not delete the Auth account first and assume remaining records are gone.
7. Remove DotSuite backend AI usage counters in the `ratelimits` document collection where `identifier` equals `dotreminder-ai:user:UID` (actions `ai-window` and `ai-day`). These are associated account data even though they are outside Firestore. Do not delete unrelated DotSuite accounts or shared network counters. Review other service-provider data that can be identified with this account and request deletion through the supported provider mechanisms. Do not claim automatic erasure of Gemini/provider security logs, anonymous aggregate analytics or user-controlled device backups. Explain any legally required retention by category, reason and applicable period to the requester.
8. Tell the user to clear app storage or uninstall on **each device** to remove local drafts, preferences, remembered email and pending alarms. Cloud deletion cannot remotely wipe offline devices or exported files. Existing local alarms can run while a request is pending. Confirm the cloud account/data deletion by email only after the checks succeed.

Keep support correspondence only as necessary to process the request and applicable obligations, and honor requests about the correspondence itself. Never put secrets or full user reminder content in routine operational logs.

## Publication checks

- Build/deploy the modified **DotSuite** checkout before relying on the production URL.
- Check the page anonymously in all five website languages; the form must make clear it opens an email draft.
- Confirm the support inbox is monitored and the operator can execute the fulfillment procedure.
- Use `/en/privacy` as the privacy URL and `/en/privacy#delete-dotreminder` as the external deletion URL in Play Console only after deployment verification.
- Complete Play Data safety from actual collection/use, including Firebase Analytics, AdMob, optional location and Gemini; a privacy page alone does not complete that form.

Sources: [Play account deletion](https://support.google.com/googleplay/android-developer/answer/13327111?hl=en),
[Firebase token revocation](https://firebase.google.com/docs/auth/admin/manage-sessions),
[Firestore deletion](https://firebase.google.com/docs/firestore/manage-data/delete-data).

## Validation on 2026-11-04

- DotSuite production build passed after full English/Arabic/German/French/Russian translations. Browser checks verified localized titles, policy sections, request labels and `support@dotsuite.dev` links in all five routes. The German heading overflow at 390 px was corrected and rechecked without horizontal overflow.
- A Russian request draft was exercised with `review@example.com`; the preview appeared and no email was sent. Five mocked support-handler scenarios checked the support recipient, successful delivery, mail failure, rate limiting, invalid input and persistence failure across those scenarios. The real support mailbox's delivery was not tested.
- Debug APK/test APK compiled and were installed with `-r` on the connected review device; 12 isolated privacy/security UI tests passed. All 301 unit tests passed. Final standalone `lintDebug` passed with 0 errors and 132 warnings. The genuine MainActivity was reopened after the isolated runner completed.
- The Android app continues to use its existing English UI labels. A trial of partially translated Android resources surfaced missing translations in unrelated screens; those partial resource additions were removed rather than disabling translation checks. The five-language website remains fully translated.
- The first isolated UI run encountered a queued boot-restoration service while using the fake test Application. Opening the real app completed that job; the repeated UI suites passed. No user credentials, account data, enrolled factors or actual deletion requests were changed by these tests.
- An isolated website release snapshot was prepared from DotSuite commit `8ac03ce` with only 20 privacy/support files. For the shared message catalogs, only the three new Footer labels were merged into the release snapshot; unrelated pending website changes and environment files were excluded.
- The user subsequently clarified that publication must use GitHub. Privacy/support changes were pushed in DotSuite commit `b34a70e`; the AI policy update and gateway followed in `4c16990`. GitHub deployment completed successfully. The public German and French policy routes were checked in the browser with the five-language switcher, deletion instructions and `support@dotsuite.dev`. No Vercel deploy command was used for this publication.
- Google Branding already shows developer contact `support@dotsuite.dev`. Its separate user-support-email selector currently offers only the existing Gmail address and no managed groups; it was left unchanged, and no account/project access was expanded.
