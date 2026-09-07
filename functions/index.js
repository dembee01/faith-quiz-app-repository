const { getApps, initializeApp } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
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
    const usernameSnap = await transaction.get(usernameRef);
    if (usernameSnap.exists && usernameSnap.get('uid') !== uid) {
      throw new HttpsError('already-exists', `The username "@${username}" is already taken. Please choose another.`);
    }

    const userSnap = await transaction.get(userRef);
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

    // Update username on global leaderboard if an entry exists
    const lbSnap = await transaction.get(globalLeaderboardRef);
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

    transaction.set(globalLeaderboardRef, {
      uid,
      displayName,
      score: newScore,
      totalAnswered: newAnswered,
      level: newLevel,
      accuracy: newAnswered > 0 ? Math.round((newScore / newAnswered) * 100) : 0,
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
  };
});

exports.createGroup = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const name = requireText(request.data.name, 'group name', 40);
  const group = db.collection('groups').doc();
  const createdAt = FieldValue.serverTimestamp();
  await db.runTransaction(async (transaction) => {
    transaction.set(group, { name, ownerUid: uid, createdAt });
    transaction.set(group.collection('members').doc(uid), { role: 'owner', joinedAt: createdAt });
    transaction.set(db.doc(`users/${uid}/groups/${group.id}`), {
      name,
      role: 'owner',
      joinedAt: createdAt,
    });
  });
  return { groupId: group.id };
});

exports.joinGroup = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const groupId = requireText(request.data.groupId, 'group ID');
  const group = db.doc(`groups/${groupId}`);
  await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(group);
    if (!snapshot.exists) throw new HttpsError('not-found', 'Group not found.');
    const joinedAt = FieldValue.serverTimestamp();
    transaction.set(group.collection('members').doc(uid), { role: 'member', joinedAt }, { merge: true });
    transaction.set(db.doc(`users/${uid}/groups/${groupId}`), {
      name: snapshot.get('name') || 'Faith Quiz group',
      role: 'member',
      joinedAt,
    }, { merge: true });
  });
  return { groupId };
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

  const requestedCount = Number(request.data.questionCount);
  const questionCount = Number.isInteger(requestedCount)
    ? Math.min(30, Math.max(5, requestedCount))
    : 10;
  const seed = Number.isInteger(request.data.seed)
    ? Math.abs(request.data.seed) % 500
    : Math.floor(Math.random() * 500);

  // If a single specific questionId was passed (legacy single-question mode)
  if (request.data.questionId && typeof request.data.questionId === 'string' && !request.data.questionCount) {
    const questionSnapshot = await db.doc(`content/${catalogue}/questions/${request.data.questionId}`).get();
    if (!questionSnapshot.exists || questionSnapshot.get('active') !== true) {
      throw new HttpsError('not-found', 'Challenge is unavailable.');
    }
    const challenge = group.collection('challenges').doc();
    await challenge.set({
      catalogue,
      questionId: request.data.questionId,
      sourceChallengeId: questionSnapshot.get('challengeId') || request.data.questionId,
      question: questionSnapshot.get('question'),
      options: questionSnapshot.get('options'),
      explanation: questionSnapshot.get('explanation'),
      scriptureReference: questionSnapshot.get('scriptureReference'),
      createdBy: uid,
      active: true,
      createdAt: FieldValue.serverTimestamp(),
    });
    return { challengeId: challenge.id };
  }

  // Multi-question rotated cloud challenge: select questionCount distinct indices
  // using coprime stride 263 to interleave Old Testament and New Testament questions.
  const targetIndices = [];
  for (let i = 0; i < questionCount; i++) {
    targetIndices.push((seed + i * 263) % 500);
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

  const title = typeof request.data.title === 'string' && request.data.title.trim().length > 0
    ? request.data.title.trim()
    : `Bible Challenge (${selectedQuestions.length} Questions)`;

  const challenge = group.collection('challenges').doc();
  await challenge.set({
    catalogue,
    title,
    questionCount: selectedQuestions.length,
    questions: selectedQuestions,
    questionIds: selectedQuestions.map((q) => q.id),
    seed,
    createdBy: uid,
    active: true,
    createdAt: FieldValue.serverTimestamp(),
  });
  return { challengeId: challenge.id, questionCount: selectedQuestions.length };
});

exports.submitGroupChallenge = onCall(callableOptions, async (request) => {
  const uid = requireUser(request);
  const groupId = requireText(request.data.groupId, 'group ID');
  const challengeId = requireText(request.data.challengeId, 'challenge ID');
  const elapsedSeconds = Number(request.data.elapsedSeconds) || 0;
  const displayName = leaderboardName(request.data.displayName);

  const membership = db.doc(`groups/${groupId}/members/${uid}`);
  const challenge = db.doc(`groups/${groupId}/challenges/${challengeId}`);
  const [membershipSnapshot, challengeSnapshot] = await Promise.all([
    membership.get(), challenge.get(),
  ]);
  if (!membershipSnapshot.exists || !challengeSnapshot.exists || challengeSnapshot.get('active') !== true) {
    throw new HttpsError('not-found', 'Group challenge is unavailable.');
  }

  const catalogue = challengeSnapshot.get('catalogue') || 'faith-quiz-global-v1';

  // Check if this is a multi-question quiz challenge
  if (Array.isArray(request.data.answers)) {
    const answers = request.data.answers;
    const questions = challengeSnapshot.get('questions') || [];
    const questionIds = challengeSnapshot.get('questionIds') || questions.map((q) => q.id);

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
    await db.runTransaction(async (transaction) => {
      const previous = await transaction.get(entry);
      if (previous.exists) {
        throw new HttpsError(
          'already-exists',
          'Each verified group challenge may be submitted only once.',
        );
      }
      transaction.set(entry, {
        displayName,
        score,
        total: questions.length,
        correct: score === questions.length,
        elapsedSeconds: Math.min(3600, Math.max(0, elapsedSeconds)),
        verified: true,
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
    });

    return {
      score,
      total: questions.length,
      elapsedSeconds,
      breakdown,
      challengeId,
      correct: score > 0,
    };
  }

  // Single-question legacy challenge
  const answerIndex = request.data.answerIndex;
  if (!Number.isInteger(answerIndex) || answerIndex < 0 || answerIndex > 3 ||
      !Number.isInteger(elapsedSeconds) || elapsedSeconds < 0 || elapsedSeconds > 600) {
    throw new HttpsError('invalid-argument', 'Invalid answer submission.');
  }
  const questionId = challengeSnapshot.get('questionId');
  const answer = await db.doc(`contentPrivate/${catalogue}/answers/${questionId}`).get();
  if (!answer.exists) throw new HttpsError('not-found', 'Challenge answer is unavailable.');
  const correct = answerIndex === answer.get('correctAnswer');
  const entry = challenge.collection('entries').doc(uid);
  await db.runTransaction(async (transaction) => {
    const previous = await transaction.get(entry);
    if (previous.exists) {
      throw new HttpsError(
        'already-exists',
        'Each verified group challenge may be submitted only once.',
      );
    }
    transaction.set(entry, {
      displayName,
      score: correct ? 1 : 0,
      total: 1,
      correct,
      elapsedSeconds,
      verified: true,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });
  return { correct, score: correct ? 1 : 0, total: 1, challengeId };
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
