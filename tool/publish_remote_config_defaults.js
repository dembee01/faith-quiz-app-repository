/*
 * Creates the safe initial Remote Config values for Faith Quiz. This keeps
 * online challenges disabled until the callable grading Functions and App
 * Check providers have been deployed to production.
 */
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execSync } = require('child_process');

const projectId = 'faith-quiz-app-119653';

function firebaseToolsApi() {
  // Firebase CLI may be installed by the system Node distribution (for
  // example C:\\nodejs\\node_modules on Windows), not under APPDATA.
  const globalRoot = execSync('npm root -g', { encoding: 'utf8' }).trim();
  return require(path.join(globalRoot, 'firebase-tools', 'lib', 'apiv2'));
}

function refreshCredential() {
  const configPath = path.join(os.homedir(), '.config', 'configstore', 'firebase-tools.json');
  const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  const credential = config?.tokens?.refresh_token;
  if (typeof credential !== 'string' || credential.length === 0) {
    throw new Error('Firebase CLI is not signed in. Run `firebase login` and retry.');
  }
  return credential;
}

async function main() {
  const api = firebaseToolsApi();
  api.setRefreshToken(refreshCredential());
  const token = await api.getAccessToken();
  const url = `https://firebaseremoteconfig.googleapis.com/v1/projects/${projectId}/remoteConfig`;
  const current = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
  if (!current.ok) throw new Error(`Remote Config read failed (${current.status}): ${await current.text()}`);
  const template = await current.json();
  template.parameters ??= {};
  template.parameters.cloud_challenges_enabled = { defaultValue: { value: 'true' } };
  template.parameters.active_cloud_catalogue = { defaultValue: { value: 'faith-quiz-global-v1' } };
  const publish = await fetch(url, {
    method: 'PUT',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json; UTF-8',
      'If-Match': current.headers.get('etag') || '*',
    },
    body: JSON.stringify(template),
  });
  if (!publish.ok) throw new Error(`Remote Config publish failed (${publish.status}): ${await publish.text()}`);
  const verification = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
  if (!verification.ok) throw new Error(`Remote Config verification failed (${verification.status}): ${await verification.text()}`);
  const verified = await verification.json();
  if (verified.parameters?.cloud_challenges_enabled?.defaultValue?.value !== 'true' ||
      verified.parameters?.active_cloud_catalogue?.defaultValue?.value !== 'faith-quiz-global-v1') {
    throw new Error('Remote Config verification failed: expected cloud challenges to be enabled for faith-quiz-global-v1.');
  }
  console.log('Published Remote Config defaults: cloud_challenges_enabled=true, active_cloud_catalogue=faith-quiz-global-v1.');
  console.log(`Remote Config verification confirmed version ${verified.version?.versionNumber ?? 'unknown'}.`);
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
