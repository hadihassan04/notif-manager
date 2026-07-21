# Notif Manager Product and UX Design

Status: Working specification  
Last updated: 2026-07-19

## Product thesis

Notif Manager reduces routine notification interruptions without making important notifications easy to miss.

Routine notifications wait by default. They are released at predictable delivery times. Open hours temporarily allow normal interruptions, while priority apps always pass through.

The interface must make the current notification state understandable at a glance:

- Are routine notifications waiting or arriving immediately?
- When is the next delivery?
- Which apps may interrupt immediately?
- Is required setup complete?

## Locked product decisions

### Default notification behavior

- Routine notifications wait by default.
- A delivery time releases all waiting notifications.
- Entering scheduled Open hours immediately releases all waiting notifications.
- New routine notifications arrive immediately throughout Open hours.
- When Open hours end, new routine notifications begin waiting again.
- Priority apps and priority channels always arrive immediately.
- A manual temporary Open period follows the same rules as scheduled Open hours, including releasing the current queue.

### Required setup

- Notification access is required.
- Permission to post notifications is required on Android versions where it is applicable.
- The user cannot complete onboarding until both required permissions are granted.
- Exact timing is optional and must be described as an accuracy improvement, not a core requirement.
- Permission status refreshes immediately when the app resumes after opening Android settings.

### Appearance

- Material You wallpaper colors remain enabled by default on Android 12 and newer.
- The app supports coherent light and dark appearances.
- The activity window, splash surface, status bar, navigation bar, onboarding, dialogs, and application screens must use the same active appearance.
- Dynamic color changes hue, not the semantic meaning or brightness hierarchy of surfaces.
- Devices without Material You use a deliberate fallback palette rather than the default generic Material purple scheme.

### Archiving

- Active notification rows do not display a trailing archive icon.
- Swiping an active row archives it.
- Archiving always offers Undo.
- Archived content provides an explicit Restore action and an accessible alternative to swipe gestures.
- User-facing copy uses “Archive” and “Restore”; it does not call archiving “Clear.”

## Product vocabulary

Use these terms consistently:

| Use | Meaning |
| --- | --- |
| Inbox | Notifications managed by Notif Manager |
| Waiting | Routine notifications held until a release event |
| Delivery time | A scheduled time that releases everything waiting |
| Open hours | A period that releases the queue and allows routine notifications to interrupt |
| Priority app | An app whose notifications always arrive immediately |
| Allow all temporarily | A manual, time-limited Open period |
| Archive | Remove an item from the active Inbox without deleting its record |
| Restore | Return an archived item to its previous list |

Avoid exposing internal or overlapping terms such as “hold-start time,” “instant window,” “batch rule,” and “schedule inactive.” Use “batch” only when describing a delivered group if user research shows that term is valuable.

## Information architecture

Recommended primary destinations:

1. **Inbox** — current state, waiting notifications, latest delivery, and history.
2. **Schedule** — delivery times, Open hours, and temporary Open state.
3. **Priority** — apps and advanced channel exceptions.

Settings remains a top-app-bar destination rather than a fifth bottom-navigation item.

Insights is secondary to Inbox and can be opened from an Inbox summary or overflow action. It should return to primary navigation only if it evolves into an actionable, frequently used product area.

The current “Rules” destination becomes “Priority.” Per-channel rules are progressive, advanced controls within an app rather than the primary organizing model.

## Onboarding

### Flow

#### 1. Welcome

Purpose: explain the product in one sentence before requesting access.

Suggested copy:

> Take back your attention
>
> Routine notifications wait for times you choose. Important apps can still reach you immediately.

Primary action: **Continue**

#### 2. Required setup

Show three full-width status rows:

1. **Notification access** — Required  
   “Lets Notif Manager hold and organize notifications from other apps.”
2. **Delivery notifications** — Required  
   “Lets Notif Manager tell you when waiting notifications are released.”
3. **Precise delivery** — Optional  
   “Improves delivery accuracy. Without it, Android may deliver a little later.”

Behavior:

- Each row shows one of: Not allowed, Opening settings, Allowed, or Unavailable on this Android version.
- The whole row is actionable when setup is incomplete; do not use a decorative chip that looks interactive.
- Refresh every status on lifecycle resume.
- The primary action remains disabled until both required permissions are allowed.
- Disabled copy explains the requirement: “Allow the two required permissions to continue.”
- Precise delivery can be skipped without friction.

#### 3. Priority apps

Suggested heading: **What should always reach you?**

- Explain that selected apps bypass waiting and Open-hour rules.
- Show recommendations separately, with a reason where possible.
- Do not silently select recommended apps.
- Provide search for long app lists.
- Allow progress with no priority apps selected.

Primary action: **Continue**

#### 4. Delivery schedule

Suggested heading: **When should waiting notifications arrive?**

Because routine notifications wait by default, the app must not finish setup without a safe release path.

- Offer a small set of editable schedule templates, such as morning and evening, three times a day, and Custom.
- Show the actual localized delivery times before the user chooses a template.
- Let the user edit every proposed time and its active days.
- Require at least one enabled delivery time before setup can finish.
- Keep Open hours optional; they can be added here through secondary disclosure or configured later from Schedule.
- Do not create or save delivery times until the user confirms this step.

Primary action: **Finish setup**

### Permission return behavior

When the user returns from Android settings:

1. Re-check all permissions on resume.
2. Update the relevant row immediately.
3. Preserve onboarding position and scroll state.
4. Announce the new state to accessibility services.
5. Enable Continue as soon as both requirements are satisfied.

Do not show a success dialog; the live status transition is the confirmation.

## Schedule redesign

### Mental model

The schedule is a single policy with three concepts:

- **Waiting** is the default state for routine notifications.
- **Delivery times** release the waiting queue.
- **Open hours** release the queue when they begin and permit immediate interruptions until they end.

Priority apps are exceptions to the entire schedule.

### Screen hierarchy

```text
Schedule

┌────────────────────────────────────┐
│ Waiting now                        │
│ Next delivery 17:00 · 12 waiting   │
│ [ Allow all temporarily ]          │
└────────────────────────────────────┘

Today
Quiet ━━━━━●━━━━ Open ━━━●━━━━ Quiet
            07:00          17:00

Delivery times
07:00   Every day                      >
17:00   Weekdays                       >
22:00   Every day                      >
[ Add delivery time ]

Open hours
12:00–13:00   Weekdays                 >
[ Add Open hours ]

Priority apps
Calls, Messages, Calendar              >
```

### Status card

The first card communicates the active state instead of presenting abstract schedule controls.

Waiting state:

- “Waiting now”
- “Next delivery today at 17:00”
- “12 notifications waiting”
- Action: **Allow all temporarily**

Open state:

- “Open until 13:00”
- “Routine notifications can interrupt you”
- Action: **End Open hours** when the state is manual, or **Start waiting now** for a deliberate override of a scheduled window.

Degraded timing state:

- “Delivery may be approximate”
- Secondary action: **Improve timing**

### Day timeline

- Show a 24-hour track with Waiting and Open segments plus delivery markers.
- Use labels, texture, or iconography in addition to color.
- Mark the current time and current state.
- Tapping a segment opens its editor; tapping a marker edits the delivery time.
- The timeline is an overview, not the only way to edit the schedule.
- Provide an equivalent structured list for accessibility.

### Delivery times

Each delivery time includes:

- Time
- Active days
- Enabled state
- Next occurrence
- Remove action inside the editor

Creation opens an editor before saving. Do not create a persisted default schedule merely because the user tapped Add.

If all delivery times are disabled or removed, show a blocking warning before routine notifications can be held indefinitely. Offer **Add delivery time** as the primary action.

### Open hours

Each Open period includes:

- Start and end time
- Active days
- Enabled state
- Plain-language summary, including overnight behavior

At the start boundary:

1. Release everything currently waiting exactly once.
2. Permit new routine notifications to arrive immediately.

At the end boundary:

1. Do not retract notifications already delivered.
2. Begin waiting for new routine notifications.

Overlapping Open periods act as one continuous Open state. The queue is released only on the transition from Waiting to Open, not at every overlapping boundary.

### Allow all temporarily

Replace the unexplained global pause icon with a labeled temporary action.

Suggested duration choices:

- 30 minutes
- 1 hour
- Until the next delivery
- Choose an end time

Starting a temporary Open period releases everything waiting. The active state and end time remain visible in the Schedule status card and, when useful, in a compact Inbox banner.

### Conflict and edge-case rules

- Priority app wins over Waiting state.
- Open state wins over routine waiting behavior.
- A delivery time during Open hours usually has no waiting items to release and therefore requires no special user-visible event.
- Multiple delivery times at the same minute are treated as one release event.
- Timezone and clock changes recalculate future transitions without duplicating a release.
- Device restart restores the next schedule transition.
- If exact alarms are unavailable, use Android-compatible approximate scheduling and communicate the degraded precision.

## Inbox redesign

### Primary hierarchy

1. Current state and next transition
2. Waiting notifications
3. Most recent delivery
4. History

Do not show multiple decorative overview cards that repeat the same counts.

### Notification rows

- Tapping a row opens the original notification destination when possible.
- Swiping archives the row and reveals an Archive background treatment.
- Use one consistent swipe direction according to layout direction.
- Confirm with a short haptic response at the action threshold.
- Show “Archived” with an Undo action.
- Do not show a permanent archive icon on active rows.
- Expose an accessibility custom action named “Archive.”

### Groups and deliveries

- A delivered group may be expanded to inspect individual notifications.
- Use one term consistently for group-level removal: **Archive delivery**.
- Group-level archive also offers Undo.
- Avoid mixing “Waiting,” “Delivered,” “History,” and “Archived” without clear chronological or state boundaries.

### Archive access

Archived notifications should be accessible from the Inbox overflow menu or a clearly labeled secondary destination. Avoid placing a large “Archived” button after the entire history list.

Archive rows provide Restore. Restore is explicit because it is infrequent and must remain accessible without requiring a reverse swipe gesture.

## Priority and advanced rules

### Primary experience

Rename Rules to **Priority** and lead with the user outcome:

> Priority apps always reach you, even while routine notifications are waiting.

Provide:

- Search
- Priority apps section
- Routine apps section
- A simple per-app priority toggle or two-state selector

### Advanced channel exceptions

- Keep notification-channel controls collapsed by default.
- Label them “Channel exceptions,” not “Custom.”
- Explain inheritance: “Uses the app setting” rather than “Same as app.”
- Surface the number of exceptions in the app summary.
- Avoid five equally prominent filter chips on first entry.

## Settings

Organize settings into labeled groups:

### Setup

- Notification access
- Delivery notifications
- Precise delivery

These statuses use the same lifecycle-aware component as onboarding.

### Appearance

- Theme: System default, Light, Dark
- Wallpaper colors: enabled by default when available

Wallpaper colors must respect the selected brightness mode. Theme changes apply immediately to the entire activity and system bars.

### Notification management

- Include system apps
- History retention

### Help and reset

- Review setup
- About notification behavior

Replace the developer-like “Replay onboarding” action with **Review setup**. Destructive cleanup actions require clear scope, confirmation when material, and completion feedback.

## Insights direction

Insights should help users tune the product rather than celebrate vanity metrics.

Until it provides clear recommendations, Insights is a secondary Inbox destination rather than a bottom-navigation item.

Prioritize:

- Interruptions prevented over a meaningful time range
- Notifications released per delivery
- Apps producing the most routine volume
- Notifications arriving during Open hours
- Suggested changes such as moving a noisy app from Priority to Routine

Avoid generic square metric grids, decorative flower graphics, and claims such as “Distraction shield” unless the calculation is clearly defined and useful.

## Visual system

### Character

Calm, trustworthy, native Android. The visual identity comes from clear temporal state and disciplined hierarchy, not decorative shapes.

### Color behavior

Use semantic roles rather than fixed component colors:

- Background and surface
- Elevated surface
- Primary action
- Waiting state
- Open state
- Success/allowed
- Warning/degraded timing
- Destructive action

Material You may supply palette values, but contrast and role assignment remain controlled. Waiting and Open states must remain distinguishable without relying on hue alone.

### Typography

- Use the Android system typeface for a native, dependable feel.
- Reserve display sizing for the current state or next delivery time.
- Use sentence case throughout.
- Keep metadata concise and visually secondary.

### Shape and elevation

- Use a restrained radius scale.
- Cards represent meaningful grouping, not every row.
- Prefer spacing and dividers for simple lists.
- Keep dark-mode elevation subtle and avoid light gray cards floating on a dark canvas.

### Signature element

The day timeline is the product’s signature visual. It communicates Waiting, Open hours, delivery moments, and the current time in one accessible overview. Decorative flower motifs should be removed unless they gain a product-specific meaning.

## Accessibility and resilience

- Support screen readers, font scaling, switch access, and keyboard navigation where Android provides it.
- Provide non-gesture alternatives for every swipe action.
- Do not communicate state by color alone.
- Maintain minimum touch targets.
- Preserve content and control usability at large font sizes.
- Respect reduced-motion preferences for state transitions and list removal.
- Support right-to-left layout and localized time formats.
- Handle overnight Open periods and daylight-saving transitions explicitly.

## Acceptance criteria

### Theme

- Light mode contains no unintended dark window or component surfaces.
- Dark mode contains no unintended light window or component surfaces.
- Status and navigation bar icon brightness matches their backgrounds.
- Onboarding, dialogs, app content, and Android window chrome change together.
- Material You works in both light and dark appearances.

### Permissions

- Returning from each Android permission screen updates status without navigating away or restarting.
- Onboarding cannot complete without both required permissions.
- Exact timing can be skipped.
- Required, optional, denied, and allowed states are visually and programmatically distinguishable.

### Scheduling

- Onboarding cannot complete without at least one enabled delivery time.
- Routine notifications wait outside Open hours.
- Delivery times release all waiting notifications once.
- Entering Open hours releases the queue once.
- Routine notifications received during Open hours arrive immediately.
- Routine notifications begin waiting after Open hours end.
- Priority notifications remain immediate in every schedule state.
- Overlapping windows and duplicate delivery times do not duplicate releases.
- The interface always shows the active state and next relevant transition.

### Archiving

- Active notification rows have no persistent archive icon.
- Swipe archives and offers Undo.
- Accessibility users can archive without swiping.
- Archived notifications can be restored explicitly.
- The interface uses Archive/Restore consistently instead of Clear.

## Current-model migration

The current implementation cannot produce the agreed experience through presentation changes alone.

### Scheduling data

- The current schedule entity combines a hidden hold-start time with a release time. Replace that user-facing model with explicit delivery times containing a time, active days, and enabled state.
- Migrate existing schedule release times into delivery times. Do not expose or preserve obsolete hidden hold-start boundaries as editable concepts.
- The current instant-window entity maps conceptually to Open hours and can be migrated to an explicitly named Open-period model.
- Preserve enabled state, active days, start/end times, and existing identifiers where practical during migration.

### Scheduling behavior

- The current Open-window behavior affects only newly arriving notifications. Add a scheduled transition at the start of Open hours that releases the existing waiting queue exactly once.
- Schedule both the next delivery event and the next Open/Waiting boundary after app setup, schedule edits, device restart, timezone changes, and clock changes.
- Keep release operations idempotent so duplicated Android alarms cannot deliver the same waiting items twice.
- Replace the indefinite pause boolean with a time-bounded temporary Open state. Starting it releases the queue; expiration returns the app to Waiting unless a scheduled Open period is active.

### App rules

- Migrate the existing Batch app mode to Routine.
- Migrate the existing Instant app mode to Priority.
- Preserve channel overrides, but present them as advanced channel exceptions.

### Archive behavior

- The active row already supports swipe-to-archive. Remove its redundant trailing archive button rather than introducing another gesture implementation.
- Add the explicit accessibility action before treating swipe-only presentation as complete.

## Recommended delivery sequence

### 1. Foundation: theme and permission truth

- Establish one activity-level light/dark source of truth.
- Synchronize system bars and splash/window surfaces.
- Keep Material You enabled while providing deliberate fallback schemes.
- Create one lifecycle-aware permission-status component shared by onboarding and Settings.
- Enforce required-permission onboarding gates.

This phase repairs trust: the interface accurately reflects both Android state and its own appearance.

### 2. Scheduling domain

- Introduce delivery times, Open periods, and time-bounded temporary Open state.
- Add migration from the current schedule and instant-window records.
- Implement queue release on Open transitions.
- Cover overlapping windows, overnight periods, duplicate alarms, timezone changes, and restarts with tests.

This phase should land before the new schedule or final onboarding screens because both depend on the new behavior.

### 3. Onboarding and Schedule UI

- Build the four-step onboarding flow.
- Build the state-first Schedule screen, structured editors, and accessible day timeline.
- Require at least one enabled delivery time before setup completes.
- Verify permission return and schedule creation on a real device or emulator.

### 4. Inbox and navigation

- Reduce primary navigation to Inbox, Schedule, and Priority.
- Replace the global pause icon with Allow all temporarily.
- Remove active-row archive icons, add accessibility actions, and normalize Archive/Restore copy.
- Move archive access and secondary Insights out of the primary content flow.

### 5. Priority, Settings, and Insights

- Reframe Rules as Priority and progressively disclose channel exceptions.
- Group Settings by Setup, Appearance, Notification management, and Help.
- Replace Replay onboarding with Review setup.
- Keep only actionable Insights and validate every metric definition.

### 6. Product verification

- Exercise the acceptance criteria in light, dark, Material You, and fallback themes.
- Verify permission denial and return paths across supported Android versions.
- Test schedule transitions at delivery start, Open start, Open end, overnight boundaries, and device restart.
- Verify gestures, accessibility alternatives, large text, RTL, and localized time formats.

## Follow-up product decisions

These decisions remain open and should be discussed before implementation:

1. Exact delivery-time templates and localized preset values for a new user.
2. Default temporary Open duration.
3. Whether users may create a one-off Open period directly from the Inbox.
4. Whether archived notification records expire with history retention or use a separate policy.
5. Which insights are trustworthy and actionable enough to keep.
