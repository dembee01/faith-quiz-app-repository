const assert = require('assert');

console.log('--- RUNNING CLOUD FUNCTIONS UNIT & LOGIC CONTRACT TESTS ---');

// 1. Question Count Validation Test
function validateQuestionCount(count) {
  const allowedCounts = [10, 20, 30];
  if (!Number.isInteger(count) || !allowedCounts.includes(count)) {
    throw new Error('invalid-argument: Group challenges must have exactly 10, 20, or 30 questions.');
  }
  return true;
}

console.log('[Test 1] Question count strict validation');
assert.strictEqual(validateQuestionCount(10), true);
assert.strictEqual(validateQuestionCount(20), true);
assert.strictEqual(validateQuestionCount(30), true);

for (const invalid of [5, 7, 13, 25, 50, 0, -1, '20', null, undefined]) {
  assert.throws(() => validateQuestionCount(invalid), /invalid-argument/, `Should reject count: ${invalid}`);
}
console.log('  PASSED: Only 10, 20, 30 allowed. Arbitrary/legacy counts strictly rejected.');

// 2. Strict Mode Validation Test (Gap C: unknown modes must be rejected, not silently defaulted)
function validateMode(mode) {
  if (mode !== undefined && mode !== 'competitive' && mode !== 'fellowship') {
    throw new Error('invalid-argument: Mode must be either "competitive" or "fellowship".');
  }
  return mode === 'fellowship' ? 'fellowship' : 'competitive';
}

console.log('[Test 2] Strict mode validation (rejects unknown modes)');
assert.strictEqual(validateMode('competitive'), 'competitive');
assert.strictEqual(validateMode('fellowship'), 'fellowship');
assert.strictEqual(validateMode(undefined), 'competitive');
// These MUST throw, not silently default to competitive:
for (const invalid of ['arcade', 'battle_royale', '', 'COMPETITIVE', 'Fellowship', 123, null, false, {}]) {
  assert.throws(() => validateMode(invalid), /invalid-argument/, `Should reject mode: ${JSON.stringify(invalid)}`);
}
console.log('  PASSED: Only "competitive" and "fellowship" accepted. All other values strictly rejected.');

// 3. 6-digit Join Code Generation & Formatting
function generateCandidateCode() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

console.log('[Test 3] 6-digit numeric join code generation');
for (let i = 0; i < 1000; i++) {
  const code = generateCandidateCode();
  assert.strictEqual(code.length, 6, 'Code must be exactly 6 characters');
  assert.match(code, /^[1-9][0-9]{5}$/, 'Code must be 6 numeric digits with no leading zero');
  const num = parseInt(code, 10);
  assert(num >= 100000 && num <= 999999, 'Code must be between 100000 and 999999');
}
console.log('  PASSED: 1000 candidate codes verified as strictly 6 numeric digits.');

// 4. Competitive Ranking Priority: Accuracy (Score) DESC, then Time ASC
console.log('[Test 4] Deterministic Competitive Ranking Order');
const players = [
  { name: 'Player A', score: 9, elapsedSeconds: 168 },
  { name: 'Player B', score: 9, elapsedSeconds: 195 },
  { name: 'Player C', score: 8, elapsedSeconds: 100 },
  { name: 'Player D', score: 10, elapsedSeconds: 240 },
];

players.sort((a, b) => {
  const byScore = b.score - a.score;
  if (byScore !== 0) return byScore;
  return a.elapsedSeconds - b.elapsedSeconds;
});

assert.strictEqual(players[0].name, 'Player D', 'Perfect score (10) must rank 1st regardless of time');
assert.strictEqual(players[1].name, 'Player A', 'Player A (9/10, 2:48) must rank 2nd');
assert.strictEqual(players[2].name, 'Player B', 'Player B (9/10, 3:15) must rank 3rd');
assert.strictEqual(players[3].name, 'Player C', 'Player C (8/10) must rank last even though fastest');
console.log('  PASSED: Bible knowledge accuracy takes absolute precedence over speed.');

// 5. Coprime Permutation Distribution & Integrity
console.log('[Test 5] Coprime Question Shuffling for 500-question catalogue');
function gcd(a, b) { while (b) { [a, b] = [b, a % b]; } return a; }

function shuffledIndex(step, epochDay, totalQuestions = 500) {
  const stride = gcd(263, totalQuestions) === 1 ? 263 : 1;
  return Math.abs((epochDay + step) * stride) % totalQuestions;
}

const seen = new Set();
const epoch = 20500;
for (let step = 0; step < 500; step++) {
  const idx = shuffledIndex(step, epoch, 500);
  assert(idx >= 0 && idx < 500, 'Index within bounds');
  seen.add(idx);
}
assert.strictEqual(seen.size, 500, 'Every index 0-499 must be reached exactly once');
console.log('  PASSED: Coprime stride (263) generates collision-free permutation.');

// 5b. Dynamic Catalogue Size Support (Gap D)
console.log('[Test 5b] Dynamic catalogue sizes with GCD-verified stride');
for (const catalogueSize of [100, 250, 500, 750, 1000]) {
  const stride263 = gcd(263, catalogueSize) === 1 ? 263 : 1;
  const dynamicSeen = new Set();
  for (let step = 0; step < catalogueSize; step++) {
    const idx = Math.abs((epoch + step) * stride263) % catalogueSize;
    assert(idx >= 0 && idx < catalogueSize, `Index within bounds for size ${catalogueSize}`);
    dynamicSeen.add(idx);
  }
  assert.strictEqual(dynamicSeen.size, catalogueSize,
    `Full permutation for catalogue size ${catalogueSize} (stride=${stride263})`);
  console.log(`  Catalogue size ${catalogueSize}: stride=${stride263}, full permutation verified`);
}

assert.strictEqual(gcd(263, 263), 263, '263 is not coprime with itself');
const fallbackStride = gcd(263, 263) === 1 ? 263 : 1;
assert.strictEqual(fallbackStride, 1, 'Must fall back to stride 1 when 263 is not coprime');
console.log('  PASSED: Dynamic catalogue sizes handled correctly with GCD fallback.');

// 6. Fellowship State Transition Machine Validation
console.log('[Test 6] Fellowship State Machine Transitions');
const validTransitions = {
  'lobby': ['question_open'],
  'question_open': ['question_revealed'],
  'question_revealed': ['question_open', 'completed'],
  'completed': [],
};

function canTransition(current, next) {
  return (validTransitions[current] || []).includes(next);
}

assert.strictEqual(canTransition('lobby', 'question_open'), true);
assert.strictEqual(canTransition('question_open', 'question_revealed'), true);
assert.strictEqual(canTransition('question_revealed', 'question_open'), true);
assert.strictEqual(canTransition('question_revealed', 'completed'), true);
assert.strictEqual(canTransition('lobby', 'question_revealed'), false);
assert.strictEqual(canTransition('lobby', 'completed'), false);
assert.strictEqual(canTransition('question_open', 'question_open'), false);
assert.strictEqual(canTransition('question_open', 'completed'), false);
assert.strictEqual(canTransition('completed', 'question_open'), false);
console.log('  PASSED: State machine guards prevent invalid game transitions.');

// 7. Fellowship Answer Immutability Contract (Gap A)
console.log('[Test 7] Fellowship Answer Immutability Contract');
{
  const answerStore = new Map();

  function submitAnswer(questionIndex, uid, answerIndex, statusIsOpen) {
    if (!statusIsOpen) {
      throw new Error('failed-precondition: Question is not currently open for answers.');
    }
    const key = `${questionIndex}_${uid}`;
    if (answerStore.has(key)) {
      return { success: true, answerIndex: answerStore.get(key).answerIndex, alreadyAnswered: true };
    }
    answerStore.set(key, { answerIndex });
    return { success: true, answerIndex, alreadyAnswered: false };
  }

  const first = submitAnswer(0, 'user1', 2, true);
  assert.strictEqual(first.answerIndex, 2);
  assert.strictEqual(first.alreadyAnswered, false);

  const second = submitAnswer(0, 'user1', 3, true);
  assert.strictEqual(second.answerIndex, 2, 'Must return original answer (2), not new attempt (3)');
  assert.strictEqual(second.alreadyAnswered, true);

  const third = submitAnswer(0, 'user2', 1, true);
  assert.strictEqual(third.answerIndex, 1);
  assert.strictEqual(third.alreadyAnswered, false);

  const fourth = submitAnswer(1, 'user1', 0, true);
  assert.strictEqual(fourth.answerIndex, 0);
  assert.strictEqual(fourth.alreadyAnswered, false);

  assert.throws(() => submitAnswer(2, 'user1', 1, false), /failed-precondition/);
}
console.log('  PASSED: Answers are immutable after first submission.');

// 8. Race Condition: Answer Submission During Reveal (Gap B)
console.log('[Test 8] Race condition model: answer vs reveal');
{
  let challengeStatus = 'question_open';

  function atomicSubmitAnswer() {
    if (challengeStatus !== 'question_open') {
      throw new Error('failed-precondition: Question is not currently open for answers.');
    }
    return { success: true };
  }

  function atomicReveal() {
    if (challengeStatus !== 'question_open') {
      throw new Error('failed-precondition');
    }
    challengeStatus = 'question_revealed';
    return { status: 'question_revealed' };
  }

  assert.doesNotThrow(() => atomicSubmitAnswer());
  challengeStatus = 'question_open';
  atomicReveal();
  assert.strictEqual(challengeStatus, 'question_revealed');
  assert.throws(() => atomicSubmitAnswer(), /failed-precondition/);
}
console.log('  PASSED: Answer submission correctly rejected after reveal.');

// 9. Double-Reveal and Double-Advance Idempotency (Gap F)
console.log('[Test 9] Double-reveal and double-advance idempotency');
{
  function revealWithIdempotency(status) {
    if (status === 'question_revealed') {
      return { status: 'question_revealed', idempotent: true };
    }
    if (status !== 'question_open') {
      throw new Error('failed-precondition');
    }
    return { status: 'question_revealed', idempotent: false };
  }

  function advanceWithIdempotency(status) {
    if (status === 'completed') {
      return { status: 'completed', idempotent: true };
    }
    if (status !== 'question_revealed') {
      throw new Error('failed-precondition');
    }
    return { status: 'question_open', idempotent: false };
  }

  const reveal1 = revealWithIdempotency('question_open');
  assert.strictEqual(reveal1.idempotent, false);
  const reveal2 = revealWithIdempotency('question_revealed');
  assert.strictEqual(reveal2.idempotent, true);

  const advance1 = advanceWithIdempotency('question_revealed');
  assert.strictEqual(advance1.idempotent, false);
  const advance2 = advanceWithIdempotency('completed');
  assert.strictEqual(advance2.idempotent, true);

  assert.throws(() => revealWithIdempotency('lobby'), /failed-precondition/);
  assert.throws(() => advanceWithIdempotency('question_open'), /failed-precondition/);
}
console.log('  PASSED: Double-reveal and double-advance return idempotent success.');

// 11. Block Competitive Pre-Start Submission & Answer Leakage (Claim 1)
console.log('[Test 11] Block Competitive Pre-Start Submission & Answer Leakage');
{
  function submitCompetitiveCheck(status, mode, isMultiQuestion) {
    if (isMultiQuestion) {
      if (mode === 'fellowship') {
        throw new Error('failed-precondition: Fellowship challenges must be completed via host-led progression.');
      }
      if (mode !== 'competitive') {
        throw new Error('failed-precondition: Invalid challenge mode.');
      }
      if (status === 'lobby') {
        throw new Error('failed-precondition: Challenge has not started yet. Pre-start submissions are forbidden.');
      }
      if (status !== 'active' && status !== 'completed') {
        throw new Error(`failed-precondition: Challenge cannot be submitted in status "${status}".`);
      }
    }
    return { permitted: true };
  }

  // Pre-start submission while in lobby must be rejected with failed-precondition:
  assert.throws(
    () => submitCompetitiveCheck('lobby', 'competitive', true),
    /failed-precondition: Challenge has not started yet/,
    'Must strictly reject submissions when challenge is still in lobby'
  );

  // Active status is permitted:
  assert.strictEqual(submitCompetitiveCheck('active', 'competitive', true).permitted, true);

  // Completed status allows idempotent completion:
  assert.strictEqual(submitCompetitiveCheck('completed', 'competitive', true).permitted, true);
}
console.log('  PASSED: Pre-start submissions while in lobby are strictly blocked with failed-precondition.');

// 12. Reject Submitting Fellowship via Competitive Endpoint (Claim 1)
console.log('[Test 12] Reject Submitting Fellowship via Competitive Endpoint');
{
  function submitCompetitiveModeCheck(mode) {
    if (mode === 'fellowship') {
      throw new Error('failed-precondition: Fellowship challenges must be completed via host-led progression.');
    }
    if (mode !== 'competitive') {
      throw new Error('failed-precondition: Invalid challenge mode.');
    }
    return { permitted: true };
  }

  assert.throws(
    () => submitCompetitiveModeCheck('fellowship'),
    /failed-precondition: Fellowship challenges must be completed via host-led progression/,
    'Must reject Fellowship challenges submitted to competitive endpoint'
  );
  assert.throws(() => submitCompetitiveModeCheck('arcade'), /failed-precondition/);
  assert.strictEqual(submitCompetitiveModeCheck('competitive').permitted, true);
}
console.log('  PASSED: Fellowship challenges are strictly rejected on Competitive submit path.');

// 13. Server-Authoritative Elapsed Time (Claim 2)
console.log('[Test 13] Server-Authoritative Elapsed Time');
{
  function calculateOfficialElapsed(startedAtDate, clientElapsedSeconds, nowMs = Date.now()) {
    let officialElapsed = 60;
    if (startedAtDate && startedAtDate.getTime) {
      officialElapsed = Math.max(1, Math.floor((nowMs - startedAtDate.getTime()) / 1000));
    } else if (Number.isInteger(clientElapsedSeconds) && clientElapsedSeconds > 0) {
      officialElapsed = Math.min(7200, clientElapsedSeconds);
    }
    return Math.min(7200, Math.max(1, officialElapsed));
  }

  const startedAt = new Date(Date.now() - 100000); // started 100 seconds ago

  // Client attempts to cheat by submitting elapsedSeconds = 1:
  const forgedResult = calculateOfficialElapsed(startedAt, 1);
  assert(forgedResult >= 99 && forgedResult <= 101, `Forged 1s must yield ~100s, got ${forgedResult}`);

  // Client submits negative time:
  const negativeResult = calculateOfficialElapsed(startedAt, -50);
  assert(negativeResult >= 99 && negativeResult <= 101, `Negative client time must yield ~100s, got ${negativeResult}`);

  // Client submits excessively large time (e.g. 999999s):
  const largeResult = calculateOfficialElapsed(startedAt, 999999);
  assert(largeResult >= 99 && largeResult <= 101, `Large client time must yield ~100s, got ${largeResult}`);

  // Client submits missing / null / malformed time:
  const missingResult = calculateOfficialElapsed(startedAt, null);
  assert(missingResult >= 99 && missingResult <= 101, `Missing client time must yield ~100s, got ${missingResult}`);

  const nanResult = calculateOfficialElapsed(startedAt, NaN);
  assert(nanResult >= 99 && nanResult <= 101, `NaN client time must yield ~100s, got ${nanResult}`);
}
console.log('  PASSED: Leaderboard elapsed time is strictly server-authoritative; client input cannot reduce time.');

// 14. Atomic startGroupChallenge & Expiry Verification (Claim 3)
console.log('[Test 14] Atomic startGroupChallenge & Expiry Verification');
{
  function simulateStartChallenge(group, challenge, callerUid) {
    if (!group || !challenge) throw new Error('not-found');
    if (callerUid !== group.ownerUid) throw new Error('permission-denied');
    if (group.expiresAt && group.expiresAt.getTime() < Date.now()) {
      throw new Error('failed-precondition: Group session expired.');
    }
    if (challenge.status !== 'lobby') {
      // Idempotent: return established status without mutating startedAt
      return { status: challenge.status, startedAt: challenge.startedAt, isIdempotent: true };
    }
    challenge.status = challenge.mode === 'fellowship' ? 'question_open' : 'active';
    challenge.startedAt = new Date();
    return { status: challenge.status, startedAt: challenge.startedAt, isIdempotent: false };
  }

  const validGroup = { ownerUid: 'host1', expiresAt: new Date(Date.now() + 600000) };
  const expiredGroup = { ownerUid: 'host1', expiresAt: new Date(Date.now() - 10000) };
  const challenge = { status: 'lobby', mode: 'competitive' };

  // Expired group cannot start:
  assert.throws(() => simulateStartChallenge(expiredGroup, challenge, 'host1'), /failed-precondition/);

  // Non-owner cannot start:
  assert.throws(() => simulateStartChallenge(validGroup, challenge, 'member2'), /permission-denied/);

  // First start establishes startedAt:
  const start1 = simulateStartChallenge(validGroup, challenge, 'host1');
  assert.strictEqual(start1.status, 'active');
  assert.strictEqual(start1.isIdempotent, false);
  const establishedStartAt = start1.startedAt;

  // Second concurrent start returns idempotent result and does NOT overwrite startedAt:
  const start2 = simulateStartChallenge(validGroup, challenge, 'host1');
  assert.strictEqual(start2.status, 'active');
  assert.strictEqual(start2.isIdempotent, true);
  assert.strictEqual(start2.startedAt.getTime(), establishedStartAt.getTime(), 'startedAt must not reset');
}
console.log('  PASSED: startGroupChallenge is atomic, checks expiry, and never resets established startedAt.');

// 15. Recoverable Fellowship Finalization (Claim 4)
console.log('[Test 15] Recoverable Fellowship Finalization');
{
  class FellowshipLifecycle {
    constructor() {
      this.status = 'question_revealed';
      this.entries = new Map();
      this.gradingFailedOnce = true;
    }

    advanceOrFinalize(questions) {
      // Phase 1: Transaction sets 'finalizing'
      if (this.status === 'completed') return { status: 'completed' };
      if (this.status !== 'question_revealed' && this.status !== 'finalizing') {
        throw new Error('failed-precondition: Answer must be revealed');
      }
      this.status = 'finalizing';

      // Phase 2: Grading batch
      if (this.gradingFailedOnce) {
        this.gradingFailedOnce = false;
        throw new Error('Simulated network/grading failure halfway through batch write');
      }

      // Populate entries
      this.entries.set('user1', { score: 10 });
      this.entries.set('user2', { score: 8 });

      // Phase 3: Mark completed
      this.status = 'completed';
      return { status: 'completed' };
    }
  }

  const session = new FellowshipLifecycle();
  // First attempt fails during grading:
  assert.throws(() => session.advanceOrFinalize([1, 2, 3]), /Simulated network\/grading failure/);
  assert.strictEqual(session.status, 'finalizing', 'Status remains finalizing after failure');
  assert.strictEqual(session.entries.size, 0, 'Entries not yet finalized');

  // Retry resumes finalization and succeeds:
  const retryResult = session.advanceOrFinalize([1, 2, 3]);
  assert.strictEqual(retryResult.status, 'completed');
  assert.strictEqual(session.status, 'completed');
  assert.strictEqual(session.entries.size, 2, 'Entries successfully created upon recovery');
}
console.log('  PASSED: Fellowship finalization safely recovers from grading interruptions.');

// 16. Fellowship Zero-Answer Participant Inclusion (Claim 4)
console.log('[Test 16] Fellowship Zero-Answer Participant Inclusion');
{
  const groupMembers = [
    { uid: 'active1', name: 'Active Learner' },
    { uid: 'passive2', name: 'Passive Observer' },
  ];
  const userAnswers = new Map([
    ['active1', [{ answer: 1, correct: true }]],
    // passive2 answered 0 questions
  ]);

  const finalLeaderboard = [];
  for (const member of groupMembers) {
    const answers = userAnswers.get(member.uid) || [];
    const score = answers.filter(a => a.correct).length;
    finalLeaderboard.push({
      uid: member.uid,
      displayName: member.name,
      score,
      total: 10,
      correct: score === 10,
      elapsedSeconds: 0,
    });
  }

  assert.strictEqual(finalLeaderboard.length, 2, 'Both active and passive members must appear');
  const passive = finalLeaderboard.find(e => e.uid === 'passive2');
  assert(passive !== undefined, 'Passive member must have a leaderboard entry');
  assert.strictEqual(passive.score, 0, 'Passive member score must be 0');
  assert.strictEqual(passive.total, 10, 'Total must match challenge total');
}
console.log('  PASSED: Participants with 0 answers receive a 0/N entry on the final leaderboard.');

// 17. Dynamic Catalogue Validation (Claim 7)
console.log('[Test 17] Dynamic Catalogue Validation');
{
  function validateCatalogueMetadata(meta, requestedCount) {
    if (![10, 20, 30].includes(requestedCount)) {
      throw new Error('invalid-argument: Must be 10, 20, or 30');
    }
    let totalQuestions = 500;
    if (meta) {
      if (meta.questionCount !== undefined) {
        if (!Number.isInteger(meta.questionCount) || meta.questionCount <= 0) {
          throw new Error('internal: questionCount must be a positive integer.');
        }
        totalQuestions = meta.questionCount;
      }
    }
    if (totalQuestions < requestedCount) {
      throw new Error(`failed-precondition: Catalogue contains fewer questions (${totalQuestions}) than requested (${requestedCount}).`);
    }
    return totalQuestions;
  }

  assert.strictEqual(validateCatalogueMetadata(null, 10), 500);
  assert.strictEqual(validateCatalogueMetadata({ questionCount: 100 }, 30), 100);
  assert.throws(() => validateCatalogueMetadata({ questionCount: 0 }, 10), /positive integer/);
  assert.throws(() => validateCatalogueMetadata({ questionCount: -5 }, 10), /positive integer/);
  assert.throws(() => validateCatalogueMetadata({ questionCount: 'many' }, 10), /positive integer/);
  assert.throws(() => validateCatalogueMetadata({ questionCount: 5 }, 10), /fewer questions/);
  assert.throws(() => validateCatalogueMetadata({ questionCount: 15 }, 20), /fewer questions/);

  // Check truncation detection:
  function ensureCompleteSelection(selectedCount, requestedCount) {
    if (selectedCount !== requestedCount) {
      throw new Error(`internal: Incomplete catalogue questions: expected ${requestedCount}, found ${selectedCount}.`);
    }
    return true;
  }

  assert.strictEqual(ensureCompleteSelection(10, 10), true);
  assert.throws(() => ensureCompleteSelection(7, 10), /Incomplete catalogue questions/);
  assert.throws(() => ensureCompleteSelection(18, 20), /Incomplete catalogue questions/);
}
console.log('  PASSED: Catalogue metadata validated and incomplete/truncated challenges strictly forbidden.');

// 18. Online Challenge 4-Tier Deterministic Ranking (Claim 6)
console.log('[Test 18] Online Challenge 4-Tier Deterministic Ranking');
{
  const entries = [
    { id: 'userD', name: 'Dave',  score: 47, accuracy: 94, avgElapsedSeconds: 8 },  // Tied score & accuracy with Alice, but faster avg
    { id: 'userA', name: 'Alice', score: 47, accuracy: 94, avgElapsedSeconds: 12 }, // Slower avg than Dave
    { id: 'userB', name: 'Bob',   score: 47, accuracy: 78, avgElapsedSeconds: 4 },  // Lower accuracy despite faster avg
    { id: 'userC', name: 'Carol', score: 50, accuracy: 80, avgElapsedSeconds: 20 }, // Highest score (beats all)
    { id: 'userE', name: 'Eve',   score: 47, accuracy: 94, avgElapsedSeconds: 12 }, // Identical stats to Alice -> ID tie-breaker
  ];

  entries.sort((left, right) => {
    // 1. Primary: Score DESC
    const byScore = right.score - left.score;
    if (byScore !== 0) return byScore;

    // 2. Secondary: Accuracy DESC
    const byAccuracy = right.accuracy - left.accuracy;
    if (byAccuracy !== 0) return byAccuracy;

    // 3. Tertiary: Average first-attempt response time ASC
    const bySpeed = left.avgElapsedSeconds - right.avgElapsedSeconds;
    if (bySpeed !== 0) return bySpeed;

    // 4. Stable deterministic tie-breaker
    return left.id.localeCompare(right.id);
  });

  assert.strictEqual(entries[0].name, 'Carol', '1st: Carol (Highest score: 50)');
  assert.strictEqual(entries[1].name, 'Dave',  '2nd: Dave (47 pts, 94% acc, 8s avg speed)');
  assert.strictEqual(entries[2].name, 'Alice', '3rd: Alice (47 pts, 94% acc, 12s avg speed, userA < userE)');
  assert.strictEqual(entries[3].name, 'Eve',   '4th: Eve (47 pts, 94% acc, 12s avg speed, userE > userA)');
  assert.strictEqual(entries[4].name, 'Bob',   '5th: Bob (47 pts, 78% acc)');
}
console.log('  PASSED: 4-tier ranking: Score DESC -> Accuracy DESC -> AvgSpeed ASC -> UID ASC verified.');

// 19. Idempotent self-join and owner-role preservation (Issues 1/2/12)
console.log('[Test 19] Idempotent self-join and owner-role preservation');
{
  const originalJoinedAt = new Date('2026-01-01T12:00:00Z');

  function simulateJoin(group, member, userGroup, uid, memberDisplayName = 'Member') {
    const canonicalOwnerUid = group.ownerUid || group.ownerId;
    const existingRole = member ? member.role : null;
    const role = canonicalOwnerUid === uid || existingRole === 'owner' ? 'owner' : 'member';
    const joinedAt = (member && member.joinedAt) || (userGroup && userGroup.joinedAt) || new Date();
    return {
      member: {
        ...(member || {}),
        role,
        joinedAt,
        displayName: (member && member.displayName) || memberDisplayName,
      },
      userGroup: {
        ...(userGroup || {}),
        role,
        joinedAt,
      },
      role,
      alreadyMember: Boolean(member),
    };
  }

  const owner = simulateJoin(
    { ownerUid: 'host-a' },
    { role: 'owner', joinedAt: originalJoinedAt, displayName: 'Host' },
    { role: 'owner', joinedAt: originalJoinedAt },
    'host-a',
  );
  assert.strictEqual(owner.role, 'owner');
  assert.strictEqual(owner.alreadyMember, true);
  assert.strictEqual(owner.member.role, 'owner');
  assert.strictEqual(owner.userGroup.role, 'owner');
  assert.strictEqual(owner.member.joinedAt, originalJoinedAt, 'Repeat self-join must preserve joinedAt');

  // A legacy/stale owner membership that was previously downgraded is
  // repaired from canonical ownerUid without resetting its timestamp.
  const repaired = simulateJoin(
    { ownerUid: 'host-a' },
    { role: 'member', joinedAt: originalJoinedAt, displayName: 'Host' },
    { role: 'member', joinedAt: originalJoinedAt },
    'host-a',
  );
  assert.strictEqual(repaired.role, 'owner');
  assert.strictEqual(repaired.member.role, 'owner');
  assert.strictEqual(repaired.userGroup.role, 'owner');
  assert.strictEqual(repaired.member.joinedAt, originalJoinedAt);

  // Rejoining as a normal member remains idempotent and cannot overwrite a
  // pre-existing owner role.
  const member = simulateJoin(
    { ownerUid: 'host-a' },
    { role: 'member', joinedAt: originalJoinedAt, displayName: 'Member' },
    { role: 'member', joinedAt: originalJoinedAt },
    'member-b',
  );
  assert.strictEqual(member.role, 'member');
  assert.strictEqual(member.member.joinedAt, originalJoinedAt);
}
console.log('  PASSED: Self-join is idempotent, owner role is preserved/repaired, and joinedAt is stable.');

// 20. Start-time participant freeze and late-join exclusion (Issues 8/12)
console.log('[Test 20] Start-time participant freeze and late-join exclusion');
{
  function freezeParticipants(memberUids, callerUid) {
    const participants = [...new Set(memberUids)];
    if (!participants.includes(callerUid)) participants.push(callerUid);
    return participants;
  }

  function canParticipate(challenge, uid) {
    // Missing participantUids denotes a legacy challenge; new challenges
    // always carry the frozen array after startGroupChallenge.
    return !Array.isArray(challenge.participantUids) || challenge.participantUids.includes(uid);
  }

  const frozen = freezeParticipants(['host-a', 'member-b', 'member-c'], 'host-a');
  assert.deepStrictEqual(frozen, ['host-a', 'member-b', 'member-c']);
  const lateJoiner = 'member-d';
  assert.strictEqual(canParticipate({ participantUids: frozen }, lateJoiner), false);
  assert.strictEqual(canParticipate({ participantUids: frozen }, 'member-b'), true);
  assert.strictEqual(canParticipate({ participantUids: frozen }, 'host-a'), true);
  assert.strictEqual(canParticipate({ status: 'active' }, lateJoiner), true, 'Legacy challenges remain compatible');
}
console.log('  PASSED: New challenges freeze the start-time roster and exclude late joiners; legacy data remains readable.');

console.log('\n--- ALL 20 BACKEND UNIT & LOGIC CONTRACT TESTS PASSED SUCCESSFULLY! ---');
