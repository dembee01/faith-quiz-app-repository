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

// 2. Mode Validation Test
function validateMode(mode) {
  const allowedModes = ['competitive', 'fellowship'];
  const resolved = typeof mode === 'string' && allowedModes.includes(mode.toLowerCase())
    ? mode.toLowerCase()
    : (mode === undefined ? 'competitive' : null);
  if (!resolved) {
    throw new Error('invalid-argument: Mode must be either "competitive" or "fellowship".');
  }
  return resolved;
}

console.log('[Test 2] Mode validation');
assert.strictEqual(validateMode('competitive'), 'competitive');
assert.strictEqual(validateMode('fellowship'), 'fellowship');
assert.strictEqual(validateMode(undefined), 'competitive');
for (const invalid of ['arcade', 'battle_royale', '', 123, null]) {
  assert.throws(() => validateMode(invalid), /invalid-argument/);
}
console.log('  PASSED: Only "competitive" and "fellowship" accepted.');

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
  { name: 'Player A', score: 9, elapsedSeconds: 168 }, // 9/10, 2:48
  { name: 'Player B', score: 9, elapsedSeconds: 195 }, // 9/10, 3:15
  { name: 'Player C', score: 8, elapsedSeconds: 100 }, // 8/10, 1:40 (faster than A and B, but lower score!)
  { name: 'Player D', score: 10, elapsedSeconds: 240 }, // 10/10, 4:00 (slowest, but perfect score!)
];

players.sort((a, b) => {
  const byScore = b.score - a.score; // Score DESC
  if (byScore !== 0) return byScore;
  return a.elapsedSeconds - b.elapsedSeconds; // Time ASC
});

assert.strictEqual(players[0].name, 'Player D', 'Perfect score (10) must rank 1st regardless of time');
assert.strictEqual(players[1].name, 'Player A', 'Player A (9/10, 2:48) must rank 2nd');
assert.strictEqual(players[2].name, 'Player B', 'Player B (9/10, 3:15) must rank 3rd');
assert.strictEqual(players[3].name, 'Player C', 'Player C (8/10) must rank last even though fastest');
console.log('  PASSED: Bible knowledge accuracy takes absolute precedence over speed. Time breaks ties deterministically.');

// 5. Coprime Permutation Distribution & Integrity
console.log('[Test 5] Coprime Question Shuffling for Catalogue');
function shuffledIndex(step, epochDay, totalQuestions = 500) {
  return Math.abs((epochDay + step) * 263) % totalQuestions;
}

const seen = new Set();
const epoch = 20500;
for (let step = 0; step < 500; step++) {
  const idx = shuffledIndex(step, epoch, 500);
  assert(idx >= 0 && idx < 500, 'Index within bounds');
  seen.add(idx);
}
assert.strictEqual(seen.size, 500, 'Every index 0-499 must be reached exactly once without collision');
console.log('  PASSED: Coprime stride (263) generates collision-free permutation across entire catalogue.');

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

// Invalid transitions that must be blocked:
assert.strictEqual(canTransition('lobby', 'question_revealed'), false, 'Cannot reveal from lobby');
assert.strictEqual(canTransition('lobby', 'completed'), false, 'Cannot complete from lobby');
assert.strictEqual(canTransition('question_open', 'question_open'), false, 'Cannot reopen open question');
assert.strictEqual(canTransition('question_open', 'completed'), false, 'Cannot complete unrevealed question');
assert.strictEqual(canTransition('completed', 'question_open'), false, 'Cannot reopen completed challenge');
console.log('  PASSED: State machine guards prevent invalid or out-of-order game transitions.');

console.log('\n--- ALL BACKEND UNIT & LOGIC TESTS PASSED SUCCESSFULLY! ---');
