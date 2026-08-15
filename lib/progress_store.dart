import 'dart:convert';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'reminder_service.dart';

class QuizSession {
  const QuizSession({
    required this.level,
    required this.mode,
    required this.index,
    required this.score,
    required this.elapsedSeconds,
    required this.seed,
    required this.lives,
    required this.remainingSeconds,
  });

  final int level;
  final String mode;
  final int index;
  final int score;
  final int elapsedSeconds;
  final int seed;
  final int lives;
  final int remainingSeconds;

  Map<String, Object> toJson() => {
    'level': level,
    'mode': mode,
    'index': index,
    'score': score,
    'elapsedSeconds': elapsedSeconds,
    'seed': seed,
    'lives': lives,
    'remainingSeconds': remainingSeconds,
  };

  static QuizSession? fromJson(Object? value) {
    if (value is! Map) return null;
    int? integer(String key) => (value[key] as num?)?.toInt();
    final level = integer('level');
    final mode = value['mode'] as String?;
    final index = integer('index');
    final score = integer('score');
    final elapsed = integer('elapsedSeconds');
    final seed = integer('seed');
    final lives = integer('lives');
    final remaining = integer('remainingSeconds');
    if ([
      level,
      index,
      score,
      elapsed,
      seed,
      lives,
      remaining,
      mode,
    ].contains(null)) {
      return null;
    }
    return QuizSession(
      level: level!,
      mode: mode!,
      index: index!,
      score: score!,
      elapsedSeconds: elapsed!,
      seed: seed!,
      lives: lives!,
      remainingSeconds: remaining!,
    );
  }
}

class MistakeEntry {
  const MistakeEntry({
    required this.level,
    required this.date,
    required this.question,
    required this.userAnswer,
    required this.correctAnswer,
    required this.explanation,
  });

  final int level;
  final DateTime date;
  final String question;
  final String userAnswer;
  final String correctAnswer;
  final String explanation;

  Map<String, Object> toJson() => {
    'level': level,
    'date': date.toIso8601String(),
    'question': question,
    'userAnswer': userAnswer,
    'correctAnswer': correctAnswer,
    'explanation': explanation,
  };

  static MistakeEntry? fromJson(Object? value) {
    if (value is! Map) return null;
    final level = (value['level'] as num?)?.toInt();
    final date = DateTime.tryParse(value['date'] as String? ?? '');
    final question = value['question'] as String?;
    final userAnswer = value['userAnswer'] as String?;
    final correctAnswer = value['correctAnswer'] as String?;
    final explanation = value['explanation'] as String?;
    if (level == null ||
        date == null ||
        question == null ||
        userAnswer == null ||
        correctAnswer == null ||
        explanation == null) {
      return null;
    }
    return MistakeEntry(
      level: level,
      date: date,
      question: question,
      userAnswer: userAnswer,
      correctAnswer: correctAnswer,
      explanation: explanation,
    );
  }
}

class LastAttempt {
  const LastAttempt({
    required this.level,
    required this.score,
    required this.total,
    required this.timeSeconds,
    required this.date,
    required this.mode,
  });
  final int level;
  final int score;
  final int total;
  final int timeSeconds;
  final DateTime date;
  final String mode;
}

class TopicQuizSession {
  const TopicQuizSession({
    required this.topic,
    required this.index,
    required this.score,
    required this.answered,
    required this.seed,
  });
  final String topic;
  final int index;
  final int score;
  final int answered;
  final int seed;

  Map<String, Object> toJson() => {
    'topic': topic,
    'index': index,
    'score': score,
    'answered': answered,
    'seed': seed,
  };

  static TopicQuizSession? fromJson(Object? value) {
    if (value is! Map) return null;
    final topic = value['topic'] as String?;
    final index = (value['index'] as num?)?.toInt();
    final score = (value['score'] as num?)?.toInt();
    final answered = (value['answered'] as num?)?.toInt();
    final seed = (value['seed'] as num?)?.toInt();
    if (topic == null ||
        index == null ||
        score == null ||
        seed == null ||
        topic.isEmpty) {
      return null;
    }
    return TopicQuizSession(
      topic: topic,
      index: index,
      score: score,
      // Sessions saved before this field was added have already advanced to
      // the next question, so the index is the best available progress value.
      answered: (answered ?? index).clamp(0, 50),
      seed: seed,
    );
  }
}

class ProgressStore extends ChangeNotifier {
  static const _highestUnlockedKey = 'highest_unlocked_level';
  static const _totalAnsweredKey = 'total_questions_answered';
  static const _highScoreKey = 'high_score';
  static const _lastCompletedKey = 'last_completed_level';
  static const _totalAttemptsKey = 'total_attempts';
  static const _dailyStreakKey = 'daily_streak';
  static const _lastQuizDayKey = 'last_quiz_epoch_day';
  static const _devotionStreakKey = 'devotion_streak';
  static const _lastDevotionDayKey = 'last_devotion_epoch_day';
  static const _lastDailyChallengeDayKey = 'last_daily_challenge_epoch_day';
  static const _adaptiveLevelKey = 'adaptive_level';
  static const _themeKey = 'theme_mode';
  static const _textScaleKey = 'text_scale';
  static const _reduceMotionKey = 'reduce_motion';
  static const _crashReportingKey = 'crash_reporting_enabled';
  static const _mistakeLogKey = 'mistake_log';
  static const _dueReviewKey = 'srs_due_map';
  static const _totalTimeKey = 'total_time_spent_seconds';
  static const _lastAttemptKey = 'last_attempt';
  static const _sessionKey = 'quiz_session';
  static const _topicSessionKey = 'topic_session';
  static const _topicScoresKey = 'topic_scores';
  static const _cloudBackupEnabledKey = 'cloud_backup_enabled';
  static const _cloudUpdatedAtKey = 'cloud_updated_at';
  static const _remindersEnabledKey = 'reminders_enabled';
  static const _leaderboardNameKey = 'leaderboard_display_name';

  SharedPreferences? _preferences;
  int highestUnlocked = 1;
  int totalAnswered = 0;
  int highScore = 0;
  int lastCompletedLevel = 1;
  int totalAttempts = 0;
  int totalTimeSpentSeconds = 0;
  int dailyStreak = 0;
  int devotionStreak = 0;
  int lastDailyChallengeDay = -1;
  int adaptiveLevel = 1;
  bool perfectAchievement = false;
  bool streakAchievement = false;
  String themeMode = 'dark';
  String textScale = 'normal';
  bool reduceMotion = false;
  bool crashReportingEnabled = false;
  List<String> mistakes = <String>[];
  List<MistakeEntry> detailedMistakes = <MistakeEntry>[];
  Map<String, int> dueReview = <String, int>{};
  Map<String, int> topicScores = <String, int>{
    'gospels': 0,
    'prophets': 0,
    'parables': 0,
  };
  QuizSession? session;
  TopicQuizSession? topicSession;
  LastAttempt? lastAttempt;
  bool cloudBackupEnabled = false;
  String cloudSyncStatus = 'Cloud backup is off';
  bool remindersEnabled = false;
  String reminderStatus = 'Off';
  String leaderboardName = 'Faith learner';
  int _cloudUpdatedAt = 0;
  bool _applyingCloudState = false;

  int get dueReviewCount =>
      dueReview.entries.where((entry) => entry.value <= _today).length;
  int get _today =>
      DateTime.now().toUtc().millisecondsSinceEpoch ~/
      Duration.millisecondsPerDay;
  bool get isDailyChallengeCompletedToday => lastDailyChallengeDay == _today;

  Future<void> load() async {
    _preferences = await SharedPreferences.getInstance();
    final prefs = _preferences!;
    highestUnlocked = prefs.getInt(_highestUnlockedKey) ?? 1;
    totalAnswered = prefs.getInt(_totalAnsweredKey) ?? 0;
    highScore = prefs.getInt(_highScoreKey) ?? 0;
    lastCompletedLevel = prefs.getInt(_lastCompletedKey) ?? 1;
    totalAttempts = prefs.getInt(_totalAttemptsKey) ?? 0;
    totalTimeSpentSeconds = prefs.getInt(_totalTimeKey) ?? 0;
    dailyStreak = prefs.getInt(_dailyStreakKey) ?? 0;
    devotionStreak = prefs.getInt(_devotionStreakKey) ?? 0;
    lastDailyChallengeDay = prefs.getInt(_lastDailyChallengeDayKey) ?? -1;
    adaptiveLevel = prefs.getInt(_adaptiveLevelKey) ?? 1;
    perfectAchievement = prefs.getBool('achievement_perfect_score') ?? false;
    streakAchievement = prefs.getBool('achievement_streak_7') ?? false;
    // Faith Quiz has one intentional visual identity: Slate Indigo dark mode.
    // Normalize older saved values that still contain the retired light option.
    themeMode = 'dark';
    textScale = prefs.getString(_textScaleKey) ?? 'normal';
    reduceMotion = prefs.getBool(_reduceMotionKey) ?? false;
    // No crash-reporting provider is configured in this app.
    crashReportingEnabled = false;
    mistakes = prefs.getStringList('mistakes') ?? <String>[];
    detailedMistakes = _decodeMistakes(prefs.getString(_mistakeLogKey));
    dueReview = _decodeDue(prefs.getString(_dueReviewKey));
    final scores = _decodeIntMap(prefs.getString(_topicScoresKey));
    topicScores = {'gospels': 0, 'prophets': 0, 'parables': 0, ...scores};
    session = QuizSession.fromJson(_decodeObject(prefs.getString(_sessionKey)));
    topicSession = TopicQuizSession.fromJson(
      _decodeObject(prefs.getString(_topicSessionKey)),
    );
    lastAttempt = _decodeLastAttempt(prefs.getString(_lastAttemptKey));
    cloudBackupEnabled = prefs.getBool(_cloudBackupEnabledKey) ?? false;
    remindersEnabled = prefs.getBool(_remindersEnabledKey) ?? false;
    reminderStatus = remindersEnabled ? 'Daily reminder enabled' : 'Off';
    leaderboardName = _normalizeLeaderboardName(
      prefs.getString(_leaderboardNameKey) ?? 'Faith learner',
    );
    if (leaderboardName.isEmpty) leaderboardName = 'Faith learner';
    _cloudUpdatedAt = prefs.getInt(_cloudUpdatedAtKey) ?? 0;
    if (cloudBackupEnabled) {
      cloudSyncStatus = 'Backup ready';
    }
    notifyListeners();
  }

  /// Enables an anonymous, private Firebase identity and synchronizes the
  /// most recently changed copy of progress. No display name or quiz answers
  /// are made public.
  Future<void> enableCloudBackup() async {
    cloudBackupEnabled = true;
    cloudSyncStatus = 'Connecting secure backup…';
    await _preferences?.setBool(_cloudBackupEnabledKey, true);
    notifyListeners();
    try {
      final user = await _authenticatedUser();
      final ref = FirebaseFirestore.instance
          .collection('users')
          .doc(user.uid)
          .collection('private')
          .doc('progress');
      final remote = await ref.get();
      final remotePayload = remote.data()?['payload'];
      final remoteUpdatedAt =
          (remote.data()?['updatedAt'] as num?)?.toInt() ?? 0;
      if (remotePayload is Map && remoteUpdatedAt > _cloudUpdatedAt) {
        _applyingCloudState = true;
        await _applyCloudPayload(Map<String, dynamic>.from(remotePayload));
        _applyingCloudState = false;
        cloudSyncStatus = 'Restored from cloud';
      } else {
        await _syncCloud();
      }
    } catch (_) {
      cloudSyncStatus =
          'Backup unavailable — your progress remains safe on this device';
    }
    notifyListeners();
  }

  Future<void> disableCloudBackup() async {
    cloudBackupEnabled = false;
    cloudSyncStatus = 'Cloud backup is off';
    await _preferences?.setBool(_cloudBackupEnabledKey, false);
    notifyListeners();
  }

  /// Registers this device only after a player chooses to receive a daily
  /// reminder. A failed permission request leaves reminders safely disabled.
  Future<void> setRemindersEnabled(bool enabled) async {
    reminderStatus = enabled ? 'Requesting permission…' : 'Turning off…';
    notifyListeners();
    try {
      if (enabled) {
        await ReminderService().enable();
      } else {
        await ReminderService().disable();
      }
      remindersEnabled = enabled;
      reminderStatus = enabled ? 'Daily reminder enabled' : 'Off';
      await _preferences?.setBool(_remindersEnabledKey, enabled);
    } catch (error) {
      remindersEnabled = false;
      reminderStatus = error is StateError
          ? error.message.toString()
          : 'Reminder setup needs an internet connection';
      await _preferences?.setBool(_remindersEnabledKey, false);
    }
    notifyListeners();
  }

  Future<void> setLeaderboardName(String value) async {
    leaderboardName = _normalizeLeaderboardName(value);
    await _preferences?.setString(_leaderboardNameKey, leaderboardName);
    notifyListeners();
  }

  Future<User> _authenticatedUser() async {
    final existing = FirebaseAuth.instance.currentUser;
    return existing ?? (await FirebaseAuth.instance.signInAnonymously()).user!;
  }

  Map<String, dynamic> _cloudPayload() => <String, dynamic>{
    'highestUnlocked': highestUnlocked,
    'totalAnswered': totalAnswered,
    'highScore': highScore,
    'lastCompletedLevel': lastCompletedLevel,
    'totalAttempts': totalAttempts,
    'totalTimeSpentSeconds': totalTimeSpentSeconds,
    'dailyStreak': dailyStreak,
    'devotionStreak': devotionStreak,
    'lastDailyChallengeDay': lastDailyChallengeDay,
    'adaptiveLevel': adaptiveLevel,
    'perfectAchievement': perfectAchievement,
    'streakAchievement': streakAchievement,
    'textScale': textScale,
    'reduceMotion': reduceMotion,
    'mistakes': mistakes,
    'detailedMistakes': detailedMistakes
        .map((entry) => entry.toJson())
        .toList(),
    'dueReview': dueReview,
    'topicScores': topicScores,
    'session': session?.toJson(),
    'topicSession': topicSession?.toJson(),
  };

  Future<void> _applyCloudPayload(Map<String, dynamic> value) async {
    int integer(String key, int fallback) =>
        (value[key] as num?)?.toInt() ?? fallback;
    highestUnlocked = integer('highestUnlocked', highestUnlocked).clamp(1, 30);
    totalAnswered = integer('totalAnswered', totalAnswered).clamp(0, 1 << 31);
    highScore = integer('highScore', highScore).clamp(0, 1 << 31);
    lastCompletedLevel = integer(
      'lastCompletedLevel',
      lastCompletedLevel,
    ).clamp(1, 30);
    totalAttempts = integer('totalAttempts', totalAttempts).clamp(0, 1 << 31);
    totalTimeSpentSeconds = integer(
      'totalTimeSpentSeconds',
      totalTimeSpentSeconds,
    ).clamp(0, 1 << 31);
    dailyStreak = integer('dailyStreak', dailyStreak).clamp(0, 100000);
    devotionStreak = integer('devotionStreak', devotionStreak).clamp(0, 100000);
    lastDailyChallengeDay = integer(
      'lastDailyChallengeDay',
      lastDailyChallengeDay,
    );
    adaptiveLevel = integer('adaptiveLevel', adaptiveLevel).clamp(1, 30);
    perfectAchievement =
        value['perfectAchievement'] as bool? ?? perfectAchievement;
    streakAchievement =
        value['streakAchievement'] as bool? ?? streakAchievement;
    textScale = value['textScale'] == 'large' ? 'large' : 'normal';
    reduceMotion = value['reduceMotion'] as bool? ?? reduceMotion;
    mistakes =
        (value['mistakes'] as List?)?.whereType<String>().take(100).toList() ??
        mistakes;
    detailedMistakes =
        (value['detailedMistakes'] as List?)
            ?.map(MistakeEntry.fromJson)
            .whereType<MistakeEntry>()
            .take(200)
            .toList() ??
        detailedMistakes;
    dueReview = _decodeDue(jsonEncode(value['dueReview']));
    topicScores = {
      'gospels': 0,
      'prophets': 0,
      'parables': 0,
      ..._decodeIntMap(jsonEncode(value['topicScores'])),
    };
    session = QuizSession.fromJson(value['session']);
    topicSession = TopicQuizSession.fromJson(value['topicSession']);
    _cloudUpdatedAt = DateTime.now().millisecondsSinceEpoch;
    await _persist();
  }

  Future<void> _syncCloud() async {
    if (!cloudBackupEnabled || _applyingCloudState) return;
    try {
      final user = await _authenticatedUser();
      _cloudUpdatedAt = DateTime.now().millisecondsSinceEpoch;
      await FirebaseFirestore.instance
          .collection('users')
          .doc(user.uid)
          .collection('private')
          .doc('progress')
          .set(<String, dynamic>{
            'schemaVersion': 1,
            'updatedAt': _cloudUpdatedAt,
            'payload': _cloudPayload(),
          });
      await _preferences?.setInt(_cloudUpdatedAtKey, _cloudUpdatedAt);
      cloudSyncStatus = 'Backed up securely';
    } catch (_) {
      cloudSyncStatus = 'Backup pending — check your connection';
    }
  }

  Future<void> completeQuiz({
    required int level,
    required int score,
    required int total,
    required List<String> missedQuestions,
    String mode = 'classic',
    int elapsedSeconds = 0,
  }) async {
    totalAnswered += total;
    totalAttempts += 1;
    totalTimeSpentSeconds += elapsedSeconds.clamp(0, 86400);
    final percentage = total == 0 ? 0 : (score * 100 ~/ total);
    if (mode != 'practice' && score > highScore) highScore = score;
    if ((mode == 'classic' || mode == 'journey') && percentage >= 60) {
      lastCompletedLevel = level.clamp(1, 30);
      if (level >= highestUnlocked) highestUnlocked = (level + 1).clamp(1, 30);
    }
    perfectAchievement = perfectAchievement || (total > 0 && score == total);
    adaptiveLevel =
        (percentage >= 80
                ? adaptiveLevel + 1
                : percentage <= 40
                ? adaptiveLevel - 1
                : adaptiveLevel)
            .clamp(1, 30);
    mistakes = <String>[...missedQuestions, ...mistakes].take(100).toList();
    _updateQuizStreak();
    if (dailyStreak >= 7) streakAchievement = true;
    lastAttempt = LastAttempt(
      level: level,
      score: score,
      total: total,
      timeSeconds: elapsedSeconds,
      date: DateTime.now(),
      mode: mode,
    );
    session = null;
    await _persist();
    notifyListeners();
  }

  Future<void> recordDailyCompletion() async {
    final prefs = _preferences;
    if (prefs == null) return;
    final today = _today;
    final previous = prefs.getInt(_lastDevotionDayKey) ?? -1;
    if (previous != today) {
      devotionStreak = previous == today - 1 ? devotionStreak + 1 : 1;
      await prefs.setInt(_lastDevotionDayKey, today);
      await prefs.setInt(_devotionStreakKey, devotionStreak);
    }
    lastDailyChallengeDay = today;
    await prefs.setInt(_lastDailyChallengeDayKey, today);
    notifyListeners();
  }

  Future<void> saveQuizSession(QuizSession value) async {
    session = value;
    await _preferences?.setString(_sessionKey, jsonEncode(value.toJson()));
    notifyListeners();
  }

  Future<void> clearQuizSession() async {
    session = null;
    await _preferences?.remove(_sessionKey);
    notifyListeners();
  }

  Future<void> saveTopicSession(TopicQuizSession value) async {
    topicSession = value;
    await _preferences?.setString(_topicSessionKey, jsonEncode(value.toJson()));
    notifyListeners();
  }

  Future<void> clearTopicSession() async {
    topicSession = null;
    await _preferences?.remove(_topicSessionKey);
    notifyListeners();
  }

  Future<void> addMistakeDetailed({
    required int level,
    required String question,
    required String userAnswer,
    required String correctAnswer,
    required String explanation,
  }) async {
    final entry = MistakeEntry(
      level: level,
      date: DateTime.now(),
      question: question,
      userAnswer: userAnswer,
      correctAnswer: correctAnswer,
      explanation: explanation,
    );
    detailedMistakes = <MistakeEntry>[
      entry,
      ...detailedMistakes,
    ].take(200).toList();
    if (!mistakes.contains(question)) {
      mistakes = <String>[question, ...mistakes].take(100).toList();
    }
    dueReview[createReviewKey(level, question)] = _today + 1;
    await _persist();
    notifyListeners();
  }

  Future<void> recordReviewResult(String key, bool wasCorrect) async {
    final current = dueReview[key];
    if (current == null && wasCorrect) return;
    if (!wasCorrect) {
      dueReview[key] = _today + 1;
    } else {
      final interval = current == null || current <= _today + 1
          ? 3
          : current <= _today + 3
          ? 7
          : current <= _today + 7
          ? 14
          : 30;
      dueReview[key] = _today + interval;
    }
    await _persist();
    notifyListeners();
  }

  List<String> get dueReviewKeys => dueReview.entries
      .where((entry) => entry.value <= _today)
      .map((entry) => entry.key)
      .toList();

  static String createReviewKey(int level, String question) =>
      '$level|${Uri.encodeComponent(question)}';
  static int? reviewLevelFromKey(String key) =>
      int.tryParse(key.split('|').first);

  Future<void> setThemeMode(String value) async {
    themeMode = 'dark';
    await _preferences?.setString(_themeKey, themeMode);
    notifyListeners();
  }

  Future<void> setTextScale(String value) async {
    textScale = value == 'large' ? 'large' : 'normal';
    await _preferences?.setString(_textScaleKey, textScale);
    notifyListeners();
  }

  Future<void> setReduceMotion(bool value) async {
    reduceMotion = value;
    await _preferences?.setBool(_reduceMotionKey, value);
    notifyListeners();
  }

  Future<void> setCrashReportingEnabled(bool value) async {
    // Kept for compatibility with earlier builds. No diagnostic data is sent.
    crashReportingEnabled = false;
    await _preferences?.setBool(_crashReportingKey, false);
    notifyListeners();
  }

  Future<void> setTopicScoreIfHigher(String topic, int score) async {
    final normalized = topic.toLowerCase();
    if (!topicScores.containsKey(normalized) ||
        score <= (topicScores[normalized] ?? 0)) {
      return;
    }
    topicScores[normalized] = score.clamp(0, 50);
    await _persist();
    notifyListeners();
  }

  Future<void> recordTopicCompletion(
    int answered, {
    int elapsedSeconds = 0,
  }) async {
    totalAnswered += answered;
    totalAttempts += 1;
    totalTimeSpentSeconds += elapsedSeconds.clamp(0, 86400);
    await _persist();
    notifyListeners();
  }

  Future<void> reset() async {
    if (remindersEnabled) {
      // The reset command also withdraws the device token so it cannot keep
      // receiving reminders after a player has cleared their local data.
      await setRemindersEnabled(false);
    }
    await _preferences?.clear();
    highestUnlocked = 1;
    totalAnswered = 0;
    highScore = 0;
    lastCompletedLevel = 1;
    totalAttempts = 0;
    totalTimeSpentSeconds = 0;
    dailyStreak = 0;
    devotionStreak = 0;
    lastDailyChallengeDay = -1;
    adaptiveLevel = 1;
    perfectAchievement = false;
    streakAchievement = false;
    themeMode = 'dark';
    textScale = 'normal';
    reduceMotion = false;
    crashReportingEnabled = false;
    mistakes = <String>[];
    detailedMistakes = <MistakeEntry>[];
    dueReview = <String, int>{};
    topicScores = {'gospels': 0, 'prophets': 0, 'parables': 0};
    session = null;
    topicSession = null;
    lastAttempt = null;
    remindersEnabled = false;
    reminderStatus = 'Off';
    leaderboardName = 'Faith learner';
    notifyListeners();
  }

  void _updateQuizStreak() {
    final prefs = _preferences;
    if (prefs == null) return;
    final today = _today;
    final previous = prefs.getInt(_lastQuizDayKey) ?? -1;
    if (previous != today) {
      dailyStreak = previous == today - 1 ? dailyStreak + 1 : 1;
      prefs.setInt(_lastQuizDayKey, today);
    }
  }

  Future<void> _persist() async {
    final prefs = _preferences;
    if (prefs == null) return;
    await prefs.setInt(_highestUnlockedKey, highestUnlocked);
    await prefs.setInt(_totalAnsweredKey, totalAnswered);
    await prefs.setInt(_highScoreKey, highScore);
    await prefs.setInt(_lastCompletedKey, lastCompletedLevel);
    await prefs.setInt(_totalAttemptsKey, totalAttempts);
    await prefs.setInt(_totalTimeKey, totalTimeSpentSeconds);
    await prefs.setInt(_dailyStreakKey, dailyStreak);
    await prefs.setInt(_devotionStreakKey, devotionStreak);
    await prefs.setInt(_lastDailyChallengeDayKey, lastDailyChallengeDay);
    await prefs.setInt(_adaptiveLevelKey, adaptiveLevel);
    await prefs.setBool('achievement_perfect_score', perfectAchievement);
    await prefs.setBool('achievement_streak_7', streakAchievement);
    await prefs.setStringList('mistakes', mistakes);
    await prefs.setString(
      _mistakeLogKey,
      jsonEncode(detailedMistakes.map((entry) => entry.toJson()).toList()),
    );
    await prefs.setString(_dueReviewKey, jsonEncode(dueReview));
    await prefs.setString(_topicScoresKey, jsonEncode(topicScores));
    if (lastAttempt == null) {
      await prefs.remove(_lastAttemptKey);
    } else {
      await prefs.setString(
        _lastAttemptKey,
        jsonEncode({
          'level': lastAttempt!.level,
          'score': lastAttempt!.score,
          'total': lastAttempt!.total,
          'time': lastAttempt!.timeSeconds,
          'date': lastAttempt!.date.toIso8601String(),
          'mode': lastAttempt!.mode,
        }),
      );
    }
    if (cloudBackupEnabled && !_applyingCloudState) {
      await _syncCloud();
    }
  }

  static Object? _decodeObject(String? raw) {
    if (raw == null || raw.isEmpty) return null;
    try {
      return jsonDecode(raw);
    } catch (_) {
      return null;
    }
  }

  static List<MistakeEntry> _decodeMistakes(String? raw) {
    final value = _decodeObject(raw);
    if (value is! List) return <MistakeEntry>[];
    return value.map(MistakeEntry.fromJson).whereType<MistakeEntry>().toList();
  }

  static Map<String, int> _decodeDue(String? raw) {
    final value = _decodeObject(raw);
    if (value is! Map) return <String, int>{};
    return value.map(
      (key, val) => MapEntry('$key', (val as num?)?.toInt() ?? 0),
    );
  }

  static Map<String, int> _decodeIntMap(String? raw) {
    final value = _decodeObject(raw);
    if (value is! Map) return <String, int>{};
    return value.map(
      (key, val) => MapEntry('$key', (val as num?)?.toInt() ?? 0),
    );
  }

  static LastAttempt? _decodeLastAttempt(String? raw) {
    final value = _decodeObject(raw);
    if (value is! Map) return null;
    final date = DateTime.tryParse(value['date'] as String? ?? '');
    if (date == null) return null;
    return LastAttempt(
      level: (value['level'] as num?)?.toInt() ?? 0,
      score: (value['score'] as num?)?.toInt() ?? 0,
      total: (value['total'] as num?)?.toInt() ?? 0,
      timeSeconds: (value['time'] as num?)?.toInt() ?? 0,
      date: date,
      mode: value['mode'] as String? ?? 'classic',
    );
  }

  static String _normalizeLeaderboardName(String value) {
    final normalized = value.trim().replaceAll(RegExp(r'\s+'), ' ');
    if (normalized.isEmpty) return 'Faith learner';
    return normalized.length <= 24 ? normalized : normalized.substring(0, 24);
  }
}
