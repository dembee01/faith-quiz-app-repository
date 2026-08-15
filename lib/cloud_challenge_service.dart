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
  Future<CloudSubmission> submit({
    required CloudChallenge challenge,
    required int answerIndex,
    required int elapsedSeconds,
    required String displayName,
  });
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

class GroupChallenge {
  const GroupChallenge({
    required this.id,
    required this.question,
    required this.options,
    required this.explanation,
    required this.scriptureReference,
  });

  final String id;
  final String question;
  final List<String> options;
  final String explanation;
  final String scriptureReference;

  factory GroupChallenge.fromDocument(
    QueryDocumentSnapshot<Map<String, dynamic>> document,
  ) {
    final data = document.data();
    return GroupChallenge(
      id: document.id,
      question: data['question'] as String? ?? '',
      options:
          (data['options'] as List?)?.whereType<String>().toList() ?? const [],
      explanation: data['explanation'] as String? ?? '',
      scriptureReference: data['scriptureReference'] as String? ?? '',
    );
  }
}

class CloudChallengeService implements CloudChallengeGateway {
  CloudChallengeService({
    FirebaseFirestore? firestore,
    FirebaseAuth? auth,
    FirebaseFunctions? functions,
  }) : _firestore = firestore ?? FirebaseFirestore.instance,
       _auth = auth ?? FirebaseAuth.instance,
       _functions = functions ?? FirebaseFunctions.instance;

  final FirebaseFirestore _firestore;
  final FirebaseAuth _auth;
  final FirebaseFunctions _functions;

  Future<bool> get isAvailable async {
    if (!RemoteFeatureService.instance.cloudChallengesEnabled) return false;
    await _user();
    return true;
  }

  @override
  Future<CloudChallenge?> loadToday() async {
    if (!await isAvailable) return null;
    final today =
        DateTime.now().toUtc().millisecondsSinceEpoch ~/
        Duration.millisecondsPerDay;
    final catalogue = RemoteFeatureService.instance.activeCloudCatalogue;
    final snapshot = await _firestore
        .collection('content')
        .doc(catalogue)
        .collection('questions')
        .where('dailyIndex', isEqualTo: today % 500)
        .limit(1)
        .get();
    if (snapshot.docs.isEmpty) return null;
    final challenge = CloudChallenge.fromDocument(snapshot.docs.first);
    if (challenge.question.isEmpty || challenge.options.length != 4) {
      return null;
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
      .limit(100)
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

  Future<String> createGroup(String name) async {
    await _user();
    final result = await _functions.httpsCallable('createGroup').call(
      <String, Object>{'name': name.trim()},
    );
    return (result.data as Map)['groupId'] as String;
  }

  Future<void> joinGroup(String groupId) async {
    await _user();
    await _functions.httpsCallable('joinGroup').call(<String, Object>{
      'groupId': groupId.trim(),
    });
  }

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

  Stream<List<GroupChallenge>> groupChallenges(String groupId) => _firestore
      .collection('groups')
      .doc(groupId)
      .collection('challenges')
      .orderBy('createdAt', descending: true)
      .snapshots()
      .map(
        (snapshot) => snapshot.docs.map(GroupChallenge.fromDocument).toList(),
      );

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
