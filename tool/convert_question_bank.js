// Convert the canonical Kotlin question catalog to Dart.
// Run with: node tool/convert_question_bank.js
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const sourcePath = path.join(root, 'app/src/main/java/com/example/faithquiz/data/QuestionBank.kt');
const outputPath = path.join(root, 'lib/question_bank.dart');
const source = fs.readFileSync(sourcePath, 'utf8');
const topicSourcePath = path.join(root, 'app/src/main/java/com/example/faithquiz/data/TopicQuestionBank.kt');
const topicOutputPath = path.join(root, 'lib/topic_question_bank.dart');
const topicSource = fs.readFileSync(topicSourcePath, 'utf8');

function balanced(text, start, opening = '(') {
  const closing = {'(': ')', '[': ']', '{': '}'}[opening];
  let depth = 0;
  let quoted = false;
  let escaped = false;
  for (let i = start; i < text.length; i += 1) {
    const c = text[i];
    if (quoted) {
      if (escaped) escaped = false;
      else if (c === '\\') escaped = true;
      else if (c === '"') quoted = false;
      continue;
    }
    if (c === '"') quoted = true;
    else if (c === opening) depth += 1;
    else if (c === closing && --depth === 0) return [text.slice(start + 1, i), i + 1];
  }
  throw new Error(`Unclosed ${opening} at ${start}`);
}

function splitTopLevel(text) {
  const values = [];
  let start = 0;
  const stack = [];
  let quoted = false;
  let escaped = false;
  const pairs = {'(': ')', '[': ']', '{': '}'};
  for (let i = 0; i < text.length; i += 1) {
    const c = text[i];
    if (quoted) {
      if (escaped) escaped = false;
      else if (c === '\\') escaped = true;
      else if (c === '"') quoted = false;
      continue;
    }
    if (c === '"') quoted = true;
    else if (pairs[c]) stack.push(pairs[c]);
    else if (stack.length && c === stack[stack.length - 1]) stack.pop();
    else if (c === ',' && stack.length === 0) {
      values.push(text.slice(start, i).trim());
      start = i + 1;
    }
  }
  const tail = text.slice(start).trim();
  if (tail) values.push(tail);
  return values;
}

function parseValue(value) {
  value = value.trim();
  if (value === 'null') return null;
  if (value.startsWith('listOf(')) {
    const [inner] = balanced(value, value.indexOf('('));
    return splitTopLevel(inner).map(parseValue);
  }
  if (value.startsWith('"')) return JSON.parse(value);
  return value;
}

function parseFields(block) {
  const fields = {};
  for (const part of splitTopLevel(block)) {
    const equals = part.indexOf('=');
    if (equals < 0) continue;
    fields[part.slice(0, equals).trim()] = parseValue(part.slice(equals + 1));
  }
  return fields;
}

function parseQuestions(region) {
  const questions = [];
  let cursor = 0;
  while (true) {
    const marker = region.indexOf('QuizQuestion(', cursor);
    if (marker < 0) return questions;
    const [block, next] = balanced(region, region.indexOf('(', marker));
    questions.push(parseFields(block));
    cursor = next;
  }
}

function dartString(value) {
  return JSON.stringify(String(value));
}

function renderQuestion(q) {
  const lines = [
    'QuizQuestion(',
    `  question: ${dartString(q.question || '')},`,
    '  options: [',
    ...(q.options || []).map((option) => `    ${dartString(option)},`),
    '  ],',
    `  correctAnswer: ${q.correctAnswer || 0},`,
    `  explanation: ${dartString(q.explanation || '')},`,
  ];
  for (const key of ['verseReference', 'verseText', 'translation', 'commentary', 'learnMoreUrl']) {
    if (q[key] != null) lines.push(`  ${key}: ${dartString(q[key])},`);
  }
  if (q.crossRefs && q.crossRefs.length) {
    lines.push('  crossRefs: [', ...q.crossRefs.map((ref) => `    ${dartString(ref)},`), '  ],');
  }
  lines.push('),');
  return lines;
}

const markers = [...source.matchAll(/private val level(\d+)Questions = (?:listOf\(|createAdvancedQuestions\("([^"]+)",\s*15\))/g)];
const levels = new Map();
markers.forEach((marker, index) => {
  const level = Number(marker[1]);
  if (marker[2]) {
    const functionName = `get${marker[2].replace(/[^A-Za-z0-9]/g, '')}Questions`;
    const functionStart = source.indexOf(`private fun ${functionName}`);
    if (functionStart < 0) throw new Error(`Cannot find ${functionName}`);
    const listStart = source.indexOf('listOf(', functionStart);
    const [region] = balanced(source, listStart);
    levels.set(level, parseQuestions(region).slice(0, 15));
    return;
  }
  const start = marker.index + marker[0].length;
  const end = index + 1 < markers.length ? markers[index + 1].index : source.length;
  levels.set(level, parseQuestions(source.slice(start, end)));
});

const lines = [
  '// GENERATED FILE. Run `node tool/convert_question_bank.js` after editing QuestionBank.kt.',
  "import 'models.dart';", '', 'class QuestionBank {',
  '  static const availableLevels = <int>[',
  ...[...levels.keys()].sort((a, b) => a - b).map((level) => `    ${level},`),
  '  ];', '', '  static const levels = <int, List<QuizQuestion>>{',
];
for (const level of [...levels.keys()].sort((a, b) => a - b)) {
  lines.push(`    ${level}: [`);
  for (const q of levels.get(level)) lines.push(...renderQuestion(q).map((line) => `      ${line}`));
  lines.push('    ],');
}
lines.push('  };', '', '  static List<QuizQuestion> forLevel(int level) =>', '      levels[level] ?? const <QuizQuestion>[];', '}', '');
fs.writeFileSync(outputPath, lines.join('\n'), 'utf8');

const topicMarkers = [...topicSource.matchAll(/private val (gospels|prophets|parables)Questions = listOf\(/g)];
const topics = new Map();
topicMarkers.forEach((marker, index) => {
  const start = marker.index + marker[0].length;
  const end = index + 1 < topicMarkers.length ? topicMarkers[index + 1].index : topicSource.length;
  topics.set(marker[1], parseQuestions(topicSource.slice(start, end)));
});
const topicLines = [
  '// GENERATED FILE. Run `node tool/convert_question_bank.js` after editing TopicQuestionBank.kt.',
  "import 'models.dart';", '', 'class TopicQuestionBank {',
  '  static const topics = <String>[',
  ...[...topics.keys()].map((topic) => `    ${JSON.stringify(topic)},`),
  '  ];', '', '  static const questions = <String, List<QuizQuestion>>{',
];
for (const [topic, questions] of topics) {
  topicLines.push(`    ${JSON.stringify(topic)}: [`);
  for (const q of questions) topicLines.push(...renderQuestion(q).map((line) => `      ${line}`));
  topicLines.push('    ],');
}
topicLines.push('  };', '', '  static List<QuizQuestion> forTopic(String topic) =>', '      questions[topic] ?? const <QuizQuestion>[];', '', '  static String achievementTitle(String topic, int score) {',
  '    final name = topic[0].toUpperCase() + topic.substring(1);',
  '    if (score == 50) return "Supreme $name Master";',
  '    if (score >= 45) return "$name Expert";',
  '    if (score > 0) return "$name Learner";',
  '    return "$name Beginner";',
  '  }', '',
  '  static String encouragement(String topic, int score) {',
  '    final percentage = (score * 100) ~/ 50;',
  '    final focus = topic == "gospels" ? "the life and teachings of Jesus" : topic == "prophets" ? "God\'s messengers" : "Jesus\' wisdom stories";',
  '    return "You scored $score/50 ($percentage%) on the ${topic[0].toUpperCase()}${topic.substring(1)} quiz. Keep exploring $focus!";',
  '  }',
  '}', '');
fs.writeFileSync(topicOutputPath, topicLines.join('\n'), 'utf8');
console.log(`Generated ${[...levels.values()].reduce((sum, items) => sum + items.length, 0)} questions across ${levels.size} levels and ${[...topics.values()].reduce((sum, items) => sum + items.length, 0)} topic questions`);
