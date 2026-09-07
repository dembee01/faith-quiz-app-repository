/**
 * Firestore Security Rules Contract Test
 * 
 * Validates the rule logic expectations by parsing the rules file and verifying
 * that critical security constraints are present. Since the Firebase Emulator
 * requires Java (not available), this tests rule structure rather than execution.
 * 
 * Run with: node functions/test_rules.js
 */

const assert = require('assert');
const fs = require('fs');
const path = require('path');

console.log('--- FIRESTORE SECURITY RULES CONTRACT TEST ---\n');

const rulesPath = path.join(__dirname, '..', 'firestore.rules');
const rules = fs.readFileSync(rulesPath, 'utf8');

// 1. contentPrivate is completely inaccessible from clients
console.log('[Rule 1] contentPrivate is client-inaccessible');
const privateMatch = rules.match(/match\s+\/contentPrivate\/{catalogue}\/answers\/{questionId}\s*{([^}]*)}/s);
assert(privateMatch, 'contentPrivate rule block must exist');
assert(privateMatch[1].includes('allow read, write: if false'), 'contentPrivate must deny all read/write');
console.log('  PASSED: contentPrivate/{catalogue}/answers/{questionId} denies all client access.');

// 2. fellowshipAnswers are read-only to the answer owner
console.log('[Rule 2] fellowshipAnswers owner-only read, no client write');
const fellowshipMatch = rules.match(/match\s+\/groups\/{groupId}\/challenges\/{challengeId}\/fellowshipAnswers\/{answerId}\s*{([^}]*)}/s);
assert(fellowshipMatch, 'fellowshipAnswers rule block must exist');
const fellowshipRules = fellowshipMatch[1];
assert(fellowshipRules.includes('resource.data.uid == request.auth.uid'),
  'fellowshipAnswers read must check resource.data.uid == request.auth.uid');
assert(fellowshipRules.includes('allow write: if false'),
  'fellowshipAnswers must deny client writes');
console.log('  PASSED: fellowshipAnswers readable only by answer owner, writes blocked.');

// 3. Group documents require membership check
console.log('[Rule 3] Group documents require membership');
const groupMatch = rules.match(/match\s+\/groups\/{groupId}\s*{([^}]*)}/s);
assert(groupMatch, 'groups/{groupId} rule block must exist');
assert(groupMatch[1].includes('exists(/databases/$(database)/documents/groups/$(groupId)/members/$(request.auth.uid))'),
  'Group read must verify membership via exists()');
assert(groupMatch[1].includes('allow write: if false'),
  'Group client writes must be blocked');
console.log('  PASSED: Group docs require authenticated membership, writes blocked.');

// 4. Group challenges require membership check
console.log('[Rule 4] Challenge documents require membership');
const challengeMatch = rules.match(/match\s+\/groups\/{groupId}\/challenges\/{challengeId}\s*{([^}]*)}/s);
assert(challengeMatch, 'challenges rule block must exist');
assert(challengeMatch[1].includes('exists(/databases/$(database)/documents/groups/$(groupId)/members/$(request.auth.uid))'),
  'Challenge read must verify membership');
assert(challengeMatch[1].includes('allow write: if false'),
  'Challenge client writes must be blocked');
console.log('  PASSED: Challenge docs require membership, writes blocked.');

// 5. Leaderboard entries are read-only for authenticated users
console.log('[Rule 5] Leaderboard entries are client-read-only');
const lbMatch = rules.match(/match\s+\/leaderboards\/{challengeId}\/entries\/{entryId}\s*{([^}]*)}/s);
assert(lbMatch, 'leaderboards rule block must exist');
assert(lbMatch[1].includes('allow read: if request.auth != null'),
  'Leaderboard must allow authenticated reads');
assert(lbMatch[1].includes('allow write: if false'),
  'Leaderboard must deny client writes');
console.log('  PASSED: Leaderboard entries readable by authenticated users, writes blocked.');

// 6. User progress requires ownership
console.log('[Rule 6] User progress requires ownership');
const progressMatch = rules.match(/match\s+\/users\/{userId}\/private\/progress\s*{([^}]*)}/s);
assert(progressMatch, 'User progress rule block must exist');
assert(progressMatch[1].includes('request.auth.uid == userId'),
  'Progress must verify ownership');
console.log('  PASSED: User progress restricted to owner.');

// 7. Default deny-all rule exists
console.log('[Rule 7] Default deny-all catch-all rule');
assert(rules.includes('match /{document=**}'),
  'Catch-all wildcard rule must exist');
const catchAll = rules.match(/match\s+\/\{document=\*\*\}\s*{([^}]*)}/s);
assert(catchAll, 'Catch-all rule block must exist');
assert(catchAll[1].includes('allow read, write: if false'),
  'Catch-all must deny all access');
console.log('  PASSED: Default deny-all boundary prevents undeclared collection access.');

// 8. Group member documents require membership
console.log('[Rule 8] Member documents require membership');
const memberMatch = rules.match(/match\s+\/groups\/{groupId}\/members\/{memberId}\s*{([^}]*)}/s);
assert(memberMatch, 'Members rule block must exist');
assert(memberMatch[1].includes('exists(/databases/$(database)/documents/groups/$(groupId)/members/$(request.auth.uid))'),
  'Member read must verify caller membership');
assert(memberMatch[1].includes('allow write: if false'),
  'Member client writes must be blocked');
console.log('  PASSED: Member documents require membership, writes blocked.');

// 9. User group index is private to the user
console.log('[Rule 9] User group index is private');
const userGroupMatch = rules.match(/match\s+\/users\/{userId}\/groups\/{groupId}\s*{([^}]*)}/s);
assert(userGroupMatch, 'User groups rule block must exist');
assert(userGroupMatch[1].includes('request.auth.uid == userId'),
  'User groups must verify ownership');
assert(userGroupMatch[1].includes('allow write: if false'),
  'User groups must deny client writes');
console.log('  PASSED: User group index private to owner, writes blocked.');

// 10. Challenge entries require membership
console.log('[Rule 10] Challenge entries require membership');
const entryMatch = rules.match(/match\s+\/groups\/{groupId}\/challenges\/{challengeId}\/entries\/{entryId}\s*{([^}]*)}/s);
assert(entryMatch, 'Challenge entries rule block must exist');
assert(entryMatch[1].includes('exists(/databases/$(database)/documents/groups/$(groupId)/members/$(request.auth.uid))'),
  'Entry read must verify membership');
assert(entryMatch[1].includes('allow write: if false'),
  'Entry client writes must be blocked');
console.log('  PASSED: Challenge entries require membership, writes blocked.');

console.log('\n--- ALL 10 FIRESTORE RULES CONTRACT TESTS PASSED ---');
console.log('Note: These validate rule structure. Full runtime rule evaluation');
console.log('requires the Firebase Emulator (needs Java) or live testing.');
