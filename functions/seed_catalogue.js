/*
 * Upload the public challenge documents and their separate server-only answer
 * keys. Requires Application Default Credentials for faith-quiz-app-119653.
 * Run only after: gcloud auth application-default login
 */
const fs = require('fs');
const path = require('path');
const { initializeApp, applicationDefault } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');

const cataloguePath = path.resolve(__dirname, '..', 'data', 'cloud_prophets_witnesses_v1.json');
const projectId = 'faith-quiz-app-119653';

initializeApp({ credential: applicationDefault(), projectId });
const db = getFirestore();

async function commitInChunks(writes) {
  while (writes.length) {
    const chunk = writes.splice(0, 500);
    const batch = db.batch();
    for (const write of chunk) batch.set(write.ref, write.data);
    await batch.commit();
  }
}

async function main() {
  const catalogue = JSON.parse(fs.readFileSync(cataloguePath, 'utf8'));
  if (catalogue.questions.length !== 500) throw new Error('Catalogue must contain exactly 500 questions.');
  const catalogueRef = db.doc(`content/${catalogue.catalogueId}`);
  await catalogueRef.set({
    title: catalogue.title,
    questionCount: catalogue.questions.length,
    oldTestamentCount: catalogue.counts.oldTestament,
    newTestamentCount: catalogue.counts.newTestament,
    sourceTranslation: catalogue.source.translation,
    schemaVersion: 1,
    updatedAt: FieldValue.serverTimestamp(),
  });
  const publicWrites = [];
  const privateWrites = [];
  catalogue.questions.forEach((question, dailyIndex) => {
    const { correctAnswer, ...publicQuestion } = question;
    publicWrites.push({
      ref: db.doc(`content/${catalogue.catalogueId}/questions/${question.id}`),
      data: { ...publicQuestion, dailyIndex, updatedAt: FieldValue.serverTimestamp() },
    });
    privateWrites.push({
      ref: db.doc(`contentPrivate/${catalogue.catalogueId}/answers/${question.id}`),
      data: { correctAnswer, challengeId: question.challengeId, updatedAt: FieldValue.serverTimestamp() },
    });
  });
  await commitInChunks(publicWrites);
  await commitInChunks(privateWrites);
  console.log(`Uploaded ${catalogue.questions.length} public questions and ${catalogue.questions.length} private answer keys.`);
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
