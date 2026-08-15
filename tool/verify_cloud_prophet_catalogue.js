/* Verify the 500-question cloud catalogue against its online public-domain source. */
const crypto = require('crypto');
const fs = require('fs');
const https = require('https');
const path = require('path');

const cataloguePath = path.resolve(__dirname, '..', 'data', 'cloud_prophets_witnesses_v1.json');

function fetchJson(url) {
  return new Promise((resolve, reject) => {
    https.get(url, (response) => {
      if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
        response.resume();
        return resolve(fetchJson(response.headers.location));
      }
      if (response.statusCode !== 200) {
        response.resume();
        return reject(new Error(`Source request failed with HTTP ${response.statusCode}`));
      }
      let body = '';
      response.setEncoding('utf8');
      response.on('data', (chunk) => { body += chunk; });
      response.on('end', () => {
        try { resolve(JSON.parse(body)); } catch (error) { reject(error); }
      });
    }).on('error', reject);
  });
}

function cleanVerse(value) {
  return value.replace(/^#\s*/, '').replace(/\[|\]/g, '').replace(/\s+/g, ' ').trim();
}

function sourceBook(reference) {
  const match = /^(.*?) \d+:\d+$/.exec(reference);
  if (!match) throw new Error(`Invalid reference: ${reference}`);
  return match[1];
}

async function main() {
  const catalogue = JSON.parse(fs.readFileSync(cataloguePath, 'utf8'));
  const source = await fetchJson(catalogue.source.url);
  const errors = [];
  const ids = new Set();
  const prompts = new Set();
  const references = new Set();
  let oldCount = 0;
  let newCount = 0;
  for (const question of catalogue.questions) {
    const prefix = question.testament === 'Old Testament' ? 'ot' : question.testament === 'New Testament' ? 'nt' : null;
    if (!prefix) errors.push(`${question.id}: invalid testament`);
    if (ids.has(question.id)) errors.push(`${question.id}: duplicate id`);
    if (prompts.has(question.question)) errors.push(`${question.id}: duplicate prompt`);
    if (references.has(question.scriptureReference)) errors.push(`${question.id}: duplicate verse reference`);
    ids.add(question.id); prompts.add(question.question); references.add(question.scriptureReference);
    if (!Array.isArray(question.options) || question.options.length !== 4 || new Set(question.options).size !== 4) {
      errors.push(`${question.id}: options must contain four unique values`);
    }
    if (!Number.isInteger(question.correctAnswer) || question.correctAnswer < 0 || question.correctAnswer > 3) {
      errors.push(`${question.id}: invalid correct answer index`);
    }
    const verse = source[question.scriptureReference];
    if (!verse) errors.push(`${question.id}: source verse is missing`);
    const sourceText = verse ? cleanVerse(verse) : '';
    const quoted = (question.question.match(/“(.+)”/s) || [])[1]?.replace(/…$/, '') || '';
    if (quoted && !sourceText.startsWith(quoted)) errors.push(`${question.id}: excerpt does not match source verse`);
    const answer = question.options?.[question.correctAnswer];
    if (answer !== sourceBook(question.scriptureReference)) {
      errors.push(`${question.id}: answer does not match source book`);
    }
    if (!question.question?.startsWith('Which ') || question.question.length > 360) {
      errors.push(`${question.id}: question is not within the clear prompt format`);
    }
    if (question.question.includes(question.scriptureReference)) {
      errors.push(`${question.id}: prompt reveals its answer reference`);
    }
    if (!question.explanation?.includes(question.scriptureReference)) errors.push(`${question.id}: explanation lacks reference`);
    if (question.testament === 'Old Testament') oldCount += 1;
    if (question.testament === 'New Testament') newCount += 1;
  }
  if (catalogue.questions.length !== 500) errors.push(`Expected 500 questions, found ${catalogue.questions.length}`);
  if (oldCount !== 400 || newCount !== 100) errors.push(`Expected 400/100 testament split, found ${oldCount}/${newCount}`);
  if (errors.length) throw new Error(`Catalogue verification failed:\n${errors.join('\n')}`);
  const digest = crypto.createHash('sha256').update(fs.readFileSync(cataloguePath)).digest('hex');
  console.log(`Verified ${catalogue.questions.length} unique questions against ${catalogue.source.translation}.`);
  console.log(`Old Testament: ${oldCount}; New Testament: ${newCount}; SHA-256: ${digest}`);
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
