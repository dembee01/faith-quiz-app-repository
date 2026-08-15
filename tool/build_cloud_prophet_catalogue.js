/*
 * Builds the cloud-only Prophets & Witnesses catalogue from a public-domain
 * KJV verse index. The output deliberately keeps the current app's offline
 * question packs untouched.
 *
 * Run: node tool/build_cloud_prophet_catalogue.js
 */
const crypto = require('crypto');
const fs = require('fs');
const https = require('https');
const path = require('path');

const sourceUrl =
  'https://raw.githubusercontent.com/farskipper/kjv/master/json/verses-1769.json';
const outputPath = path.resolve(
  __dirname,
  '..',
  'data',
  'cloud_prophets_witnesses_v1.json',
);

const oldTestamentBooks = [
  'Isaiah', 'Jeremiah', 'Ezekiel', 'Daniel',
  'Hosea', 'Joel', 'Amos', 'Obadiah', 'Jonah', 'Micah', 'Nahum',
  'Habakkuk', 'Zephaniah', 'Haggai', 'Zechariah', 'Malachi',
];
const oldTestamentCounts = Object.fromEntries(oldTestamentBooks.map((book) => [book, 25]));
// Obadiah contains 21 verses. The four remaining questions are added to Isaiah
// so the Old Testament portion still contains exactly 400 genuine verses.
oldTestamentCounts.Obadiah = 20;
oldTestamentCounts.Isaiah = 30;

// Each New Testament range is a passage explicitly associated with a prophet,
// a prophetic witness, or the prophetic teaching of Jesus. Counts total 100.
const newTestamentPassages = [
  { book: 'Matthew', start: [3, 1], end: [3, 12], count: 12, focus: 'John the Baptist' },
  { book: 'Mark', start: [1, 1], end: [1, 10], count: 10, focus: 'John the Baptist' },
  { book: 'Luke', start: [1, 67], end: [1, 79], count: 13, focus: 'Zechariah\'s prophecy' },
  { book: 'Luke', start: [2, 25], end: [2, 35], count: 11, focus: 'Simeon\'s prophecy' },
  { book: 'Luke', start: [2, 36], end: [2, 38], count: 3, focus: 'Anna the prophetess' },
  { book: 'Acts', start: [11, 27], end: [11, 30], count: 4, focus: 'Agabus and the prophets' },
  { book: 'Acts', start: [13, 1], end: [13, 4], count: 4, focus: 'prophets and teachers at Antioch' },
  { book: 'Acts', start: [15, 30], end: [15, 35], count: 6, focus: 'Judas and Silas' },
  { book: 'Acts', start: [21, 8], end: [21, 9], count: 2, focus: 'Philip\'s prophetic daughters' },
  { book: 'Acts', start: [21, 10], end: [21, 14], count: 5, focus: 'Agabus' },
  { book: 'Matthew', start: [24, 4], end: [24, 17], count: 14, focus: 'Jesus\' prophetic teaching' },
  { book: 'Revelation', start: [1, 1], end: [1, 7], count: 7, focus: 'John\'s prophetic vision' },
  { book: 'Revelation', start: [10, 1], end: [10, 6], count: 6, focus: 'John\'s prophetic commission' },
  { book: 'John', start: [1, 6], end: [1, 19], count: 3, focus: 'John the Baptist' },
];

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
  return value
    .replace(/^#\s*/, '')
    .replace(/\[|\]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

function referenceParts(reference) {
  const match = /^(.*?) (\d+):(\d+)$/.exec(reference);
  if (!match) throw new Error(`Unexpected verse reference: ${reference}`);
  return { book: match[1], chapter: Number(match[2]), verse: Number(match[3]) };
}

function quotedExcerpt(text) {
  if (text.length <= 178) return text;
  const cut = text.slice(0, 175).lastIndexOf(' ');
  return `${text.slice(0, Math.max(cut, 120)).trim()}…`;
}

function usefulVerse(text) {
  return text.length >= 48 && text.length <= 430 && /[A-Za-z]{4}/.test(text);
}

function evenlySelect(values, count) {
  if (values.length < count) {
    throw new Error(`Needed ${count} verses but only found ${values.length}`);
  }
  if (count === 1) return [values[Math.floor(values.length / 2)]];
  return Array.from({ length: count }, (_, index) =>
    values[Math.round((index * (values.length - 1)) / (count - 1))],
  );
}

function seededOptions(answer, pool, key) {
  const hash = crypto.createHash('sha256').update(key).digest();
  const others = pool.filter((entry) => entry !== answer);
  const start = hash[0] % others.length;
  const chosen = [];
  for (let offset = 0; chosen.length < 3; offset += 1) {
    const candidate = others[(start + offset) % others.length];
    if (!chosen.includes(candidate)) chosen.push(candidate);
  }
  const result = [answer, ...chosen];
  for (let index = result.length - 1; index > 0; index -= 1) {
    const swap = hash[index] % (index + 1);
    [result[index], result[swap]] = [result[swap], result[index]];
  }
  return { options: result, correctAnswer: result.indexOf(answer) };
}

function idFor(prefix, reference) {
  return `${prefix}-${reference.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`.replace(/-$/, '');
}

function entriesForBook(source, book, minimumLength = 48, avoidBookName = false) {
  const bookPattern = new RegExp(`\\b${book.replace(/[^A-Za-z]/g, '')}\\b`, 'i');
  return Object.entries(source)
    .map(([reference, raw]) => ({ reference, text: cleanVerse(raw), ...referenceParts(reference) }))
    .filter((entry) => entry.book === book && entry.text.length >= minimumLength && entry.text.length <= 430)
    .filter((entry) => !avoidBookName || !bookPattern.test(entry.text))
    .sort((a, b) => a.chapter - b.chapter || a.verse - b.verse);
}

function inRange(entry, start, end) {
  const afterStart = entry.chapter > start[0] || (entry.chapter === start[0] && entry.verse >= start[1]);
  const beforeEnd = entry.chapter < end[0] || (entry.chapter === end[0] && entry.verse <= end[1]);
  return afterStart && beforeEnd;
}

function oldQuestion(entry, book) {
  const answer = seededOptions(book, oldTestamentBooks, entry.reference);
  return {
    id: idFor('ot', entry.reference),
    challengeId: idFor('prophets-v1', entry.reference),
    active: true,
    testament: 'Old Testament',
    category: 'old-testament-prophets',
    propheticFocus: book,
    question: `Which Old Testament prophetic book contains this passage?\n\n“${quotedExcerpt(entry.text)}”`,
    ...answer,
    explanation: `This passage is from ${entry.reference}, in the book of ${book}.`,
    scriptureReference: entry.reference,
    sourceTranslation: 'King James Version (public domain)',
  };
}

function newQuestion(entry, spec, newBooks) {
  const answer = seededOptions(spec.book, newBooks, entry.reference);
  return {
    id: idFor('nt', entry.reference),
    challengeId: idFor('prophets-v1', entry.reference),
    active: true,
    testament: 'New Testament',
    category: 'new-testament-prophetic-witnesses',
    propheticFocus: spec.focus,
    question: `Which New Testament book records this prophetic witness or prophecy passage?\n\n“${quotedExcerpt(entry.text)}”`,
    ...answer,
    explanation: `This passage is from ${entry.reference}. It belongs to the New Testament witness of ${spec.focus}.`,
    scriptureReference: entry.reference,
    sourceTranslation: 'King James Version (public domain)',
  };
}

async function main() {
  const source = await fetchJson(sourceUrl);
  const oldQuestions = oldTestamentBooks.flatMap((book) =>
    evenlySelect(entriesForBook(source, book, 48, true), oldTestamentCounts[book])
      .map((entry) => oldQuestion(entry, book)),
  );
  const newBooks = [...new Set(newTestamentPassages.map((spec) => spec.book))];
  const newQuestions = newTestamentPassages.flatMap((spec) => {
    // A few short prophetic speech lines are still clear in their immediate
    // named passage, so New Testament passages accept 24+ character verses.
    const candidates = entriesForBook(source, spec.book, 24)
      .filter((entry) => inRange(entry, spec.start, spec.end));
    return evenlySelect(candidates, spec.count).map((entry) => newQuestion(entry, spec, newBooks));
  });
  const questions = [...oldQuestions, ...newQuestions];
  if (questions.length !== 500) throw new Error(`Expected 500 questions, found ${questions.length}`);
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify({
    catalogueId: 'faith-quiz-global-v1',
    title: 'Prophets & Witnesses: Global Challenge',
    source: { url: sourceUrl, translation: 'King James Version', publicDomain: true },
    generatedAt: '2026-08-13',
    counts: { total: questions.length, oldTestament: oldQuestions.length, newTestament: newQuestions.length },
    questions,
  }, null, 2)}\n`, 'utf8');
  console.log(`Generated ${oldQuestions.length} Old Testament and ${newQuestions.length} New Testament cloud questions.`);
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
