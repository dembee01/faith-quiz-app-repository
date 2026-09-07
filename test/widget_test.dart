import 'package:cloud_functions/cloud_functions.dart'
    show FirebaseFunctionsException;
import 'package:faithquiz/app.dart';
import 'package:faithquiz/cloud_challenge_service.dart';
import 'package:faithquiz/models.dart';
import 'package:faithquiz/progress_store.dart';
import 'package:firebase_auth/firebase_auth.dart' show User;
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

class _FakeCloudGateway implements CloudChallengeGateway {
  _FakeCloudGateway(this.question, {required this.correct, this.nextQuestion});

  final CloudChallenge question;
  final CloudChallenge? nextQuestion;
  final bool correct;
  int? submittedAnswer;
  int _callCount = 0;

  @override
  Future<CloudChallenge?> loadToday() async => question;

  @override
  Future<CloudChallenge?> loadQuestion({int? index}) async {
    _callCount++;
    return (_callCount > 1 && nextQuestion != null) ? nextQuestion : question;
  }

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

class _FakeGroupGateway implements CloudGroupGateway {
  _FakeGroupGateway({
    this.groups = const [],
    this.challenges = const [],
  });

  final List<QuizGroup> groups;
  final List<GroupChallenge> challenges;

  @override
  Future<bool> get isAvailable async => true;

  @override
  User? get currentUser => null;

  @override
  Future<User?> signInWithGoogle() async => null;

  @override
  Future<void> signOut() async {}

  @override
  Stream<List<QuizGroup>> myGroups() => Stream.value(groups);

  @override
  Stream<List<GroupChallenge>> groupChallenges(String groupId) =>
      Stream.value(challenges);

  @override
  Future<CloudChallenge?> loadToday() async => null;

  @override
  Future<String> createGroup(String name) async => 'grp-mock-123';

  @override
  Future<void> joinGroup(String groupId) async {}

  @override
  Future<String> createGroupChallenge({
    required String groupId,
    required CloudChallenge challenge,
  }) async => 'ch-mock-123';

  @override
  Future<String> createGroupQuiz({
    required String groupId,
    required int questionCount,
    String? title,
  }) async => 'quiz-mock-123';

  @override
  Future<CloudSubmission> submitGroupChallenge({
    required String groupId,
    required GroupChallenge challenge,
    required int answerIndex,
    required int elapsedSeconds,
    required String displayName,
  }) async => CloudSubmission(correct: true, challengeId: challenge.id);

  @override
  Future<GroupQuizResult> submitGroupQuiz({
    required String groupId,
    required String challengeId,
    required List<int> answers,
    required int elapsedSeconds,
    required String displayName,
  }) async => GroupQuizResult(
    challengeId: challengeId,
    score: answers.length,
    total: answers.length,
    elapsedSeconds: elapsedSeconds,
    breakdown: [
      for (var i = 0; i < answers.length; i++)
        QuestionReview(
          questionId: 'q-$i',
          userAnswer: answers[i],
          correctAnswer: answers[i],
          correct: true,
          explanation: 'Mock explanation $i',
          scriptureReference: 'Genesis 1:1',
          question: 'Mock Question $i',
          options: const ['A', 'B', 'C', 'D'],
        ),
    ],
  );

  @override
  Stream<List<LeaderboardEntry>> groupLeaderboard(
    String groupId,
    String challengeId,
  ) => Stream.value(const []);
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

      // Verify notifying store during active menu does not crash with _dependents.isEmpty assertion
      await store.completeQuiz(
        level: 1,
        score: 5,
        total: 5,
        missedQuestions: const [],
        elapsedSeconds: 10,
      );
      await tester.pump();
      expect(find.text('Daily Challenge'), findsOneWidget);
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
    'cloud challenge confirms selection, does not leak option in subtitle, shows next question button and loads next question',
    (tester) async {
      const question1 = CloudChallenge(
        id: 'test-prophet-1',
        challengeId: 'prophets-v1-test-1',
        question: 'Which prophetic book contains this passage?',
        options: ['Isaiah', 'Jeremiah', 'Ezekiel', 'Daniel'],
        explanation: 'This verified learning note appears only after grading.',
        scriptureReference: 'Isaiah 1:2',
        testament: 'Old Testament',
        propheticFocus: 'Isaiah',
      );
      const question2 = CloudChallenge(
        id: 'test-prophet-2',
        challengeId: 'prophets-v1-test-2',
        question: 'Which New Testament book records this prophetic witness?',
        options: ['Matthew', 'Mark', 'Luke', 'Acts'],
        explanation: 'This is question two in the continuous online session.',
        scriptureReference: 'Acts 1:8',
        testament: 'New Testament',
        propheticFocus: 'Apostolic witness',
      );
      final gateway = _FakeCloudGateway(
        question1,
        correct: true,
        nextQuestion: question2,
      );
      await tester.pumpWidget(
        MaterialApp(
          home: CloudChallengeScreen(store: ProgressStore(), service: gateway),
        ),
      );
      await tester.pump();
      await tester.pump();

      expect(find.text('QUESTION 1'), findsOneWidget);
      expect(find.text('LOCK IN ANSWER'), findsOneWidget);
      // Verify the answer "Isaiah" is NOT leaked in the card subtitle before answering!
      expect(find.text('Prophetic Scripture & Books'), findsOneWidget);
      expect(find.text(question1.explanation), findsNothing);
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
      expect(find.text(question1.explanation), findsOneWidget);
      expect(find.text('SCRIPTURE: Isaiah 1:2'), findsOneWidget);
      expect(find.text('FOCUS: Isaiah'), findsOneWidget);
      expect(find.text('NEXT QUESTION'), findsOneWidget);
      expect(find.text("View today's leaderboard"), findsOneWidget);
      expect(find.text('SCORE: 1'), findsOneWidget);

      // Tap NEXT QUESTION and verify next question loads without leaving the quiz interface
      await tester.ensureVisible(find.text('NEXT QUESTION'));
      await tester.pump();
      await tester.tap(find.text('NEXT QUESTION'));
      await tester.pump();
      await tester.pump();

      expect(find.text('QUESTION 2'), findsOneWidget);
      expect(find.text(question2.question), findsOneWidget);
      expect(find.text('LOCK IN ANSWER'), findsOneWidget);
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

  testWidgets(
    'group detail screen renders and handles interaction without assertion errors',
    (tester) async {
      final store = ProgressStore();
      const group = QuizGroup(
        id: 'grp-test-123',
        name: 'Fellowship',
        role: 'owner',
      );
      const challenge = GroupChallenge(
        id: 'challenge-123',
        question: 'Who led the Israelites out of Egypt?',
        options: ['Moses', 'Aaron', 'Joshua', 'Caleb'],
        explanation: 'God called Moses to deliver His people out of Egypt.',
        scriptureReference: 'Exodus 3:10',
      );
      final service = _FakeGroupGateway(challenges: [challenge]);
      await tester.pumpWidget(
        MaterialApp(
          home: GroupDetailScreen(store: store, group: group, service: service),
        ),
      );
      await tester.pump();

      expect(find.text('FELLOWSHIP'), findsOneWidget);
      expect(find.text('GROUP CODE: grp-test-123'), findsOneWidget);
      expect(find.text('CREATE GROUP QUIZ (10 - 30 Qs)'), findsOneWidget);
      expect(find.text('Bible Challenge'), findsOneWidget);
    },
  );

  testWidgets(
    'GroupQuestionScreen plays multi-question session without spoiling answers until submission',
    (tester) async {
      final store = ProgressStore();
      const items = [
        GroupChallengeItem(
          id: 'q1',
          question: 'Who led the Israelites out of Egypt?',
          options: ['Moses', 'Aaron', 'Joshua', 'Caleb'],
          scriptureReference: 'Exodus 3:10',
          testament: 'Old Testament',
          propheticFocus: 'Deliverance',
        ),
        GroupChallengeItem(
          id: 'q2',
          question: 'Where was Jesus born?',
          options: ['Nazareth', 'Bethlehem', 'Jerusalem', 'Capernaum'],
          scriptureReference: 'Micah 5:2',
          testament: 'New Testament',
          propheticFocus: 'Messiah',
        ),
      ];
      const challenge = GroupChallenge(
        id: 'challenge-multi',
        title: 'Multi Bible Challenge',
        questionCount: 2,
        question: 'Who led the Israelites out of Egypt?',
        options: ['Moses', 'Aaron', 'Joshua', 'Caleb'],
        explanation: 'God called Moses to deliver His people out of Egypt.',
        scriptureReference: 'Exodus 3:10',
        items: items,
      );
      final service = _FakeGroupGateway();
      await tester.pumpWidget(
        MaterialApp(
          home: GroupQuestionScreen(
            store: store,
            groupId: 'grp-test-123',
            challenge: challenge,
            service: service,
          ),
        ),
      );
      await tester.pump();

      // Verify timer hud and question 1
      expect(find.text('QUESTION 1 OF 2'), findsOneWidget);
      expect(find.text('Who led the Israelites out of Egypt?'), findsOneWidget);
      expect(find.text('Exodus 3:10'), findsNothing); // No answer spoiler during quiz!

      // Select option A (Moses)
      await tester.tap(find.text('Moses'));
      await tester.pump();

      // Tap NEXT QUESTION
      expect(find.text('NEXT QUESTION'), findsOneWidget);
      await tester.tap(find.text('NEXT QUESTION'));
      await tester.pump();

      // Verify question 2
      expect(find.text('QUESTION 2 OF 2'), findsOneWidget);
      expect(find.text('Where was Jesus born?'), findsOneWidget);
      expect(find.text('Micah 5:2'), findsNothing); // No spoiler!

      // Select option B (Bethlehem)
      await tester.tap(find.text('Bethlehem'));
      await tester.pump();

      // Now on question 2 with both answered: SUBMIT GROUP QUIZ
      expect(find.text('SUBMIT GROUP QUIZ (2/2)'), findsOneWidget);
      await tester.tap(find.text('SUBMIT GROUP QUIZ (2/2)'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 500));

      // After submission, results and explanations are revealed!
      expect(find.text('CHALLENGE RESULTS'), findsOneWidget);
      expect(find.text('2 / 2 CORRECT'), findsOneWidget);
      expect(find.text('100% Score • Total Time: 00:00'), findsOneWidget);
      expect(find.text('GROUP LEADERBOARD'), findsOneWidget);
      expect(find.text('QUESTION REVIEW (2 QUESTIONS)'), findsOneWidget);

      await tester.scrollUntilVisible(find.text('BACK TO GROUP'), 200.0);
      expect(find.text('BACK TO GROUP'), findsOneWidget);
    },
  );

  testWidgets(
    'groups screen renders and displays user groups without assertion errors',
    (tester) async {
      final store = ProgressStore();
      const group = QuizGroup(
        id: 'grp-test-456',
        name: 'Grace Church',
        role: 'owner',
      );
      final service = _FakeGroupGateway(groups: [group]);
      await tester.pumpWidget(
        MaterialApp(home: GroupsScreen(store: store, service: service)),
      );
      await tester.pump();
      await tester.pump();

      expect(find.text('GROUP CHALLENGES'), findsOneWidget);
      expect(find.text('Grace Church'), findsOneWidget);
    },
  );
}
