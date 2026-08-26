import 'package:flutter_test/flutter_test.dart';
import 'package:faithquiz/question_bank.dart';
import 'package:faithquiz/progress_store.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues(<String, Object>{});
  });

  test('every level has playable valid questions', () {
    for (final level in QuestionBank.availableLevels) {
      final questions = QuestionBank.forLevel(level);
      expect(questions, isNotEmpty, reason: 'level $level has no questions');
      final valid = questions.where((q) => q.isValid).toList();
      expect(valid.length, questions.length,
          reason: 'level $level has invalid questions');
    }
  });

  test('passing each level 1-30 with exactly 60% unlocks the next', () async {
    final store = ProgressStore();
    await store.load();
    for (final level in QuestionBank.availableLevels) {
      final total = QuestionBank.forLevel(level).length;
      // smallest passing score under integer math: score*100 ~/ total >= 60
      final passScore = (total * 3 + 4) ~/ 5; // ceil(60%) of total
      expect(passScore * 100 ~/ total, greaterThanOrEqualTo(60));
      await store.completeQuiz(
        level: level,
        score: passScore,
        total: total,
        missedQuestions: const [],
        mode: 'classic',
      );
      expect(store.highestUnlocked, level == 30 ? 30 : level + 1,
          reason: 'passing level $level must unlock ${level + 1}');
    }
    expect(store.highestUnlocked, 30);
  });

  test('failing a level keeps progress and allows retry unlock', () async {
    final store = ProgressStore();
    await store.load();
    for (var level = 1; level <= 6; level++) {
      await store.completeQuiz(
          level: level, score: 4, total: 5, missedQuestions: const []);
    }
    expect(store.highestUnlocked, 7);
    await store.completeQuiz(
        level: 7, score: 2, total: 5, missedQuestions: const []);
    expect(store.highestUnlocked, 7,
        reason: 'a failed attempt must not change unlocks');
    await store.completeQuiz(
        level: 7, score: 4, total: 5, missedQuestions: const []);
    expect(store.highestUnlocked, 8);
  });

  test('journey mode unlocks identically', () async {
    final store = ProgressStore();
    await store.load();
    await store.completeQuiz(
        level: 12,
        score: 3,
        total: 5,
        missedQuestions: const [],
        mode: 'journey');
    expect(store.highestUnlocked, 13);
  });

  test('stale cloud payload never lowers local level progress', () async {
    final store = ProgressStore();
    await store.load();
    await store.completeQuiz(
        level: 15, score: 5, total: 5, missedQuestions: const []);
    expect(store.highestUnlocked, 16);

    final stale = <String, dynamic>{
      'highestUnlocked': 8,
      'lastCompletedLevel': 7,
      'totalAnswered': 40,
      'highScore': 5,
      'lastCompletedLevelClamp': null,
    };
    await store.applyCloudPayloadForTest(stale);
    expect(store.highestUnlocked, 16,
        reason: 'cloud restore must never regress unlocked levels');
    expect(store.lastCompletedLevel, 15,
        reason: 'cloud restore must never regress completed levels');
  });
}
