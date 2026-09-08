/**
 * Focused contract tests for the multiplayer room rules.
 *
 * These tests load the real callable handlers with a small optimistic
 * Firestore double.  That keeps the concurrency and authorization checks
 * attached to the production code rather than duplicating them in helpers.
 */

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

function createFixture() {
  const records = new Map();
  const versions = new Map();
  const collectionVersions = new Map();
  let generatedId = 0;
  let transactionBarrier = null;

  function touch(path) {
    versions.set(path, (versions.get(path) || 0) + 1);
    const segments = path.split('/');
    for (let length = 1; length <= segments.length; length += 2) {
      const collectionPath = segments.slice(0, length).join('/');
      collectionVersions.set(
        collectionPath,
        (collectionVersions.get(collectionPath) || 0) + 1,
      );
    }
  }

  function timestamp(date = new Date()) {
    return {
      toDate: () => date,
      toMillis: () => date.getTime(),
    };
  }

  function snapshot(path) {
    const value = records.get(path);
    return {
      exists: value !== undefined,
      id: path.split('/').at(-1),
      get: (key) => value?.[key],
      data: () => value,
    };
  }

  function immediateDocs(path) {
    const prefix = path + '/';
    return [...records.keys()]
      .filter((candidate) => {
        if (!candidate.startsWith(prefix)) return false;
        return !candidate.slice(prefix.length).includes('/');
      })
      .map(snapshot);
  }

  function collection(path) {
    return {
      _kind: 'collection',
      path,
      doc(id) {
        const resolvedId = id || `generated-${++generatedId}`;
        return ref(`${path}/${resolvedId}`);
      },
      get: async () => ({
        docs: immediateDocs(path),
        size: immediateDocs(path).length,
        empty: immediateDocs(path).length === 0,
      }),
      where(field, op, values) {
        return {
          get: async () => ({
            docs: immediateDocs(path).filter((doc) => {
              if (op !== 'in') return false;
              return values.includes(doc.get(field));
            }),
            empty: immediateDocs(path).length === 0,
          }),
        };
      },
    };
  }

  function ref(path) {
    return {
      _kind: 'document',
      path,
      id: path.split('/').at(-1),
      get: async () => snapshot(path),
      set: async (value, options = {}) => {
        const next = options.merge && records.has(path)
          ? { ...records.get(path), ...value }
          : { ...value };
        records.set(path, next);
        touch(path);
      },
      update: async (value) => {
        assert(records.has(path), `Cannot update missing document ${path}`);
        records.set(path, { ...records.get(path), ...value });
        touch(path);
      },
      delete: async () => {
        records.delete(path);
        touch(path);
      },
      collection: (name) => collection(`${path}/${name}`),
    };
  }

  function applyWrite(path, value, merge) {
    const existing = records.get(path);
    const next = merge && existing ? { ...existing, ...value } : { ...value };
    records.set(path, next);
    touch(path);
  }

  const db = {
    doc: ref,
    collection,
    getAll: async (...refs) => refs.map((item) => snapshot(item.path)),
    recursiveDelete: async (target) => {
      for (const path of [...records.keys()]) {
        if (path === target.path || path.startsWith(target.path + '/')) {
          records.delete(path);
          touch(path);
        }
      }
    },
    async runTransaction(callback) {
      for (let attempt = 0; attempt < 20; attempt += 1) {
        const reads = new Map();
        const writes = [];
        const transaction = {
          get: async (target) => {
            assert.equal(writes.length, 0, 'All reads must precede writes');
            if (target._kind === 'collection') {
              reads.set(`collection:${target.path}`, collectionVersions.get(target.path) || 0);
              return {
                docs: immediateDocs(target.path),
                size: immediateDocs(target.path).length,
                empty: immediateDocs(target.path).length === 0,
              };
            }
            reads.set(`document:${target.path}`, versions.get(target.path) || 0);
            return snapshot(target.path);
          },
          set: (target, value, options) => writes.push({
            path: target.path,
            value,
            merge: options?.merge === true,
          }),
          update: (target, value) => writes.push({
            path: target.path,
            value,
            merge: true,
          }),
          delete: (target) => writes.push({ path: target.path, delete: true }),
        };

        const result = await callback(transaction);
        if (transactionBarrier) await transactionBarrier();

        const changed = [...reads].some(([key, version]) => {
          const [kind, path] = key.split(':');
          const current = kind === 'collection'
            ? collectionVersions.get(path) || 0
            : versions.get(path) || 0;
          return current !== version;
        });
        if (changed) continue;

        for (const write of writes) {
          if (write.delete) {
            records.delete(write.path);
            touch(write.path);
          } else {
            applyWrite(write.path, write.value, write.merge);
          }
        }
        return result;
      }
      throw new Error('Transaction retries exhausted');
    },
  };

  class HttpsError extends Error {
    constructor(code, message) {
      super(message);
      this.code = code;
    }
  }

  const modules = {
    'firebase-admin/app': { getApps: () => [{}], initializeApp: () => {} },
    'firebase-admin/firestore': {
      getFirestore: () => db,
      FieldValue: {
        serverTimestamp: () => timestamp(),
        arrayUnion: (...values) => ({ __arrayUnion: values }),
        delete: () => ({ __delete: true }),
      },
      Timestamp: { fromDate: timestamp },
    },
    'firebase-admin/messaging': { getMessaging: () => ({}) },
    'firebase-functions/v2': { setGlobalOptions: () => {} },
    'firebase-functions/v2/https': {
      HttpsError,
      onCall: (_, handler) => handler,
    },
    'firebase-functions/v2/scheduler': { onSchedule: (_, handler) => handler },
  };
  const exported = {};
  vm.runInNewContext(fs.readFileSync(require.resolve('./index.js'), 'utf8'), {
    require: (name) => {
      if (!modules[name]) throw new Error(`Unexpected module: ${name}`);
      return modules[name];
    },
    exports: exported,
    console,
    Date,
    Math,
    Map,
    Set,
    Promise,
  });

  return {
    db,
    records,
    put(path, value) {
      records.set(path, value);
      touch(path);
    },
    call(name, data, uid = 'host') {
      return exported[name]({ auth: { uid }, data });
    },
    timestamp,
    synchronizeFirstTwoTransactions() {
      let waiters = [];
      let readers = 0;
      let used = false;
      transactionBarrier = async () => {
        if (used) return;
        readers += 1;
        if (readers === 2) {
          used = true;
          for (const resolve of waiters) resolve();
          waiters = [];
          return;
        }
        await new Promise((resolve) => waiters.push(resolve));
      };
    },
  };
}

function seedGroup(fixture, {
  id = 'g',
  maximumParticipants = 4,
  members = ['host'],
  challengeStatus,
} = {}) {
  fixture.put(`groups/${id}`, {
    ownerUid: 'host',
    name: 'Edge Case Group',
    joinCode: '123456',
    maximumParticipants,
    expiresAt: fixture.timestamp(new Date(Date.now() + 600000)),
    ...(challengeStatus ? { roomChallengeId: 'ch' } : {}),
  });
  for (const uid of members) {
    fixture.put(`groups/${id}/members/${uid}`, {
      role: uid === 'host' ? 'owner' : 'member',
      joinedAt: fixture.timestamp(new Date(Date.now() - 1000)),
      displayName: uid,
    });
  }
  fixture.put('joinCodes/123456', {
    groupId: id,
    ownerUid: 'host',
    expiresAt: fixture.timestamp(new Date(Date.now() + 600000)),
  });
  if (challengeStatus) {
    fixture.records.get(`groups/${id}`).roomChallengeId = 'ch';
    fixture.put(`groups/${id}/challenges/ch`, {
      roomMode: true,
      mode: 'competitive',
      status: challengeStatus,
      questionCount: 10,
      questions: [],
      questionIds: [],
    });
  }
}

test('atomic room creation persists authoritative mode, count, capacity, and host-inclusive roster', async () => {
  const fixture = createFixture();
  const catalogue = 'faith-quiz-global-v1';
  fixture.put(`content/${catalogue}`, { questionCount: 30 });
  for (let index = 0; index < 30; index += 1) {
    fixture.put(`content/${catalogue}/questions/q${index}`, {
      dailyIndex: index,
      question: `Question ${index}`,
      options: ['A', 'B', 'C', 'D'],
      scriptureReference: 'Gen 1:1',
      testament: index % 2 ? 'NT' : 'OT',
      propheticFocus: '',
    });
  }

  const result = await fixture.call('createGroupChallengeRoom', {
    name: 'Room Group',
    mode: 'fellowship',
    questionCount: 20,
    maximumParticipants: 4,
    catalogue,
  });
  assert.equal(result.mode, 'fellowship');
  assert.equal(result.questionCount, 20);
  assert.equal(result.maximumParticipants, 4);
  assert.equal(result.participantCount, 1);
  assert.match(result.joinCode, /^\d{6}$/);

  const group = fixture.records.get(`groups/${result.groupId}`);
  const challenge = fixture.records.get(
    `groups/${result.groupId}/challenges/${result.challengeId}`,
  );
  assert.equal(group.maximumParticipants, 4);
  assert.equal(group.participantCount, 1);
  assert.equal(group.roomChallengeId, result.challengeId);
  assert.equal(challenge.mode, 'fellowship');
  assert.equal(challenge.questionCount, 20);
  assert.equal(challenge.maximumParticipants, 4);
  assert.equal(challenge.participantCount, 1);
  assert.equal(challenge.questions.length, 20);
  assert.equal(fixture.records.get(
    `groups/${result.groupId}/members/host`,
  ).role, 'owner');
});

test('room creation rejects unsupported modes, counts, and capacities', async () => {
  const fixture = createFixture();
  for (const [field, value] of [
    ['mode', 'arcade'],
    ['questionCount', 15],
    ['maximumParticipants', 1],
    ['maximumParticipants', 11],
  ]) {
    const data = {
      name: 'Invalid Room',
      mode: 'competitive',
      questionCount: 10,
      maximumParticipants: 4,
    };
    data[field] = value;
    await assert.rejects(
      fixture.call('createGroupChallengeRoom', data),
      (error) => error.code === 'invalid-argument',
    );
  }
});

test('createGroup persists the host-inclusive maximum participant capacity', async () => {
  const fixture = createFixture();
  const result = await fixture.call('createGroup', {
    name: 'Capacity Group',
    durationMinutes: 10,
    maximumParticipants: 3,
  });

  assert.equal(result.maximumParticipants, 3);
  assert.equal(fixture.records.get(`groups/${result.groupId}`).maximumParticipants, 3);
  assert.equal(fixture.records.get(`groups/${result.groupId}/members/host`).role, 'owner');
});

test('joinGroup counts the host, caps membership, and keeps rejoin idempotent', async () => {
  const fixture = createFixture();
  seedGroup(fixture, { maximumParticipants: 3, members: ['host', 'member1'] });

  const firstJoin = await fixture.call('joinGroup', { joinCode: '123456' }, 'member2');
  assert.equal(firstJoin.role, 'member');
  assert.equal(fixture.records.get('groups/g/members/member2').role, 'member');

  await assert.rejects(
    fixture.call('joinGroup', { joinCode: '123456' }, 'member3'),
    (error) => (error.code === 'resource-exhausted' || error.code === 'failed-precondition')
      && /full/i.test(error.message),
  );

  const joinedAt = fixture.records.get('groups/g/members/member2').joinedAt;
  const retry = await fixture.call('joinGroup', { joinCode: '123456' }, 'member2');
  assert.equal(retry.alreadyMember, true);
  assert.equal(fixture.records.get('groups/g/members/member2').joinedAt, joinedAt);
});

test('a member who leaves before start can reopen the room without duplicating capacity', async () => {
  const fixture = createFixture();
  seedGroup(fixture, { maximumParticipants: 3, members: ['host', 'member1'] });

  // Simulate the member leaving or clearing local app state before the next
  // lobby refresh. Re-entering the PIN should restore one membership record,
  // not create an extra participant slot.
  fixture.records.delete('groups/g/members/member1');
  fixture.records.delete('users/member1/groups/g');
  const reopened = await fixture.call('joinGroup', { joinCode: '123456' }, 'member1');

  assert.equal(reopened.alreadyMember, false);
  assert.equal(reopened.participantCount, 2);
  assert.equal(fixture.records.get('groups/g/members/member1').role, 'member');
  assert.equal(fixture.records.get('groups/g').participantCount, 2);
});

test('two simultaneous joins cannot take the final capacity slot', async () => {
  const fixture = createFixture();
  seedGroup(fixture, { maximumParticipants: 3, members: ['host', 'member1'] });
  fixture.synchronizeFirstTwoTransactions();

  const results = await Promise.allSettled([
    fixture.call('joinGroup', { joinCode: '123456' }, 'member2'),
    fixture.call('joinGroup', { joinCode: '123456' }, 'member3'),
  ]);
  assert.equal(results.filter((result) => result.status === 'fulfilled').length, 1);
  assert.equal(results.filter((result) => result.status === 'rejected').length, 1);
  const memberDocs = [...fixture.records.keys()]
    .filter((path) => path.startsWith('groups/g/members/'));
  assert.equal(memberDocs.length, 3, 'Host-inclusive capacity must remain three members');
});

for (const mode of ['competitive', 'fellowship']) {
  test(`startGroupChallenge rejects a ${mode} room with only the host`, async () => {
    const fixture = createFixture();
    seedGroup(fixture, { members: ['host'] });
    fixture.put('groups/g/challenges/ch', {
      mode,
      status: 'lobby',
      questionCount: 10,
      questions: [],
      questionIds: [],
    });

    await assert.rejects(
      fixture.call('startGroupChallenge', { groupId: 'g', challengeId: 'ch' }),
      (error) => error.code === 'failed-precondition'
        && /one other player|at least one other/i.test(error.message),
    );
    assert.equal(fixture.records.get('groups/g/challenges/ch').status, 'lobby');
  });
}

test('a partially filled room can start and freezes the current roster', async () => {
  const fixture = createFixture();
  seedGroup(fixture, { maximumParticipants: 5, members: ['host', 'member1', 'member2'] });
  fixture.put('groups/g/challenges/ch', {
    mode: 'competitive',
    status: 'lobby',
    questionCount: 20,
    questions: [],
    questionIds: [],
  });

  const result = await fixture.call('startGroupChallenge', {
    groupId: 'g',
    challengeId: 'ch',
  });
  assert.equal(result.status, 'active');
  const challenge = fixture.records.get('groups/g/challenges/ch');
  assert.deepEqual(new Set(challenge.participantUids), new Set(['host', 'member1', 'member2']));
  assert.equal(challenge.participantCount, 3);
});

test('PIN entry is rejected after a challenge starts', async () => {
  const fixture = createFixture();
  seedGroup(fixture, {
    maximumParticipants: 5,
    members: ['host', 'member1'],
    challengeStatus: 'active',
  });

  await assert.rejects(
    fixture.call('joinGroup', { joinCode: '123456' }, 'lateMember'),
    (error) => (error.code === 'failed-precondition' || error.code === 'permission-denied')
      && /already started/i.test(error.message),
  );
  assert.equal(fixture.records.has('groups/g/members/lateMember'), false);
});
