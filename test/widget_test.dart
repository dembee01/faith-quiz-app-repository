import 'package:cloud_functions/cloud_functions.dart'
    show FirebaseFunctionsException;
import 'package:faithquiz/app.dart';
import 'package:faithquiz/cloud_challenge_service.dart';
import 'package:faithquiz/models.dart';
import 'package:faithquiz/progress_store.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

class _FakeCloudGateway implements CloudChallengeGateway {
  _FakeCloudGateway(this.question, {required this.correct});

  final CloudChallenge question;
  final bool correct;
  int? submittedAnswer;

  @override
  Future<CloudChallenge?> loadToday() async => question;

  @override
  Future<CloudSubmission> submit({
    required CloudChallenge challenge,
    required int answerIndex,
    required int elapsedSeconds,
    required String displayName,
  }) async {
    submittedAnswer = answerIndex;
    return CloudSubmission(
      correct: correct,
      challengeId: challenge.challengeId,
    );
  }
}

void main() {
  testWidgets(
    'Faith Quiz starts with the original slate splash and opens the menu',
    (tester) async {
      final store = ProgressStore();
      await tester.pumpWidget(FaithQuizApp(store: store));

      expect(find.text('GET STARTED'), findsOneWidget);
      await tester.tap(find.text('GET STARTED'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 500));

      expect(find.text('Daily Challenge'), findsOneWidget);
      expect(find.text('The Covenant Journey'), findsOneWidget);
      expect(find.text('Adaptive Levels'), findsNothing);
    },
  );

  testWidgets(
    'quiz selection is confirmed before correctness feedback is shown',
    (tester) async {
      const question = QuizQuestion(
        question: 'What is the first book of the Bible?',
        options: ['Genesis', 'Exodus', 'Psalms', 'Matthew'],
        correctAnswer: 0,
        explanation: 'Genesis begins the biblical story.',
      );
      final store = ProgressStore();
      await tester.pumpWidget(
        MaterialApp(
          home: QuizScreen(store: store, level: 1, questions: const [question]),
        ),
      );

      expect(find.text('SELECT AN ANSWER'), findsOneWidget);
      await tester.tap(find.text('Genesis'));
      await tester.pump();

      expect(find.text('SUBMIT ANSWER'), findsOneWidget);
      expect(find.text('EXPLANATION & INSIGHT'), findsNothing);

      await tester.ensureVisible(find.text('SUBMIT ANSWER'));
      await tester.pump();
      await tester.tap(find.text('SUBMIT ANSWER'));
      await tester.pump();

      expect(find.text('CORRECT!'), findsOneWidget);
      expect(find.text('Genesis begins the biblical story.'), findsOneWidget);
    },
  );

  testWidgets(
    'cloud challenge confirms selection before a server verdict is shown',
    (tester) async {
      const question = CloudChallenge(
        id: 'test-prophet',
        challengeId: 'prophets-v1-test',
        question: 'Which prophetic book contains this passage?',
        options: ['Isaiah', 'Jeremiah', 'Ezekiel', 'Daniel'],
        explanation: 'This verified learning note appears only after grading.',
        scriptureReference: 'Isaiah 1:2',
        testament: 'Old Testament',
        propheticFocus: 'Writing prophets',
      );
      final gateway = _FakeCloudGateway(question, correct: true);
      await tester.pumpWidget(
        MaterialApp(
          home: CloudChallengeScreen(store: ProgressStore(), service: gateway),
        ),
      );
      await tester.pump();
      await tester.pump();

      expect(find.text('LOCK IN ANSWER'), findsOneWidget);
      expect(find.text(question.explanation), findsNothing);
      await tester.tap(find.text('Isaiah'));
      await tester.pump();
      expect(find.text('LOCK IN ANSWER'), findsOneWidget);
      expect(gateway.submittedAnswer, isNull);

      await tester.ensureVisible(find.text('LOCK IN ANSWER'));
      await tester.pump();
      await tester.tap(find.text('LOCK IN ANSWER'));
      await tester.pump();
      await tester.pump();
      expect(gateway.submittedAnswer, 0);
      expect(find.text('VERIFIED CORRECT'), findsOneWidget);
      expect(find.text(question.explanation), findsOneWidget);
      expect(find.text('SCRIPTURE: Isaiah 1:2'), findsOneWidget);
    },
  );

  testWidgets(
    'quiz timer tracks total elapsed time while an answer is selected',
    (tester) async {
      const question = QuizQuestion(
        question: 'What is the first book of the Bible?',
        options: ['Genesis', 'Exodus', 'Psalms', 'Matthew'],
        correctAnswer: 0,
        explanation: 'Genesis begins the biblical story.',
      );
      final store = ProgressStore();
      await tester.pumpWidget(
        MaterialApp(
          home: QuizScreen(store: store, level: 1, questions: const [question]),
        ),
      );

      await tester.tap(find.text('Genesis'));
      await tester.pump(const Duration(seconds: 2));

      expect(find.text('00:02'), findsOneWidget);
      expect(find.text('2s'), findsOneWidget);
    },
  );

  testWidgets(
    'an unfinished topic quiz displays its current score and answered progress',
    (tester) async {
      final store = ProgressStore();
      await store.saveTopicSession(
        const TopicQuizSession(
          topic: 'parables',
          index: 3,
          score: 2,
          answered: 3,
          seed: 123,
        ),
      );

      await tester.pumpWidget(
        MaterialApp(home: TopicPacksScreen(store: store)),
      );
      await tester.tap(find.text('PARABLES'));
      await tester.pump();

      expect(find.text('CURRENT SESSION'), findsOneWidget);
      expect(find.text('CURRENT SCORE: 2/50'), findsOneWidget);
      expect(
        find.text(
          '3/50 questions answered • 2 correct so far. Your session is saved automatically.',
        ),
        findsOneWidget,
      );
      expect(find.text('CONTINUE QUIZ'), findsOneWidget);
    },
  );

  testWidgets(
    'a compact phone can scroll the daily challenge result without overflow',
    (tester) async {
      tester.view.physicalSize = const Size(432, 770);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await tester.pumpWidget(
        MaterialApp(
          home: ResultsScreen(
            store: ProgressStore(),
            score: 1,
            total: 1,
            level: 0,
            mode: 'daily',
            timeSpentSeconds: 15,
          ),
        ),
      );
      await tester.pump();

      expect(find.text('DAILY CHALLENGE COMPLETE'), findsOneWidget);
      expect(find.text('DONE'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  test('cloud submit failures map to actionable messages', () {
    expect(
      cloudSubmitErrorMessage(FirebaseFunctionsException(
        code: 'already-exists',
        message: 'submitted',
      )),
      'You already locked in this answer.',
    );
    expect(
      cloudSubmitErrorMessage(FirebaseFunctionsException(
        code: 'not-found',
        message: 'gone',
      )),
      'This challenge is no longer available.',
    );
    expect(
      cloudSubmitErrorMessage(FirebaseFunctionsException(
        code: 'unavailable',
        message: 'offline',
      )),
      contains('unreachable'),
    );
    expect(
      cloudSubmitErrorMessage(Exception('socket')),
      contains('not verified'),
    );
  });

  test('every topic quiz question includes a Scripture location', () {
    for (final topic in const ['gospels', 'prophets', 'parables']) {
      final questions = TopicQuizScreen.questionsForTopic(topic);
      expect(questions, hasLength(50));
      expect(
        questions.every(
          (question) => question.verseReference?.trim().isNotEmpty == true,
        ),
        isTrue,
      );
    }
  });

  test(
    'reset progress clears persisted scores, sessions, and accessibility choices',
    () async {
      SharedPreferences.setMockInitialValues(<String, Object>{
        'highest_unlocked_level': 12,
        'total_questions_answered': 48,
        'topic_scores': '{"gospels":22}',
        'reduce_motion': true,
        'quiz_session':
            '{"level":4,"mode":"classic","index":2,"score":1,"elapsedSeconds":10,"seed":7,"lives":0,"remainingSeconds":0}',
      });
      final store = ProgressStore();
      await store.load();

      await store.reset();

      expect(store.highestUnlocked, 1);
      expect(store.totalAnswered, 0);
      expect(store.topicScores['gospels'], 0);
      expect(store.session, isNull);
      expect(store.reduceMotion, isFalse);
    },
  );
}
