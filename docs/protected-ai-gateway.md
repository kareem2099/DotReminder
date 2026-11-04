# Protected AI integration — 0.0.7

The Android app now calls `https://dotsuite.dev/api/dotreminder/ai` over HTTPS.
It does not read `GEMINI_API_KEY`, generate that BuildConfig field, or depend on the direct Gemini client SDK. The server has its own sensitive `DOTREMINDER_GEMINI_API_KEY` environment setting. Its value was transferred privately using stdin; it was not printed, copied into the website source, or committed.

## Request boundary

The app obtains the currently signed-in user's Firebase ID token and binds the result to the initiating UID. A 401 triggers one forced token refresh. Sign-out/account changes discard the old request result. Cancellation stops the OkHttp call. Requests have bounded connection/read/call timeouts, no redirect following, and bounded response size. The application does not supply a model/provider URL or key.

The backend verifies Firebase ID tokens for `remindme-56f27` using Firebase Admin's public-key token verifier, rejects anonymous accounts, and refuses emulator mode. No service-account key or user-management permission was added. Token verification does not independently check revocation; existing tokens may remain usable until expiry. Account deletion must account for that interval before its final purge.

Backend requests accept only `chat`/`extract`, bounded text, a timestamp and a recognized device timezone. They are limited to 128 KiB; model output and extraction structure are validated. Atomic PostgreSQL limits cover account windows, daily account usage and hashed network-address windows. Storage/verification/limit failures deny model access. API responses are not cached and contain generic errors. The server does not log user conversations, Firebase tokens or provider credentials.

These controls protect the provider key and bound authenticated usage. Firebase App Check/Play Integrity attestation is not claimed to be enforced.

## Data deletion and policy

The five-language DotSuite policy discloses backend token validation, identifiers and usage counters. Verified account deletion also removes the associated `dotreminder-ai:user:UID` records in DotSuite's `ratelimits` document collection, covering the `ai-window` and `ai-day` actions. Conversation text is not stored in this backend. Provider retention and user-controlled local copies remain described in the public policy.

## Verification

- 309 JVM tests passed, including eight new account-boundary/token-refresh/cancellation/limit cases.
- 19 Node backend tests passed, including the real SDK rejecting malformed/unsigned tokens, invalid request shapes, unbounded bodies, model failures, quota failures and accidental emulator configuration.
- The updated DotSuite production build passed locally.
- A real provider connectivity check used only the public test instruction “Reply only with OK”; it received HTTP 200. No user content or Firebase token was used by that check.
- A private byte comparison against the original key found no occurrence in the new debug APK or signed version-code-8 AAB.
- Production deployment was triggered by GitHub commits, ending with `79f31f9` (Firebase Admin 13.10.0 compatibility pin). Live missing/invalid-token probes returned HTTP 401 JSON with `no-store`.
- The updated debug app was installed on the connected review device. The user opened it with their own fingerprint and confirmed a normal AI reply from their signed-in account. No Firebase token was extracted for testing.
- Play accepted the signed version-code-8 AAB and displayed target SDK 36. The release preview had no blocking errors and one advisory about native debug symbols. Submission status is recorded in the release report.

The production model is controlled by the server, so a provider model upgrade does not require embedding a new key or shipping another Android app. The current default retains `gemini-2.5-flash`; the hosting operator should follow [Google's model lifecycle](https://ai.google.dev/gemini-api/docs/deprecations) before the announced December 1, 2026 routing change.

Existing pre-migration local binaries may contain the old key and must not be used as the new release artifact. Any previously distributed key-bearing binary also warrants rotating that provider key; moving a key to a server does not revoke old copies.
