// Reads repository CI state, optionally enables the two quality gates.
// Uses Git Credential Manager in memory; credentials are never printed or saved.
const { execFileSync } = require('node:child_process');
const repository = 'dembee01/faith-quiz-app-repository';
const contexts = ['Flutter analyze and tests', 'Firebase handler and rules checks'];

async function main() {
  const credential = execFileSync('git', ['credential', 'fill'], {
    input: 'protocol=https\nhost=github.com\n\n', encoding: 'utf8',
    env: { ...process.env, GIT_TERMINAL_PROMPT: '0', GCM_INTERACTIVE: 'never' },
    stdio: ['pipe', 'pipe', 'pipe'],
  });
  const token = credential.split(/\r?\n/).find((line) => line.startsWith('password='))?.slice(9);
  if (!token) throw new Error('GitHub credential is unavailable.');
  async function api(endpoint, options = {}) {
    const response = await fetch('https://api.github.com/repos/' + repository + endpoint, {
      ...options,
      headers: { Authorization: 'Bearer ' + token, Accept: 'application/vnd.github+json',
        'Content-Type': 'application/json', 'X-GitHub-Api-Version': '2022-11-28' },
    });
    const result = await response.json();
    if (!response.ok) throw new Error('GitHub ' + response.status + ': ' + result.message);
    return result;
  }
  const branch = await api('/branches/master');
  const checks = await api('/commits/' + branch.commit.sha + '/check-runs');
  const runs = await api('/actions/runs?head_sha=' + branch.commit.sha);
  console.log(JSON.stringify({
    sha: branch.commit.sha, protected: branch.protected,
    checks: checks.check_runs.map(({ name, status, conclusion, html_url }) => ({ name, status, conclusion, html_url })),
    runs: runs.workflow_runs.map(({ id, name, status, conclusion, html_url }) => ({ id, name, status, conclusion, html_url })),
  }, null, 2));
  if (process.argv.includes('--details')) {
    for (const check of checks.check_runs) {
      console.log(JSON.stringify({
        check: check.name, output: check.output,
        annotations: await api('/check-runs/' + check.id + '/annotations'),
      }, null, 2));
    }
  }
  if (process.argv.includes('--protect')) {
    if (branch.protected) {
      console.log('Existing protection retained:');
      console.log(JSON.stringify(await api('/branches/master/protection'), null, 2));
      return;
    }
    for (const name of contexts) {
      if (!checks.check_runs.some((check) => check.name === name && check.conclusion === 'success')) {
        throw new Error('Cannot require an unverified check: ' + name);
      }
    }
    await api('/branches/master/protection', {
      method: 'PUT',
      body: JSON.stringify({
        required_status_checks: { strict: true, contexts },
        enforce_admins: true,
        required_pull_request_reviews: null,
        restrictions: null,
        allow_force_pushes: false,
        allow_deletions: false,
      }),
    });
    const verified = await api('/branches/master/protection');
    console.log(JSON.stringify({
      requiredChecks: verified.required_status_checks.contexts,
      enforceAdmins: verified.enforce_admins.enabled,
    }, null, 2));
  }
}

main().catch((error) => {
  // Avoid printing child-process objects, which can contain captured credentials.
  console.error(error.status !== undefined ? 'Git credential lookup failed.' : error.message);
  process.exitCode = 1;
});
