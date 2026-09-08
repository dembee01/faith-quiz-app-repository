import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:cloud_functions/cloud_functions.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/foundation.dart';
import 'package:google_sign_in/google_sign_in.dart';

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
  const CloudSubmission({
    required this.correct,
    required this.challengeId,
    this.alreadySubmitted = false,
    this.score = 0,
    this.totalAnswered = 0,
    this.level = 1,
    this.correctAnswer,
  });
  final bool correct;
  final String challengeId;
  final bool alreadySubmitted;
  final int score;
  final int totalAnswered;
  final int level;
  final int? correctAnswer;
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
  Stream<QuizGroup> streamGroup(String groupId);
  Stream<GroupChallenge> streamChallenge(String groupId, String challengeId);
  Future<CloudChallenge?> loadToday();
  Future<String> createGroup(String name, {int durationMinutes = 10});
  Future<void> joinGroup(String groupId);
  Future<void> extendGroup(String groupId, {int additionalMinutes = 10});
  Future<String> createGroupChallenge({
    required String groupId,
    required CloudChallenge challenge,
  });
  Future<String> createGroupQuiz({
    required String groupId,
    required int questionCount,
    String? title,
    String mode = 'competitive',
  });
  Future<void> startGroupChallenge({
    required String groupId,
    required String challengeId,
  });
  Future<void> submitFellowshipAnswer({
    required String groupId,
    required String challengeId,
    required int questionIndex,
    required String questionId,
    required int selectedOptionIndex,
    required int responseLatencyMs,
    String? username,
  });
  Future<void> revealFellowshipAnswer({
    required String groupId,
    required String challengeId,
  });
  Future<void> advanceFellowshipQuestion({
    required String groupId,
    required String challengeId,
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
  Stream<List<LeaderboardEntry>> globalLeaderboard();
  Future<void> claimUsername(String username);
  Future<String?> getClaimedUsername();
  Future<bool> checkUsernameAvailable(String username);
}

class LeaderboardEntry {
  const LeaderboardEntry({
    required this.id,
    required this.displayName,
    required this.score,
    required this.elapsedSeconds,
    this.level = 1,
    this.totalAnswered = 0,
    this.accuracy = 0,
    this.avgElapsedSeconds = 0,
  });

  final String id;
  final String displayName;
  final int score;
  final int elapsedSeconds;
  final int level;
  final int totalAnswered;
  final int accuracy;
  final int avgElapsedSeconds;

  factory LeaderboardEntry.fromDocument(
    QueryDocumentSnapshot<Map<String, dynamic>> document,
  ) {
    final data = document.data();
    final rawAvg = data['avgElapsedSeconds'] ?? data['elapsedSeconds'];
    return LeaderboardEntry(
      id: document.id,
      displayName: data['displayName'] as String? ?? 'Faith learner',
      score: (data['score'] as num?)?.toInt() ?? 0,
      elapsedSeconds: (data['elapsedSeconds'] as num?)?.toInt() ?? 0,
      level: (data['level'] as num?)?.toInt() ?? 1,
      totalAnswered: (data['totalAnswered'] as num?)?.toInt() ?? 0,
      accuracy: (data['accuracy'] as num?)?.toInt() ?? 0,
      avgElapsedSeconds: (rawAvg as num?)?.toInt() ?? 0,
    );
  }
}

class QuizGroup {
  const QuizGroup({
    required this.id,
    required this.name,
    required this.role,
    this.joinCode,
    this.expiresAt,
    this.durationMinutes,
  });

  final String id;
  final String name;
  final String role;
  final String? joinCode;
  final DateTime? expiresAt;
  final int? durationMinutes;

  bool get isOwner => role == 'owner';
  bool get isExpired =>
      expiresAt != null && DateTime.now().isAfter(expiresAt!);

  Duration? get remainingTime {
    if (expiresAt == null) return null;
    final diff = expiresAt!.difference(DateTime.now());
    return diff.isNegative ? Duration.zero : diff;
  }

  factory QuizGroup.fromDocument(
    DocumentSnapshot<Map<String, dynamic>> document,
  ) {
    final data = document.data() ?? const <String, dynamic>{};
    DateTime? exp;
    final rawExp = data['expiresAt'];
    if (rawExp is Timestamp) {
      exp = rawExp.toDate();
    } else if (rawExp is String) {
      exp = DateTime.tryParse(rawExp);
    }
    final currentUid = FirebaseAuth.instance.currentUser?.uid;
    final ownerId = data['ownerId'] as String?;
    final role = data['role'] as String? ??
        (ownerId != null && ownerId == currentUid ? 'owner' : 'member');
    return QuizGroup(
      id: document.id,
      name: data['name'] as String? ?? 'Faith Quiz group',
      role: role,
      joinCode: data['joinCode'] as String?,
      expiresAt: exp,
      durationMinutes: (data['durationMinutes'] as num?)?.toInt(),
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
    this.mode = 'competitive',
    this.status = 'active',
    this.currentQuestionIndex = 0,
    this.startedAt,
    this.currentQuestionOpenedAt,
    this.revealedAt,
    this.revealedAnswer,
    this.revealedExplanation,
    this.revealedScriptureReference,
    this.answeredUids = const [],
    this.ownerId,
  });

  final String id;
  final String title;
  final int questionCount;
  final String question;
  final List<String> options;
  final String explanation;
  final String scriptureReference;
  final List<GroupChallengeItem> items;
  final String mode; // 'competitive' or 'fellowship'
  final String status; // 'lobby', 'active', 'question_open', 'question_revealed', 'completed'
  final int currentQuestionIndex;
  final DateTime? startedAt;
  final DateTime? currentQuestionOpenedAt;
  final DateTime? revealedAt;
  final int? revealedAnswer;
  final String? revealedExplanation;
  final String? revealedScriptureReference;
  final List<String> answeredUids;
  final String? ownerId;

  bool get isCompetitive => mode == 'competitive';
  bool get isFellowship => mode == 'fellowship';
  bool get isLobby => status == 'lobby';
  bool get isCompleted => status == 'completed';
  bool get isQuestionOpen => status == 'question_open';
  bool get isQuestionRevealed => status == 'question_revealed';

  factory GroupChallenge.fromDocument(
    DocumentSnapshot<Map<String, dynamic>> document,
  ) {
    final data = document.data() ?? const <String, dynamic>{};
    final rawQuestions = data['questions'] as List?;
    final items = rawQuestions != null
        ? rawQuestions
            .whereType<Map>()
            .map((m) => GroupChallengeItem.fromMap(Map<String, dynamic>.from(m)))
            .toList()
        : const <GroupChallengeItem>[];

    DateTime? parseTimestamp(dynamic val) {
      if (val is Timestamp) return val.toDate();
      if (val is String) return DateTime.tryParse(val);
      return null;
    }

    final rawAnswered = data['answeredUids'] as List?;
    final answeredUids = rawAnswered != null
        ? rawAnswered.whereType<String>().toList()
        : const <String>[];

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
      mode: data['mode'] as String? ?? 'competitive',
      status: data['status'] as String? ?? 'active',
      currentQuestionIndex: (data['currentQuestionIndex'] as num?)?.toInt() ?? 0,
      startedAt: parseTimestamp(data['startedAt']),
      currentQuestionOpenedAt: parseTimestamp(data['currentQuestionOpenedAt']),
      revealedAt: parseTimestamp(data['revealedAt']),
      revealedAnswer: (data['revealedAnswer'] as num?)?.toInt(),
      revealedExplanation: data['revealedExplanation'] as String?,
      revealedScriptureReference: data['revealedScriptureReference'] as String?,
      answeredUids: answeredUids,
      ownerId: data['ownerId'] as String?,
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
      alreadySubmitted: data['alreadySubmitted'] == true,
      score: (data['score'] as num?)?.toInt() ?? 0,
      totalAnswered: (data['totalAnswered'] as num?)?.toInt() ?? 0,
      level: (data['level'] as num?)?.toInt() ?? 1,
      correctAnswer: (data['correctAnswer'] as num?)?.toInt(),
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
  Stream<List<LeaderboardEntry>> globalLeaderboard() => _firestore
      .collection('leaderboards')
      .doc('global_challenge')
      .collection('entries')
      .orderBy('score', descending: true)
      .limit(50)
      .snapshots()
      .map((snapshot) {
        final entries = snapshot.docs
            .map(LeaderboardEntry.fromDocument)
            .toList();
        entries.sort((left, right) {
          // 1. Primary: total verified correct answers / score DESC
          final byScore = right.score.compareTo(left.score);
          if (byScore != 0) return byScore;

          // 2. Secondary: accuracy percentage DESC
          final byAccuracy = right.accuracy.compareTo(left.accuracy);
          if (byAccuracy != 0) return byAccuracy;

          // 3. Tertiary: average first-attempt response time ASC
          final bySpeed = left.avgElapsedSeconds.compareTo(right.avgElapsedSeconds);
          if (bySpeed != 0) return bySpeed;

          // 4. Stable deterministic tie-breaker
          return left.id.compareTo(right.id);
        });
        return entries;
      });

  @override
  Future<String> createGroup(String name, {int durationMinutes = 10}) async {
    await _user();
    final result = await _functions.httpsCallable('createGroup').call(
      <String, Object>{
        'name': name.trim(),
        'durationMinutes': durationMinutes,
      },
    );
    final data = Map<String, dynamic>.from(result.data as Map);
    return (data['joinCode'] as String?) ?? (data['groupId'] as String);
  }

  @override
  Future<void> joinGroup(String groupId) async {
    await _user();
    await _functions.httpsCallable('joinGroup').call(<String, Object>{
      'groupId': groupId.trim(),
      'joinCode': groupId.trim(),
    });
  }

  @override
  Future<void> extendGroup(String groupId, {int additionalMinutes = 10}) async {
    await _user();
    await _functions.httpsCallable('extendGroup').call(<String, Object>{
      'groupId': groupId.trim(),
      'additionalMinutes': additionalMinutes,
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
  Stream<QuizGroup> streamGroup(String groupId) => _firestore
      .collection('groups')
      .doc(groupId)
      .snapshots()
      .where((doc) => doc.exists && doc.data() != null)
      .map((doc) => QuizGroup.fromDocument(doc));

  @override
  Stream<GroupChallenge> streamChallenge(String groupId, String challengeId) =>
      _firestore
          .collection('groups')
          .doc(groupId)
          .collection('challenges')
          .doc(challengeId)
          .snapshots()
          .where((doc) => doc.exists && doc.data() != null)
          .map((doc) => GroupChallenge.fromDocument(doc));

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
      debugPrint('[AuthDebug] Starting GoogleSignIn with serverClientId...');
      final googleSignIn = GoogleSignIn(
        serverClientId:
            '349710501534-vmhpfs7okhq6p7rr5mnio7kqt53v7smn.apps.googleusercontent.com',
        scopes: ['email', 'profile'],
      );
      final googleAccount = await googleSignIn.signIn();
      debugPrint('[AuthDebug] googleAccount: $googleAccount');
      if (googleAccount == null) {
        debugPrint('[AuthDebug] User cancelled account selection');
        return null;
      }
      final googleAuth = await googleAccount.authentication;
      debugPrint(
        '[AuthDebug] googleAuth accessToken: ${googleAuth.accessToken != null}, '
        'idToken: ${googleAuth.idToken != null} (${googleAuth.idToken?.length ?? 0} chars)',
      );
      final credential = GoogleAuthProvider.credential(
        accessToken: googleAuth.accessToken,
        idToken: googleAuth.idToken,
      );
      debugPrint('[AuthDebug] Signing in with credential...');
      final userCredential = await _auth.signInWithCredential(credential);
      debugPrint(
        '[AuthDebug] Signed in successfully: ${userCredential.user?.uid}, email: ${userCredential.user?.email}',
      );
      return userCredential.user;
    } catch (e, st) {
      debugPrint('[AuthDebug] GoogleSignIn exception: $e\n$st');
      return null;
    }
  }

  @override
  Future<String?> getClaimedUsername() async {
    final user = _auth.currentUser;
    if (user == null) return null;
    try {
      final doc = await _firestore.collection('users').doc(user.uid).get();
      if (doc.exists && doc.data() != null) {
        return doc.data()!['username'] as String?;
      }
    } catch (_) {}
    return null;
  }

  @override
  Future<void> claimUsername(String username) async {
    await _user();
    await _functions.httpsCallable('claimUsername').call(<String, Object>{
      'username': username.trim(),
    });
  }

  @override
  Future<bool> checkUsernameAvailable(String username) async {
    try {
      final result = await _functions
          .httpsCallable('checkUsernameAvailable')
          .call(<String, Object>{'username': username.trim()});
      final data = Map<String, dynamic>.from(result.data as Map);
      return data['available'] == true;
    } catch (_) {
      return false;
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
    String mode = 'competitive',
  }) async {
    await _user();
    final result = await _functions
        .httpsCallable('createGroupChallenge')
        .call(<String, Object>{
          'groupId': groupId,
          'catalogue': RemoteFeatureService.instance.activeCloudCatalogue,
          'questionCount': questionCount,
          'mode': mode,
          'title': ?title,
        });
    return (result.data as Map)['challengeId'] as String;
  }

  @override
  Future<void> startGroupChallenge({
    required String groupId,
    required String challengeId,
  }) async {
    await _user();
    await _functions.httpsCallable('startGroupChallenge').call(<String, Object>{
      'groupId': groupId,
      'challengeId': challengeId,
    });
  }

  @override
  Future<void> submitFellowshipAnswer({
    required String groupId,
    required String challengeId,
    required int questionIndex,
    required String questionId,
    required int selectedOptionIndex,
    required int responseLatencyMs,
    String? username,
  }) async {
    await _user();
    await _functions.httpsCallable('submitFellowshipAnswer').call(<String, Object>{
      'groupId': groupId,
      'challengeId': challengeId,
      'questionIndex': questionIndex,
      'questionId': questionId,
      'answerIndex': selectedOptionIndex,
      'selectedOptionIndex': selectedOptionIndex,
      'responseLatencyMs': responseLatencyMs,
      'username': ?username,
    });
  }

  @override
  Future<void> revealFellowshipAnswer({
    required String groupId,
    required String challengeId,
  }) async {
    await _user();
    await _functions.httpsCallable('revealFellowshipAnswer').call(<String, Object>{
      'groupId': groupId,
      'challengeId': challengeId,
    });
  }

  @override
  Future<void> advanceFellowshipQuestion({
    required String groupId,
    required String challengeId,
  }) async {
    await _user();
    await _functions.httpsCallable('advanceFellowshipQuestion').call(<String, Object>{
      'groupId': groupId,
      'challengeId': challengeId,
    });
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
