/*
 * Creates the safe initial Remote Config values for Faith Quiz. This keeps
 * online challenges disabled until the callable grading Functions and App
 * Check providers have been deployed to production.
 */
const fs = require('fs');
const os = require('os');
const path = require('path');

const projectId = 'faith-quiz-app-119653';

function firebaseToolsApi() {
  const globalRoot = process.env.APPDATA
    ? path.join(process.env.APPDATA, 'npm', 'node_modules')
    : path.join(os.homedir(), '.npm-global', 'lib', 'node_modules');
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
  template.parameters.cloud_challenges_enabled = { defaultValue: { value: 'false' } };
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
  console.log('Published Remote Config defaults: cloud_challenges_enabled=false, active_cloud_catalogue=faith-quiz-global-v1.');
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
