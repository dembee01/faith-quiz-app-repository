const { getApps, initializeApp } = require('firebase-admin/app');
const { getFirestore, FieldValue, Timestamp } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');
const { setGlobalOptions } = require('firebase-functions/v2');
const { HttpsError, onCall } = require('firebase-functions/v2/https');
const { onSchedule } = require('firebase-functions/v2/scheduler');

// Cost optimization: low memory tier, short timeout, high concurrency, strict instance ceiling
setGlobalOptions({
  region: 'us-central1',
  memory: '256MiB',
  timeoutSeconds: 15,
  concurrency: 40,
  maxInstances: 10,
});

if (getApps().length === 0) {
  initializeApp();
}
const db = getFirestore();

// In-memory cache for question answers across container lifespan
const answerCache = new Map();

async function getCachedAnswer(catalogue, questionId) {
  const key = `${catalogue}/${questionId}`;
  const hit = answerCache.get(key);
  if (hit) {
    return hit;
  }
  const privateAnswerRef = db.doc(`contentPrivate/${catalogue}/answers/${questionId}`);
  const privateAnswer = await privateAnswerRef.get();
  if (!privateAnswer.exists) {
    return null;
  }
  const data = {
    correctAnswer: privateAnswer.get('correctAnswer'),
    challengeId: privateAnswer.get('challengeId') || questionId,
  };
  answerCache.set(key, data);
  return data;
}

function requireUser(request) {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Sign in is required.');
  return request.auth.uid;
}

// App Check enforcement stays off because Faith Quiz is distributed as a
// sideloaded APK: Play Integrity attestation cannot pass outside Google Play,
// so enforced checks would reject every real player. Score integrity does not
// depend on it — correct answers never reach the client, grading happens here,
// and each account may submit once per challenge. Revisit enforcement only if
// the app is ever published on Google Play.
const callableOptions = { enforceAppCheck: false };

function requireText(value, name, maxLength = 80) {
  if (typeof value !== 'string' || !value.trim() || value.length > maxLength) {
    throw new HttpsError('invalid-argument', `Invalid ${name}.`);
  }
  return value.trim();
}

function leaderboardName(value) {
  // A nickname is optional and deliberately kept short. It is displayed only
  // beside a verified score, never linked to an email address or device ID.
  return requireText(value || 'Faith learner', 'display name', 24);
}

function validateUsername(value) {
  if (typeof value !== 'string') {
    throw new HttpsError('invalid-argument', 'Username must be a string.');
  }
  const trimmed = value.trim();
  if (trimmed.length < 3 || trimmed.length > 20) {
    throw new HttpsError('invalid-argument', 'Username must be between 3 and 20 characters.');
  }
  if (!/^[a-zA-Z0-9_]+$/.test(trimmed)) {
    throw new HttpsError('invalid-argument', 'Username can only contain letters, numbers, and underscores.');
  }
  return trimmed;
}

exports.claimUsername = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const username = validateUsername(request.data.username);
  const usernameKey = username.toLowerCase();

  const usernameRef = db.doc(`usernames/${usernameKey}`);
  const userRef = db.doc(`users/${uid}`);
  const globalLeaderboardRef = db.doc(`leaderboards/global_challenge/entries/${uid}`);

  await db.runTransaction(async (transaction) => {
    // 1. ALL READS MUST PRECEDE ALL WRITES
    const usernameSnap = await transaction.get(usernameRef);
    const userSnap = await transaction.get(userRef);
    const lbSnap = await transaction.get(globalLeaderboardRef);

    if (usernameSnap.exists && usernameSnap.get('uid') !== uid) {
      throw new HttpsError('already-exists', `The username "@${username}" is already taken. Please choose another.`);
    }

    // 2. ALL WRITES AFTER READS
    const oldUsername = userSnap.exists ? userSnap.get('username') : null;
    if (oldUsername && oldUsername.toLowerCase() !== usernameKey) {
      transaction.delete(db.doc(`usernames/${oldUsername.toLowerCase()}`));
    }

    transaction.set(usernameRef, {
      uid,
      username,
      claimedAt: FieldValue.serverTimestamp(),
    });

    transaction.set(userRef, {
      username,
      displayName: username,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    if (lbSnap.exists) {
      transaction.set(globalLeaderboardRef, {
        displayName: username,
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
    }
  });

  return { username };
});

exports.checkUsernameAvailable = onCall(callableOptions, async (request) => {
  const uid = request.auth ? request.auth.uid : null;
  const username = validateUsername(request.data.username);
  const usernameKey = username.toLowerCase();
  const usernameSnap = await db.doc(`usernames/${usernameKey}`).get();
  if (!usernameSnap.exists) {
    return { available: true, username };
  }
  return { available: usernameSnap.get('uid') === uid, username };
});

// A client supplies only its selected answer. The correct answer is verified
// server-side so modified apps cannot submit fabricated scores.
exports.submitCloudChallenge = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const catalogue = requireText(request.data.catalogue, 'catalogue');
  const questionId = requireText(request.data.questionId, 'question ID');
  let displayName = leaderboardName(request.data.displayName);
  const answerIndex = request.data.answerIndex;
  const elapsedSeconds = request.data.elapsedSeconds;
  if (!Number.isInteger(answerIndex) || answerIndex < 0 || answerIndex > 3 ||
      !Number.isInteger(elapsedSeconds) || elapsedSeconds < 0 || elapsedSeconds > 600) {
    throw new HttpsError('invalid-argument', 'Invalid answer submission.');
  }

  const answerData = await getCachedAnswer(catalogue, questionId);
  if (!answerData) {
    throw new HttpsError('not-found', 'Challenge is unavailable.');
  }
  const correct = answerIndex === answerData.correctAnswer;
  const challengeId = answerData.challengeId;

  // Use user's claimed username if available
  const userDoc = await db.doc(`users/${uid}`).get();
  if (userDoc.exists && userDoc.get('username')) {
    displayName = userDoc.get('username');
  }

  const userProgressRef = db.doc(`users/${uid}/cloudAnswers/${questionId}`);
  const globalLeaderboardRef = db.doc(`leaderboards/global_challenge/entries/${uid}`);
  const singleChallengeEntryRef = db.doc(`leaderboards/${challengeId}/entries/${uid}`);

  let updatedStats = { score: 0, totalAnswered: 0, level: 1 };
  let alreadySubmitted = false;

  await db.runTransaction(async (transaction) => {
    const [progressSnap, globalSnap, singleSnap] = await Promise.all([
      transaction.get(userProgressRef),
      transaction.get(globalLeaderboardRef),
      transaction.get(singleChallengeEntryRef),
    ]);

    alreadySubmitted = progressSnap.exists;
    const previouslyCorrect = progressSnap.exists && progressSnap.get('correct') === true;

    // Record question submission in user history
    transaction.set(userProgressRef, {
      catalogue,
      questionId,
      challengeId,
      answerIndex,
      correct,
      elapsedSeconds,
      answeredAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    // Calculate global leaderboard stats
    const currentScore = globalSnap.exists ? (globalSnap.get('score') || 0) : 0;
    const currentAnswered = globalSnap.exists ? (globalSnap.get('totalAnswered') || 0) : 0;

    // If new question, increment totalAnswered
    const newAnswered = currentAnswered + (alreadySubmitted ? 0 : 1);
    // If not previously correct and now correct, increment score
    const newScore = currentScore + (!previouslyCorrect && correct ? 1 : 0);
    const newLevel = Math.floor(newScore / 5) + 1; // 5 correct answers per level

    updatedStats = {
      score: newScore,
      totalAnswered: newAnswered,
      level: newLevel,
    };

    // Design note: the global cumulative leaderboard ranks by accuracy
    // (score DESC, i.e. total correct answers) with average first-attempt response
    // time as a tie-breaker.
    const currentTotalResponseTime = globalSnap.exists ? (globalSnap.get('totalResponseTime') || 0) : 0;
    const newTotalResponseTime = currentTotalResponseTime + (alreadySubmitted ? 0 : elapsedSeconds);
    const avgElapsedSeconds = newAnswered > 0 ? Math.round(newTotalResponseTime / newAnswered) : 0;

    transaction.set(globalLeaderboardRef, {
      uid,
      displayName,
      score: newScore,
      totalAnswered: newAnswered,
      level: newLevel,
      accuracy: newAnswered > 0 ? Math.round((newScore / newAnswered) * 100) : 0,
      totalResponseTime: newTotalResponseTime,
      avgElapsedSeconds,
      verified: true,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    // Single question leaderboard (for backward compatibility)
    if (!singleSnap.exists || (!previouslyCorrect && correct)) {
      transaction.set(singleChallengeEntryRef, {
        score: correct ? 1 : 0,
        correct,
        elapsedSeconds,
        displayName,
        updatedAt: FieldValue.serverTimestamp(),
        verified: true,
      }, { merge: true });
    }
  });

  return {
    correct,
    challengeId,
    alreadySubmitted,
    score: updatedStats.score,
    totalAnswered: updatedStats.totalAnswered,
    level: updatedStats.level,
    correctAnswer: answerData.correctAnswer,
  };
});

function generateJoinCode() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

// Challenges created after participant freezing carry an explicit UID list.
// Missing lists are treated as legacy challenges so deployed historical data
// remains usable; all new starts write participantUids atomically.
function challengeHasParticipant(challengeSnapshot, uid) {
  const participantUids = challengeSnapshot.get('participantUids');
  return !Array.isArray(participantUids) || participantUids.includes(uid);
}

// The join window is intentionally separate from challenge timing. Every new
// invitation defaults to (and may not exceed) ten minutes; an owner can
// explicitly extend an active group later through extendGroup.
const DEFAULT_GROUP_JOIN_WINDOW_MINUTES = 10;

exports.createGroup = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const name = requireText(request.data.name, 'group name', 40);
  const hasDuration = Object.prototype.hasOwnProperty.call(
    request.data || {},
    'durationMinutes',
  );
  const durationMinutes = request.data.durationMinutes;
  // Every newly created invitation has a ten-minute join window. Longer or
  // non-expiring windows are intentionally rejected; the host can explicitly
  // extend an active group later with extendGroup.
  if (hasDuration &&
      (typeof durationMinutes !== 'number' ||
       !Number.isInteger(durationMinutes) ||
       durationMinutes < 1 ||
       durationMinutes > DEFAULT_GROUP_JOIN_WINDOW_MINUTES)) {
    throw new HttpsError(
      'invalid-argument',
      'Group join window must be between 1 and 10 minutes.'
    );
  }
  const minutes = hasDuration
    ? durationMinutes
    : DEFAULT_GROUP_JOIN_WINDOW_MINUTES;
  const now = Date.now();
  const expiresAt = minutes > 0 ? new Date(now + minutes * 60 * 1000) : null;
  const expiresAtTimestamp = expiresAt ? Timestamp.fromDate(expiresAt) : null;

  const group = db.collection('groups').doc();
  const createdAt = FieldValue.serverTimestamp();
  let allocatedJoinCode = null;

  // Concurrency-safe atomic reservation: test and set inside transaction
  for (let attempt = 0; attempt < 10; attempt++) {
    const candidateCode = generateJoinCode();
    const joinCodeRef = db.collection('joinCodes').doc(candidateCode);

    try {
      await db.runTransaction(async (transaction) => {
        const joinCodeDoc = await transaction.get(joinCodeRef);
        if (joinCodeDoc.exists) {
          const exp = joinCodeDoc.get('expiresAt');
          // If code is still active, collision occurred -> throw to retry with fresh code
          if (!exp || exp.toDate() >= new Date()) {
            throw new Error('JOIN_CODE_COLLISION');
          }
        }

        // Reservation succeeded atomically within transaction
        transaction.set(group, {
          name,
          ownerUid: uid,
          createdAt,
          joinCode: candidateCode,
          expiresAt: expiresAtTimestamp,
          durationMinutes: minutes,
        });
        transaction.set(joinCodeRef, {
          groupId: group.id,
          name,
          ownerUid: uid,
          createdAt,
          expiresAt: expiresAtTimestamp,
          durationMinutes: minutes,
        });
        transaction.set(group.collection('members').doc(uid), {
          role: 'owner',
          joinedAt: createdAt,
          displayName: 'Host',
        });
        transaction.set(db.doc(`users/${uid}/groups/${group.id}`), {
          name,
          role: 'owner',
          joinedAt: createdAt,
          joinCode: candidateCode,
          expiresAt: expiresAtTimestamp,
          durationMinutes: minutes,
        });
      });

      allocatedJoinCode = candidateCode;
      break;
    } catch (err) {
      if (err.message === 'JOIN_CODE_COLLISION') {
        continue;
      }
      throw err;
    }
  }

  if (!allocatedJoinCode) {
    throw new HttpsError('resource-exhausted', 'Could not generate a unique join code. Please try again.');
  }

  return {
    groupId: group.id,
    joinCode: allocatedJoinCode,
    expiresAt: expiresAt ? expiresAt.toISOString() : null,
    durationMinutes: minutes,
  };
});

exports.joinGroup = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const rawInput = requireText(request.data.groupId || request.data.joinCode, 'group code').trim();

  let targetGroupId = rawInput;
  const joinCodeDoc = await db.collection('joinCodes').doc(rawInput).get();

  if (joinCodeDoc.exists) {
    const exp = joinCodeDoc.get('expiresAt');
    if (exp && exp.toDate() < new Date()) {
      throw new HttpsError(
        'deadline-exceeded',
        'This group invitation has expired. Ask the host for an updated code.',
      );
    }
    targetGroupId = joinCodeDoc.get('groupId');
  }

  const group = db.doc(`groups/${targetGroupId}`);
  let groupName = 'Faith Quiz group';
  let groupJoinCode = null;
  let groupExpiresAt = null;
  let groupDurationMinutes = null;
  let memberRole = 'member';
  let alreadyMember = false;
  let ownerMessage = null;

  // Read user profile to get claimed username or displayName
  const userSnap = await db.doc(`users/${uid}`).get();
  const memberDisplayName = userSnap.exists
    ? (userSnap.get('username') || userSnap.get('displayName') || 'Member')
    : 'Member';

  await db.runTransaction(async (transaction) => {
    // Read the existing membership and user index before writing either one.
    // In particular, a host re-entering their own six-digit code must never
    // turn role: owner into role: member or refresh joinedAt.
    const memberRef = group.collection('members').doc(uid);
    const userGroupRef = db.doc(`users/${uid}/groups/${targetGroupId}`);
    const [snapshot, memberSnapshot, userGroupSnapshot] = await Promise.all([
      transaction.get(group),
      transaction.get(memberRef),
      transaction.get(userGroupRef),
    ]);
    if (!snapshot.exists) throw new HttpsError('not-found', 'Group not found. Please verify the code.');

    const exp = snapshot.get('expiresAt');
    if (exp && exp.toDate() < new Date()) {
      throw new HttpsError('deadline-exceeded', 'This group session has expired.');
    }

    groupName = snapshot.get('name') || 'Faith Quiz group';
    groupJoinCode = snapshot.get('joinCode') || null;
    groupExpiresAt = snapshot.get('expiresAt') || null;
    groupDurationMinutes = snapshot.get('durationMinutes') || null;

    const canonicalOwnerUid = snapshot.get('ownerUid') || snapshot.get('ownerId');
    const existingRole = memberSnapshot.exists ? memberSnapshot.get('role') : null;
    // Preserve an existing owner role even for older groups whose canonical
    // owner field was not migrated.  For canonical groups, repair a stale
    // member role back to owner without changing the original joinedAt.
    memberRole = canonicalOwnerUid === uid || existingRole === 'owner' ? 'owner' : 'member';
    alreadyMember = memberSnapshot.exists;
    if (memberRole === 'owner' && canonicalOwnerUid === uid) {
      ownerMessage = 'You already own this group.';
    }

    const existingJoinedAt = memberSnapshot.exists
      ? memberSnapshot.get('joinedAt')
      : (userGroupSnapshot.exists ? userGroupSnapshot.get('joinedAt') : null);
    const joinedAt = existingJoinedAt || FieldValue.serverTimestamp();
    const existingDisplayName = memberSnapshot.exists
      ? memberSnapshot.get('displayName')
      : null;

    // Keep the original membership timestamp and role stable on repeat joins.
    // A write is still allowed here to repair legacy/stale owner metadata.
    transaction.set(memberRef, {
      role: memberRole,
      joinedAt,
      displayName: existingDisplayName || memberDisplayName,
    }, { merge: true });
    transaction.set(userGroupRef, {
      name: groupName,
      role: memberRole,
      joinedAt,
      joinCode: groupJoinCode,
      expiresAt: groupExpiresAt,
      durationMinutes: groupDurationMinutes,
    }, { merge: true });
  });

  return {
    groupId: targetGroupId,
    name: groupName,
    joinCode: groupJoinCode,
    role: memberRole,
    alreadyMember,
    message: ownerMessage,
  };
});

exports.extendGroup = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const groupId = requireText(request.data.groupId, 'group ID');
  const additionalMinutes = Number(request.data.additionalMinutes) || 10;

  const group = db.doc(`groups/${groupId}`);
  const member = db.doc(`groups/${groupId}/members/${uid}`);
  const [groupSnap, memberSnap] = await Promise.all([group.get(), member.get()]);

  if (!groupSnap.exists || !memberSnap.exists) {
    throw new HttpsError('not-found', 'Group not found.');
  }
  if (memberSnap.get('role') !== 'owner') {
    throw new HttpsError('permission-denied', 'Only the group owner can extend the session.');
  }

  const currentExp = groupSnap.get('expiresAt');
  const baseTime = (currentExp && currentExp.toDate() > new Date())
    ? currentExp.toDate().getTime()
    : Date.now();
  const newExp = new Date(baseTime + additionalMinutes * 60 * 1000);
  const newExpTimestamp = Timestamp.fromDate(newExp);
  const joinCode = groupSnap.get('joinCode');

  const batch = db.batch();
  batch.update(group, { expiresAt: newExpTimestamp });
  if (joinCode) {
    batch.set(db.collection('joinCodes').doc(joinCode), { expiresAt: newExpTimestamp }, { merge: true });
  }
  batch.set(db.doc(`users/${uid}/groups/${groupId}`), { expiresAt: newExpTimestamp }, { merge: true });
  await batch.commit();

  return {
    groupId,
    expiresAt: newExp.toISOString(),
  };
});

// Group owners may revoke a challenge at any point in its lifecycle. This is
// a hard delete of the challenge subtree so stale entries and private
// Fellowship answers cannot remain readable or be mistaken for a live
// challenge. Parent group membership is deliberately left intact.
exports.deleteGroupChallenge = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const groupId = requireText(request.data.groupId, 'group ID');
  const challengeId = requireText(request.data.challengeId, 'challenge ID');

  const groupRef = db.doc(`groups/${groupId}`);
  const memberRef = db.doc(`groups/${groupId}/members/${uid}`);
  const challengeRef = db.doc(`groups/${groupId}/challenges/${challengeId}`);
  const [groupSnap, memberSnap] = await Promise.all([
    groupRef.get(),
    memberRef.get(),
  ]);
  if (!groupSnap.exists || !memberSnap.exists) {
    throw new HttpsError('not-found', 'Group not found.');
  }
  const canonicalOwnerUid = groupSnap.get('ownerUid') || groupSnap.get('ownerId');
  if (memberSnap.get('role') !== 'owner' || canonicalOwnerUid !== uid) {
    throw new HttpsError('permission-denied', 'Only the group owner can delete challenges.');
  }

  // recursiveDelete also removes entries and fellowshipAnswers beneath the
  // challenge document. Repeating the call is safe and idempotent.
  await db.recursiveDelete(challengeRef);
  return { status: 'deleted', groupId, challengeId };
});

// Group hosts select from the same curated public catalogue. Members can
// view the question, but only a callable function can read the hidden answer
// key and write a verified group score.
exports.createGroupChallenge = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const groupId = requireText(request.data.groupId, 'group ID');
  const catalogue = requireText(request.data.catalogue || 'faith-quiz-global-v1', 'catalogue');
  const group = db.doc(`groups/${groupId}`);
  const membership = db.doc(`groups/${groupId}/members/${uid}`);
  const [groupSnapshot, membershipSnapshot] = await Promise.all([
    group.get(), membership.get(),
  ]);
  if (!groupSnapshot.exists || !membershipSnapshot.exists) {
    throw new HttpsError('permission-denied', 'Join the group before creating a challenge.');
  }
  if (membershipSnapshot.get('role') !== 'owner') {
    throw new HttpsError('permission-denied', 'Only the group owner can create a challenge.');
  }
  if (groupSnapshot.get('expiresAt') && groupSnapshot.get('expiresAt').toDate() < new Date()) {
    throw new HttpsError('failed-precondition', 'This group session has expired. Please extend the session to create a new challenge.');
  }

  // Enforce mode: only 'competitive' or 'fellowship' accepted; reject all others
  const rawMode = request.data.mode;
  if (rawMode !== undefined && rawMode !== 'competitive' && rawMode !== 'fellowship') {
    throw new HttpsError('invalid-argument', 'Mode must be either "competitive" or "fellowship".');
  }
  const mode = rawMode === 'fellowship' ? 'fellowship' : 'competitive';

  // Strict Question Count Validation: MUST be exactly 10, 20, or 30
  const requestedCount = Number(request.data.questionCount);
  if (![10, 20, 30].includes(requestedCount)) {
    throw new HttpsError('invalid-argument', 'Question count must be exactly 10, 20, or 30.');
  }
  const questionCount = requestedCount;

  // Read authoritative catalogue metadata for question count.
  // Falls back to 500 for backward compatibility with faith-quiz-global-v1.
  const catalogueMeta = await db.doc(`content/${catalogue}`).get();
  let totalQuestions = 500;
  if (catalogueMeta.exists) {
    const metaCount = catalogueMeta.get('questionCount');
    if (metaCount !== undefined) {
      if (!Number.isInteger(metaCount) || metaCount <= 0) {
        throw new HttpsError('internal', 'Catalogue metadata questionCount must be a positive integer.');
      }
      totalQuestions = metaCount;
    }
  }

  if (totalQuestions < requestedCount) {
    throw new HttpsError(
      'failed-precondition',
      `Catalogue contains fewer questions (${totalQuestions}) than requested (${requestedCount}).`
    );
  }

  // Verify the coprime stride 263 is actually coprime with the catalogue size.
  // GCD must be 1 for a full permutation cycle.
  function gcd(a, b) { while (b) { [a, b] = [b, a % b]; } return a; }
  const stride = gcd(263, totalQuestions) === 1 ? 263 : 1; // fallback to sequential if not coprime

  // Server-controlled randomized seed (client seed disallowed to prevent manipulation)
  const seed = Math.floor(Math.random() * totalQuestions);

  // Multi-question rotated cloud challenge: select questionCount distinct indices
  // using coprime stride to interleave Old Testament and New Testament questions.
  const targetIndices = [];
  for (let i = 0; i < questionCount; i++) {
    targetIndices.push((seed + i * stride) % totalQuestions);
  }

  // Firestore allows `in` queries with up to 30 elements in a single read query
  const querySnapshot = await db
    .collection(`content/${catalogue}/questions`)
    .where('dailyIndex', 'in', targetIndices)
    .get();

  if (querySnapshot.empty) {
    throw new HttpsError('not-found', 'No questions available in the cloud catalogue.');
  }

  const docMap = new Map();
  querySnapshot.docs.forEach((doc) => {
    const dailyIndex = doc.get('dailyIndex');
    if (dailyIndex !== undefined) {
      docMap.set(dailyIndex, doc);
    }
  });

  const selectedQuestions = [];
  for (const idx of targetIndices) {
    const doc = docMap.get(idx);
    if (doc) {
      selectedQuestions.push({
        id: doc.id,
        question: doc.get('question') || '',
        options: doc.get('options') || [],
        scriptureReference: doc.get('scriptureReference') || '',
        testament: doc.get('testament') || '',
        propheticFocus: doc.get('propheticFocus') || '',
      });
    }
  }

  if (selectedQuestions.length !== requestedCount) {
    throw new HttpsError(
      'internal',
      `Incomplete catalogue questions: expected ${requestedCount} questions for challenge, but only ${selectedQuestions.length} were found in the database.`
    );
  }

  const defaultTitle = mode === 'fellowship'
    ? `Bible Fellowship (${selectedQuestions.length} Questions)`
    : `Bible Challenge (${selectedQuestions.length} Questions)`;
  const title = typeof request.data.title === 'string' && request.data.title.trim().length > 0
    ? request.data.title.trim()
    : defaultTitle;

  const challenge = group.collection('challenges').doc();
  await challenge.set({
    catalogue,
    title,
    mode,
    status: 'lobby',
    questionCount: selectedQuestions.length,
    questions: selectedQuestions,
    questionIds: selectedQuestions.map((q) => q.id),
    seed,
    createdBy: uid,
    active: true,
    currentQuestionIndex: 0,
    answeredUids: [],
    createdAt: FieldValue.serverTimestamp(),
  });

  return {
    challengeId: challenge.id,
    questionCount: selectedQuestions.length,
    mode,
    status: 'lobby',
  };
});

exports.startGroupChallenge = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const groupId = requireText(request.data.groupId, 'group ID');
  const challengeId = requireText(request.data.challengeId, 'challenge ID');

  const groupRef = db.doc(`groups/${groupId}`);
  const memberRef = db.doc(`groups/${groupId}/members/${uid}`);
  const challengeRef = db.doc(`groups/${groupId}/challenges/${challengeId}`);

  let resultStatus = 'lobby';

  await db.runTransaction(async (transaction) => {
    const [groupSnap, memberSnap, challengeSnap] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(memberRef),
      transaction.get(challengeRef),
    ]);

    if (!groupSnap.exists || !memberSnap.exists || !challengeSnap.exists) {
      throw new HttpsError('not-found', 'Challenge or group not found.');
    }
    if (memberSnap.get('role') !== 'owner') {
      throw new HttpsError('permission-denied', 'Only the group owner can start the challenge.');
    }

    const expiresAt = groupSnap.get('expiresAt');
    if (expiresAt && expiresAt.toDate && expiresAt.toDate() < new Date()) {
      throw new HttpsError(
        'failed-precondition',
        'This group session has expired. Please extend the session before starting the challenge.'
      );
    }

    const currentStatus = challengeSnap.get('status') || (challengeSnap.get('active') ? 'active' : 'lobby');
    if (currentStatus !== 'lobby') {
      // Idempotent: already started, do not overwrite established startedAt
      resultStatus = currentStatus;
      return;
    }

    const mode = challengeSnap.get('mode') || 'competitive';
    const now = FieldValue.serverTimestamp();
    const updates = {
      startedAt: now,
    };

    // Freeze the lobby roster at the exact start transition.  The parent
    // group may still accept members for a future challenge, but those late
    // joiners must not appear in this challenge halfway through play.
    if (!Array.isArray(challengeSnap.get('participantUids'))) {
      const membersSnapshot = await transaction.get(groupRef.collection('members'));
      const participantUids = membersSnapshot.docs.map((doc) => doc.id);
      if (!participantUids.includes(uid)) participantUids.push(uid);
      updates.participantUids = [...new Set(participantUids)];
      updates.participantCount = updates.participantUids.length;
    }

    if (mode === 'fellowship') {
      updates.status = 'question_open';
      updates.currentQuestionIndex = 0;
      updates.currentQuestionOpenedAt = now;
      updates.answeredUids = [];
      resultStatus = 'question_open';
    } else {
      updates.status = 'active';
      resultStatus = 'active';
    }

    transaction.update(challengeRef, updates);
  });

  return { status: resultStatus, challengeId };
});

exports.submitFellowshipAnswer = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const groupId = requireText(request.data.groupId, 'group ID');
  const challengeId = requireText(request.data.challengeId, 'challenge ID');
  const questionIndex = Number(request.data.questionIndex);
  const answerIndex = Number(
    request.data.answerIndex !== undefined
      ? request.data.answerIndex
      : request.data.selectedOptionIndex
  );

  if (!Number.isInteger(questionIndex) || questionIndex < 0 ||
      !Number.isInteger(answerIndex) || answerIndex < 0 || answerIndex > 3) {
    throw new HttpsError('invalid-argument', 'Invalid answer parameter.');
  }

  const membership = await db.doc(`groups/${groupId}/members/${uid}`).get();
  if (!membership.exists) {
    throw new HttpsError('permission-denied', 'Only group members can submit answers.');
  }

  // Atomic transaction: validate challenge state + check answer immutability + create answer
  // This eliminates the race between answer submission and host reveal/advance,
  // and guarantees exactly-once semantics (answer immutability after first submission).
  const challengeRef = db.doc(`groups/${groupId}/challenges/${challengeId}`);
  const answerRef = challengeRef.collection('fellowshipAnswers').doc(`${questionIndex}_${uid}`);
  let resultAnswerIndex = answerIndex;
  let alreadyAnswered = false;

  await db.runTransaction(async (transaction) => {
    // ALL READS FIRST
    const [challengeSnap, existingAnswer] = await Promise.all([
      transaction.get(challengeRef),
      transaction.get(answerRef),
    ]);

    if (!challengeSnap.exists) {
      throw new HttpsError('not-found', 'Challenge not found.');
    }

    if (!challengeHasParticipant(challengeSnap, uid)) {
      throw new HttpsError(
        'permission-denied',
        'You joined after this challenge started and cannot participate in it.'
      );
    }

    // Validate challenge is in the correct state for answers
    if (challengeSnap.get('status') !== 'question_open') {
      throw new HttpsError('failed-precondition', 'Question is not currently open for answers.');
    }

    const currentQIndex = challengeSnap.get('currentQuestionIndex') || 0;
    if (questionIndex !== currentQIndex) {
      throw new HttpsError('failed-precondition', 'Can only answer the active question.');
    }

    // Immutability: if answer already exists, return idempotent success
    if (existingAnswer.exists) {
      resultAnswerIndex = existingAnswer.get('answerIndex');
      alreadyAnswered = true;
      return; // Transaction completes without writes
    }

    // Calculate response time from question open timestamp
    const openedAt = challengeSnap.get('currentQuestionOpenedAt');
    const responseTimeSeconds = openedAt && openedAt.toDate
      ? Math.max(0, Math.floor((Date.now() - openedAt.toDate().getTime()) / 1000))
      : 0;

    // ALL WRITES AFTER READS
    transaction.set(answerRef, {
      uid,
      questionIndex,
      answerIndex,
      answeredAt: FieldValue.serverTimestamp(),
      responseTimeSeconds,
    });

    transaction.update(challengeRef, {
      answeredUids: FieldValue.arrayUnion(uid),
    });
  });

  return { success: true, questionIndex, answerIndex: resultAnswerIndex, alreadyAnswered };
});

exports.revealFellowshipAnswer = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const groupId = requireText(request.data.groupId, 'group ID');
  const challengeId = requireText(request.data.challengeId, 'challenge ID');

  const membership = await db.doc(`groups/${groupId}/members/${uid}`).get();
  if (!membership.exists || membership.get('role') !== 'owner') {
    throw new HttpsError('permission-denied', 'Only the group owner can reveal answers.');
  }

  // Atomic transaction: validate status is 'question_open' before transitioning
  // to 'question_revealed'. Prevents double-reveal race conditions.
  const challengeRef = db.doc(`groups/${groupId}/challenges/${challengeId}`);
  let revealResult = {};

  await db.runTransaction(async (transaction) => {
    const challengeSnap = await transaction.get(challengeRef);
    if (!challengeSnap.exists) {
      throw new HttpsError('not-found', 'Challenge not found.');
    }

    const currentStatus = challengeSnap.get('status');
    // Idempotent: if already revealed, return current state
    if (currentStatus === 'question_revealed') {
      const currentQIndex = challengeSnap.get('currentQuestionIndex') || 0;
      revealResult = {
        status: 'question_revealed',
        currentQuestionIndex: currentQIndex,
        revealedAnswer: challengeSnap.get('revealedAnswer'),
        revealedExplanation: challengeSnap.get('revealedExplanation') || '',
        revealedScriptureReference: challengeSnap.get('revealedScriptureReference') || '',
      };
      return;
    }

    if (currentStatus !== 'question_open') {
      throw new HttpsError('failed-precondition', 'Question must be open before revealing.');
    }

    const catalogue = challengeSnap.get('catalogue') || 'faith-quiz-global-v1';
    const currentQIndex = challengeSnap.get('currentQuestionIndex') || 0;
    const questions = challengeSnap.get('questions') || [];
    if (currentQIndex >= questions.length) {
      throw new HttpsError('out-of-range', 'Invalid question index.');
    }

    const q = questions[currentQIndex];
    // Note: reading contentPrivate inside a transaction is safe because it's
    // a read-only server-owned document that never changes during gameplay.
    const privateAnswer = await transaction.get(db.doc(`contentPrivate/${catalogue}/answers/${q.id}`));
    const correctAnswer = privateAnswer.exists ? privateAnswer.get('correctAnswer') : 0;
    const explanation = privateAnswer.exists ? (privateAnswer.get('explanation') || '') : '';
    const scriptureReference = q.scriptureReference || '';

    transaction.update(challengeRef, {
      status: 'question_revealed',
      revealedAnswer: correctAnswer,
      revealedExplanation: explanation,
      revealedScriptureReference: scriptureReference,
      revealedAt: FieldValue.serverTimestamp(),
    });

    revealResult = {
      status: 'question_revealed',
      currentQuestionIndex: currentQIndex,
      revealedAnswer: correctAnswer,
      revealedExplanation: explanation,
      revealedScriptureReference: scriptureReference,
    };
  });

  return revealResult;
});

exports.advanceFellowshipQuestion = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const groupId = requireText(request.data.groupId, 'group ID');
  const challengeId = requireText(request.data.challengeId, 'challenge ID');

  const membership = await db.doc(`groups/${groupId}/members/${uid}`).get();
  if (!membership.exists || membership.get('role') !== 'owner') {
    throw new HttpsError('permission-denied', 'Only the group owner can advance questions.');
  }

  // Atomic transaction: validate status is 'question_revealed' before advancing.
  // Prevents double-advance race conditions and ensures state machine integrity.
  const challengeRef = db.doc(`groups/${groupId}/challenges/${challengeId}`);
  let advanceResult = {};
  let needsCompletion = false;
  let completionData = {};

  await db.runTransaction(async (transaction) => {
    const challengeSnap = await transaction.get(challengeRef);
    if (!challengeSnap.exists) {
      throw new HttpsError('not-found', 'Challenge not found.');
    }

    const currentStatus = challengeSnap.get('status');

    // Idempotent: if already completed, return current state
    if (currentStatus === 'completed') {
      advanceResult = { status: 'completed' };
      return;
    }

    // If currently in 'finalizing' state (e.g. from an earlier interrupted completion),
    // allow resuming grading
    if (currentStatus === 'finalizing') {
      needsCompletion = true;
      completionData = {
        catalogue: challengeSnap.get('catalogue') || 'faith-quiz-global-v1',
        questions: challengeSnap.get('questions') || [],
        questionIds: challengeSnap.get('questionIds') || (challengeSnap.get('questions') || []).map((q) => q.id),
        participantUids: Array.isArray(challengeSnap.get('participantUids'))
          ? challengeSnap.get('participantUids')
          : null,
      };
      return;
    }

    if (currentStatus !== 'question_revealed') {
      throw new HttpsError('failed-precondition', 'Answer must be revealed before advancing.');
    }

    const currentQIndex = challengeSnap.get('currentQuestionIndex') || 0;
    const questions = challengeSnap.get('questions') || [];
    const nextIndex = currentQIndex + 1;

    if (nextIndex < questions.length) {
      // Advance to next question within the transaction
      transaction.update(challengeRef, {
        status: 'question_open',
        currentQuestionIndex: nextIndex,
        currentQuestionOpenedAt: FieldValue.serverTimestamp(),
        revealedAnswer: FieldValue.delete(),
        revealedExplanation: FieldValue.delete(),
        revealedScriptureReference: FieldValue.delete(),
        answeredUids: [],
      });
      advanceResult = {
        status: 'question_open',
        currentQuestionIndex: nextIndex,
      };
      return;
    }

    // All questions finished — mark as 'finalizing' so grading can run safely
    // and be retried if interrupted
    needsCompletion = true;
    completionData = {
      catalogue: challengeSnap.get('catalogue') || 'faith-quiz-global-v1',
      questions,
      questionIds: challengeSnap.get('questionIds') || questions.map((q) => q.id),
      participantUids: Array.isArray(challengeSnap.get('participantUids'))
        ? challengeSnap.get('participantUids')
        : null,
    };

    transaction.update(challengeRef, {
      status: 'finalizing',
      finalizingAt: FieldValue.serverTimestamp(),
    });
  });

  if (needsCompletion) {
    // Grade all members outside the transaction (subcollection reads)
    const { catalogue, questions, questionIds, participantUids } = completionData;

    // Fetch all answer docs from contentPrivate in batch
    const answerRefs = questionIds.map((qId) => db.doc(`contentPrivate/${catalogue}/answers/${qId}`));
    const answerSnaps = await db.getAll(...answerRefs);
    const correctMap = new Map();
    answerSnaps.forEach((doc) => {
      if (doc.exists) correctMap.set(doc.id, doc.get('correctAnswer'));
    });

    // Fetch all fellowshipAnswers
    const allAnswersSnap = await challengeRef.collection('fellowshipAnswers').get();
    const userAnswers = new Map(); // uid -> Map(qIndex -> { answerIndex, responseTime })
    allAnswersSnap.docs.forEach((d) => {
      const data = d.data();
      if (!userAnswers.has(data.uid)) userAnswers.set(data.uid, new Map());
      userAnswers.get(data.uid).set(data.questionIndex, {
        answerIndex: data.answerIndex,
        responseTime: data.responseTimeSeconds || 0,
      });
    });

    // New challenges grade only the frozen start-time roster.  Legacy
    // challenges without participantUids retain their historical behavior.
    let memberRecords;
    if (Array.isArray(participantUids)) {
      const memberRefs = participantUids.map((participantUid) =>
        db.doc(`groups/${groupId}/members/${participantUid}`));
      const memberSnaps = memberRefs.length > 0 ? await db.getAll(...memberRefs) : [];
      memberRecords = participantUids.map((participantUid, index) => ({
        uid: participantUid,
        snapshot: memberSnaps[index],
      }));
    } else {
      const membersSnap = await db.collection(`groups/${groupId}/members`).get();
      memberRecords = membersSnap.docs.map((snapshot) => ({
        uid: snapshot.id,
        snapshot,
      }));
    }
    const batch = db.batch();

    for (const memberRecord of memberRecords) {
      const memberUid = memberRecord.uid;
      const memberDoc = memberRecord.snapshot;
      const memberName = memberDoc && memberDoc.exists
        ? (memberDoc.get('displayName') || 'Faith learner')
        : 'Faith learner';
      const answersMap = userAnswers.get(memberUid) || new Map();

      let score = 0;
      let totalResponseTime = 0;
      for (let i = 0; i < questions.length; i++) {
        const q = questions[i];
        const correctAns = correctMap.get(q.id);
        const userAns = answersMap.get(i);
        if (userAns && userAns.answerIndex === correctAns) {
          score++;
        }
        if (userAns) {
          totalResponseTime += userAns.responseTime;
        }
      }

      const entryRef = challengeRef.collection('entries').doc(memberUid);
      batch.set(entryRef, {
        displayName: memberName,
        score,
        total: questions.length,
        correct: score === questions.length && questions.length > 0,
        elapsedSeconds: totalResponseTime,
        verified: true,
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
    }

    // Transition challenge to 'completed' ONLY AFTER all entries are written
    batch.update(challengeRef, {
      status: 'completed',
      completedAt: FieldValue.serverTimestamp(),
    });

    await batch.commit();
    advanceResult = { status: 'completed' };
  }

  return advanceResult;
});

exports.submitGroupChallenge = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const groupId = requireText(request.data.groupId, 'group ID');
  const challengeId = requireText(request.data.challengeId, 'challenge ID');
  const clientElapsedSeconds = Number(request.data.elapsedSeconds) || 0;
  const displayName = leaderboardName(request.data.displayName);

  const membership = db.doc(`groups/${groupId}/members/${uid}`);
  const challenge = db.doc(`groups/${groupId}/challenges/${challengeId}`);
  const [membershipSnapshot, challengeSnapshot] = await Promise.all([
    membership.get(), challenge.get(),
  ]);
  if (!membershipSnapshot.exists || !challengeSnapshot.exists) {
    throw new HttpsError('not-found', 'Group challenge is unavailable.');
  }

  if (!challengeHasParticipant(challengeSnapshot, uid)) {
    throw new HttpsError(
      'permission-denied',
      'You joined after this challenge started and cannot participate in it.'
    );
  }

  const catalogue = challengeSnapshot.get('catalogue') || 'faith-quiz-global-v1';

  // Check if this is a multi-question quiz challenge
  if (Array.isArray(request.data.answers)) {
    // 1. Mode check — reject using Competitive submit path for Fellowship challenges
    const mode = challengeSnapshot.get('mode') || 'competitive';
    if (mode === 'fellowship') {
      throw new HttpsError(
        'failed-precondition',
        'Fellowship challenges must be completed via host-led progression.'
      );
    }
    if (mode !== 'competitive') {
      throw new HttpsError('failed-precondition', 'Invalid challenge mode.');
    }

    // 2. Status check — strictly forbid pre-start submissions and answer leakage during lobby
    const status = challengeSnapshot.get('status') || 'lobby';
    if (status === 'lobby') {
      throw new HttpsError(
        'failed-precondition',
        'Challenge has not started yet. Pre-start submissions are forbidden.'
      );
    }
    if (status !== 'active' && status !== 'completed') {
      throw new HttpsError(
        'failed-precondition',
        `Challenge cannot be submitted in status "${status}".`
      );
    }

    const answers = request.data.answers;
    const questions = challengeSnapshot.get('questions') || [];
    const questionIds = challengeSnapshot.get('questionIds') || questions.map((q) => q.id);

    // Strict validation of answer array length
    if (answers.length !== questions.length) {
      throw new HttpsError('invalid-argument', 'Answer count does not match challenge questions.');
    }

    // 3. Server-authoritative elapsed time:
    // Leaderboard elapsed time must not be reducible by client input.
    // Use server-authoritative startedAt and current server time for official ranking.
    const startedAt = challengeSnapshot.get('startedAt');
    let officialElapsedSeconds = 60;
    if (startedAt && startedAt.toDate) {
      officialElapsedSeconds = Math.max(1, Math.floor((Date.now() - startedAt.toDate().getTime()) / 1000));
    } else if (Number.isInteger(clientElapsedSeconds) && clientElapsedSeconds > 0) {
      officialElapsedSeconds = Math.min(7200, clientElapsedSeconds);
    }
    officialElapsedSeconds = Math.min(7200, Math.max(1, officialElapsedSeconds));

    // Fetch all answers in a single batch read from contentPrivate
    const answerRefs = questionIds.map((qId) =>
      db.doc(`contentPrivate/${catalogue}/answers/${qId}`),
    );
    const answerSnapshots = await db.getAll(...answerRefs);
    const answerMap = new Map();
    answerSnapshots.forEach((doc) => {
      if (doc.exists) answerMap.set(doc.id, doc.data());
    });

    let score = 0;
    const breakdown = [];
    for (let i = 0; i < questions.length; i++) {
      const q = questions[i];
      const answerData = answerMap.get(q.id);
      const correctAnswer = answerData ? answerData.correctAnswer : 0;
      const explanation = answerData ? (answerData.explanation || '') : '';
      const userAnswer = Number.isInteger(answers[i]) ? answers[i] : -1;
      const isCorrect = userAnswer === correctAnswer;
      if (isCorrect) score++;
      breakdown.push({
        questionId: q.id,
        question: q.question || '',
        options: q.options || [],
        userAnswer,
        correctAnswer,
        correct: isCorrect,
        explanation,
        scriptureReference: q.scriptureReference || '',
      });
    }

    const entry = challenge.collection('entries').doc(uid);
    let finalScore = score;
    let finalElapsed = officialElapsedSeconds;
    let isAlreadySubmitted = false;

    await db.runTransaction(async (transaction) => {
      const previous = await transaction.get(entry);
      if (previous.exists) {
        // Idempotent: return existing stored result
        finalScore = previous.get('score') || score;
        finalElapsed = previous.get('elapsedSeconds') || officialElapsedSeconds;
        isAlreadySubmitted = true;
        return;
      }
      transaction.set(entry, {
        displayName,
        score,
        total: questions.length,
        correct: score === questions.length,
        elapsedSeconds: officialElapsedSeconds,
        clientReportedElapsedSeconds: Number.isInteger(clientElapsedSeconds) ? clientElapsedSeconds : null,
        verified: true,
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
    });

    return {
      score: finalScore,
      total: questions.length,
      elapsedSeconds: finalElapsed,
      breakdown,
      challengeId,
      correct: finalScore > 0,
      alreadySubmitted: isAlreadySubmitted,
    };
  }

  // Single-question legacy challenge
  const answerIndex = request.data.answerIndex;
  if (!Number.isInteger(answerIndex) || answerIndex < 0 || answerIndex > 3 ||
      !Number.isInteger(clientElapsedSeconds) || clientElapsedSeconds < 0 || clientElapsedSeconds > 600) {
    throw new HttpsError('invalid-argument', 'Invalid answer submission.');
  }
  const questionId = challengeSnapshot.get('questionId');
  const answer = await db.doc(`contentPrivate/${catalogue}/answers/${questionId}`).get();
  if (!answer.exists) throw new HttpsError('not-found', 'Challenge answer is unavailable.');
  const correct = answerIndex === answer.get('correctAnswer');
  const entry = challenge.collection('entries').doc(uid);

  let legacyScore = correct ? 1 : 0;
  await db.runTransaction(async (transaction) => {
    const previous = await transaction.get(entry);
    if (previous.exists) {
      legacyScore = previous.get('score') || legacyScore;
      return;
    }
    transaction.set(entry, {
      displayName,
      score: correct ? 1 : 0,
      total: 1,
      correct,
      elapsedSeconds: clientElapsedSeconds,
      verified: true,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });

  return { correct, score: legacyScore, total: 1, challengeId };
});

// Requires the Blaze plan when deployed because Cloud Scheduler invokes it.
// The app never registers a token unless the player has enabled the optional
// reminder switch. Invalid tokens are removed from the recipient list by FCM.
exports.sendDailyReminders = onSchedule({
  schedule: '0 18 * * *',
  timeZone: 'Africa/Accra',
  region: 'us-central1',
}, async () => {
  const snapshot = await db.collectionGroup('notifications')
      .where('remindersEnabled', '==', true)
      .get();
  const tokens = snapshot.docs
      .map((document) => document.get('fcmToken'))
      .filter((token) => typeof token === 'string' && token.length > 0);
  for (let start = 0; start < tokens.length; start += 500) {
    await getMessaging().sendEachForMulticast({
      tokens: tokens.slice(start, start + 500),
      notification: {
        title: 'Faith Quiz',
        body: 'Your daily Bible challenge is ready.',
      },
      data: { destination: 'daily_challenge' },
      android: { notification: { channelId: 'faith_quiz_reminders' } },
    });
  }
  return { sentTo: tokens.length };
});
