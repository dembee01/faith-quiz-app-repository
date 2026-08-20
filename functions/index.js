const { getApps, initializeApp } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');
const { HttpsError, onCall } = require('firebase-functions/v2/https');
const { onSchedule } = require('firebase-functions/v2/scheduler');

if (getApps().length === 0) {
  initializeApp();
}
const db = getFirestore();

function requireUser(request) {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Sign in is required.');
  return request.auth.uid;
}

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

// A client supplies only its selected answer. The correct answer is read from
// the curated catalogue on the server, so a modified app cannot submit a
// fabricated score directly to the leaderboard.
exports.submitCloudChallenge = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = requireUser(request);
  const catalogue = requireText(request.data.catalogue, 'catalogue');
  const questionId = requireText(request.data.questionId, 'question ID');
  const displayName = leaderboardName(request.data.displayName);
  const answerIndex = request.data.answerIndex;
  const elapsedSeconds = request.data.elapsedSeconds;
  if (!Number.isInteger(answerIndex) || answerIndex < 0 || answerIndex > 3 ||
      !Number.isInteger(elapsedSeconds) || elapsedSeconds < 0 || elapsedSeconds > 600) {
    throw new HttpsError('invalid-argument', 'Invalid answer submission.');
  }

  const publicQuestionRef = db.doc(`content/${catalogue}/questions/${questionId}`);
  const privateAnswerRef = db.doc(`contentPrivate/${catalogue}/answers/${questionId}`);
  const [question, privateAnswer] = await Promise.all([
    publicQuestionRef.get(),
    privateAnswerRef.get(),
  ]);
  if (!question.exists || !privateAnswer.exists || question.get('active') !== true) {
    throw new HttpsError('not-found', 'Challenge is unavailable.');
  }
  const correct = answerIndex === privateAnswer.get('correctAnswer');
  const challengeId = question.get('challengeId') || questionId;
  const entryRef = db.doc(`leaderboards/${challengeId}/entries/${uid}`);
  await db.runTransaction(async (transaction) => {
    const previous = await transaction.get(entryRef);
    if (previous.exists) {
      throw new HttpsError(
        'already-exists',
        'Each verified cloud challenge may be submitted only once.',
      );
    }
    transaction.set(entryRef, {
      score: correct ? 1 : 0,
      correct,
      elapsedSeconds,
      displayName,
      updatedAt: FieldValue.serverTimestamp(),
      verified: true,
    }, { merge: true });
  });
  return { correct, challengeId };
});

exports.createGroup = onCall({ enforceAppCheck: true }, async (request) => {
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

exports.joinGroup = onCall({ enforceAppCheck: true }, async (request) => {
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
exports.createGroupChallenge = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = requireUser(request);
  const groupId = requireText(request.data.groupId, 'group ID');
  const catalogue = requireText(request.data.catalogue, 'catalogue');
  const questionId = requireText(request.data.questionId, 'question ID');
  const group = db.doc(`groups/${groupId}`);
  const membership = db.doc(`groups/${groupId}/members/${uid}`);
  const question = db.doc(`content/${catalogue}/questions/${questionId}`);
  const [groupSnapshot, membershipSnapshot, questionSnapshot] = await Promise.all([
    group.get(), membership.get(), question.get(),
  ]);
  if (!groupSnapshot.exists || !membershipSnapshot.exists) {
    throw new HttpsError('permission-denied', 'Join the group before creating a challenge.');
  }
  if (membershipSnapshot.get('role') !== 'owner') {
    throw new HttpsError('permission-denied', 'Only the group owner can create a challenge.');
  }
  if (!questionSnapshot.exists || questionSnapshot.get('active') !== true) {
    throw new HttpsError('not-found', 'Challenge is unavailable.');
  }
  const challenge = group.collection('challenges').doc();
  await challenge.set({
    catalogue,
    questionId,
    sourceChallengeId: questionSnapshot.get('challengeId') || questionId,
    question: questionSnapshot.get('question'),
    options: questionSnapshot.get('options'),
    explanation: questionSnapshot.get('explanation'),
    scriptureReference: questionSnapshot.get('scriptureReference'),
    createdBy: uid,
    active: true,
    createdAt: FieldValue.serverTimestamp(),
  });
  return { challengeId: challenge.id };
});

exports.submitGroupChallenge = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = requireUser(request);
  const groupId = requireText(request.data.groupId, 'group ID');
  const challengeId = requireText(request.data.challengeId, 'challenge ID');
  const answerIndex = request.data.answerIndex;
  const elapsedSeconds = request.data.elapsedSeconds;
  const displayName = leaderboardName(request.data.displayName);
  if (!Number.isInteger(answerIndex) || answerIndex < 0 || answerIndex > 3 ||
      !Number.isInteger(elapsedSeconds) || elapsedSeconds < 0 || elapsedSeconds > 600) {
    throw new HttpsError('invalid-argument', 'Invalid answer submission.');
  }
  const membership = db.doc(`groups/${groupId}/members/${uid}`);
  const challenge = db.doc(`groups/${groupId}/challenges/${challengeId}`);
  const [membershipSnapshot, challengeSnapshot] = await Promise.all([
    membership.get(), challenge.get(),
  ]);
  if (!membershipSnapshot.exists || !challengeSnapshot.exists || challengeSnapshot.get('active') !== true) {
    throw new HttpsError('not-found', 'Group challenge is unavailable.');
  }
  const catalogue = challengeSnapshot.get('catalogue');
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
      correct,
      elapsedSeconds,
      verified: true,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });
  return { correct, challengeId };
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
