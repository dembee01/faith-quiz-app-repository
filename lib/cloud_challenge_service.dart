import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:cloud_functions/cloud_functions.dart';
import 'package:firebase_auth/firebase_auth.dart';

import 'remote_feature_service.dart';

class CloudChallenge {
  const CloudChallenge({
    required this.id,
    required this.challengeId,
    required this.question,
    required this.options,
    required this.explanation,
    required this.scriptureReference,
    required this.testament,
    required this.propheticFocus,
  });

  final String id;
  final String challengeId;
  final String question;
  final List<String> options;
  final String explanation;
  final String scriptureReference;
  final String testament;
  final String propheticFocus;

  factory CloudChallenge.fromDocument(
    QueryDocumentSnapshot<Map<String, dynamic>> document,
  ) {
    final data = document.data();
    return CloudChallenge(
      id: document.id,
      challengeId: data['challengeId'] as String? ?? document.id,
      question: data['question'] as String? ?? '',
      options:
          (data['options'] as List?)?.whereType<String>().toList() ?? const [],
      explanation: data['explanation'] as String? ?? '',
      scriptureReference: data['scriptureReference'] as String? ?? '',
      testament: data['testament'] as String? ?? '',
      propheticFocus: data['propheticFocus'] as String? ?? '',
    );
  }
}

class CloudSubmission {
  const CloudSubmission({required this.correct, required this.challengeId});
  final bool correct;
  final String challengeId;
}

abstract interface class CloudChallengeGateway {
  Future<CloudChallenge?> loadToday();
  Future<CloudChallenge?> loadQuestion({int? index});
  Future<CloudSubmission> submit({
    required CloudChallenge challenge,
    required int answerIndex,
    required int elapsedSeconds,
    required String displayName,
  });
}

class QuestionReview {
  const QuestionReview({
    required this.questionId,
    required this.userAnswer,
    required this.correctAnswer,
    required this.correct,
    required this.explanation,
    required this.scriptureReference,
    this.question = '',
    this.options = const [],
  });

  final String questionId;
  final int userAnswer;
  final int correctAnswer;
  final bool correct;
  final String explanation;
  final String scriptureReference;
  final String question;
  final List<String> options;

  factory QuestionReview.fromMap(Map<String, dynamic> data) {
    return QuestionReview(
      questionId: data['questionId'] as String? ?? '',
      userAnswer: (data['userAnswer'] as num?)?.toInt() ?? -1,
      correctAnswer: (data['correctAnswer'] as num?)?.toInt() ?? 0,
      correct: data['correct'] == true,
      explanation: data['explanation'] as String? ?? '',
      scriptureReference: data['scriptureReference'] as String? ?? '',
      question: data['question'] as String? ?? '',
      options:
          (data['options'] as List?)?.whereType<String>().toList() ??
          const [],
    );
  }
}

class GroupQuizResult {
  const GroupQuizResult({
    required this.challengeId,
    required this.score,
    required this.total,
    required this.elapsedSeconds,
    required this.breakdown,
  });

  final String challengeId;
  final int score;
  final int total;
  final int elapsedSeconds;
  final List<QuestionReview> breakdown;
}

abstract interface class CloudGroupGateway {
  Future<bool> get isAvailable;
  User? get currentUser;
  Future<User?> signInWithGoogle();
  Future<void> signOut();
  Stream<List<QuizGroup>> myGroups();
  Stream<List<GroupChallenge>> groupChallenges(String groupId);
  Future<CloudChallenge?> loadToday();
  Future<String> createGroup(String name);
  Future<void> joinGroup(String groupId);
  Future<String> createGroupChallenge({
    required String groupId,
    required CloudChallenge challenge,
  });
  Future<String> createGroupQuiz({
    required String groupId,
    required int questionCount,
    String? title,
  });
  Future<CloudSubmission> submitGroupChallenge({
    required String groupId,
    required GroupChallenge challenge,
    required int answerIndex,
    required int elapsedSeconds,
    required String displayName,
  });
  Future<GroupQuizResult> submitGroupQuiz({
    required String groupId,
    required String challengeId,
    required List<int> answers,
    required int elapsedSeconds,
    required String displayName,
  });
  Stream<List<LeaderboardEntry>> groupLeaderboard(
    String groupId,
    String challengeId,
  );
}

class LeaderboardEntry {
  const LeaderboardEntry({
    required this.id,
    required this.displayName,
    required this.score,
    required this.elapsedSeconds,
  });

  final String id;
  final String displayName;
  final int score;
  final int elapsedSeconds;

  factory LeaderboardEntry.fromDocument(
    QueryDocumentSnapshot<Map<String, dynamic>> document,
  ) {
    final data = document.data();
    return LeaderboardEntry(
      id: document.id,
      displayName: data['displayName'] as String? ?? 'Faith learner',
      score: (data['score'] as num?)?.toInt() ?? 0,
      elapsedSeconds: (data['elapsedSeconds'] as num?)?.toInt() ?? 0,
    );
  }
}

class QuizGroup {
  const QuizGroup({required this.id, required this.name, required this.role});

  final String id;
  final String name;
  final String role;

  factory QuizGroup.fromDocument(
    QueryDocumentSnapshot<Map<String, dynamic>> document,
  ) {
    final data = document.data();
    return QuizGroup(
      id: document.id,
      name: data['name'] as String? ?? 'Faith Quiz group',
      role: data['role'] as String? ?? 'member',
    );
  }
}

class GroupChallengeItem {
  const GroupChallengeItem({
    required this.id,
    required this.question,
    required this.options,
    required this.scriptureReference,
    required this.testament,
    required this.propheticFocus,
  });

  final String id;
  final String question;
  final List<String> options;
  final String scriptureReference;
  final String testament;
  final String propheticFocus;

  factory GroupChallengeItem.fromMap(Map<String, dynamic> data) {
    return GroupChallengeItem(
      id: data['id'] as String? ?? '',
      question: data['question'] as String? ?? '',
      options:
          (data['options'] as List?)?.whereType<String>().toList() ?? const [],
      scriptureReference: data['scriptureReference'] as String? ?? '',
      testament: data['testament'] as String? ?? '',
      propheticFocus: data['propheticFocus'] as String? ?? '',
    );
  }
}

class GroupChallenge {
  const GroupChallenge({
    required this.id,
    this.title = 'Bible Challenge',
    this.questionCount = 1,
    required this.question,
    required this.options,
    required this.explanation,
    required this.scriptureReference,
    this.items = const [],
  });

  final String id;
  final String title;
  final int questionCount;
  final String question;
  final List<String> options;
  final String explanation;
  final String scriptureReference;
  final List<GroupChallengeItem> items;

  factory GroupChallenge.fromDocument(
    QueryDocumentSnapshot<Map<String, dynamic>> document,
  ) {
    final data = document.data();
    final rawQuestions = data['questions'] as List?;
    final items = rawQuestions != null
        ? rawQuestions
            .whereType<Map>()
            .map((m) => GroupChallengeItem.fromMap(Map<String, dynamic>.from(m)))
            .toList()
        : const <GroupChallengeItem>[];
    return GroupChallenge(
      id: document.id,
      title: data['title'] as String? ?? 'Bible Challenge',
      questionCount: (data['questionCount'] as num?)?.toInt() ??
          (items.isNotEmpty ? items.length : 1),
      question: data['question'] as String? ??
          (items.isNotEmpty ? items.first.question : ''),
      options:
          (data['options'] as List?)?.whereType<String>().toList() ??
              (items.isNotEmpty ? items.first.options : const []),
      explanation: data['explanation'] as String? ?? '',
      scriptureReference: data['scriptureReference'] as String? ??
          (items.isNotEmpty ? items.first.scriptureReference : ''),
      items: items,
    );
  }
}

class CloudChallengeService
    implements CloudChallengeGateway, CloudGroupGateway {
  CloudChallengeService({
    FirebaseFirestore? firestore,
    FirebaseAuth? auth,
    FirebaseFunctions? functions,
  }) : _firestore = firestore ?? FirebaseFirestore.instance,
       _auth = auth ?? FirebaseAuth.instance,
       _functions = functions ??
           FirebaseFunctions.instanceFor(region: 'us-central1');

  Future<void> warmUp() async {
    try {
      await _user();
    } catch (_) {}
  }

  final FirebaseFirestore _firestore;
  final FirebaseAuth _auth;
  final FirebaseFunctions _functions;

  static CloudChallenge? _cachedTodayChallenge;
  static int? _cachedTodayEpoch;
  static final Map<int, CloudChallenge> _questionCache = <int, CloudChallenge>{};

  @override
  Future<bool> get isAvailable async {
    try {
      // A launch-time offline period must not disable online play for the whole
      // session; re-check the remote gate before concluding it is off.
      await RemoteFeatureService.instance.refreshIfDisabled();
      if (!RemoteFeatureService.instance.cloudChallengesEnabled) return false;
      await _user();
      return true;
    } catch (_) {
      return false;
    }
  }

  static int _shuffledIndex(int step, int epochDay) {
    // Permute across all 500 questions using a coprime stride (263) so that
    // questions evenly interleave Old Testament and New Testament books
    // instead of clustering 25 questions from the same book in sequence.
    return ((epochDay + step) * 263).abs() % 500;
  }

  @override
  Future<CloudChallenge?> loadQuestion({int? index}) async {
    if (!await isAvailable) return null;
    final today =
        DateTime.now().toUtc().millisecondsSinceEpoch ~/
        Duration.millisecondsPerDay;
    final step = index ?? 0;
    final targetIndex = _shuffledIndex(step, today);

    if (_questionCache.containsKey(targetIndex)) {
      return _questionCache[targetIndex];
    }

    try {
      final catalogue = RemoteFeatureService.instance.activeCloudCatalogue;
      final snapshot = await _firestore
          .collection('content')
          .doc(catalogue)
          .collection('questions')
          .where('dailyIndex', isEqualTo: targetIndex)
          .limit(1)
          .get();
      if (snapshot.docs.isEmpty) return null;
      final challenge = CloudChallenge.fromDocument(snapshot.docs.first);
      if (challenge.question.isEmpty || challenge.options.length != 4) {
        return null;
      }
      _questionCache[targetIndex] = challenge;
      return challenge;
    } catch (_) {
      return null;
    }
  }

  @override
  Future<CloudChallenge?> loadToday() async {
    final today =
        DateTime.now().toUtc().millisecondsSinceEpoch ~/
        Duration.millisecondsPerDay;

    // Client-side cost optimization: reuse today's fetched challenge across screen transitions
    if (_cachedTodayEpoch == today && _cachedTodayChallenge != null) {
      return _cachedTodayChallenge;
    }

    final challenge = await loadQuestion(index: 0);
    if (challenge != null) {
      _cachedTodayEpoch = today;
      _cachedTodayChallenge = challenge;
    }
    return challenge;
  }

  @override
  Future<CloudSubmission> submit({
    required CloudChallenge challenge,
    required int answerIndex,
    required int elapsedSeconds,
    required String displayName,
  }) async {
    await _user();
    final result = await _functions
        .httpsCallable('submitCloudChallenge')
        .call(<String, Object>{
          'catalogue': RemoteFeatureService.instance.activeCloudCatalogue,
          'questionId': challenge.id,
          'answerIndex': answerIndex,
          'elapsedSeconds': elapsedSeconds.clamp(0, 600),
          'displayName': displayName.trim().isEmpty
              ? 'Faith learner'
              : displayName.trim(),
        });
    final data = Map<String, dynamic>.from(result.data as Map);
    return CloudSubmission(
      correct: data['correct'] == true,
      challengeId: data['challengeId'] as String? ?? challenge.challengeId,
    );
  }

  Stream<List<LeaderboardEntry>> leaderboard(String challengeId) => _firestore
      .collection('leaderboards')
      .doc(challengeId)
      .collection('entries')
      .orderBy('score', descending: true)
      .limit(25)
      .snapshots()
      .map((snapshot) {
        final entries = snapshot.docs
            .map(LeaderboardEntry.fromDocument)
            .toList();
        entries.sort((left, right) {
          final byScore = right.score.compareTo(left.score);
          return byScore != 0
              ? byScore
              : left.elapsedSeconds.compareTo(right.elapsedSeconds);
        });
        return entries;
      });

  @override
  Future<String> createGroup(String name) async {
    await _user();
    final result = await _functions.httpsCallable('createGroup').call(
      <String, Object>{'name': name.trim()},
    );
    return (result.data as Map)['groupId'] as String;
  }

  @override
  Future<void> joinGroup(String groupId) async {
    await _user();
    await _functions.httpsCallable('joinGroup').call(<String, Object>{
      'groupId': groupId.trim(),
    });
  }

  @override
  Future<String> createGroupChallenge({
    required String groupId,
    required CloudChallenge challenge,
  }) async {
    await _user();
    final result = await _functions
        .httpsCallable('createGroupChallenge')
        .call(<String, Object>{
          'groupId': groupId,
          'catalogue': RemoteFeatureService.instance.activeCloudCatalogue,
          'questionId': challenge.id,
        });
    return (result.data as Map)['challengeId'] as String;
  }

  @override
  Stream<List<GroupChallenge>> groupChallenges(String groupId) => _firestore
      .collection('groups')
      .doc(groupId)
      .collection('challenges')
      .orderBy('createdAt', descending: true)
      .snapshots()
      .map(
        (snapshot) => snapshot.docs.map(GroupChallenge.fromDocument).toList(),
      );

  @override
  Stream<List<LeaderboardEntry>> groupLeaderboard(
    String groupId,
    String challengeId,
  ) => _firestore
      .collection('groups')
      .doc(groupId)
      .collection('challenges')
      .doc(challengeId)
      .collection('entries')
      .orderBy('score', descending: true)
      .limit(25)
      .snapshots()
      .map((snapshot) {
        final entries = snapshot.docs
            .map(LeaderboardEntry.fromDocument)
            .toList();
        entries.sort((left, right) {
          final byScore = right.score.compareTo(left.score);
          return byScore != 0
              ? byScore
              : left.elapsedSeconds.compareTo(right.elapsedSeconds);
        });
        return entries;
      });

  @override
  Future<CloudSubmission> submitGroupChallenge({
    required String groupId,
    required GroupChallenge challenge,
    required int answerIndex,
    required int elapsedSeconds,
    required String displayName,
  }) async {
    await _user();
    final result = await _functions
        .httpsCallable('submitGroupChallenge')
        .call(<String, Object>{
          'groupId': groupId,
          'challengeId': challenge.id,
          'answerIndex': answerIndex,
          'elapsedSeconds': elapsedSeconds.clamp(0, 600),
          'displayName': displayName.trim().isEmpty
              ? 'Faith learner'
              : displayName.trim(),
        });
    final data = Map<String, dynamic>.from(result.data as Map);
    return CloudSubmission(
      correct: data['correct'] == true,
      challengeId: data['challengeId'] as String? ?? challenge.id,
    );
  }

  @override
  User? get currentUser => _auth.currentUser;

  @override
  Future<User?> signInWithGoogle() async {
    try {
      final provider = GoogleAuthProvider();
      final userCredential = await _auth.signInWithProvider(provider);
      return userCredential.user;
    } catch (_) {
      return null;
    }
  }

  @override
  Future<void> signOut() async {
    await _auth.signOut();
  }

  @override
  Future<String> createGroupQuiz({
    required String groupId,
    required int questionCount,
    String? title,
  }) async {
    await _user();
    final result = await _functions
        .httpsCallable('createGroupChallenge')
        .call(<String, Object>{
          'groupId': groupId,
          'catalogue': RemoteFeatureService.instance.activeCloudCatalogue,
          'questionCount': questionCount,
          'title': ?title,
        });
    return (result.data as Map)['challengeId'] as String;
  }

  @override
  Future<GroupQuizResult> submitGroupQuiz({
    required String groupId,
    required String challengeId,
    required List<int> answers,
    required int elapsedSeconds,
    required String displayName,
  }) async {
    await _user();
    final result = await _functions
        .httpsCallable('submitGroupChallenge')
        .call(<String, Object>{
          'groupId': groupId,
          'challengeId': challengeId,
          'answers': answers,
          'elapsedSeconds': elapsedSeconds.clamp(0, 3600),
          'displayName': displayName.trim().isEmpty
              ? 'Faith learner'
              : displayName.trim(),
        });
    final data = Map<String, dynamic>.from(result.data as Map);
    final rawBreakdown = data['breakdown'] as List? ?? const [];
    final breakdown = rawBreakdown
        .whereType<Map>()
        .map((m) => QuestionReview.fromMap(Map<String, dynamic>.from(m)))
        .toList();
    return GroupQuizResult(
      challengeId: challengeId,
      score: (data['score'] as num?)?.toInt() ?? 0,
      total: (data['total'] as num?)?.toInt() ?? answers.length,
      elapsedSeconds:
          (data['elapsedSeconds'] as num?)?.toInt() ?? elapsedSeconds,
      breakdown: breakdown,
    );
  }

  @override
  Stream<List<QuizGroup>> myGroups() async* {
    final user = await _user();
    yield* _firestore
        .collection('users')
        .doc(user.uid)
        .collection('groups')
        .orderBy('name')
        .snapshots()
        .map((snapshot) => snapshot.docs.map(QuizGroup.fromDocument).toList());
  }

  Future<User> _user() async {
    return _auth.currentUser ?? (await _auth.signInAnonymously()).user!;
  }
}
