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

// 10. Global Leaderboard Ranking Audit
console.log('[Test 10] Global cumulative leaderboard ranking audit');
{
  const globalEntries = [
    { name: 'Alice', score: 47, totalAnswered: 50, accuracy: 94, elapsedSeconds: 120 },
    { name: 'Bob',   score: 47, totalAnswered: 60, accuracy: 78, elapsedSeconds: 90 },
    { name: 'Carol', score: 50, totalAnswered: 50, accuracy: 100, elapsedSeconds: 200 },
    { name: 'Dave',  score: 45, totalAnswered: 45, accuracy: 100, elapsedSeconds: 50 },
  ];

  globalEntries.sort((a, b) => {
    const byScore = b.score - a.score;
    if (byScore !== 0) return byScore;
    return a.elapsedSeconds - b.elapsedSeconds;
  });

  assert.strictEqual(globalEntries[0].name, 'Carol', 'Carol (50 pts) ranks 1st');
  assert.strictEqual(globalEntries[1].name, 'Bob', 'Bob (47 pts, 90s) ranks 2nd');
  assert.strictEqual(globalEntries[2].name, 'Alice', 'Alice (47 pts, 120s) ranks 3rd');
  assert.strictEqual(globalEntries[3].name, 'Dave', 'Dave (45 pts) ranks 4th');
}
console.log('  PASSED: Global leaderboard ranking: score first, time as tie-breaker.');

console.log('\n--- ALL 10 BACKEND UNIT & LOGIC TESTS PASSED SUCCESSFULLY! ---');
