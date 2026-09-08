// Runs the actual exported handlers against an optimistic transaction store.
// No production credentials, duplicated business logic, or external services.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const test = require('node:test');

function fixture() {
  const records = new Map();
  const versions = new Map();
  const ref = (path) => ({
    path, id: path.split('/').at(-1),
    get: async () => snap(path),
    collection: (name) => collection(path + '/' + name),
  });
  const collection = (path) => ({
    doc: (id) => ref(path + '/' + id),
    get: async () => ({ docs: [...records.keys()]
      .filter((p) => p.startsWith(path + '/') && !p.slice(path.length + 1).includes('/'))
      .map(snap) }),
  });
  const snap = (path) => {
    const value = records.get(path);
    return { exists: value !== undefined, id: path.split('/').at(-1),
      get: (key) => value?.[key], data: () => value };
  };
  const put = (path, value) => {
    records.set(path, value);
    versions.set(path, (versions.get(path) || 0) + 1);
  };
  const db = {
    doc: ref, collection,
    getAll: async (...refs) => refs.map((r) => snap(r.path)),
    recursiveDelete: async (r) => {
      for (const path of records.keys()) {
        if (path === r.path || path.startsWith(r.path + '/')) {
          records.delete(path);
          versions.set(path, (versions.get(path) || 0) + 1);
        }
      }
    },
    async runTransaction(callback) {
      for (let attempt = 0; attempt < 20; attempt++) {
        const reads = new Map();
        const writes = [];
        const result = await callback({
          get: async (r) => {
            assert.equal(writes.length, 0, 'All reads precede writes');
            reads.set(r.path, versions.get(r.path) || 0);
            return snap(r.path);
          },
          set: (r, data, options) => writes.push([r.path, data, options?.merge]),
          update: (r, data) => {
            assert(records.has(r.path), 'Cannot update a missing document');
            writes.push([r.path, data, true]);
          },
        });
        if ([...reads].some(([path, version]) => (versions.get(path) || 0) !== version)) continue;
        for (const [path, data, merge] of writes) {
          put(path, merge ? { ...records.get(path), ...data } : data);
        }
        return result;
      }
      throw new Error('Transaction retries exhausted');
    },
  };
  class HttpsError extends Error {
    constructor(code, message) { super(message); this.code = code; }
  }
  const timestamp = (date) => ({ toDate: () => date, toMillis: () => date.getTime() });
  const modules = {
    'firebase-admin/app': { getApps: () => [{}] },
    'firebase-admin/firestore': { getFirestore: () => db,
      FieldValue: { serverTimestamp: () => timestamp(new Date()) },
      Timestamp: { fromDate: timestamp } },
    'firebase-admin/messaging': { getMessaging: () => ({}) },
    'firebase-functions/v2': { setGlobalOptions: () => {} },
    'firebase-functions/v2/https': { HttpsError, onCall: (_, handler) => handler },
    'firebase-functions/v2/scheduler': { onSchedule: (_, handler) => handler },
  };
  const exported = {};
  vm.runInNewContext(fs.readFileSync(require.resolve('./index.js'), 'utf8'), {
    require: (name) => {
      if (!modules[name]) throw new Error('Unexpected module: ' + name);
      return modules[name];
    },
    exports: exported, console, Date, Math, Map, Set,
  });
  const call = (name, data, uid = 'host') => exported[name]({ auth: { uid }, data });
  put('groups/g', { ownerUid: 'host', joinCode: '123456',
    expiresAt: timestamp(new Date(Date.now() + 600000)) });
  put('groups/g/members/host', { role: 'owner' });
  put('joinCodes/123456', { groupId: 'g' });
  put('groups/g/members/member', { role: 'member' });
  put('contentPrivate/c/answers/q', { correctAnswer: 1, challengeId: 'q' });
  return { db, put, records, call, timestamp };
}

test('extensions reject coercion, add exactly ten minutes, and preserve concurrent increments', async () => {
  const f = fixture();
  for (const additionalMinutes of [30, 60, 500, '100', '10', 5.5, -10, 0, null, false]) {
    await assert.rejects(f.call('extendGroup', { groupId: 'g', additionalMinutes }),
      { code: 'invalid-argument' });
  }
  const before = f.records.get('groups/g').expiresAt.toMillis();
  await Promise.all([
    f.call('extendGroup', { groupId: 'g', additionalMinutes: 10 }),
    f.call('extendGroup', { groupId: 'g' }),
  ]);
  for (const path of ['groups/g', 'joinCodes/123456', 'users/host/groups/g']) {
    assert.equal(f.records.get(path).expiresAt.toMillis(), before + 1200000);
  }
});

test('every host control rejects a stale owner membership with a different canonical owner', async () => {
  const f = fixture();
  f.put('groups/g/members/member', { role: 'owner' });
  for (const name of ['extendGroup', 'createGroupChallenge', 'startGroupChallenge',
    'deleteGroupChallenge', 'revealFellowshipAnswer', 'advanceFellowshipQuestion']) {
    f.put('groups/g/challenges/ch', { status: 'lobby' });
    await assert.rejects(f.call(name, { groupId: 'g', challengeId: 'ch', questionCount: 10 }, 'member'),
      { code: 'permission-denied' }, name);
  }
});

test('extension rotates a reused invitation code without modifying its new owner', async () => {
  const f = fixture();
  f.put('joinCodes/123456', { groupId: 'another-group', expiresAt: f.timestamp(new Date(Date.now() + 600000)) });
  const original = f.records.get('joinCodes/123456');
  const result = await f.call('extendGroup', { groupId: 'g' });
  assert.notEqual(result.joinCode, '123456');
  assert.equal(f.records.get('joinCodes/123456'), original);
  assert.equal(f.records.get('joinCodes/' + result.joinCode).groupId, 'g');
  assert.equal(f.records.get('groups/g').joinCode, result.joinCode);
});

test('online attempts are immutable and repeated wrong/correct toggles cannot inflate scores', async () => {
  const f = fixture();
  const submit = (answerIndex) => f.call('submitCloudChallenge', {
    catalogue: 'c', questionId: 'q', answerIndex, elapsedSeconds: 20,
  });
  await submit(0);
  for (const answer of [1, 0, 1, 0, 1]) {
    const result = await submit(answer);
    assert.equal(result.score, 0);
    assert.equal(result.totalAnswered, 1);
    assert.equal(result.correct, false);
    assert.equal(result.alreadySubmitted, true);
  }
  assert.equal(f.records.get('users/host/cloudAnswers/q').answerIndex, 0);
});

function competitive(f) {
  f.put('groups/g/challenges/ch', { mode: 'competitive', status: 'active',
    participantUids: ['host'], catalogue: 'c', questions: [{ id: 'q', options: ['A','B','C','D'] }],
    questionIds: ['q'], startedAt: f.timestamp(new Date(Date.now() - 10000)) });
}

test('competitive zero score remains zero on a changed retry', async () => {
  const f = fixture();
  competitive(f);
  const data = { groupId: 'g', challengeId: 'ch', answers: [0], elapsedSeconds: 5 };
  assert.equal((await f.call('submitGroupChallenge', data)).score, 0);
  assert.equal((await f.call('submitGroupChallenge', { ...data, answers: [1] })).score, 0);
});

test('deletion during grading cannot recreate orphan leaderboard entries', async () => {
  const f = fixture();
  competitive(f);
  const original = f.db.getAll;
  f.db.getAll = async (...refs) => {
    const result = await original(...refs);
    await f.call('deleteGroupChallenge', { groupId: 'g', challengeId: 'ch' });
    return result;
  };
  await assert.rejects(f.call('submitGroupChallenge', {
    groupId: 'g', challengeId: 'ch', answers: [1],
  }), { code: 'not-found' });
  assert.equal([...f.records.keys()].some((p) => p.startsWith('groups/g/challenges/ch')), false);
});

test('missing answer keys fail closed instead of silently grading option A', async () => {
  const f = fixture();
  competitive(f);
  f.records.delete('contentPrivate/c/answers/q');
  await assert.rejects(f.call('submitGroupChallenge', {
    groupId: 'g', challengeId: 'ch', answers: [0],
  }), { code: 'failed-precondition' });
  assert.equal(f.records.has('groups/g/challenges/ch/entries/host'), false);
});

test('deletion during Fellowship finalization cannot restore results', async () => {
  const f = fixture();
  f.put('groups/g/challenges/ch', { mode: 'fellowship', status: 'finalizing',
    catalogue: 'c', questions: [{ id: 'q' }], questionIds: ['q'], participantUids: ['host'] });
  const original = f.db.getAll;
  let deleted = false;
  f.db.getAll = async (...refs) => {
    const result = await original(...refs);
    if (!deleted) {
      deleted = true;
      await f.call('deleteGroupChallenge', { groupId: 'g', challengeId: 'ch' });
    }
    return result;
  };
  await assert.rejects(f.call('advanceFellowshipQuestion', { groupId: 'g', challengeId: 'ch' }),
    { code: 'not-found' });
  assert.equal([...f.records.keys()].some((p) => p.startsWith('groups/g/challenges/ch')), false);
});
