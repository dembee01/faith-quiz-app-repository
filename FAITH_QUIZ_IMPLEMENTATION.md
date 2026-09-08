# Faith Quiz implementation guide

This document is the product and engineering source of truth for the Faith
Quiz repository. It describes the Flutter application, the Firebase backend,
the data contract, the security boundary, and the Group Challenge lifecycle.
It is intentionally based on the source currently in this repository rather
than on an old function count, an old screen name, or a historical design
document.

> Status snapshot: 2026-09-08, current `master` includes the host challenge
> deletion and strict ten-minute invitation-window changes. Re-check the
> source and update this status section after each architecture or product-
> flow change.

## 1. Product overview

Faith Quiz is an offline-first Bible quiz for Android and iOS. The Flutter
application provides a 30-level Covenant Journey, local topic packs, a local
Daily Challenge, resumable quiz sessions, mistake review, streaks,
achievements, settings, and optional cloud services. Firebase adds a verified
online question catalogue, cumulative leaderboard, usernames, private
progress backup, daily reminders, and multiplayer Group Challenges.

The current online product has two different multiplayer modes:

* **Competitive**: players receive the same ordered question set and work
  independently. Verified accuracy is the primary ranking value; official
  elapsed time is the tie-breaker.
* **Fellowship**: the host controls a shared question, answer reveal, and
  advancement so a church group, class, family, or Bible study can discuss
  Scripture together.

The 6-digit group code is the user-facing invitation mechanism. Firestore
document IDs are implementation details and must not be the normal way a
player joins a group.

## 2. Application architecture

### Flutter client

`lib/main.dart` initializes Flutter, Firebase (when available), persistent
Firestore caching, App Check, and Remote Config without blocking the offline
launch path. `lib/app.dart` contains the current screens and navigation.
`lib/cloud_challenge_service.dart` is the typed gateway for Auth, Firestore,
and callable Functions. `lib/progress_store.dart` owns local progress and
optional private cloud backup. The Android application ID and iOS bundle ID
are `com.example.faithquiz`.

The repository also contains an older Kotlin/Jetpack Compose module under
`app/`. The active product described here is the Flutter application under
`lib/` and the Flutter host under `android/`. The old module and the historical
Kotlin wording in `README.md` are migration artifacts; do not use them as the
authority for current Flutter navigation or Firebase behavior.

### Firebase services

* **Firebase Auth** supplies an anonymous identity for cloud operations. The
  Groups screen also supports native Google sign-in for a durable account and
  username claim.
* **Cloud Firestore** stores public catalogue questions, private answer keys,
  user-owned indexes and progress, group membership, challenge state, private
  Fellowship answers, and server-written leaderboard entries.
* **Cloud Functions for Firebase (v2)** expose all writes that require
  authority: username reservation, grading, group lifecycle, challenge
  lifecycle, and scheduled reminders.
* **Firebase Cloud Messaging (FCM)** is optional and opt-in. A token is only
  registered after the player enables Daily Reminders.
* **Firebase Remote Config** gates cloud play and selects the active catalogue.
  Question bodies are not stored in Remote Config.
* **Firebase App Check** uses debug providers in debug builds and Play
  Integrity/App Attest providers in release builds on the client. Callable
  Functions deliberately set `enforceAppCheck: false` because the product is
  distributed as a sideloaded APK; answer secrecy and server grading remain
  the integrity controls. Revisit this decision if distribution moves to a
  supported Play Integrity channel.

### Project and runtime configuration

* Firebase project: `faith-quiz-app-119653`
* Functions region: `us-central1`
* Functions runtime: Node.js 22 (`functions/package.json`)
* Functions global defaults: 256 MiB, 15-second timeout, concurrency 40,
  maximum 10 instances
* Flutter callable client region: `us-central1`
* Scheduled reminder timezone: `Africa/Accra`

No credentials, refresh tokens, passwords, or private signing material belong
in this file. `firebase_options.dart` and Android `google-services.json` hold
client configuration and are not a substitute for backend authorization.

## 3. Main quiz systems

These systems intentionally have separate scoring and progression models.

### Covenant Journey

The Journey is the local, classic progression. `lib/question_bank.dart`
contains the generated 30-level catalogue, and `lib/models.dart` contains the
30 Journey nodes. `QuizScreen` runs the questions locally; `ProgressStore`
persists unlocked levels, attempts, scores, total time, streaks, mistakes,
and review scheduling in `SharedPreferences`. A classic/journey level with at
least 60% advances the unlock state. The player receives explanations and
local review data; no Firebase score is needed to progress.

The same local `QuizScreen` also supports local `speed`, `survival`, `daily`,
topic, and review modes. Local speed mode has a 60-second session timer and
survival starts with three lives. These local timers are unrelated to the
Group Challenge timer.

### Daily Challenge

The menu's **Daily Challenge** is a local daily question selected from the
bundled question bank using the UTC epoch day. Completion updates the local
devotion streak and records the day in `ProgressStore`. It does not submit to
the verified cloud leaderboard.

### Online / Cloud Challenge

The menu's **Cloud Challenge** is a cloud-backed, one-question-at-a-time
experience. It reads a public question after authentication and sends only the
selected option to `submitCloudChallenge`. The private answer is read by the
Function, not the app. Cloud submissions update the cumulative global
leaderboard and per-question history; they do not unlock local Journey levels.

### Group Challenge

Group Challenge is a host-created multi-question session under a group. A
host chooses `competitive` or `fellowship` and exactly 10, 20, or 30 questions.
The backend stores one ordered public question array in the challenge document
and the client opens the mode-specific screen from the authoritative `mode`
field. Competitive and Fellowship use different Functions and different
state machines even though they share catalogue questions and group
membership.

## 4. Online Challenge architecture

### Catalogue

The checked-in source catalogue is
`data/cloud_prophets_witnesses_v1.json`, catalogue ID
`faith-quiz-global-v1`, titled *Prophets & Witnesses: Global Challenge*. It
contains 500 questions: 400 Old Testament and 100 New Testament prophetic
witness questions. It is generated and validated against a public-domain KJV
verse index. The design uses catalogue metadata and is intended to grow beyond
500; Functions validate the stored `questionCount` and fall back to 500 only
for backward compatibility when metadata is absent.

Online question selection uses a deterministic client permutation over the
current catalogue size: `((epochDay + step) * 263) % totalQuestions` when 263
is coprime to the catalogue size. The client reads only
`content/{catalogue}/questions` documents. It never reads the correct answer.

### Public/private split and grading

Public question documents contain the prompt, four options, Scripture
reference, testament, category/focus, and source metadata. Private answer
documents under `contentPrivate` contain the correct option index and the
challenge ID; the optional explanation is used by server-side review paths.

`submitCloudChallenge` validates the authenticated user, catalogue, question,
answer index (0-3), and client elapsed range (0-600 seconds), then obtains the
private key through the Admin SDK. It writes the user's cloud-answer history
and a verified global entry in one transaction. A repeat submission for the
same user/question is idempotent for cumulative totals and preserves the
previous score. Claimed usernames replace ad-hoc display names on the global
entry.

### Leaderboard and usernames

`claimUsername` reserves a case-insensitive username in `usernames/{key}` and
updates the private user profile and any existing global entry transactionally.
Usernames are 3-20 characters and may contain letters, numbers, and
underscores. `checkUsernameAvailable` permits availability checking and does
not require authentication; claiming still requires authentication.

The global cumulative entry stores verified `score`, `totalAnswered`,
`accuracy`, `totalResponseTime`, `avgElapsedSeconds`, `level`, and display
name. The client orders global entries by score, then accuracy, then average
response time, then UID for a stable tie-break. A per-challenge legacy
leaderboard is retained for compatibility.

## 5. Group Challenge architecture

### Group and membership

`createGroup` creates a random Firestore group ID and a six-digit numeric code.
The creator is written immediately as
`groups/{groupId}/members/{ownerUid}` with `role: owner`; the creator also
receives a private index entry under `users/{ownerUid}/groups/{groupId}`.
The group document's canonical owner field is `ownerUid`. `ownerId` remains a
legacy compatibility field in client parsers.

`joinCodes/{code}` is a server-owned reservation that maps a code to its group,
owner, name, and expiry. Reservation is an atomic transaction. Active code
collisions cause the creator to retry with a new code, and expired reservations
may be reused. The user-facing flow should always share and enter this code,
not the internal group ID.

The current client lists groups from the caller's private user index. The
group detail screen shows the code, expiry countdown, challenges, and (for the
owner) challenge creation and extension controls. Group reads, challenge reads,
member reads, and entry reads are allowed only to authenticated group members.

### Join window / session expiry

`expiresAt` is the group invitation/session expiry, not a quiz-completion
timer. The normal default and the current Flutter create-dialog choice are
10 minutes. New invitations accept only an integer duration from 1 through 10
minutes (omitting the field defaults to 10); zero, negative, non-integer, null,
and longer values are rejected by the callable. The UI uses the ten-minute
choice. The owner may extend an existing group by 10 minutes. The extension
updates the group, join-code reservation, and owner's index.

The Function rejects joins after `expiresAt` and rejects challenge creation or
start after expiry. There is currently no scheduled cleanup requirement; an
expired document may remain stored but cannot be used through the protected
callables. The ten-minute maximum is enforced in the Function as well as the
UI, so an older client cannot create a longer or non-expiring invitation.

### Lobby and challenge creation

Only an owner can call `createGroupChallenge` (the Flutter convenience method
used by the current UI is `createGroupQuiz`). The Function selects the requested
number of distinct questions from the catalogue using a server-generated seed,
stores the public question array and IDs, and creates a challenge in `lobby`.
Valid modes are exactly `competitive` and `fellowship`; valid counts are
exactly 10, 20, and 30. The challenge stores `createdBy` as the authoritative
creator identity. `ownerUid` and legacy `ownerId` are accepted by the Flutter
model when reading older data.

While a challenge is in `lobby`, the Group Lobby streams the challenge and
group. The host sees a mode-specific START button; members see a waiting state.
On start, the host-only callable atomically writes `startedAt` and transitions
the status to `active` (Competitive) or `question_open` (Fellowship). Repeated
starts are idempotent and do not reset `startedAt`.

At the lobby-to-start transition, `startGroupChallenge` now freezes the
participant set in `participantUids` and `participantCount` while it writes
`startedAt` and the active/question-open status. New Competitive and
Fellowship submissions reject authenticated group members who were not in that
snapshot. Fellowship finalization grades the frozen roster and writes a zero
entry for a roster member who submitted no answer. Legacy challenges without
`participantUids` retain their historical current-member behavior so old data
remains usable.
### Mode-specific routing

`GroupDetailScreen` opens `GroupLobbyScreen` for `status == lobby`. Once a
challenge is started, it routes Fellowship challenges to
`FellowshipQuestionScreen` and Competitive challenges to
`GroupQuestionScreen`. `GroupLobbyScreen` also listens for the authoritative
challenge snapshot and replacement-navigates to the corresponding mode when
the host starts. Do not route based only on a button label or a cached
constructor value; `challenge.mode` is the source of truth.

### Host and member behavior

Ownership is derived from canonical `ownerUid` (falling back to legacy
`ownerId`) and challenge ownership from `createdBy`, then legacy fields. The
Flutter lobby shows host-only start controls and the Fellowship screen shows
host-only reveal/next/complete controls. The server independently checks the
membership role for every host operation.

The current lobby does not render a full member list, but it does show the
host/member state explicitly: the host sees `YOU • HOST` and a member sees
`MEMBER • HOST-LED LOBBY`. Group cards also label the caller `HOST` or
`MEMBER`. These labels are presentation only; Functions independently check
the canonical owner and membership role for every host operation.

### Reconnect

Firestore snapshots are authoritative for group/challenge state, so reopening
a group can discover a lobby, active challenge, Fellowship question index,
reveal state, finalizing state, or completed state. The current Flutter
screens do not persist a complete local Group Challenge navigation/session
object. Competitive answer selections, per-question timer bookkeeping, and
Fellowship's local answer map are in-memory. Reopening a completed
Competitive challenge shows read-only results/leaderboard rather than
starting Question 1 again. A backgrounded or closed app otherwise requires
the player to open the group/challenge again; it must never create a second
challenge or trust locally cached answers as official results. Automatic
resume into the exact active screen/question remains a device-validation and
follow-up item.

## 6. Competitive mode

### Lifecycle

1. Owner creates a challenge with `mode: competitive` and 10/20/30 questions.
2. Challenge is visible in `lobby`; members can open the lobby.
3. Owner calls `startGroupChallenge`; status becomes `active` and `startedAt`
   is established once.
4. Each member answers the same ordered public questions independently in the
   Flutter Competitive screen.
5. The client submits the complete answer array to
   `submitGroupChallenge`. The server reads every private key, computes the
   score, and writes one immutable-by-idempotency entry per member.
6. The client shows final review only after its submission response. The group
   leaderboard is read-only to clients and sorted score descending, then
   elapsed time ascending.

The current backend accepts a challenge status of `active` or `completed` for
multi-question submissions but does not automatically transition the
challenge document to `completed` after the last individual submission;
completion is represented by the member entry and client result today. Do not
interpret a still-active document as permission to change an existing entry.

### Ranking and zero-spoiler behavior

The score is the number of correct answers. Accuracy/score always outranks
speed; a perfect but slower player beats a faster lower-score player. Official
multi-question elapsed time is calculated from server `startedAt` to server
now, clamped to 1-7200 seconds. A forged low client duration therefore cannot
reduce the official elapsed value. The client-reported value is retained only
as diagnostic metadata.

Questions contain no answer key. The current result response includes the
server-verified breakdown only after submission, so pre-start and in-progress
reads do not expose correct answers through the callable. Firestore rules also
deny `contentPrivate` completely.

### 30-second question timer decision

The product decision is **30 seconds per Competitive question**, separate from
the group join window. The Flutter Competitive screen now displays a local
30-second countdown for each question, auto-advances after expiry (except on
the last question), and submits an unanswered/timeout value when the player
does not answer. The session clock is anchored to the authoritative
challenge `startedAt`; the client does not write per-second timer updates to
Firestore.

The remaining caveat is server enforcement: the challenge schema does not
store a per-player `questionStartedAt`, so the Function verifies only the
challenge-level elapsed time from `startedAt` to server now. A modified client
could therefore submit an answer array with different per-question timing;
the server still owns correctness, official elapsed ranking time, and entry
idempotency. A future hardening pass should add an authoritative per-question
deadline or server-normalize timed-out positions.

## 7. Fellowship mode

Fellowship is intentionally host-paced. It must not inherit the Competitive
30-second timer. Participants can think, read Scripture, and discuss before
the host reveals.

### State machine and responsibilities

* `lobby`: challenge exists; only the owner can start.
* `question_open`: one shared question is available; each member may submit
  one immutable answer for the current index.
* `question_revealed`: the owner has revealed the server-read correct answer,
  explanation, and Scripture reference; further answers are rejected.
* `finalizing`: the final advance has marked the challenge for server grading.
  The host can retry if the process was interrupted; members wait.
* `completed`: all group entries have been written and the client shows the
  Fellowship leaderboard/results.

`submitFellowshipAnswer` uses the document ID
`${questionIndex}_${uid}` and a transaction that validates status, current
index, membership, and answer absence. A duplicate returns the first answer
and does not overwrite it. The Function records server-derived response time
from `currentQuestionOpenedAt`, but it does not impose a hard timeout.

`revealFellowshipAnswer` is owner-only and atomically changes
`question_open` to `question_revealed` after reading the private key. Repeating
reveal is idempotent. `advanceFellowshipQuestion` is owner-only and changes a
revealed question to the next `question_open`, clearing transient reveal data.
On the last question it writes `finalizing`, grades all answer documents, adds
verified entries (including 0/N for members with no answers), and only then
writes `completed`. Repeating completion is safe; a `finalizing` challenge can
be retried.

The Flutter copy reflects these states: answer locked/waiting for reveal,
host discussion, finalizing results, and the completed Fellowship leaderboard.
The reveal key is never directly client-readable before the host action.

## 8. Firestore structure

The following paths are authoritative. All writes are Admin SDK/Function-owned
unless explicitly noted by the rules.

| Path | Important fields and purpose |
| --- | --- |
| `content/{catalogue}` | Catalogue metadata: `title`, `questionCount`, testament counts, source, `schemaVersion`, `updatedAt`. |
| `content/{catalogue}/questions/{questionId}` | Public prompt, four `options`, `dailyIndex`, Scripture/source metadata. No correct answer. |
| `contentPrivate/{catalogue}/answers/{questionId}` | Private `correctAnswer`, `challengeId`, and server-only answer data. Client access denied. |
| `groups/{groupId}` | `name`, canonical `ownerUid`, user-facing `joinCode`, `expiresAt`, `durationMinutes`, `createdAt`. |
| `groups/{groupId}/members/{uid}` | `role` (`owner`/`member`), `joinedAt`, display name. The owner UID is authoritative even if an old role literal is stale. |
| `users/{uid}/groups/{groupId}` | Private group index used by `myGroups()`: name, role, join code, expiry, duration. |
| `joinCodes/{code}` | Server-reserved six-digit code mapping to group and expiry. Not a public client collection. |
| `groups/{groupId}/challenges/{challengeId}` | `catalogue`, `title`, `mode`, `status`, `questionCount`, public `questions`, `questionIds`, server seed, `createdBy`, `startedAt`, current/reveal fields, `answeredUids`, and (for new starts) frozen `participantUids` plus `participantCount`. |
| `.../fellowshipAnswers/{questionIndex_uid}` | One immutable answer per member/question: UID, index, answer option, server timestamp, response seconds. |
| `.../entries/{uid}` | Server-written verified group result: display name, score, total, correct, elapsed seconds, timestamps. |
| `leaderboards/global_challenge/entries/{uid}` | Cumulative server-verified score, totals, accuracy, average response time, level, username/display name. |
| `leaderboards/{challengeId}/entries/{uid}` | Per-online-challenge compatibility entry. |
| `users/{uid}/cloudAnswers/{questionId}` | Server-written online answer history. It is not a client-writable path. |
| `users/{uid}/private/progress` | Optional local-progress backup with `schemaVersion`, integer `updatedAt`, and a bounded `payload`. |
| `users/{uid}/private/notifications` | Optional FCM token, `remindersEnabled`, platform, timestamp. |
| `users/{uid}` | Readable signed-in profile with username/display name; writes go through Functions. |
| `usernames/{lowercaseUsername}` | Unique username reservation and owning UID. |

Important identity compatibility rules:

* New groups write `ownerUid`; old data may have `ownerId`.
* New challenges write `createdBy`; old challenge records may use
  `ownerUid`/`ownerId`.
* `QuizGroup.fromMap` gives the signed-in UID owner status when it matches the
  canonical/legacy owner identity, even if a stale literal role says member.
* `GroupChallenge.fromMap` resolves challenge ownership in the order
  `createdBy`, `ownerUid`, `ownerId`.
* Do not rename or remove legacy fields without a migration and live-data
  audit.

## 9. Cloud Functions inventory

The actual export inventory in `functions/index.js` currently contains 14
exports. All except `sendDailyReminders` are v2 HTTPS callable Functions in
`us-central1`. Callable App Check enforcement is disabled deliberately as
described in the architecture section.

| Export | Caller | Responsibility and important validation |
| --- | --- | --- |
| `claimUsername` | Authenticated user | Transactionally reserves a case-insensitive 3-20 character username, updates the user profile, and updates an existing global entry. Rejects collisions. |
| `checkUsernameAvailable` | Any caller; validation applies | Validates username format and reports whether it is free or already owned by the caller. |
| `submitCloudChallenge` | Authenticated user | Reads the private answer, grades one online question, updates private history and cumulative/per-question verified entries transactionally, and is idempotent per user/question. |
| `createGroup` | Authenticated user | Creates group, six-digit code reservation, owner membership, and private user index atomically. Defaults to a 10-minute expiry; retries active code collisions. |
| `joinGroup` | Authenticated user | Resolves code or legacy group ID, checks group/code expiry, and writes membership/index data. Owner self-join is owner-preserving and idempotent, preserves the existing joined timestamp/display name, and returns an owner message. |
| `extendGroup` | Authenticated group owner | Adds minutes to the group expiry and synchronizes join code and owner index. |
| `deleteGroupChallenge` | Authenticated group owner | Recursively deletes a challenge and its entries/private Fellowship answers; idempotent and parent-group preserving. |
| `createGroupChallenge` | Authenticated group owner | Validates membership, owner role, expiry, mode, count, catalogue metadata, and question availability; stores a server-seeded public question set in `lobby`. |
| `startGroupChallenge` | Authenticated group owner | Transactionally validates group/challenge/role/expiry, freezes `participantUids`/`participantCount`, establishes `startedAt` once, and enters Competitive `active` or Fellowship `question_open`. |
| `submitFellowshipAnswer` | Authenticated group member | Validates current open question and index, rejects members outside the frozen roster for new challenges, then immutably records one answer and updates answered UID state. |
| `revealFellowshipAnswer` | Authenticated group owner | Reads the private answer in a transaction and moves open to revealed with answer/explanation/Scripture; repeat reveal is idempotent. |
| `advanceFellowshipQuestion` | Authenticated group owner | Moves revealed to next open question or finalizing, grades all answers, writes verified entries, and completes with retry/idempotency behavior. |
| `submitGroupChallenge` | Authenticated group member | Grades modern Competitive answer arrays server-side with private keys and authoritative elapsed time, rejecting members outside a new challenge's frozen roster. Also retains a single-question legacy path. Rejects Fellowship use and pre-start submissions. |
| `sendDailyReminders` | Cloud Scheduler | Runs at 18:00 Africa/Accra, reads opt-in FCM tokens, and sends the Daily Challenge notification in batches. Requires a billing plan supporting Scheduler. |

The old `CloudGroupGateway.createGroupChallenge` Dart method and the
single-question branch of `submitGroupChallenge` are compatibility surfaces.
The current Group UI uses `createGroupQuiz`, which calls
`createGroupChallenge` with mode/count.

## 10. Security model

### Server authority

Correct answers, scores, timing values used for official ranking, usernames,
group ownership, challenge state transitions, and leaderboard writes are
server-owned. A client can choose an option, but it cannot declare that option
correct or write a score.

### Firestore rules

`firestore.rules` is deny-by-default. In summary:

* Public catalogue questions are authenticated-read and never client-written.
* `contentPrivate` is denied for all client reads and writes.
* Global and group leaderboard entries are authenticated-read (group entries
  additionally require membership) and never client-written.
* Groups, members, challenges, and group entries require authenticated
  membership for reads and deny direct writes.
* A Fellowship answer can be read only by its answer owner, with membership;
  all client writes are denied.
* Private progress is readable/writable only by its UID and only in the
  declared shape (`schemaVersion`, integer `updatedAt`, map `payload`).
* Private notifications are writable only by their UID and limited to the
  declared fields.
* User profiles and username reservations are readable to signed-in users but
  writable only through Admin SDK Functions.
* Unmatched paths are denied.

The rules do not make callable validation redundant: Functions still check
membership, role, mode, state, expiry, answer range, and idempotency. App
Check is not the score authority in the current sideloaded distribution.

### Idempotency and collision handling

Username claim, challenge start, Competitive entries, Fellowship answer,
Fellowship reveal, Fellowship completion/retry, and repeated completed advance
are designed to be safe under retries. Join-code allocation is transactionally
reserved and retries active collisions. Group creation and challenge creation
do not currently accept a client idempotency key; retain Flutter in-flight
guards and add server idempotency before treating arbitrary double taps as
safe.

### Ownership invariant (implemented; keep covered by regression tests)

The historical `joinGroup` hazard was an unconditional merged write with
`role: member`, which could downgrade an owner entering their own code and
reset `joinedAt`. The current transaction resolves the canonical owner
(`ownerUid`, with legacy fallback), preserves an existing owner role and
timestamp/display name, and returns the idempotent owner result:

> If the resolved caller UID equals the group's canonical owner UID (or a
> supported legacy owner identity), return an idempotent “You already own this
> group” result or navigate to the group without writing a member role.

This is enforced in the callable, not only by hiding the Join button.
Regression tests cover owner creation, owner self-join, stable owner role and
timestamp, and ordinary members joining via the six-digit code.

## 11. Data compatibility

The backend evolved from single-question group records to multi-question
Competitive/Fellowship challenges. Compatibility is intentional:

* Keep long Firestore IDs internally; expose six-digit `joinCode` to people.
* Keep parsing `ownerUid`/`ownerId` and `createdBy`/`ownerUid`/`ownerId` until
  production data is migrated and verified.
* Keep `questionId`, `answerIndex`, and the legacy single-question submit path
  readable for old records, but do not route new UI through it.
* New challenges always store an ordered `questions` array, `questionIds`,
  `questionCount`, `mode`, `status`, `createdBy`, and server seed.
* New Fellowship data uses `fellowshipAnswers` and server finalization.
* Public questions and private keys are separate by design; never “simplify”
  them into a single client-readable document.

Compatibility cleanup requires a live-data query and a rules/Functions rollout,
not a field rename based on one sample document.

## 12. State machines

### Competitive

```text
lobby --owner start--> active --member submits--> verified entry/result
                                  \--optional future lifecycle close--> completed/result
```

Allowed server transitions today:

* `lobby` to `active` through owner-only `startGroupChallenge`.
* Repeated start while already active returns the existing status and does not
  reset `startedAt`.
* `submitGroupChallenge` accepts active (and compatibility completed) records,
  grades once per member, and refuses Fellowship or pre-start submissions.

The current implementation does not automatically close a Competitive
challenge after every member submits. A future participant snapshot/close
policy must preserve the accuracy-first ranking and zero-spoiler boundary.

### Fellowship

```text
lobby --owner start--> question_open
question_open --owner reveal--> question_revealed
question_revealed --owner advance--> question_open (next question)
question_revealed --owner advance on last--> finalizing --> completed
finalizing --owner retry--> completed
completed --repeat advance--> completed
```

Allowed writes are callable-only. Members answer only in the current
`question_open`; after reveal, the answer set is immutable. Host-only reveal,
advance, and finalization are checked both in Flutter and in Functions.

## 13. Timing model

Do not conflate these clocks:

### Join window

The normal default is 10 minutes from group creation. It controls code/group
availability for joining and owner ability to create/start a challenge. The
current UI offers only 10 minutes, and the backend accepts only integer values
from 1 through 10 (omitted defaults to 10). An expired code is rejected by
the Function. This duration is not the time allowed to finish a quiz.

### Competitive question timer

Product decision: 30 seconds per question. The current Competitive screen
implements a local countdown anchored to the challenge's `startedAt`,
auto-advances on expiry, and leaves timed-out positions unanswered. Timer
bookkeeping is in memory and does not write once per second. The server ranks
using `startedAt` to server-now elapsed time, but does not yet enforce a
separate per-question deadline because it stores no per-player
`questionStartedAt`; server-side deadline/normalization remains future
hardening.

### Fellowship pacing

Fellowship has no automatic 30-second hard timeout. It remains open until the
host reveals, then the host advances after discussion. `responseTimeSeconds`
is recorded for audit/ranking data but does not force progression.

### Server timing

Client elapsed values are untrusted. Competitive multi-question official time
comes from server `startedAt` and server now, clamped to a bounded range. The
Fellowship response-time value is derived from server `currentQuestionOpenedAt`.
Do not add client-provided timestamps to leaderboard authority.

## 14. Testing

### Flutter

Run from the repository root:

```powershell
flutter analyze
flutter test
```

The widget suite covers splash/menu navigation, answer confirmation and
feedback, timers, Group Challenge mode/count UI, owner/challenge model parsing,
Competitive timeout behavior, authoritative mode routing, host challenge
deletion, and the Fellowship finalizing presentation. The current run passes
38 Flutter tests, and
`flutter analyze` reports no issues.

### Cloud Function contract tests

```powershell
node functions/test_functions.js
node functions/test_rules.js
node functions/test_data_model.js
```

`test_functions.js` is a pure Node contract/logic suite; its current result is
20 tests, including owner self-join stability, frozen participant-roster
validation, mode/count validation, code generation, accuracy-first ranking,
catalogue permutation, and server-time logic.
`test_rules.js` parses the rules and checks 10 deny/read invariants. It is a
structural test, not a replacement for the Firestore Rules emulator.
`test_data_model.js` uses Admin SDK data patterns and reports 7 integration
checks; it verifies group/code data, competitive and Fellowship shapes,
expiry, validation, and collision behavior. It does not simulate client rules.

### Real authenticated client-path integration

```powershell
node functions/test_client_integration.js
```

This suite obtains real Firebase Auth ID tokens, calls deployed HTTPS
callables, reads Firestore through the client REST boundary, and cleans up
temporary users/documents. The latest live run covered the ten-minute default,
rejection of a 30-minute create request, host plus two members, 6-digit
joining, pre-start submission rejection, server-overridden elapsed time,
Fellowship immutability/reveal/late-answer rejection, full 10-question
finalization including a 0/10 member, retry/idempotency, host-only recursive
challenge deletion, and 403 checks for private answers, client leaderboard/
challenge writes, direct Fellowship writes, and peer-answer reads.

The real suite requires an authenticated Firebase CLI environment and deployed
Functions. Never commit its temporary credentials. If a test claims to use an
emulator, confirm Java and emulator configuration first; the checked-in rules
contract explicitly falls back to source inspection when the emulator is not
available.

### Required regression matrix

Before declaring a Group Challenge release complete, exercise:

* Owner automatically exists as the sole initial owner/member; self-join does
  not add, downgrade, or reset the owner.
* Member B and Member C join with the six-digit code; exactly three members
  remain.
* Both visible mode cards create the requested backend mode and route to the
  authoritative mode screen.
* Competitive ranking remains accuracy first, speed second; timeout behavior
  is covered by the Flutter 30-second timer regression.
* Fellowship stays open until reveal, blocks duplicate/late answers, and
  finalizes/retries safely without a Competitive timer.
* Join expiry rejects expired codes and active-challenge late joining is
  prevented by the frozen participant UID snapshot.
* Existing security/rules and real client-path tests remain green.

## 15. Deployment

The Firebase project is selected by `.firebaserc` and is
`faith-quiz-app-119653`. The normal end-to-end setup command is:

```powershell
powershell -ExecutionPolicy Bypass -File tool\setup_cloud_play.ps1
```

The script verifies Firebase CLI availability and login, selects the project,
deploys Functions and Firestore rules, uploads/verifies the 500-question
public/private catalogue, and publishes/verifies Remote Config values:

```text
cloud_challenges_enabled = true
active_cloud_catalogue    = faith-quiz-global-v1
```

For a Functions/rules-only rollout:

```powershell
firebase login
firebase use faith-quiz-app-119653
firebase deploy --only functions,firestore:rules
```

For catalogue validation/upload or Remote Config verification, use the
checked-in scripts under `tool/` and keep the Remote Config gate off until the
server grading path is deployed and verified. The setup script intentionally
uses the active Firebase CLI login rather than writing a service-account key.

The Flutter release build and Firebase deployment are separate operations. A
release APK should be built and smoke-tested after backend changes; do not
assume a successful Firebase deploy proves the physical-device UI flow.

## 16. Current implementation status

This status reflects the audited baseline. Update it in the same change as
substantial code or product-flow work.

### Complete

* Flutter app, Firebase project wiring, anonymous Auth, Firestore persistence,
  Functions region, and Remote Config gate are present.
* 500-question cloud catalogue generation, validation, public/private upload,
  and Remote Config setup tooling are present.
* Server-side answer grading, protected answer keys, cumulative leaderboard,
  usernames, and idempotent modern online submissions are present.
* Group creation creates a six-digit code and an initial owner membership;
  active code collision handling is transactional.
* Group challenge mode/count validation, server question selection, lobby,
  mode-specific routing, server-authorized host operations, and Fellowship
  reveal/advance/finalization state handling are present.
* Hosts can delete/revoke challenges through a callable that recursively
  removes the challenge subtree from Firestore; members cannot invoke it.
* Firestore rules deny client writes to groups, challenges, answers, and
  leaderboards; the preceding real client integration run verified the main
  403 boundaries.
* Prior stabilization added canonical owner/challenge identity parsing and a
  visible Fellowship `finalizing` state with host retry controls.

### Implemented but needs manual device validation

* Host/member Google sign-in, group creation/join, mode selection, lobby start,
  Competitive result review, Fellowship reveal/discussion, finalization, and
  reconnect-by-reopening-group should be tested on a real Android phone.
* Verify real Firebase snapshots update on two or more devices and that host
  controls never appear functionally for a member.
* Verify Android release App Check/provider behavior for the distribution
  channel; callable enforcement remains intentionally off today.

### Known gaps / planned future work

* New group creation rejects join-window values outside 1–10 minutes; owners
  can extend an active group explicitly through `extendGroup`.
* The Competitive countdown is client-enforced today. Add a server-enforced
  per-player timing contract if tamper-proof per-question deadlines become a
  requirement.
* Persist or otherwise safely restore Group Challenge navigation and answer
  state so background/close/reopen returns to the authoritative screen without
  starting a duplicate or resetting to Question 1.
* Render a full member list with an explicit host badge; current lobby copy
  identifies host/member and waiting state but does not show every roster row.
* Remove or isolate legacy long-ID presentation and obsolete single-question
  UI only after confirming no production callers depend on compatibility
  fields/methods.
* Add a server idempotency key for challenge creation to protect against
  arbitrary duplicate callable requests.

## 17. Known decisions

* Online and Group Challenges draw from the same canonical cloud catalogue.
* The server owns correct answers and all official scores.
* Competitive ranking is accuracy first, then official time.
* Competitive and Fellowship are distinct user experiences and state machines.
* Fellowship is host-paced and does not use the Competitive 30-second timer.
* Six-digit codes are user-facing; long Firestore IDs remain internal.
* Group creator is automatically the owner/member and does not need to join
  their own group.
* A user entering their own code must be idempotent and must never downgrade
  owner to member or reset `joinedAt`.
* Join-window expiry is distinct from challenge/question duration.
* Firestore clients read server results but do not write leaderboard entries,
  challenge state, or answer keys.
* App Check is not enforced for the current sideloaded release channel;
  revisit it when the distribution/attestation channel changes.
* Private cloud progress backup is optional and never part of public scoring.

## 18. Future agent instructions

Before changing a major feature:

1. Read this document completely.
2. Inspect the actual current source, exports, rules, tests, and working tree.
3. Verify schema compatibility, especially `ownerUid`/`ownerId` and
   `createdBy`/legacy owner fields.
4. Preserve deployed behavior and security boundaries; do not infer authority
   from Flutter visibility or stale documentation.
5. Run the relevant Flutter, Function, rules, and real client-path tests.
6. Update `FAITH_QUIZ_IMPLEMENTATION.md` after every substantial
   architecture/product-flow change, including new states, fields, exports,
   timers, or deployment behavior.

Do not add a second Group Challenge service or silently rebuild the feature.
Trace the callable, Firestore path, parser, stream, navigation, and tests
before editing. Keep compatibility fields until live data is accounted for.

## Implementation history

The meaningful architecture evolution is:

* The original Android/Kotlin quiz established the 30-level local Journey and
  topic content.
* The product migrated to Flutter while retaining the local catalog and
  adding resumable local progress, review, daily challenge, and settings.
* Cloud Challenge added a curated 500-question Prophet/Witness catalogue,
  protected answer keys, server grading, usernames, and a cumulative global
  leaderboard.
* Group Challenge evolved from a single-question/long-ID shape to six-digit
  codes and multi-question challenges with 10/20/30 question counts.
* Competitive and Fellowship modes were introduced as separate experiences.
* Backend hardening added strict mode/count validation, atomic code collision
  handling, server-authoritative elapsed time, pre-start submission blocking,
  Fellowship answer immutability/race protection, finalization recovery, and
  real authenticated client-path security tests.
* Flutter stabilization added canonical owner/challenge identity fallbacks and
  explicit Fellowship finalizing/retry presentation.

This history is a concise architecture record, not a substitute for inspecting
the current source or running the current tests.
