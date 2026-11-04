# Reminder delivery review — 2026-10-31

## Confirmed failure and changes

The connected phone had `POST_NOTIFICATIONS` denied. The app declared the
permission but never requested it at runtime. Reminders could therefore be
saved and their alarms could run without any visible or audible notification.

Saving a reminder now checks notification access, blocked reminder channels
and precise-alarm access. The owner receives a contextual explanation and the
native permission/settings flow. Cancelling leaves the draft intact. Returning
from settings can resume the save after any required biometric app unlock;
validation runs again so a date that elapsed during permission setup is not
silently accepted.

Timed reminders use `AlarmManager.setAlarmClock` when exact-alarm access is
available. On this phone, ordinary allow-while-idle scheduling was represented
by the OS with a nonzero batching window. The alarm-clock registration was
verified in `dumpsys alarm` as `windowLength 0`, with no battery-saver delay.
Android can display the next-alarm indicator for these user-visible alarms.
If exact access is revoked, existing records retain an approximate fallback;
new saves request precise access again.

Device-private alarm records preserve the reminder owner, original repetition
anchor, next trigger, sound and snooze duration. They restore alarms without a
network request after boot, package replacement, clock changes and exact-access
grants. Starting the app reconciles existing reminders as well. Restoring an
account does not revive notifications belonging to a different signed-in user.
These device-owned records are excluded from cloud backup and device transfer.

Each delivered recurring alarm schedules the next occurrence, respecting its
end date without drifting the original interval. Snoozing replaces the next
trigger and remains recoverable. PendingIntent identity uses the full reminder
ID, and notification tags avoid Java string-hash collisions. Cancel/delete
removes the local registration and displayed alert, including legacy IDs.

Notification channels are no longer deleted before every alert. Sound/vibration
configurations have stable channel IDs, a blocked legacy channel is respected,
and the custom tone uses its resource name instead of a build-dependent numeric
resource ID. The tone is explicitly retained during resource shrinking.

Active recurring reminders display/filter their upcoming occurrence rather
than remaining overdue after the first occurrence. A one-time reminder whose
date has passed can still be labelled overdue after its alert; notification
delivery is separate from completing a task.

## Evidence

- The user confirmed notification permission was granted and an actual saved
  reminder produced an alert.
- After the alarm-clock timing refinement, the user repeated a two-minute
  reminder and confirmed that it sounded at the chosen minute without the
  previously observed delay.
- All 301 JVM unit tests passed after the timing and authentication fixes.
- Three local device alarm tests passed: disk round-trip, colliding-ID
  isolation, and actual sound-enabled notification delivery with restored
  registration, repetition and the real Snooze action.
- Latest measured notification posting delays on the connected phone were
  57 ms and 82 ms. The test checks delivery within two seconds. These measurements
  are evidence for that run, not a hard real-time guarantee for every Android
  device or power state.
- Device tests use synthetic local records and remove them afterwards. They do
  not write Firebase reminder documents or automatically change permissions,
  sound volume or Do Not Disturb settings.

Final UI and release validation are recorded in `auth-release-review.md`.

References:
- https://developer.android.com/develop/ui/compose/notifications/notification-permission
- https://developer.android.com/develop/background-work/services/alarms
- https://developer.android.com/reference/android/app/AlarmManager
- https://developer.android.com/develop/ui/compose/notifications/channels
