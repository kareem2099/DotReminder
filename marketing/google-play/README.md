# DotReminder Google Play artwork

The listing text and upload-ready PNGs are in `fastlane/metadata/android/ar`
and `fastlane/metadata/android/en-US`. Each language has six phone screenshots,
four responsive tablet screenshots and a localized feature graphic. The tablet
set is supplied in both the 7-inch and 10-inch store slots.

The PNGs use the app's production Compose content with local demonstration
data. `StoreListingScreenshotTest` runs only with `LoginUiReviewRunner`; it
does not authenticate, call AI/ad services, or save reminders. Ten capture
cases rendered successfully. The account name, email, reminders and AI replies
shown in these captures are examples. Arabic captions do not imply an Arabic
translation of the app UI; the Arabic listing explicitly explains that the
current app interface is English.

## Rebuild the artwork

1. Build the isolated capture APKs:

   ```sh
   ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest \
     -PuiReviewRunner=com.FreeRave.remindme.screens.auth.LoginUiReviewRunner
   ```

2. Install the debug and test APKs on a review device, then run only the capture
   class with the isolated runner:

   ```sh
   adb shell am instrument -w -r \
     -e class com.FreeRave.remindme.review.StoreListingScreenshotTest \
     com.FreeRave.remindme.test/com.FreeRave.remindme.screens.auth.LoginUiReviewRunner
   ```

3. Copy the `cache/store-phone-*.png` and `cache/store-tablet-*.png` files from
   the debug app's sandbox into `screens/`, using `adb exec-out run-as` for the
   specific generated filenames. Reopen the regular app after the capture run.
   Never replace these fixtures with private account screenshots.
4. Update `captions.json`, then run:

   ```sh
   python3 marketing/google-play/export.py
   python3 marketing/google-play/render.py
   ```

The exporter requires Cairo, PyGObject with Rsvg 2, and the Noto Sans / Noto Sans
Arabic fonts. It builds vector layouts around the original captures and renders
opaque RGB PNGs directly, avoiding browser full-page capture artifacts. The
gallery generator copies only public artwork into `preview/`; serve that
directory, never the repository root. `vectors/` and `preview/` are generated
and ignored. `contact-sheet.png` provides a quick review of both phone sets.

## Validation and listing scope

- Phone/tablet posters: **1080 × 1920**, RGB PNG, each below 8 MB.
- Feature graphics: **1024 × 500**, RGB PNG.
- App name: **DotReminder** in both languages; title ≤30 characters, short
  description ≤80 and full description ≤4000.
- Text describes the implemented reminders, calendar, templates, optional AI,
  authentication and biometric app lock. Unsupported voice-input and
  habit-learning claims from the previous listing were removed.
- No app binary, signing configuration, version number or release tag changes
  are needed for this store-metadata update.

The layouts were checked against [Google Play's preview asset guidance](https://support.google.com/googleplay/android-developer/answer/9866151).

## Console status — 2026-11-05

Both localized listings were saved, including all six phone posters, four
screenshots in each tablet slot and the feature graphic per language. Google
Play recorded **13 store-listing changes** under **Changes in review** after
submission. Automated quick checks were still running at the final check; the
Console will forward the changes after those checks succeed. This is not a
claim of approval or public availability. Managed publishing remains enabled;
unrelated changes already ready to publish were left untouched.
