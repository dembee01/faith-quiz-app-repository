import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';

import 'models.dart';
import 'progress_store.dart';
import 'question_bank.dart';
import 'topic_question_bank.dart';

const _indigo = Color(0xFF4054B2);
const _slate = Color(0xFF111827);
const _gold = Color(0xFFFFD166);
const _correct = Color(0xFF45C486);
const _wrong = Color(0xFFE36A7A);

class FaithQuizApp extends StatelessWidget {
  const FaithQuizApp({super.key, required this.store});
  final ProgressStore store;

  ThemeData _theme(Brightness brightness) {
    final dark = brightness == Brightness.dark;
    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: ColorScheme.fromSeed(seedColor: _indigo, brightness: brightness),
      scaffoldBackgroundColor: dark ? _slate : const Color(0xFFF6F7FB),
      cardTheme: CardThemeData(
        color: dark ? const Color(0xFF1C2538) : Colors.white,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      ),
      appBarTheme: AppBarTheme(backgroundColor: dark ? _slate : const Color(0xFFF6F7FB), elevation: 0),
      navigationBarTheme: NavigationBarThemeData(backgroundColor: dark ? const Color(0xFF151E31) : Colors.white, indicatorColor: _indigo.withValues(alpha: 0.22)),
    );
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
        listenable: store,
        builder: (context, _) => MaterialApp(
          title: 'Faith Quiz',
          debugShowCheckedModeBanner: false,
          theme: _theme(Brightness.light),
          darkTheme: _theme(Brightness.dark),
          themeMode: store.themeMode == 'light' ? ThemeMode.light : ThemeMode.dark,
          home: AppShell(store: store),
        ),
      );
}

class AppShell extends StatefulWidget {
  const AppShell({super.key, required this.store});
  final ProgressStore store;
  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  int _index = 0;

  @override
  Widget build(BuildContext context) {
    final pages = [
      HomeScreen(store: widget.store),
      LevelsScreen(store: widget.store),
      JourneyScreen(store: widget.store),
      SettingsScreen(store: widget.store),
    ];
    return Scaffold(
      body: SafeArea(child: pages[_index]),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (index) => setState(() => _index = index),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'Home'),
          NavigationDestination(icon: Icon(Icons.grid_view_outlined), selectedIcon: Icon(Icons.grid_view), label: 'Levels'),
          NavigationDestination(icon: Icon(Icons.route_outlined), selectedIcon: Icon(Icons.route), label: 'Journey'),
          NavigationDestination(icon: Icon(Icons.settings_outlined), selectedIcon: Icon(Icons.settings), label: 'Settings'),
        ],
      ),
    );
  }
}

class PageFrame extends StatelessWidget {
  const PageFrame({super.key, required this.child, this.title});
  final Widget child;
  final String? title;

  @override
  Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        if (title != null) Padding(padding: const EdgeInsets.fromLTRB(20, 22, 20, 12), child: Text(title!, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800))),
        Expanded(child: child),
      ]);
}

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key, required this.store});
  final ProgressStore store;

  void _quiz(BuildContext context, int level, {String mode = 'classic'}) => Navigator.of(context).push(MaterialPageRoute(builder: (_) => QuizScreen(store: store, level: level, mode: mode)));

  @override
  Widget build(BuildContext context) => PageFrame(
        child: ListView(padding: const EdgeInsets.fromLTRB(20, 18, 20, 24), children: [
          Row(children: [
            Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('FAITH QUIZ', style: Theme.of(context).textTheme.labelLarge?.copyWith(color: _gold, letterSpacing: 2, fontWeight: FontWeight.bold)), const SizedBox(height: 5), Text('Grow in wisdom.', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w800))])),
            CircleAvatar(radius: 28, backgroundColor: _indigo.withValues(alpha: 0.24), child: const Icon(Icons.auto_awesome, color: _gold)),
          ]),
          const SizedBox(height: 24),
          ClipRRect(borderRadius: BorderRadius.circular(22), child: Image.asset('assets/images/faith_quiz_logo.jpg', height: 150, width: double.infinity, fit: BoxFit.cover)),
          const SizedBox(height: 16),
          Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Your covenant journey', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold)), const SizedBox(height: 10), Row(children: [Expanded(child: _Stat(label: 'Level', value: '${store.highestUnlocked}/30', icon: Icons.flag_outlined)), Expanded(child: _Stat(label: 'Best score', value: '${store.highScore}', icon: Icons.emoji_events_outlined)), Expanded(child: _Stat(label: 'Answered', value: '${store.totalAnswered}', icon: Icons.quiz_outlined))])]))),
          const SizedBox(height: 14),
          FilledButton.icon(onPressed: () => _quiz(context, store.highestUnlocked), icon: const Icon(Icons.play_arrow_rounded), label: Text('Continue level ${store.highestUnlocked}'), style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16))),
          const SizedBox(height: 10),
          OutlinedButton.icon(onPressed: () => _quiz(context, 1, mode: 'daily'), icon: const Icon(Icons.today_outlined), label: Text('Daily challenge  •  ${store.dailyStreak} day streak'), style: OutlinedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16))),
          const SizedBox(height: 18),
          Row(children: [
            Expanded(child: _HomeAction(icon: Icons.menu_book_outlined, label: 'Topics', onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => TopicPacksScreen(store: store))))),
            const SizedBox(width: 10),
            Expanded(child: _HomeAction(icon: Icons.replay_outlined, label: 'Review', onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => ReviewScreen(store: store))))),
            const SizedBox(width: 10),
            Expanded(child: _HomeAction(icon: Icons.leaderboard_outlined, label: 'Scores', onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => LeaderboardScreen(store: store))))),
          ]),
        ]),
      );
}

class _Stat extends StatelessWidget {
  const _Stat({required this.label, required this.value, required this.icon});
  final String label;
  final String value;
  final IconData icon;
  @override
  Widget build(BuildContext context) => Column(children: [Icon(icon, color: _gold, size: 22), const SizedBox(height: 4), Text(value, style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)), Text(label, style: Theme.of(context).textTheme.labelSmall)]);
}

class _HomeAction extends StatelessWidget {
  const _HomeAction({required this.icon, required this.label, required this.onTap});
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => Card(child: InkWell(borderRadius: BorderRadius.circular(18), onTap: onTap, child: Padding(padding: const EdgeInsets.symmetric(vertical: 18), child: Column(children: [Icon(icon, color: _gold), const SizedBox(height: 7), Text(label)]))));
}

class LevelsScreen extends StatelessWidget {
  const LevelsScreen({super.key, required this.store});
  final ProgressStore store;
  @override
  Widget build(BuildContext context) => PageFrame(
        title: 'Choose your level',
        child: GridView.builder(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, crossAxisSpacing: 12, mainAxisSpacing: 12),
          itemCount: journeyNodes.length,
          itemBuilder: (context, index) {
            final node = journeyNodes[index];
            final unlocked = node.level <= store.highestUnlocked;
            return Card(
              child: InkWell(
                borderRadius: BorderRadius.circular(18),
                onTap: unlocked ? () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => QuizScreen(store: store, level: node.level))) : null,
                child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                  Icon(unlocked ? Icons.star_rounded : Icons.lock_outline, color: unlocked ? _gold : Colors.grey),
                  const SizedBox(height: 5),
                  Text('${node.level}', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
                  Text(node.title, textAlign: TextAlign.center, maxLines: 2, overflow: TextOverflow.ellipsis, style: Theme.of(context).textTheme.labelSmall),
                ]),
              ),
            );
          },
        ),
      );
}

class JourneyScreen extends StatelessWidget {
  const JourneyScreen({super.key, required this.store});
  final ProgressStore store;
  @override
  Widget build(BuildContext context) => PageFrame(title: 'The covenant journey', child: ListView.separated(padding: const EdgeInsets.fromLTRB(20, 8, 20, 24), itemCount: journeyNodes.length, separatorBuilder: (_, _) => const SizedBox(height: 10), itemBuilder: (context, index) {
        final node = journeyNodes[index];
        final unlocked = node.level <= store.highestUnlocked;
        return Card(child: ListTile(leading: CircleAvatar(backgroundColor: unlocked ? _indigo : Colors.grey.shade700, child: Text('${node.level}')), title: Text(node.title, style: const TextStyle(fontWeight: FontWeight.bold)), subtitle: Text(node.description), trailing: Icon(unlocked ? Icons.chevron_right : Icons.lock_outline, color: unlocked ? _gold : Colors.grey), onTap: unlocked ? () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => QuizScreen(store: store, level: node.level))) : null));
      }));
}

class QuizScreen extends StatefulWidget {
  const QuizScreen({super.key, required this.store, required this.level, this.mode = 'classic', this.questions});
  final ProgressStore store;
  final int level;
  final String mode;
  final List<QuizQuestion>? questions;
  @override
  State<QuizScreen> createState() => _QuizScreenState();
}

class _QuizScreenState extends State<QuizScreen> {
  late final List<QuizQuestion> _questions;
  late final int _seed;
  int _index = 0;
  int _selected = -1;
  int _score = 0;
  int _elapsedSeconds = 0;
  late int _remainingSeconds;
  late int _lives;
  bool _finished = false;
  final List<String> _missed = [];
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    final supplied = widget.questions ?? QuestionBank.forLevel(widget.level);
    final clean = supplied.where((question) => question.isValid).toList();
    final saved = _savedSession;
    final topicSaved = _savedTopicSession;
    final savedIndex = saved?.index ?? topicSaved?.index;
    final savedScore = saved?.score ?? topicSaved?.score;
    _seed = saved?.seed ?? topicSaved?.seed ?? DateTime.now().microsecondsSinceEpoch;
    _questions = _prepareQuestions(clean.isEmpty ? _fallbackQuestions : clean, _seed);
    if (savedIndex != null) {
      _index = savedIndex.clamp(0, _questions.length - 1);
      _score = (savedScore ?? 0).clamp(0, _questions.length);
      _elapsedSeconds = saved?.elapsedSeconds.clamp(0, 86400) ?? 0;
    }
    _remainingSeconds = saved?.remainingSeconds ?? (widget.mode == 'speed' ? 60 : 0);
    _lives = saved?.lives ?? (widget.mode == 'survival' ? 3 : 0);
    _timer = Timer.periodic(const Duration(seconds: 1), _onTick);
  }

  QuizSession? get _savedSession {
    final value = widget.store.session;
    if (value != null && value.level == widget.level && value.mode == widget.mode) return value;
    return null;
  }

  TopicQuizSession? get _savedTopicSession {
    if (!{'gospels', 'prophets', 'parables'}.contains(widget.mode)) return null;
    final value = widget.store.topicSession;
    if (value != null && value.topic == widget.mode) return value;
    return null;
  }

  List<QuizQuestion> _prepareQuestions(List<QuizQuestion> source, int seed) {
    final prepared = source.asMap().entries.map((entry) {
      final question = entry.value;
      final indexed = question.options.asMap().entries.toList()..shuffle(Random(seed + entry.key));
      return QuizQuestion(
        question: question.question,
        options: indexed.map((item) => item.value).toList(),
        correctAnswer: indexed.indexWhere((item) => item.key == question.correctAnswer),
        explanation: question.explanation,
        verseReference: question.verseReference,
        verseText: question.verseText,
        translation: question.translation,
        commentary: question.commentary,
        crossRefs: question.crossRefs,
        learnMoreUrl: question.learnMoreUrl,
      );
    }).toList();
    if (widget.mode == 'daily') {
      final day = DateTime.now().toUtc().millisecondsSinceEpoch ~/ Duration.millisecondsPerDay;
      prepared.shuffle(Random(day));
      return prepared.take(1).toList();
    }
    prepared.shuffle(Random(seed));
    return prepared;
  }

  void _onTick(Timer timer) {
    if (!mounted || _finished || _selected != -1) return;
    var speedExpired = false;
    setState(() {
      _elapsedSeconds++;
      if (widget.mode == 'speed') {
        _remainingSeconds = (_remainingSeconds - 1).clamp(0, 3600);
        speedExpired = _remainingSeconds == 0;
      }
    });
    if (_elapsedSeconds % 2 == 0) unawaited(_saveProgress());
    if (speedExpired) _finish();
  }

  Future<void> _saveProgress() async {
    if (_finished) return;
    if ({'gospels', 'prophets', 'parables'}.contains(widget.mode)) {
      await widget.store.saveTopicSession(TopicQuizSession(topic: widget.mode, index: _index, score: _score, seed: _seed));
    } else {
      await widget.store.saveQuizSession(QuizSession(level: widget.level, mode: widget.mode, index: _index, score: _score, elapsedSeconds: _elapsedSeconds, seed: _seed, lives: _lives, remainingSeconds: _remainingSeconds));
    }
  }

  List<QuizQuestion> get _fallbackQuestions => const [QuizQuestion(question: 'What is the first book of the Bible?', options: ['Genesis', 'Exodus', 'Psalms', 'Matthew'], correctAnswer: 0, explanation: 'Genesis begins the biblical story.')];

  void _answer(int choice) {
    if (_selected != -1) return;
    final current = _questions[_index];
    setState(() => _selected = choice);
    if (choice == current.correctAnswer) {
      setState(() => _score++);
    } else {
      _missed.add(current.question);
      unawaited(widget.store.addMistakeDetailed(level: widget.level, question: current.question, userAnswer: current.options[choice], correctAnswer: current.options[current.correctAnswer], explanation: current.explanation));
      if (widget.mode == 'survival') {
        setState(() => _lives = (_lives - 1).clamp(0, 99));
      }
    }
    unawaited(widget.store.recordReviewResult(ProgressStore.createReviewKey(widget.level, current.question), choice == current.correctAnswer));
    unawaited(_saveProgress());
    if (widget.mode == 'survival' && _lives == 0) _finish();
  }

  Future<void> _next() async {
    if (_index < _questions.length - 1) {
      setState(() { _index++; _selected = -1; });
      await _saveProgress();
      return;
    }
    _finish();
  }

  void _finish() {
    if (_finished) return;
    _finished = true;
    _timer?.cancel();
    unawaited(_complete());
  }

  Future<void> _complete() async {
    if ({'gospels', 'prophets', 'parables'}.contains(widget.mode)) {
      await widget.store.setTopicScoreIfHigher(widget.mode, _score);
      await widget.store.recordTopicCompletion(_questions.length);
      await widget.store.clearTopicSession();
    } else {
      await widget.store.completeQuiz(level: widget.level, score: _score, total: _questions.length, missedQuestions: _missed, mode: widget.mode, elapsedSeconds: _elapsedSeconds);
      if (widget.mode == 'daily') await widget.store.recordDailyCompletion();
      await widget.store.clearQuizSession();
    }
    if (!mounted) return;
    Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => ResultsScreen(store: widget.store, score: _score, total: _questions.length, mode: widget.mode)));
  }

  @override
  void dispose() {
    if (!_finished) unawaited(_saveProgress());
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final current = _questions[_index];
    return Scaffold(appBar: AppBar(title: Text(widget.mode == 'daily' ? 'Daily challenge' : widget.mode == 'classic' ? 'Level ${widget.level}' : '${widget.mode[0].toUpperCase()}${widget.mode.substring(1)} quiz'), actions: [if (widget.mode == 'survival') Padding(padding: const EdgeInsets.only(right: 12), child: Center(child: Text('♥ $_lives'))), if (widget.mode == 'speed') Padding(padding: const EdgeInsets.only(right: 12), child: Center(child: Text('${_remainingSeconds}s'))), Padding(padding: const EdgeInsets.only(right: 18), child: Center(child: Text('${_index + 1}/${_questions.length}')))]), body: ListView(padding: const EdgeInsets.fromLTRB(20, 12, 20, 30), children: [
      LinearProgressIndicator(value: (_index + 1) / _questions.length, minHeight: 7, borderRadius: BorderRadius.circular(8)),
      const SizedBox(height: 28),
      Text(current.question, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
      const SizedBox(height: 22),
      ...List.generate(current.options.length, (choice) {
        final chosen = _selected == choice;
        final correct = choice == current.correctAnswer;
        final color = _selected == -1 ? null : correct ? _correct : chosen ? _wrong : null;
        return Padding(padding: const EdgeInsets.only(bottom: 12), child: OutlinedButton(onPressed: () => _answer(choice), style: OutlinedButton.styleFrom(alignment: Alignment.centerLeft, padding: const EdgeInsets.all(17), side: BorderSide(color: color ?? Theme.of(context).colorScheme.outlineVariant, width: color == null ? 1 : 2), backgroundColor: color?.withValues(alpha: 0.12)), child: Row(children: [CircleAvatar(radius: 15, backgroundColor: (color ?? _indigo).withValues(alpha: 0.22), child: Text(String.fromCharCode(65 + choice))), const SizedBox(width: 12), Expanded(child: Text(current.options[choice]))])));
      }),
      if (_selected != -1) ...[
        Card(child: Padding(padding: const EdgeInsets.all(16), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(_selected == current.correctAnswer ? 'Correct!' : 'Keep learning', style: TextStyle(color: _selected == current.correctAnswer ? _correct : _wrong, fontWeight: FontWeight.bold)), const SizedBox(height: 6), Text(current.explanation), if (current.verseReference != null) ...[const SizedBox(height: 8), Text('${current.verseReference} • ${current.translation}', style: Theme.of(context).textTheme.labelMedium)], if (current.commentary != null) ...[const SizedBox(height: 8), Text(current.commentary!, style: Theme.of(context).textTheme.bodySmall)] ]))),
        const SizedBox(height: 14),
        FilledButton(onPressed: _next, child: Text(_index == _questions.length - 1 ? 'Finish quiz' : 'Next question')),
      ],
    ]));
  }
}

class ResultsScreen extends StatelessWidget {
  const ResultsScreen({super.key, required this.store, required this.score, required this.total, required this.mode});
  final ProgressStore store;
  final int score;
  final int total;
  final String mode;
  @override
  Widget build(BuildContext context) {
    final percent = total == 0 ? 0 : (score / total * 100).round();
    return Scaffold(
      appBar: AppBar(title: const Text('Quiz complete')),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
            const Icon(Icons.emoji_events_rounded, color: _gold, size: 82),
            const SizedBox(height: 18),
            Text('$score / $total', style: Theme.of(context).textTheme.displaySmall?.copyWith(fontWeight: FontWeight.w900)),
            Text('$percent%  •  ${mode == 'daily' ? 'Daily challenge' : 'Level quiz'}', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 28),
            FilledButton(onPressed: () => Navigator.of(context).popUntil((route) => route.isFirst), child: const Text('Back to Faith Quiz')),
          ]),
        ),
      ),
    );
  }
}

class TopicPacksScreen extends StatelessWidget {
  const TopicPacksScreen({super.key, required this.store});
  final ProgressStore store;
  @override
  Widget build(BuildContext context) => Scaffold(appBar: AppBar(title: const Text('Topic packs')), body: ListView(padding: const EdgeInsets.all(20), children: [
        _TopicCard(store: store, topic: 'Gospels', icon: Icons.auto_stories_outlined),
        _TopicCard(store: store, topic: 'Prophets', icon: Icons.campaign_outlined),
        _TopicCard(store: store, topic: 'Parables', icon: Icons.lightbulb_outline),
      ]));
}

class _TopicCard extends StatelessWidget {
  const _TopicCard({required this.store, required this.topic, required this.icon});
  final ProgressStore store;
  final String topic;
  final IconData icon;
  @override
  Widget build(BuildContext context) {
    final key = topic.toLowerCase();
    final score = store.topicScores[key] ?? 0;
    return Card(margin: const EdgeInsets.only(bottom: 14), child: ListTile(leading: Icon(icon, color: _gold), title: Text(topic, style: const TextStyle(fontWeight: FontWeight.bold)), subtitle: Text('$score/50 • ${TopicQuestionBank.achievementTitle(key, score)}'), trailing: const Icon(Icons.chevron_right), onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => TopicQuizScreen(store: store, topic: topic)))));
  }
}

class TopicQuizScreen extends StatelessWidget {
  const TopicQuizScreen({super.key, required this.store, required this.topic});
  final ProgressStore store;
  final String topic;
  @override
  Widget build(BuildContext context) {
    final topicKey = topic.toLowerCase();
    final primary = TopicQuestionBank.forTopic(topicKey);
    final keywords = switch (topicKey) {
      'gospels' => ['jesus', 'gospel', 'disciple', 'apostle', 'pharisee', 'miracle', 'kingdom', 'galilee'],
      'prophets' => ['prophet', 'elijah', 'elisha', 'isaiah', 'jeremiah', 'ezekiel', 'daniel', 'jonah'],
      _ => ['parable', 'sower', 'mustard', 'lost', 'samaritan', 'talent', 'vineyard', 'wedding', 'sheep', 'coin'],
    };
    final existing = primary.map((question) => question.question.toLowerCase()).toSet();
    final supplemental = QuestionBank.levels.values.expand((items) => items).where((question) => question.isValid && !existing.contains(question.question.toLowerCase()) && keywords.any((keyword) => '${question.question} ${question.explanation}'.toLowerCase().contains(keyword))).toList();
    final focused = [...primary, ...supplemental].take(50).toList();
    return QuizScreen(store: store, level: 1, mode: topicKey, questions: focused);
  }
}

class ReviewScreen extends StatelessWidget {
  const ReviewScreen({super.key, required this.store});
  final ProgressStore store;
  @override
  Widget build(BuildContext context) {
    final detailed = store.detailedMistakes;
    return Scaffold(
      appBar: AppBar(title: Text('Spaced review${store.dueReviewCount == 0 ? '' : ' • ${store.dueReviewCount} due'}')),
      body: detailed.isEmpty && store.mistakes.isEmpty
          ? const Center(child: Text('No missed questions yet. Keep learning!'))
          : ListView.separated(
              padding: const EdgeInsets.all(20),
              itemCount: detailed.isEmpty ? store.mistakes.length : detailed.length,
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (_, index) {
                if (detailed.isEmpty) return Card(child: ListTile(leading: const Icon(Icons.replay, color: _gold), title: Text(store.mistakes[index])));
                final entry = detailed[index];
                return Card(child: ExpansionTile(leading: const Icon(Icons.replay, color: _gold), title: Text(entry.question), subtitle: Text('Level ${entry.level} • Your answer: ${entry.userAnswer}'), childrenPadding: const EdgeInsets.fromLTRB(72, 0, 18, 16), children: [Align(alignment: Alignment.centerLeft, child: Text('Correct answer: ${entry.correctAnswer}', style: const TextStyle(fontWeight: FontWeight.bold))), const SizedBox(height: 6), Align(alignment: Alignment.centerLeft, child: Text(entry.explanation))]));
              },
            ),
    );
  }
}

class LeaderboardScreen extends StatelessWidget {
  const LeaderboardScreen({super.key, required this.store});
  final ProgressStore store;
  @override
  Widget build(BuildContext context) => Scaffold(appBar: AppBar(title: const Text('Leaderboard')), body: ListView(padding: const EdgeInsets.all(20), children: [ClipRRect(borderRadius: BorderRadius.circular(20), child: Image.asset('assets/images/leaderboard_coming_soon.jpg', height: 190, fit: BoxFit.cover)), const SizedBox(height: 18), Card(child: ListTile(leading: const Icon(Icons.emoji_events, color: _gold), title: const Text('Your best score'), trailing: Text('${store.highScore}', style: const TextStyle(fontWeight: FontWeight.bold))))]));
}

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key, required this.store});
  final ProgressStore store;
  @override
  Widget build(BuildContext context) => PageFrame(title: 'Settings', child: ListView(padding: const EdgeInsets.fromLTRB(20, 8, 20, 24), children: [
        Card(child: Column(children: [SwitchListTile(title: const Text('Light theme'), subtitle: const Text('Use a brighter reading surface'), value: store.themeMode == 'light', onChanged: (value) => store.setThemeMode(value ? 'light' : 'dark')), SwitchListTile(title: const Text('Reduce motion'), subtitle: const Text('Use calmer transitions and feedback'), value: store.reduceMotion, onChanged: store.setReduceMotion), SwitchListTile(title: const Text('Crash reports'), subtitle: const Text('Share diagnostics only with your consent'), value: store.crashReportingEnabled, onChanged: store.setCrashReportingEnabled)])),
        const SizedBox(height: 14),
        Card(child: ListTile(leading: const Icon(Icons.text_fields_outlined), title: const Text('Reading size'), subtitle: Text(store.textScale == 'large' ? 'Large' : 'Normal'), trailing: DropdownButton<String>(value: store.textScale, underline: const SizedBox.shrink(), items: const [DropdownMenuItem(value: 'normal', child: Text('Normal')), DropdownMenuItem(value: 'large', child: Text('Large'))], onChanged: (value) { if (value != null) store.setTextScale(value); }))),
        const SizedBox(height: 14),
        Card(child: ListTile(leading: const Icon(Icons.delete_outline, color: _wrong), title: const Text('Reset progress'), subtitle: const Text('Clear scores, levels, and review items'), onTap: () async { final confirmed = await showDialog<bool>(context: context, builder: (context) => AlertDialog(title: const Text('Reset progress?'), content: const Text('This cannot be undone.'), actions: [TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')), FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Reset'))])); if (confirmed == true) await store.reset(); })),
        const SizedBox(height: 18),
        Text('Faith Quiz • Flutter migration', textAlign: TextAlign.center, style: Theme.of(context).textTheme.labelSmall),
      ]));
}
