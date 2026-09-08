# Challenge hardening audit — 2026-09-08

The three reported extension defects were confirmed in the previous implementation.
Extensions now accept omitted minutes or the number 10 only, calculate expiry in
a Firestore transaction, and require both owner membership and canonical group
ownership (with the legacy ownerId fallback).

## Additional confirmed defects and fixes

- An expired invitation code could be reassigned to another group. Extending the
  original group now reserves a fresh code atomically without touching the new
  owner's invitation.
- The Flutter extension handler could add another ten minutes after already
  receiving the server update. It now displays only the streamed server expiry.
- Create/start/reveal/advance controls now use the same canonical ownership check.
- Online submissions could alternate correct and wrong answers to inflate a score.
  The first ranked attempt is now immutable, including its time and correctness.
  Retries return the stored outcome. Existing historical scores are not rewritten.
- Competitive and legacy retries used truthiness to read stored scores, so a
  stored zero could be reported as the new attempt's score. Zero is now preserved.
  Competitive retries omit a new answer breakdown rather than presenting new
  answers beside the original immutable score.
- Deletion now marks the challenge as deleting before recursive removal. Both
  competitive submission and Fellowship result publication recheck the challenge
  transactionally, preventing in-flight writers from restoring deleted results.
- Missing answer keys now fail grading/reveal rather than silently treating A as
  correct. Counts and Fellowship answer indexes no longer accept coerced strings.
- Deleted challenge snapshots now reach the UI. Member lobbies, Competitive play,
  and Fellowship play show a deletion message and a back-to-group action.

## Deletion policy

The existing user-requested policy is retained: the host can permanently delete
any challenge, including active and completed challenges, after confirmation.
Its answers and leaderboard entries are removed; the group remains. A failed
cleanup can be retried because deletion is idempotent. This change does not
introduce cancellation/archive semantics.

## Verification

- `node --test functions/test_hardening.js` executes the actual exported handlers
  using an optimistic transactional store, including concurrent extensions,
  stale owner roles, invitation-code reuse, immutable scores, missing keys, and
  deletion during Competitive grading and Fellowship finalization.
- `flutter test` includes three member-screen deletion regression tests.
- `node functions/test_client_integration.js` verifies deployed handlers with
  authenticated clients, concurrent extensions and rejection cases, stale owner
  roles, immutable online attempts, and post-deletion submission rejection.
- `flutter analyze`, existing backend contract checks, and rule contract checks
  remain part of verification. Rule contract checks are structural checks; the
  authenticated integration test exercises deployed Firestore rules.

## GitHub

At inspection, master was unprotected. The latest commit did have one check:
the legacy Android workflow had failed. The claim that it had no checks was
therefore outdated at the time of this audit.

The replacement workflow runs Flutter analysis/tests and Firebase handler/rule
checks on pushes and pull requests, without production credentials.
`node tool/check_github_quality.js` reports actual remote state.
`node tool/check_github_quality.js --protect` requires the two checks only after
they have passed on master; it enforces checks for administrators too and blocks
force pushes/deletion. Existing branch protection is retained if already present.

## Limits of this audit

This is a targeted authorization, validation, concurrency, replay, and deletion
audit, not proof that every possible app defect is absent. The existing
Competitive per-question countdown remains client-side; the official group
elapsed time uses the server start time. Online answer elapsed time remains
client-reported. These checks do not establish tamper-proof per-question timing.
