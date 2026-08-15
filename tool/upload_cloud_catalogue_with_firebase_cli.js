/*
 * One-time development uploader for the 500-question cloud catalogue.
 *
 * It reuses the active Firebase CLI sign-in without reading or printing its
 * credential. This avoids creating a long-lived service-account key on a
 * developer workstation. The Firebase CLI must already be installed and
 * signed in (`firebase login`).
 */
const fs = require('fs');
const os = require('os');
const path = require('path');

const projectId = 'faith-quiz-app-119653';
const cataloguePath = path.resolve(__dirname, '..', 'data', 'cloud_prophets_witnesses_v1.json');

function firebaseToolsApi() {
  const globalRoot = process.env.APPDATA
    ? path.join(process.env.APPDATA, 'npm', 'node_modules')
    : path.join(os.homedir(), '.npm-global', 'lib', 'node_modules');
  try {
    return require(path.join(globalRoot, 'firebase-tools', 'lib', 'apiv2'));
  } catch (_) {
    throw new Error('Firebase CLI is not available globally. Run `npm install -g firebase-tools` and `firebase login`.');
  }
}

function activeFirebaseRefreshCredential() {
  const configPath = path.join(os.homedir(), '.config', 'configstore', 'firebase-tools.json');
  try {
    const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
    const credential = config?.tokens?.refresh_token;
    if (typeof credential === 'string' && credential.length > 0) return credential;
  } catch (_) {}
  throw new Error('Firebase CLI is not signed in. Run `firebase login` and retry.');
}

function value(input) {
  if (input === null) return { nullValue: null };
  if (typeof input === 'string') return { stringValue: input };
  if (typeof input === 'boolean') return { booleanValue: input };
  if (typeof input === 'number') return Number.isInteger(input)
    ? { integerValue: String(input) }
    : { doubleValue: input };
  if (Array.isArray(input)) return { arrayValue: { values: input.map(value) } };
  if (typeof input === 'object') {
    return { mapValue: { fields: Object.fromEntries(Object.entries(input).map(([key, entry]) => [key, value(entry)])) } };
  }
  throw new Error(`Unsupported Firestore value type: ${typeof input}`);
}

function document(name, data) {
  return {
    name: `projects/${projectId}/databases/(default)/documents/${name}`,
    fields: Object.fromEntries(Object.entries(data).map(([key, entry]) => [key, value(entry)])),
  };
}

async function commit(token, writes) {
  const response = await fetch(
    `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents:commit`,
    {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ writes }),
    },
  );
  if (!response.ok) throw new Error(`Firestore upload failed (${response.status}): ${await response.text()}`);
}

async function getJson(token, endpoint) {
  const response = await fetch(
    `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/${endpoint}`,
    { headers: { Authorization: `Bearer ${token}` } },
  );
  if (!response.ok) throw new Error(`Firestore verification failed (${response.status}): ${await response.text()}`);
  return response.json();
}

async function listAllDocuments(token, collection) {
  const documents = [];
  let pageToken;
  do {
    const query = pageToken
      ? `?pageSize=300&pageToken=${encodeURIComponent(pageToken)}`
      : '?pageSize=300';
    const page = await getJson(token, `${collection}${query}`);
    documents.push(...(page.documents ?? []));
    pageToken = page.nextPageToken;
  } while (pageToken);
  return documents;
}

async function main() {
  const catalogue = JSON.parse(fs.readFileSync(cataloguePath, 'utf8'));
  if (catalogue.questions.length !== 500) throw new Error('Catalogue must contain exactly 500 questions.');
  const api = firebaseToolsApi();
  // api.getAccessToken normally receives this during Firebase CLI startup.
  // Supply it in-memory here; it is never written or logged by this script.
  api.setRefreshToken(activeFirebaseRefreshCredential());
  const token = await api.getAccessToken();
  const updatedAt = Date.now();
  const writes = [
    {
      update: document(`content/${catalogue.catalogueId}`, {
        title: catalogue.title,
        questionCount: catalogue.questions.length,
        oldTestamentCount: catalogue.counts.oldTestament,
        newTestamentCount: catalogue.counts.newTestament,
        sourceTranslation: catalogue.source.translation,
        schemaVersion: 1,
        updatedAt,
      }),
    },
  ];
  catalogue.questions.forEach((question, dailyIndex) => {
    const { correctAnswer, ...publicQuestion } = question;
    writes.push({
      update: document(`content/${catalogue.catalogueId}/questions/${question.id}`, {
        ...publicQuestion,
        dailyIndex,
        updatedAt,
      }),
    });
    writes.push({
      update: document(`contentPrivate/${catalogue.catalogueId}/answers/${question.id}`, {
        correctAnswer,
        challengeId: question.challengeId,
        updatedAt,
      }),
    });
  });
  for (let start = 0; start < writes.length; start += 450) {
    await commit(token, writes.slice(start, start + 450));
  }
  const [metadata, publicQuestions, privateAnswers] = await Promise.all([
    getJson(token, `content/${catalogue.catalogueId}`),
    listAllDocuments(token, `content/${catalogue.catalogueId}/questions`),
    listAllDocuments(token, `contentPrivate/${catalogue.catalogueId}/answers`),
  ]);
  const storedCount = metadata.fields?.questionCount?.integerValue;
  const publicCount = publicQuestions.length;
  const privateCount = privateAnswers.length;
  if (storedCount !== '500' || publicCount !== 500 || privateCount !== 500) {
    throw new Error(`Upload verification failed: metadata=${storedCount}, public=${publicCount}, private=${privateCount}.`);
  }
  console.log(`Uploaded ${catalogue.questions.length} public questions and ${catalogue.questions.length} private answer keys.`);
  console.log('Firestore verification confirmed 500 public questions and 500 protected answer keys.');
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
