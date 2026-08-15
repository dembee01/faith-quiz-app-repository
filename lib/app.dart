import 'dart:async';
import 'dart:math';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';

import 'answer_feedback.dart';
import 'cloud_challenge_service.dart';
import 'models.dart';
import 'progress_store.dart';
import 'question_bank.dart';
import 'topic_question_bank.dart';

// Canonical Slate Indigo tokens from the Android Compose app.
const _slateTop = Color(0xFF474E76);
const _slateBottom = Color(0xFF2C314E);
const _slateSurface = Color(0xFF383E62);
const _slateSurfaceVariant = Color(0xFF2F3452);
const _slateCardLight = Color(0xFFF6F7FB);
const _slateTextPrimary = Colors.white;
const _slateTextSecondary = Color(0xFFC5CAE9);
const _slateTextMuted = Color(0xFF9AA0C7);
const _slateButtonText = Color(0xFF383E62);
const _gold = Color(0xFFFBBF24);
const _goldDark = Color(0xFFD97706);
const _correct = Color(0xFF10B981);
const _wrong = Color(0xFFEF4444);

bool _isTopicMode(String mode) =>
    const {'gospels', 'prophets', 'parables'}.contains(mode);

String _formatTime(int seconds) {
  final safe = seconds.clamp(0, 86400);
  return '${(safe ~/ 60).toString().padLeft(2, '0')}:${(safe % 60).toString().padLeft(2, '0')}';
}

class FaithQuizApp extends StatelessWidget {
  const FaithQuizApp({super.key, required this.store});

  final ProgressStore store;

  ThemeData _theme() => ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    scaffoldBackgroundColor: _slateBottom,
    colorScheme: const ColorScheme.dark(
      primary: _slateTop,
      secondary: _gold,
      tertiary: Color(0xFF60A5FA),
      surface: _slateSurface,
      error: _wrong,
      onPrimary: _slateTextPrimary,
      onSecondary: _slateButtonText,
      onSurface: _slateTextPrimary,
    ),
    textTheme: ThemeData.dark().textTheme.apply(
      bodyColor: _slateTextPrimary,
      displayColor: _slateTextPrimary,
    ),
  );

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: store,
    builder: (context, _) => MaterialApp(
      title: 'Faith Quiz',
      debugShowCheckedModeBanner: false,
      theme: _theme(),
      darkTheme: _theme(),
      themeMode: ThemeMode.dark,
      builder: (context, child) {
        final media = MediaQuery.of(context);
        return MediaQuery(
          data: media.copyWith(
            textScaler: TextScaler.linear(store.textScale == 'large' ? 1.2 : 1),
          ),
          child: child ?? const SizedBox.shrink(),
        );
      },
      home: SplashScreen(store: store),
    ),
  );
}

class DivineBackground extends StatefulWidget {
  const DivineBackground({
    super.key,
    required this.child,
    this.reduceMotion = false,
  });

  final Widget child;
  final bool reduceMotion;

  @override
  State<DivineBackground> createState() => _DivineBackgroundState();
}

class _DivineBackgroundState extends State<DivineBackground>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 90),
    );
    if (!widget.reduceMotion) _controller.repeat();
  }

  @override
  void didUpdateWidget(covariant DivineBackground oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.reduceMotion && !oldWidget.reduceMotion) {
      _controller.stop();
    } else if (!widget.reduceMotion && oldWidget.reduceMotion) {
      _controller.repeat();
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    animation: _controller,
    builder: (context, _) => CustomPaint(
      painter: _DivineBackgroundPainter(
        phase: widget.reduceMotion ? 0 : _controller.value,
        reduceMotion: widget.reduceMotion,
      ),
      child: widget.child,
    ),
  );
}

class _DivineBackgroundPainter extends CustomPainter {
  const _DivineBackgroundPainter({
    required this.phase,
    required this.reduceMotion,
  });

  final double phase;
  final bool reduceMotion;

  static final List<_SlateParticle> _particles = List<_SlateParticle>.generate(
    24,
    (index) {
      final random = Random(101 + index);
      return _SlateParticle(
        random.nextDouble(),
        random.nextDouble(),
        random.nextDouble() * 2 + 1,
        random.nextDouble() * .3 + .05,
        random.nextDouble() * pi * 2,
      );
    },
  );

  @override
  void paint(Canvas canvas, Size size) {
    final bounds = Offset.zero & size;
    canvas.drawRect(
      bounds,
      Paint()
        ..shader = ui.Gradient.linear(
          Offset.zero,
          Offset(0, size.height),
          const [_slateTop, _slateBottom],
        ),
    );
    if (reduceMotion) return;

    final center = Offset(size.width / 2, size.height * .35);
    canvas.save();
    canvas.translate(center.dx, center.dy);
    canvas.rotate(phase * pi * 2);
    canvas.drawCircle(
      Offset.zero,
      max(size.width, size.height) * 1.4,
      Paint()
        ..shader = ui.Gradient.radial(
          Offset.zero,
          max(size.width, size.height) * 1.4,
          [const Color(0x08C5CAE9), Colors.transparent],
        ),
    );
    canvas.restore();

    for (final particle in _particles) {
      final x =
          (particle.x * size.width +
              sin(phase * pi * 2 + particle.phase) * 30) %
          size.width;
      final y =
          (particle.y * size.height - phase * size.height * particle.speed) %
          size.height;
      final alpha = (sin(phase * pi * 8 + particle.phase) + 1) / 2 * .15 + .04;
      canvas.drawCircle(
        Offset(x < 0 ? x + size.width : x, y < 0 ? y + size.height : y),
        particle.size,
        Paint()..color = _slateTextSecondary.withValues(alpha: alpha),
      );
    }
  }

  @override
  bool shouldRepaint(covariant _DivineBackgroundPainter oldDelegate) =>
      oldDelegate.phase != phase || oldDelegate.reduceMotion != reduceMotion;
}

class _SlateParticle {
  const _SlateParticle(this.x, this.y, this.size, this.speed, this.phase);
  final double x;
  final double y;
  final double size;
  final double speed;
  final double phase;
}

class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key, required this.store});

  final ProgressStore store;

  @override
  Widget build(BuildContext context) => Scaffold(
    body: DivineBackground(
      reduceMotion: store.reduceMotion,
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 40),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                'FAITH QUIZ',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.displaySmall?.copyWith(
                  fontWeight: FontWeight.w900,
                  letterSpacing: 2,
                ),
              ),
              const SizedBox(height: 10),
              Text(
                'Test your Bible knowledge',
                textAlign: TextAlign.center,
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(color: _slateTextSecondary),
              ),
              const SizedBox(height: 48),
              SlatePillButton(
                label: 'GET STARTED',
                onPressed: () => Navigator.of(context).pushReplacement(
                  MaterialPageRoute(
                    builder: (_) => MainMenuScreen(store: store),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    ),
  );
}

class MainMenuScreen extends StatelessWidget {
  const MainMenuScreen({super.key, required this.store});

  final ProgressStore store;

  void _open(BuildContext context, Widget screen) =>
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => screen));

  @override
  Widget build(BuildContext context) => Scaffold(
    body: DivineBackground(
      reduceMotion: store.reduceMotion,
      child: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(24, 38, 24, 36),
          children: [
            Text(
              'FAITH QUIZ',
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                fontWeight: FontWeight.w900,
                letterSpacing: 2,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              'Test your Bible knowledge',
              textAlign: TextAlign.center,
              style: Theme.of(
                context,
              ).textTheme.titleMedium?.copyWith(color: _slateTextSecondary),
            ),
            const SizedBox(height: 24),
            SlateProgressDashboard(store: store),
            const SizedBox(height: 28),
            SlateMenuCard(
              title: 'Daily Challenge',
              subtitle: 'One new question every day',
              icon: Icons.calendar_month,
              onTap: () => _open(context, DailyChallengeScreen(store: store)),
            ),
            SlateMenuCard(
              title: 'Cloud Challenge',
              subtitle: 'Verified global prophet questions',
              icon: Icons.public_outlined,
              onTap: () => _open(context, CloudChallengeScreen(store: store)),
            ),
            SlateMenuCard(
              title: 'The Covenant Journey',
              subtitle: 'Your Biblical Adventure Map',
              icon: Icons.map_outlined,
              onTap: () => _open(context, JourneyScreen(store: store)),
            ),
            SlateMenuCard(
              title: 'Adaptive Levels',
              subtitle: '30 Progressive Quiz Levels',
              icon: Icons.format_list_bulleted,
              onTap: () => _open(context, LevelsScreen(store: store)),
            ),
            SlateMenuCard(
              title: 'Review Wisdom',
              subtitle: 'Study Past & Missed Questions',
              icon: Icons.bookmarks_outlined,
              onTap: () => _open(context, ReviewScreen(store: store)),
            ),
            SlateMenuCard(
              title: 'Topic Scrolls',
              subtitle: 'Specific Books & Bible Themes',
              icon: Icons.category_outlined,
              onTap: () => _open(context, TopicPacksScreen(store: store)),
            ),
            SlateMenuCard(
              title: 'Leaderboard',
              subtitle: 'See Top Scores & Achievements',
              icon: Icons.leaderboard_outlined,
              onTap: () => _open(context, LeaderboardScreen(store: store)),
            ),
            SlateMenuCard(
              title: 'Group Challenges',
              subtitle: 'Create or join a private faith group',
              icon: Icons.groups_outlined,
              onTap: () => _open(context, GroupsScreen(store: store)),
            ),
            SlateMenuCard(
              title: 'Settings',
              subtitle: 'Theme & Accessibility',
              icon: Icons.settings_outlined,
              onTap: () => _open(context, SettingsScreen(store: store)),
            ),
          ],
        ),
      ),
    ),
  );
}

class SlateProgressDashboard extends StatelessWidget {
  const SlateProgressDashboard({super.key, required this.store});

  final ProgressStore store;

  @override
  Widget build(BuildContext context) => SlateCard(
    padding: const EdgeInsets.all(18),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Icon(Icons.star, color: _gold, size: 20),
            const SizedBox(width: 8),
            Text(
              'Your Progress',
              style: Theme.of(
                context,
              ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
            ),
          ],
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: SlateStat(
                value: '${store.highScore}',
                label: 'High Score',
                icon: Icons.emoji_events,
              ),
            ),
            Expanded(
              child: SlateStat(
                value: '${store.lastCompletedLevel}',
                label: 'Level',
                icon: Icons.flag,
              ),
            ),
            Expanded(
              child: SlateStat(
                value: '${store.totalAnswered}',
                label: 'Questions',
                icon: Icons.help_outline,
              ),
            ),
            Expanded(
              child: SlateStat(
                value: '${store.devotionStreak}',
                label: 'Streak',
                icon: Icons.local_fire_department,
              ),
            ),
          ],
        ),
      ],
    ),
  );
}

class SlateStat extends StatelessWidget {
  const SlateStat({
    super.key,
    required this.value,
    required this.label,
    required this.icon,
  });
  final String value;
  final String label;
  final IconData icon;

  @override
  Widget build(BuildContext context) => Column(
    children: [
      Container(
        width: 36,
        height: 36,
        decoration: const BoxDecoration(
          shape: BoxShape.circle,
          color: _slateSurfaceVariant,
        ),
        child: Icon(icon, size: 18, color: _gold),
      ),
      const SizedBox(height: 6),
      Text(
        value,
        style: Theme.of(
          context,
        ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
      ),
      Text(
        label,
        style: Theme.of(
          context,
        ).textTheme.labelSmall?.copyWith(color: _slateTextMuted),
      ),
    ],
  );
}

class SlateMenuCard extends StatelessWidget {
  const SlateMenuCard({
    super.key,
    required this.title,
    required this.subtitle,
    required this.icon,
    this.onTap,
  });
  final String title;
  final String subtitle;
  final IconData icon;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 14),
    child: Material(
      color: _slateCardLight,
      borderRadius: BorderRadius.circular(24),
      child: InkWell(
        borderRadius: BorderRadius.circular(24),
        onTap: onTap,
        child: SizedBox(
          height: 76,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 18),
            child: Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: _slateTop.withValues(alpha: .12),
                  ),
                  child: Icon(icon, color: _slateButtonText),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: const TextStyle(
                          color: _slateButtonText,
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        subtitle,
                        style: TextStyle(
                          color: _slateButtonText.withValues(alpha: .7),
                          fontSize: 13,
                        ),
                      ),
                    ],
                  ),
                ),
                Icon(
                  Icons.chevron_right,
                  color: _slateButtonText.withValues(alpha: .5),
                ),
              ],
            ),
          ),
        ),
      ),
    ),
  );
}

class SlateCard extends StatelessWidget {
  const SlateCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(18),
    this.color = _slateSurface,
  });
  final Widget child;
  final EdgeInsetsGeometry padding;
  final Color color;

  @override
  Widget build(BuildContext context) => Container(
    padding: padding,
    decoration: BoxDecoration(
      color: color,
      borderRadius: BorderRadius.circular(20),
      border: Border.all(color: Colors.white.withValues(alpha: .13)),
    ),
    child: child,
  );
}

class SlatePillButton extends StatelessWidget {
  const SlatePillButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.inverse = true,
    this.icon,
  });
  final String label;
  final VoidCallback? onPressed;
  final bool inverse;
  final IconData? icon;

  @override
  Widget build(BuildContext context) => SizedBox(
    height: 56,
    width: double.infinity,
    child: FilledButton.icon(
      onPressed: onPressed,
      icon: icon == null ? const SizedBox.shrink() : Icon(icon),
      label: Text(
        label,
        style: const TextStyle(fontWeight: FontWeight.bold, letterSpacing: .2),
      ),
      style: FilledButton.styleFrom(
        backgroundColor: inverse ? _slateCardLight : _slateSurfaceVariant,
        foregroundColor: inverse ? _slateButtonText : _slateTextPrimary,
        disabledBackgroundColor: _slateSurface.withValues(alpha: .55),
        disabledForegroundColor: _slateTextMuted,
        shape: const StadiumBorder(),
      ),
    ),
  );
}

class SlatePageHeader extends StatelessWidget {
  const SlatePageHeader({super.key, required this.title, this.trailing});
  final String title;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) => Row(
    children: [
      IconButton(
        onPressed: () => Navigator.of(context).pop(),
        icon: const Icon(Icons.arrow_back),
        color: _slateTextPrimary,
      ),
      Expanded(
        child: Text(
          title,
          textAlign: TextAlign.center,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: Theme.of(context).textTheme.titleLarge?.copyWith(
            fontWeight: FontWeight.w800,
            letterSpacing: .6,
          ),
        ),
      ),
      SizedBox(width: 48, child: Center(child: trailing)),
    ],
  );
}

class LevelsScreen extends StatelessWidget {
  const LevelsScreen({super.key, required this.store});
  final ProgressStore store;

  @override
  Widget build(BuildContext context) => Scaffold(
    body: DivineBackground(
      reduceMotion: store.reduceMotion,
      child: SafeArea(
        child: Column(
          children: [
            const SizedBox(height: 16),
            const SlatePageHeader(title: 'CHOOSE LEVEL'),
            const SizedBox(height: 24),
            Expanded(
              child: GridView.builder(
                padding: const EdgeInsets.fromLTRB(20, 0, 20, 28),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 3,
                  crossAxisSpacing: 14,
                  mainAxisSpacing: 14,
                  childAspectRatio: 1,
                ),
                itemCount: 30,
                itemBuilder: (context, index) {
                  final level = index + 1;
                  final unlocked = level <= store.highestUnlocked;
                  return Material(
                    color: unlocked ? _slateCardLight : _slateSurfaceVariant,
                    borderRadius: BorderRadius.circular(20),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(20),
                      onTap: unlocked
                          ? () => Navigator.of(context).push(
                              MaterialPageRoute(
                                builder: (_) =>
                                    QuizScreen(store: store, level: level),
                              ),
                            )
                          : null,
                      child: Center(
                        child: unlocked
                            ? Text(
                                '$level',
                                style: const TextStyle(
                                  color: _slateButtonText,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 24,
                                ),
                              )
                            : const Icon(
                                Icons.lock,
                                color: _slateTextMuted,
                                size: 26,
                              ),
                      ),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

class JourneyScreen extends StatelessWidget {
  const JourneyScreen({super.key, required this.store});
  final ProgressStore store;

  @override
  Widget build(BuildContext context) => Scaffold(
    body: DivineBackground(
      reduceMotion: store.reduceMotion,
      child: SafeArea(
        child: Column(
          children: [
            const SizedBox(height: 8),
            const SlatePageHeader(title: 'THE COVENANT JOURNEY'),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 6, 16, 12),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(20),
                child: Image.asset(
                  'assets/images/journey_map_header.jpg',
                  height: 130,
                  width: double.infinity,
                  fit: BoxFit.cover,
                ),
              ),
            ),
            Expanded(
              child: ListView.builder(
                padding: const EdgeInsets.only(bottom: 32),
                itemCount: journeyNodes.length,
                itemBuilder: (context, index) {
                  final node = journeyNodes[index];
                  return JourneyNodeTile(
                    node: node,
                    highestUnlocked: store.highestUnlocked,
                    reduceMotion: store.reduceMotion,
                    onTap: () => Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => QuizScreen(
                          store: store,
                          level: node.level,
                          mode: 'journey',
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

class JourneyNodeTile extends StatelessWidget {
  const JourneyNodeTile({
    super.key,
    required this.node,
    required this.highestUnlocked,
    required this.reduceMotion,
    required this.onTap,
  });
  final JourneyNode node;
  final int highestUnlocked;
  final bool reduceMotion;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final unlocked = node.level <= highestUnlocked;
    final current = node.level == highestUnlocked;
    final completed = node.level < highestUnlocked;
    final alignment = switch (node.level % 4) {
      1 => Alignment.center,
      2 => const Alignment(.55, 0),
      3 => Alignment.center,
      _ => const Alignment(-.55, 0),
    };
    return SizedBox(
      height: 136,
      child: CustomPaint(
        painter: _JourneyPathPainter(node.level % 4),
        child: Align(
          alignment: alignment,
          child: InkWell(
            borderRadius: BorderRadius.circular(48),
            onTap: unlocked ? onTap : null,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AnimatedScale(
                  scale: current && !reduceMotion ? 1.06 : 1,
                  duration: const Duration(milliseconds: 700),
                  child: Container(
                    width: current ? 76 : 68,
                    height: current ? 76 : 68,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: unlocked
                          ? const RadialGradient(colors: [_gold, _goldDark])
                          : const RadialGradient(
                              colors: [_slateSurfaceVariant, _slateBottom],
                            ),
                      border: Border.all(
                        color: unlocked
                            ? _slateCardLight
                            : Colors.white.withValues(alpha: .13),
                        width: 3,
                      ),
                    ),
                    child: Center(
                      child: completed
                          ? const Icon(
                              Icons.star,
                              color: _slateButtonText,
                              size: 30,
                            )
                          : !unlocked
                          ? const Icon(
                              Icons.lock,
                              color: _slateTextMuted,
                              size: 28,
                            )
                          : Text(
                              '${node.level}',
                              style: const TextStyle(
                                color: _slateButtonText,
                                fontSize: 24,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                    ),
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  node.title,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: unlocked ? _gold : _slateTextMuted,
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                  ),
                ),
                if (unlocked)
                  Text(
                    node.description,
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      color: _slateTextSecondary,
                      fontSize: 12,
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _JourneyPathPainter extends CustomPainter {
  const _JourneyPathPainter(this.pattern);
  final int pattern;

  @override
  void paint(Canvas canvas, Size size) {
    final center = size.width / 2;
    final node = switch (pattern) {
      2 => center + 50,
      0 => center - 50,
      _ => center,
    };
    final paint = Paint()
      ..color = _gold.withValues(alpha: .25)
      ..strokeWidth = 2;
    void dashed(Offset from, Offset to) {
      for (double t = 0; t < 1; t += .12) {
        final end = min(t + .06, 1.0);
        canvas.drawLine(
          Offset.lerp(from, to, t)!,
          Offset.lerp(from, to, end)!,
          paint,
        );
      }
    }

    dashed(Offset(center, 0), Offset(node, size.height / 2));
    dashed(Offset(node, size.height / 2), Offset(center, size.height));
  }

  @override
  bool shouldRepaint(covariant _JourneyPathPainter oldDelegate) =>
      oldDelegate.pattern != pattern;
}

class DailyChallengeScreen extends StatelessWidget {
  const DailyChallengeScreen({super.key, required this.store});
  final ProgressStore store;

  QuizQuestion _questionForToday() {
    final questions = QuestionBank.levels.values
        .expand((items) => items)
        .where((question) => question.isValid)
        .toList();
    final today =
        DateTime.now().toUtc().millisecondsSinceEpoch ~/
        Duration.millisecondsPerDay;
    return questions[today % questions.length];
  }

  @override
  Widget build(BuildContext context) {
    if (!store.isDailyChallengeCompletedToday) {
      return QuizScreen(
        store: store,
        level: 0,
        mode: 'daily',
        questions: [_questionForToday()],
      );
    }
    return Scaffold(
      body: DivineBackground(
        reduceMotion: store.reduceMotion,
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(22),
            child: Column(
              children: [
                const SizedBox(height: 8),
                const SlatePageHeader(title: 'DAILY CHALLENGE'),
                const Spacer(),
                SlateCard(
                  child: Column(
                    children: [
                      const Icon(Icons.calendar_month, color: _gold, size: 48),
                      const SizedBox(height: 16),
                      Text(
                        "Today's challenge is complete",
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 8),
                      const Text(
                        'Come back tomorrow for a new question. Your devotion streak has been updated.',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: _slateTextSecondary),
                      ),
                      const SizedBox(height: 24),
                      SlatePillButton(
                        label: 'BACK TO MENU',
                        onPressed: () => Navigator.of(context).pop(),
                      ),
                    ],
                  ),
                ),
                const Spacer(),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// A cloud-only daily question. The app deliberately receives no answer key:
/// the server grades the selected index and writes the leaderboard result.
class CloudChallengeScreen extends StatefulWidget {
  const CloudChallengeScreen({super.key, required this.store, this.service});

  final ProgressStore store;
  final CloudChallengeGateway? service;

  @override
  State<CloudChallengeScreen> createState() => _CloudChallengeScreenState();
}

class _CloudChallengeScreenState extends State<CloudChallengeScreen> {
  late final CloudChallengeGateway _service;
  final Stopwatch _stopwatch = Stopwatch();
  CloudChallenge? _challenge;
  String? _error;
  int _selected = -1;
  bool _loading = true;
  bool _submitting = false;
  bool _feedback = false;
  bool _wasCorrect = false;
  String? _challengeId;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? CloudChallengeService();
    unawaited(_load());
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
      _challenge = null;
    });
    try {
      _challenge = await _service.loadToday();
      if (_challenge != null) {
        _stopwatch
          ..reset()
          ..start();
      }
    } catch (_) {
      _error =
          'The cloud challenge could not be loaded. Check your connection and try again.';
    }
    if (mounted) setState(() => _loading = false);
  }

  Future<void> _submit() async {
    final challenge = _challenge;
    if (challenge == null || _selected < 0 || _submitting || _feedback) return;
    setState(() => _submitting = true);
    try {
      final result = await _service.submit(
        challenge: challenge,
        answerIndex: _selected,
        elapsedSeconds: _stopwatch.elapsed.inSeconds,
        displayName: widget.store.leaderboardName,
      );
      _stopwatch.stop();
      if (!mounted) return;
      setState(() {
        _wasCorrect = result.correct;
        _challengeId = result.challengeId;
        _feedback = true;
      });
      unawaited(
        result.correct ? AnswerFeedback.correct() : AnswerFeedback.incorrect(),
      );
    } catch (_) {
      if (mounted) {
        setState(() {
          _error =
              'Your answer was not verified, so no score was recorded. Please try again.';
        });
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  void dispose() {
    _stopwatch.stop();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    body: DivineBackground(
      reduceMotion: widget.store.reduceMotion,
      child: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(22, 16, 22, 40),
          children: [
            const SlatePageHeader(title: 'CLOUD CHALLENGE'),
            const SizedBox(height: 8),
            const Text(
              '500 source-verified questions from Old Testament prophets and New Testament prophetic witnesses.',
              textAlign: TextAlign.center,
              style: TextStyle(color: _slateTextSecondary, height: 1.35),
            ),
            const SizedBox(height: 24),
            if (_loading)
              const Padding(
                padding: EdgeInsets.all(42),
                child: Center(child: CircularProgressIndicator(color: _gold)),
              )
            else if (_error != null)
              _CloudMessageCard(
                icon: Icons.cloud_off_outlined,
                title: 'Unable to verify online',
                message: _error!,
                actionLabel: 'TRY AGAIN',
                onPressed: _load,
              )
            else if (_challenge == null)
              _CloudMessageCard(
                icon: Icons.hourglass_top_outlined,
                title: 'Cloud challenges are not live yet',
                message:
                    'The verified catalogue is being prepared. Your offline quiz packs remain fully available.',
                actionLabel: 'BACK TO MENU',
                onPressed: () => Navigator.of(context).pop(),
              )
            else
              _challengeBody(context, _challenge!),
          ],
        ),
      ),
    ),
  );

  Widget _challengeBody(
    BuildContext context,
    CloudChallenge challenge,
  ) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      SlateCard(
        child: Column(
          children: [
            Text(
              challenge.testament.toUpperCase(),
              style: const TextStyle(
                color: _gold,
                fontWeight: FontWeight.bold,
                letterSpacing: 1.2,
                fontSize: 12,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              challenge.propheticFocus,
              textAlign: TextAlign.center,
              style: const TextStyle(color: _slateTextSecondary),
            ),
            const SizedBox(height: 20),
            Text(
              challenge.question,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.bold,
                height: 1.25,
              ),
            ),
          ],
        ),
      ),
      const SizedBox(height: 18),
      for (var index = 0; index < challenge.options.length; index++)
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: SlateAnswerTile(
            letter: String.fromCharCode(65 + index),
            text: challenge.options[index],
            selected: _selected == index,
            // Only the selected response is coloured after a server verdict;
            // the unselected answer key is never sent to the client.
            correct: _feedback && _selected == index && _wasCorrect,
            feedback: _feedback && _selected == index,
            onTap: () {
              if (!_feedback && !_submitting) setState(() => _selected = index);
            },
          ),
        ),
      if (_feedback) ...[
        const SizedBox(height: 6),
        SlateCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                _wasCorrect ? 'VERIFIED CORRECT' : 'ANSWER RECORDED',
                style: TextStyle(
                  color: _wasCorrect ? _correct : _gold,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 1,
                ),
              ),
              const SizedBox(height: 8),
              Text(challenge.explanation, style: const TextStyle(height: 1.35)),
              const SizedBox(height: 8),
              Text(
                'SCRIPTURE: ${challenge.scriptureReference}',
                style: const TextStyle(
                  color: _slateTextSecondary,
                  fontWeight: FontWeight.bold,
                  fontSize: 12,
                ),
              ),
            ],
          ),
        ),
      ],
      const SizedBox(height: 14),
      SlatePillButton(
        label: _feedback
            ? 'VIEW LEADERBOARD'
            : _submitting
            ? 'VERIFYING…'
            : 'LOCK IN ANSWER',
        inverse: _feedback,
        onPressed: _feedback
            ? () => Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => LeaderboardScreen(
                    store: widget.store,
                    challengeId: _challengeId,
                  ),
                ),
              )
            : _selected >= 0 && !_submitting
            ? _submit
            : null,
      ),
    ],
  );
}

class _CloudMessageCard extends StatelessWidget {
  const _CloudMessageCard({
    required this.icon,
    required this.title,
    required this.message,
    required this.actionLabel,
    required this.onPressed,
  });

  final IconData icon;
  final String title;
  final String message;
  final String actionLabel;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) => SlateCard(
    child: Column(
      children: [
        Icon(icon, color: _gold, size: 42),
        const SizedBox(height: 14),
        Text(
          title,
          textAlign: TextAlign.center,
          style: Theme.of(
            context,
          ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        Text(
          message,
          textAlign: TextAlign.center,
          style: const TextStyle(color: _slateTextSecondary, height: 1.35),
        ),
        const SizedBox(height: 22),
        SlatePillButton(label: actionLabel, onPressed: onPressed),
      ],
    ),
  );
}

class QuizScreen extends StatefulWidget {
  const QuizScreen({
    super.key,
    required this.store,
    required this.level,
    this.mode = 'classic',
    this.questions,
  });

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
  late int _remainingSeconds;
  late int _lives;
  int _index = 0;
  int _selected = -1;
  int _score = 0;
  int _answered = 0;
  int _questionSeconds = 0;
  int _elapsedSeconds = 0;
  bool _showAnswerFeedback = false;
  bool _finished = false;
  bool _restored = false;
  final List<String> _missed = <String>[];
  Timer? _timer;

  QuizSession? get _savedSession {
    final value = widget.store.session;
    return value != null &&
            value.level == widget.level &&
            value.mode == widget.mode
        ? value
        : null;
  }

  TopicQuizSession? get _savedTopicSession {
    final value = widget.store.topicSession;
    return _isTopicMode(widget.mode) &&
            value != null &&
            value.topic == widget.mode
        ? value
        : null;
  }

  @override
  void initState() {
    super.initState();
    final session = _savedSession;
    final topicSession = _savedTopicSession;
    _restored = session != null || topicSession != null;
    _seed =
        session?.seed ??
        topicSession?.seed ??
        DateTime.now().microsecondsSinceEpoch;
    final source = (widget.questions ?? QuestionBank.forLevel(widget.level))
        .where((question) => question.isValid)
        .toList();
    _questions = _prepareQuestions(
      source.isEmpty ? _fallbackQuestions : source,
      _seed,
    );
    final restoredIndex = session?.index ?? topicSession?.index;
    if (restoredIndex != null) {
      _index = restoredIndex.clamp(0, _questions.length - 1);
      _score = (session?.score ?? topicSession?.score ?? 0).clamp(
        0,
        _questions.length,
      );
      _answered = topicSession?.answered.clamp(0, _questions.length) ?? _index;
      _elapsedSeconds = session?.elapsedSeconds.clamp(0, 86400) ?? 0;
    }
    _remainingSeconds =
        session?.remainingSeconds ?? (widget.mode == 'speed' ? 60 : 0);
    _lives = session?.lives ?? (widget.mode == 'survival' ? 3 : 0);
    _timer = Timer.periodic(const Duration(seconds: 1), _tick);
  }

  List<QuizQuestion> get _fallbackQuestions => const [
    QuizQuestion(
      question: 'What is the first book of the Bible?',
      options: ['Genesis', 'Exodus', 'Psalms', 'Matthew'],
      correctAnswer: 0,
      explanation: 'Genesis begins the biblical story.',
    ),
  ];

  List<QuizQuestion> _prepareQuestions(List<QuizQuestion> source, int seed) {
    final prepared = <QuizQuestion>[];
    for (var index = 0; index < source.length; index++) {
      final question = source[index];
      final choices = question.options.asMap().entries.toList()
        ..shuffle(Random(seed + index));
      prepared.add(
        QuizQuestion(
          question: question.question,
          options: choices.map((entry) => entry.value).toList(),
          correctAnswer: choices.indexWhere(
            (entry) => entry.key == question.correctAnswer,
          ),
          explanation: question.explanation,
          verseReference: question.verseReference,
          verseText: question.verseText,
          translation: question.translation,
          commentary: question.commentary,
          crossRefs: question.crossRefs,
          learnMoreUrl: question.learnMoreUrl,
        ),
      );
    }
    if (widget.mode == 'daily' && widget.questions == null) {
      final day =
          DateTime.now().toUtc().millisecondsSinceEpoch ~/
          Duration.millisecondsPerDay;
      prepared.shuffle(Random(day));
      return prepared.take(1).toList();
    }
    prepared.shuffle(Random(seed));
    return prepared;
  }

  void _tick(Timer timer) {
    if (!mounted || _finished || _showAnswerFeedback) return;
    var timedOut = false;
    setState(() {
      _questionSeconds++;
      _elapsedSeconds++;
      if (widget.mode == 'speed') {
        _remainingSeconds = (_remainingSeconds - 1).clamp(0, 3600);
        timedOut = _remainingSeconds == 0;
      }
    });
    if (_elapsedSeconds % 2 == 0) unawaited(_saveSession());
    if (timedOut) unawaited(_finish());
  }

  Future<void> _saveSession() async {
    if (_finished) return;
    if (_isTopicMode(widget.mode)) {
      await widget.store.saveTopicSession(
        TopicQuizSession(
          topic: widget.mode,
          index: _index,
          score: _score,
          answered: _answered,
          seed: _seed,
        ),
      );
    } else {
      await widget.store.saveQuizSession(
        QuizSession(
          level: widget.level,
          mode: widget.mode,
          index: _index,
          score: _score,
          elapsedSeconds: _elapsedSeconds,
          seed: _seed,
          lives: _lives,
          remainingSeconds: _remainingSeconds,
        ),
      );
    }
  }

  void _selectAnswer(int answer) {
    if (_showAnswerFeedback) return;
    setState(() => _selected = answer);
  }

  Future<void> _submitAnswer() async {
    if (_selected < 0 || _showAnswerFeedback) return;
    final question = _questions[_index];
    final correct = _selected == question.correctAnswer;
    setState(() {
      _showAnswerFeedback = true;
      _answered++;
      if (correct) {
        _score++;
      } else {
        _missed.add(question.question);
        if (widget.mode == 'survival') _lives = (_lives - 1).clamp(0, 3);
      }
    });
    unawaited(
      widget.store.recordReviewResult(
        ProgressStore.createReviewKey(widget.level, question.question),
        correct,
      ),
    );
    unawaited(correct ? AnswerFeedback.correct() : AnswerFeedback.incorrect());
    if (!correct) {
      unawaited(
        widget.store.addMistakeDetailed(
          level: widget.level,
          question: question.question,
          userAnswer: question.options[_selected],
          correctAnswer: question.options[question.correctAnswer],
          explanation: question.explanation,
        ),
      );
    }
    await _saveSession();
  }

  Future<void> _advance() async {
    if (!_showAnswerFeedback) return;
    if (_lives == 0 && widget.mode == 'survival' ||
        _index == _questions.length - 1) {
      await _finish();
      return;
    }
    setState(() {
      _index++;
      _selected = -1;
      _showAnswerFeedback = false;
      _questionSeconds = 0;
    });
    await _saveSession();
  }

  Future<void> _finish() async {
    if (_finished) return;
    _finished = true;
    _timer?.cancel();
    if (_isTopicMode(widget.mode)) {
      await widget.store.setTopicScoreIfHigher(widget.mode, _score);
      await widget.store.recordTopicCompletion(
        _answered,
        elapsedSeconds: _elapsedSeconds,
      );
      await widget.store.clearTopicSession();
    } else {
      await widget.store.completeQuiz(
        level: widget.level,
        score: _score,
        total: _questions.length,
        missedQuestions: _missed,
        mode: widget.mode,
        elapsedSeconds: _elapsedSeconds,
      );
      if (widget.mode == 'daily') await widget.store.recordDailyCompletion();
      await widget.store.clearQuizSession();
    }
    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(
        builder: (_) => ResultsScreen(
          store: widget.store,
          score: _score,
          total: _questions.length,
          level: widget.level,
          mode: widget.mode,
          timeSpentSeconds: _elapsedSeconds,
        ),
      ),
    );
  }

  String get _screenTitle {
    if (widget.mode == 'daily') return 'DAILY CHALLENGE';
    if (_isTopicMode(widget.mode)) return '${widget.mode.toUpperCase()} QUIZ';
    return 'LEVEL ${widget.level}';
  }

  @override
  void dispose() {
    if (!_finished) unawaited(_saveSession());
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final question = _questions[_index];
    final canSubmit = _selected >= 0;
    final isFinal =
        _index == _questions.length - 1 ||
        widget.mode == 'survival' && _lives == 0;
    final actionLabel = _showAnswerFeedback
        ? (isFinal ? 'FINISH QUIZ' : 'NEXT QUESTION')
        : canSubmit
        ? 'SUBMIT ANSWER'
        : 'SELECT AN ANSWER';
    return Scaffold(
      body: DivineBackground(
        reduceMotion: widget.store.reduceMotion,
        child: SafeArea(
          child: Stack(
            children: [
              SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(22, 82, 22, 36),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(
                      children: [
                        IconButton(
                          onPressed: () => Navigator.of(context).pop(),
                          icon: const Icon(Icons.arrow_back),
                          color: _slateTextPrimary,
                        ),
                        Expanded(
                          child: Text(
                            _screenTitle,
                            textAlign: TextAlign.center,
                            style: Theme.of(context).textTheme.titleMedium
                                ?.copyWith(
                                  fontWeight: FontWeight.bold,
                                  letterSpacing: 1,
                                ),
                          ),
                        ),
                        SizedBox(
                          width: 48,
                          child: Center(
                            child: widget.mode == 'survival'
                                ? Text(
                                    '♥ $_lives',
                                    style: const TextStyle(
                                      color: _gold,
                                      fontWeight: FontWeight.bold,
                                      fontSize: 17,
                                    ),
                                  )
                                : widget.mode == 'speed'
                                ? Text(
                                    '${_remainingSeconds}s',
                                    style: const TextStyle(
                                      color: _gold,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  )
                                : null,
                          ),
                        ),
                      ],
                    ),
                    if (_restored)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Text(
                          'RESUMED SESSION',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            color: _gold.withValues(alpha: .9),
                            fontSize: 11,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 1,
                          ),
                        ),
                      ),
                    const SizedBox(height: 14),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(4),
                      child: LinearProgressIndicator(
                        value: (_index + 1) / _questions.length,
                        minHeight: 6,
                        color: _gold,
                        backgroundColor: _slateSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 24),
                    if (_isTopicMode(widget.mode))
                      SlateCard(
                        padding: const EdgeInsets.all(22),
                        child: Text(
                          question.question,
                          textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.titleLarge
                              ?.copyWith(fontWeight: FontWeight.bold),
                        ),
                      )
                    else
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        child: Text(
                          question.question,
                          textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.titleLarge
                              ?.copyWith(fontWeight: FontWeight.bold),
                        ),
                      ),
                    const SizedBox(height: 12),
                    for (
                      var answer = 0;
                      answer < question.options.length;
                      answer++
                    )
                      Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: SlateAnswerTile(
                          letter: String.fromCharCode(65 + answer),
                          text: question.options[answer],
                          selected: _selected == answer,
                          correct: answer == question.correctAnswer,
                          feedback: _showAnswerFeedback,
                          onTap: () => _selectAnswer(answer),
                        ),
                      ),
                    if (_showAnswerFeedback) ...[
                      const SizedBox(height: 8),
                      SlateCard(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              _selected == question.correctAnswer
                                  ? 'CORRECT!'
                                  : 'EXPLANATION & INSIGHT',
                              style: TextStyle(
                                color: _selected == question.correctAnswer
                                    ? _correct
                                    : _gold,
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                                letterSpacing: 1,
                              ),
                            ),
                            const SizedBox(height: 7),
                            Text(
                              question.explanation,
                              style: const TextStyle(
                                color: _slateTextPrimary,
                                height: 1.35,
                              ),
                            ),
                            if (question.verseReference != null) ...[
                              const SizedBox(height: 8),
                              Text(
                                'SCRIPTURE: ${question.verseReference}',
                                style: const TextStyle(
                                  color: _slateTextSecondary,
                                  fontSize: 12,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ],
                            if (question.commentary != null) ...[
                              const SizedBox(height: 8),
                              Text(
                                question.commentary!,
                                style: const TextStyle(
                                  color: _slateTextSecondary,
                                  fontSize: 13,
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                    ],
                    const SizedBox(height: 24),
                    SlatePillButton(
                      label: actionLabel,
                      inverse: _showAnswerFeedback,
                      onPressed: _showAnswerFeedback
                          ? _advance
                          : canSubmit
                          ? _submitAnswer
                          : null,
                    ),
                  ],
                ),
              ),
              Positioned(
                top: 8,
                left: 0,
                right: 0,
                child: Center(
                  child: DivineTimerHud(
                    questionSeconds: _questionSeconds,
                    totalSeconds: _elapsedSeconds,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class SlateAnswerTile extends StatelessWidget {
  const SlateAnswerTile({
    super.key,
    required this.letter,
    required this.text,
    required this.selected,
    required this.correct,
    required this.feedback,
    required this.onTap,
  });
  final String letter;
  final String text;
  final bool selected;
  final bool correct;
  final bool feedback;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final isWrongSelection = feedback && selected && !correct;
    final isCorrect = feedback && correct;
    final background = isCorrect
        ? _correct
        : isWrongSelection
        ? _wrong
        : selected
        ? _slateTop
        : _slateCardLight;
    final border = isCorrect
        ? _correct
        : isWrongSelection
        ? _wrong
        : selected
        ? _gold
        : Colors.white.withValues(alpha: .16);
    final foreground = selected || isCorrect || isWrongSelection
        ? Colors.white
        : _slateButtonText;
    return Material(
      color: background,
      borderRadius: BorderRadius.circular(28),
      child: InkWell(
        borderRadius: BorderRadius.circular(28),
        onTap: feedback ? null : onTap,
        child: Container(
          constraints: const BoxConstraints(minHeight: 56),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(28),
            border: Border.all(
              color: border,
              width: selected || feedback ? 2 : 1,
            ),
          ),
          child: Row(
            children: [
              Container(
                width: 32,
                height: 32,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: selected || isCorrect || isWrongSelection
                      ? Colors.white.withValues(alpha: .25)
                      : _slateTop.withValues(alpha: .12),
                ),
                child: Text(
                  letter,
                  style: TextStyle(
                    color: foreground,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Text(
                  text,
                  style: TextStyle(
                    color: foreground,
                    fontWeight: FontWeight.w600,
                    fontSize: 16,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class DivineTimerHud extends StatelessWidget {
  const DivineTimerHud({
    super.key,
    required this.questionSeconds,
    required this.totalSeconds,
  });
  final int questionSeconds;
  final int totalSeconds;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
    decoration: BoxDecoration(
      color: _slateSurface,
      borderRadius: BorderRadius.circular(24),
      border: Border.all(color: Colors.white.withValues(alpha: .13)),
    ),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox(
          width: 54,
          height: 54,
          child: Stack(
            alignment: Alignment.center,
            children: [
              CircularProgressIndicator(
                value: (questionSeconds % 60) / 60,
                strokeWidth: 4,
                color: _gold,
                backgroundColor: _slateSurfaceVariant,
              ),
              Text(
                '${questionSeconds}s',
                style: const TextStyle(
                  color: _gold,
                  fontWeight: FontWeight.bold,
                  fontSize: 13,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: 16),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'TOTAL TIME',
              style: TextStyle(
                color: _slateTextSecondary,
                fontSize: 10,
                fontWeight: FontWeight.bold,
                letterSpacing: .6,
              ),
            ),
            Text(
              _formatTime(totalSeconds),
              style: const TextStyle(
                color: _slateTextPrimary,
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
      ],
    ),
  );
}

class ResultsScreen extends StatelessWidget {
  const ResultsScreen({
    super.key,
    required this.store,
    required this.score,
    required this.total,
    required this.level,
    required this.mode,
    required this.timeSpentSeconds,
  });
  final ProgressStore store;
  final int score;
  final int total;
  final int level;
  final String mode;
  final int timeSpentSeconds;

  int get percentage => total == 0 ? 0 : score * 100 ~/ total;
  String get rating => percentage == 100
      ? 'LEGENDARY'
      : percentage >= 90
      ? 'EXCELLENT'
      : percentage >= 80
      ? 'BLESSED'
      : percentage >= 60
      ? 'PASSED'
      : 'SEEKER';
  String get title => _isTopicMode(mode)
      ? '${mode.toUpperCase()} COMPLETE'
      : mode == 'daily'
      ? 'DAILY CHALLENGE COMPLETE'
      : 'LEVEL $level COMPLETE';

  void _menu(BuildContext context) => Navigator.of(context).pushAndRemoveUntil(
    MaterialPageRoute(builder: (_) => MainMenuScreen(store: store)),
    (route) => false,
  );

  void _primary(BuildContext context) {
    if (_isTopicMode(mode)) {
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => TopicPacksScreen(store: store)),
        (route) => false,
      );
    } else if (mode == 'daily') {
      _menu(context);
    } else if (percentage >= 60 && level == 30) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => GrandCompletionScreen(store: store)),
      );
    } else if (percentage >= 60 && level < 30) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => QuizScreen(store: store, level: level + 1),
        ),
      );
    } else {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => QuizScreen(store: store, level: level, mode: mode),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    body: DivineBackground(
      reduceMotion: store.reduceMotion,
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: LayoutBuilder(
            builder: (context, constraints) => SingleChildScrollView(
              child: ConstrainedBox(
                constraints: BoxConstraints(minHeight: constraints.maxHeight),
                child: IntrinsicHeight(
                  child: Column(
                    children: [
                      const SizedBox(height: 24),
                      Text(
                        title,
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.bold,
                          letterSpacing: 1.5,
                        ),
                      ),
                      const SizedBox(height: 36),
                      TweenAnimationBuilder<double>(
                        tween: Tween(
                          begin: 0,
                          end: score / total.clamp(1, 999),
                        ),
                        duration: store.reduceMotion
                            ? Duration.zero
                            : const Duration(milliseconds: 900),
                        builder: (context, value, _) => SizedBox(
                          width: 190,
                          height: 190,
                          child: CustomPaint(
                            painter: _ResultRingPainter(value),
                            child: Center(
                              child: Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Text(
                                    '$percentage%',
                                    style: const TextStyle(
                                      fontSize: 36,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  const Text(
                                    'ACCURACY',
                                    style: TextStyle(
                                      color: _slateTextSecondary,
                                      fontWeight: FontWeight.bold,
                                      fontSize: 12,
                                      letterSpacing: 1,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 40),
                      Row(
                        children: [
                          Expanded(
                            child: ResultStatCard(
                              label: 'TOTAL SCORE',
                              value: '$score/$total',
                            ),
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: ResultStatCard(
                              label: 'TIME SPENT',
                              value: _formatTime(timeSpentSeconds),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 14),
                      Row(
                        children: [
                          Expanded(
                            child: ResultStatCard(
                              label: 'AVG PACE',
                              value:
                                  '${total == 0 ? 0 : timeSpentSeconds ~/ total}s / Q',
                            ),
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: ResultStatCard(
                              label: 'RATING',
                              value: rating,
                            ),
                          ),
                        ],
                      ),
                      const Spacer(),
                      Row(
                        children: [
                          Expanded(
                            child: SlatePillButton(
                              label: 'MENU',
                              inverse: false,
                              onPressed: () => _menu(context),
                            ),
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: SlatePillButton(
                              label: _isTopicMode(mode)
                                  ? 'TOPICS'
                                  : mode == 'daily'
                                  ? 'DONE'
                                  : percentage >= 60 && level == 30
                                  ? 'CELEBRATE'
                                  : percentage >= 60
                                  ? 'NEXT'
                                  : 'TRY AGAIN',
                              onPressed: () => _primary(context),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    ),
  );
}

class _ResultRingPainter extends CustomPainter {
  const _ResultRingPainter(this.value);
  final double value;
  @override
  void paint(Canvas canvas, Size size) {
    final width = 18.0;
    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.width / 2 - width / 2;
    canvas.drawCircle(
      center,
      radius,
      Paint()
        ..color = _slateSurfaceVariant
        ..style = PaintingStyle.stroke
        ..strokeWidth = width,
    );
    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius),
      -pi / 2,
      pi * 2 * value,
      false,
      Paint()
        ..color = _gold
        ..style = PaintingStyle.stroke
        ..strokeWidth = width
        ..strokeCap = StrokeCap.round,
    );
  }

  @override
  bool shouldRepaint(covariant _ResultRingPainter oldDelegate) =>
      oldDelegate.value != value;
}

class ResultStatCard extends StatelessWidget {
  const ResultStatCard({super.key, required this.label, required this.value});
  final String label;
  final String value;
  @override
  Widget build(BuildContext context) => SlateCard(
    padding: const EdgeInsets.all(16),
    child: Column(
      children: [
        Text(
          value,
          textAlign: TextAlign.center,
          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
        ),
        const SizedBox(height: 2),
        Text(
          label,
          textAlign: TextAlign.center,
          style: const TextStyle(
            color: _slateTextSecondary,
            fontWeight: FontWeight.w600,
            fontSize: 11,
          ),
        ),
      ],
    ),
  );
}

class GrandCompletionScreen extends StatelessWidget {
  const GrandCompletionScreen({super.key, required this.store});
  final ProgressStore store;

  @override
  Widget build(BuildContext context) {
    final correct = (store.totalAnswered - store.detailedMistakes.length).clamp(
      0,
      store.totalAnswered,
    );
    final accuracy = store.totalAnswered == 0
        ? 0
        : correct * 100 ~/ store.totalAnswered;
    return Scaffold(
      body: DivineBackground(
        reduceMotion: store.reduceMotion,
        child: SafeArea(
          child: ListView(
            padding: const EdgeInsets.fromLTRB(22, 44, 22, 32),
            children: [
              const Icon(Icons.emoji_events, size: 80, color: _gold),
              const SizedBox(height: 16),
              Text(
                'THE JOURNEY COMPLETE',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.bold,
                  letterSpacing: 1,
                ),
              ),
              const SizedBox(height: 6),
              const Text(
                'Biblical Master',
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: _gold,
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 32),
              const Text(
                'Your Covenant Stats',
                style: TextStyle(
                  color: _slateTextSecondary,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: GrandStatCard(
                      icon: Icons.query_builder,
                      label: 'Time Spent',
                      value: _formatTime(store.totalTimeSpentSeconds),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: GrandStatCard(
                      icon: Icons.check_circle,
                      label: 'Questions',
                      value: '${store.totalAnswered}',
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: GrandStatCard(
                      icon: Icons.star,
                      label: 'Accuracy',
                      value: '$accuracy%',
                    ),
                  ),
                  const SizedBox(width: 12),
                  const Expanded(
                    child: GrandStatCard(
                      icon: Icons.history,
                      label: 'Levels',
                      value: '30/30',
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 32),
              SlateCard(
                child: Column(
                  children: [
                    Text(
                      '“Well done, good and faithful servant!”',
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'You have traversed the entire Covenant Journey. Your dedication to learning the Word is inspiring.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        color: _slateTextSecondary,
                        height: 1.35,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 36),
              SlatePillButton(
                label: 'RETURN TO MENU',
                icon: Icons.arrow_forward,
                onPressed: () => Navigator.of(context).pushAndRemoveUntil(
                  MaterialPageRoute(
                    builder: (_) => MainMenuScreen(store: store),
                  ),
                  (route) => false,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class GrandStatCard extends StatelessWidget {
  const GrandStatCard({
    super.key,
    required this.icon,
    required this.label,
    required this.value,
  });
  final IconData icon;
  final String label;
  final String value;
  @override
  Widget build(BuildContext context) => SlateCard(
    padding: const EdgeInsets.all(16),
    child: Column(
      children: [
        Icon(icon, color: _gold, size: 24),
        const SizedBox(height: 6),
        Text(
          value,
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        Text(
          label,
          textAlign: TextAlign.center,
          style: const TextStyle(color: _slateTextSecondary, fontSize: 12),
        ),
      ],
    ),
  );
}

class TopicPacksScreen extends StatefulWidget {
  const TopicPacksScreen({super.key, required this.store});
  final ProgressStore store;
  @override
  State<TopicPacksScreen> createState() => _TopicPacksScreenState();
}

class _TopicPacksScreenState extends State<TopicPacksScreen> {
  int _selected = 0;
  static const _topics = ['gospels', 'prophets', 'parables'];
  String get _topic => _topics[_selected];

  String _achievementTitle(String topic, int score) {
    final labels = switch (topic) {
      'gospels' => [
        'Gospel Beginner',
        'Gospel Learner',
        'Gospel Expert',
        'Supreme Gospel Scholar',
      ],
      'prophets' => [
        'Prophet Beginner',
        'Prophet Student',
        'Prophet Expert',
        'Supreme Prophet Master',
      ],
      _ => [
        'Parable Beginner',
        'Parable Student',
        'Parable Expert',
        'Supreme Parable Sage',
      ],
    };
    return score == 50
        ? labels[3]
        : score >= 45
        ? labels[2]
        : score > 0
        ? labels[1]
        : labels[0];
  }

  String _description(String topic) => switch (topic) {
    'gospels' =>
      'Test your knowledge of the four Gospels: Matthew, Mark, Luke, and John. Learn about Jesus\' life, teachings, miracles, and the foundation of Christianity.',
    'prophets' =>
      'Explore the messages of God\'s prophets throughout the Bible. Discover their warnings, promises, and insights into God\'s plan for His people.',
    _ =>
      'Master Jesus\' parables and wisdom teachings. Understand the deeper meanings behind His stories and how they apply to our lives today.',
  };

  @override
  Widget build(BuildContext context) {
    final bestScore = widget.store.topicScores[_topic] ?? 0;
    final savedSession = widget.store.topicSession;
    final activeSession = savedSession?.topic == _topic ? savedSession : null;
    final score = activeSession?.score ?? bestScore;
    final answered = activeSession?.answered ?? 0;
    const topicTotal = 50;
    return Scaffold(
      body: DivineBackground(
        reduceMotion: widget.store.reduceMotion,
        child: SafeArea(
          child: Column(
            children: [
              const SizedBox(height: 8),
              const SlatePageHeader(title: 'TOPIC PACKS'),
              const SizedBox(height: 14),
              Row(
                children: List<Widget>.generate(
                  _topics.length,
                  (index) => Expanded(
                    child: InkWell(
                      onTap: () => setState(() => _selected = index),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        child: Column(
                          children: [
                            Text(
                              _topics[index].toUpperCase(),
                              style: TextStyle(
                                color: _selected == index
                                    ? _gold
                                    : _slateTextSecondary,
                                fontWeight: FontWeight.bold,
                                fontSize: 13,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Container(
                              height: 3,
                              color: _selected == index
                                  ? _gold
                                  : Colors.transparent,
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(22, 20, 22, 32),
                  children: [
                    SlateCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            activeSession == null
                                ? 'YOUR ACHIEVEMENT'
                                : 'CURRENT SESSION',
                            style: const TextStyle(
                              color: _gold,
                              fontWeight: FontWeight.bold,
                              fontSize: 12,
                              letterSpacing: 1,
                            ),
                          ),
                          const SizedBox(height: 10),
                          Row(
                            children: [
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      activeSession == null
                                          ? _achievementTitle(_topic, bestScore)
                                          : 'Quiz in progress',
                                      style: Theme.of(context)
                                          .textTheme
                                          .titleLarge
                                          ?.copyWith(
                                            fontWeight: FontWeight.bold,
                                          ),
                                    ),
                                    Text(
                                      activeSession == null
                                          ? 'BEST SCORE: $bestScore/$topicTotal'
                                          : 'CURRENT SCORE: $score/$topicTotal',
                                      style: const TextStyle(
                                        color: _slateTextSecondary,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              Icon(
                                activeSession == null
                                    ? Icons.emoji_events
                                    : Icons.bookmark_added_outlined,
                                color: _gold,
                                size: 44,
                              ),
                            ],
                          ),
                          const SizedBox(height: 14),
                          ClipRRect(
                            borderRadius: BorderRadius.circular(3),
                            child: LinearProgressIndicator(
                              value:
                                  (activeSession == null
                                      ? bestScore
                                      : answered) /
                                  topicTotal.clamp(1, 999),
                              minHeight: 6,
                              color: _gold,
                              backgroundColor: _slateSurfaceVariant,
                            ),
                          ),
                          const SizedBox(height: 12),
                          Text(
                            activeSession == null
                                ? 'Your best score is $bestScore/$topicTotal (${topicTotal == 0 ? 0 : bestScore * 100 ~/ topicTotal}%). Keep growing in wisdom!'
                                : '$answered/$topicTotal questions answered • $score correct so far. Your session is saved automatically.',
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              color: _slateTextSecondary,
                              fontSize: 13,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),
                    SlateCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '${_topic.toUpperCase()} QUIZ',
                            style: Theme.of(context).textTheme.titleMedium
                                ?.copyWith(fontWeight: FontWeight.bold),
                          ),
                          const SizedBox(height: 10),
                          Text(
                            _description(_topic),
                            style: const TextStyle(
                              color: _slateTextSecondary,
                              height: 1.35,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 24),
                    SlatePillButton(
                      label: activeSession == null
                          ? 'START QUIZ'
                          : 'CONTINUE QUIZ',
                      icon: activeSession == null
                          ? Icons.play_arrow
                          : Icons.play_circle_outline,
                      onPressed: () async {
                        await Navigator.of(context).push(
                          MaterialPageRoute(
                            builder: (_) => TopicQuizScreen(
                              store: widget.store,
                              topic: _topic,
                            ),
                          ),
                        );
                        if (mounted) setState(() {});
                      },
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class TopicQuizScreen extends StatelessWidget {
  const TopicQuizScreen({super.key, required this.store, required this.topic});
  final ProgressStore store;
  final String topic;

  @override
  Widget build(BuildContext context) => QuizScreen(
    store: store,
    level: 0,
    mode: topic,
    questions: questionsForTopic(topic),
  );

  static List<QuizQuestion> questionsForTopic(String topic) {
    final primary = TopicQuestionBank.forTopic(topic);
    final keywords = switch (topic) {
      'gospels' => [
        'jesus',
        'gospel',
        'disciple',
        'apostle',
        'pharisee',
        'miracle',
        'kingdom',
        'galilee',
      ],
      'prophets' => [
        'prophet',
        'elijah',
        'elisha',
        'isaiah',
        'jeremiah',
        'ezekiel',
        'daniel',
        'jonah',
      ],
      _ => [
        'parable',
        'sower',
        'mustard',
        'lost',
        'samaritan',
        'talent',
        'vineyard',
        'wedding',
        'sheep',
        'coin',
      ],
    };
    final existing = primary
        .map((question) => question.question.toLowerCase())
        .toSet();
    final supplemental = QuestionBank.levels.values
        .expand((items) => items)
        .where(
          (question) =>
              question.isValid &&
              question.verseReference?.trim().isNotEmpty == true &&
              !existing.contains(question.question.toLowerCase()) &&
              keywords.any(
                (keyword) => '${question.question} ${question.explanation}'
                    .toLowerCase()
                    .contains(keyword),
              ),
        );
    return [...primary, ...supplemental].take(50).toList();
  }
}

class ReviewScreen extends StatelessWidget {
  const ReviewScreen({super.key, required this.store});
  final ProgressStore store;

  @override
  Widget build(BuildContext context) {
    final dueLevel = store.dueReviewKeys
        .map(ProgressStore.reviewLevelFromKey)
        .whereType<int>()
        .firstOrNull;
    return Scaffold(
      body: DivineBackground(
        reduceMotion: store.reduceMotion,
        child: SafeArea(
          child: Column(
            children: [
              const SizedBox(height: 8),
              const SlatePageHeader(title: 'WISDOM REVIEW'),
              Padding(
                padding: const EdgeInsets.fromLTRB(34, 6, 22, 16),
                child: Text(
                  'Reflect on your journey and strengthen your Bible knowledge.',
                  style: Theme.of(
                    context,
                  ).textTheme.bodyMedium?.copyWith(color: _slateTextSecondary),
                ),
              ),
              if (store.dueReviewCount > 0)
                Padding(
                  padding: const EdgeInsets.fromLTRB(22, 0, 22, 16),
                  child: SlateCard(
                    child: Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                '${store.dueReviewCount} ${store.dueReviewCount == 1 ? 'item' : 'items'} due for review',
                                style: const TextStyle(
                                  color: _gold,
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 2),
                              const Text(
                                'Spaced repetition reinforces past answers.',
                                style: TextStyle(
                                  color: _slateTextSecondary,
                                  fontSize: 12,
                                ),
                              ),
                            ],
                          ),
                        ),
                        SizedBox(
                          width: 100,
                          child: SlatePillButton(
                            label: 'PRACTICE',
                            onPressed: () => Navigator.of(context).push(
                              MaterialPageRoute(
                                builder: (_) => QuizScreen(
                                  store: store,
                                  level: dueLevel == null || dueLevel == 0
                                      ? 1
                                      : dueLevel,
                                  mode: 'practice',
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              Expanded(
                child: store.detailedMistakes.isEmpty
                    ? const Center(
                        child: Text(
                          'No reviews yet. Keep playing to learn!',
                          style: TextStyle(color: _slateTextMuted),
                        ),
                      )
                    : ListView.separated(
                        padding: const EdgeInsets.fromLTRB(22, 0, 22, 32),
                        itemCount: store.detailedMistakes.length,
                        separatorBuilder: (_, _) => const SizedBox(height: 14),
                        itemBuilder: (context, index) => SlateReviewCard(
                          entry: store.detailedMistakes[index],
                        ),
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class SlateReviewCard extends StatelessWidget {
  const SlateReviewCard({super.key, required this.entry});
  final MistakeEntry entry;
  @override
  Widget build(BuildContext context) => SlateCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text(
              'LEVEL ${entry.level}',
              style: const TextStyle(
                color: _gold,
                fontWeight: FontWeight.bold,
                fontSize: 13,
              ),
            ),
            const Spacer(),
            Text(
              '${entry.date.month}/${entry.date.day}/${entry.date.year}',
              style: const TextStyle(color: _slateTextMuted, fontSize: 12),
            ),
          ],
        ),
        const SizedBox(height: 10),
        Text(
          entry.question,
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 14),
        Row(
          children: [
            Expanded(
              child: _ReviewAnswerBox(
                label: 'YOU ANSWERED',
                text: entry.userAnswer,
                color: _wrong,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: _ReviewAnswerBox(
                label: 'CORRECT ANSWER',
                text: entry.correctAnswer,
                color: _correct,
              ),
            ),
          ],
        ),
        if (entry.explanation.isNotEmpty) ...[
          const SizedBox(height: 12),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(Icons.info, size: 16, color: _gold),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  entry.explanation,
                  style: const TextStyle(
                    color: _slateTextSecondary,
                    fontSize: 13,
                  ),
                ),
              ),
            ],
          ),
        ],
      ],
    ),
  );
}

class _ReviewAnswerBox extends StatelessWidget {
  const _ReviewAnswerBox({
    required this.label,
    required this.text,
    required this.color,
  });
  final String label;
  final String text;
  final Color color;
  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.all(12),
    decoration: BoxDecoration(
      color: color.withValues(alpha: .15),
      borderRadius: BorderRadius.circular(12),
      border: Border.all(color: color.withValues(alpha: .35)),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: TextStyle(
            color: color,
            fontWeight: FontWeight.bold,
            fontSize: 10,
          ),
        ),
        const SizedBox(height: 4),
        Text(text, style: const TextStyle(fontSize: 13)),
      ],
    ),
  );
}

class GroupsScreen extends StatefulWidget {
  const GroupsScreen({super.key, required this.store});
  final ProgressStore store;

  @override
  State<GroupsScreen> createState() => _GroupsScreenState();
}

class _GroupsScreenState extends State<GroupsScreen> {
  final CloudChallengeService _service = CloudChallengeService();
  late Future<bool> _available;

  @override
  void initState() {
    super.initState();
    _available = _service.isAvailable;
  }

  Future<void> _showNameDialog({required bool join}) async {
    final controller = TextEditingController();
    final value = await showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        backgroundColor: _slateSurface,
        title: Text(join ? 'Join group' : 'Create group'),
        content: TextField(
          controller: controller,
          maxLength: join ? 80 : 40,
          decoration: InputDecoration(
            hintText: join ? 'Group code' : 'e.g. Grace Fellowship',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('CANCEL'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, controller.text),
            child: Text(join ? 'JOIN' : 'CREATE'),
          ),
        ],
      ),
    );
    controller.dispose();
    if (value == null || value.trim().isEmpty || !mounted) return;
    try {
      if (join) {
        await _service.joinGroup(value);
        if (mounted) _notice('You joined the group.');
      } else {
        final code = await _service.createGroup(value);
        if (mounted) _notice('Group created. Share code: $code');
      }
    } catch (_) {
      if (mounted) {
        _notice(
          'That group action could not be verified. Try again when online.',
        );
      }
    }
  }

  void _notice(String message) => ScaffoldMessenger.of(
    context,
  ).showSnackBar(SnackBar(content: Text(message)));

  @override
  Widget build(BuildContext context) => Scaffold(
    body: DivineBackground(
      reduceMotion: widget.store.reduceMotion,
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(22, 16, 22, 40),
          child: FutureBuilder<bool>(
            future: _available,
            builder: (context, available) {
              if (available.connectionState != ConnectionState.done) {
                return const Center(
                  child: CircularProgressIndicator(color: _gold),
                );
              }
              if (available.hasError || available.data != true) {
                return ListView(
                  children: [
                    const SlatePageHeader(title: 'GROUP CHALLENGES'),
                    const SizedBox(height: 28),
                    _CloudMessageCard(
                      icon: Icons.groups_outlined,
                      title: 'Groups are not live yet',
                      message:
                          'This feature opens when verified cloud challenges are enabled. Your group information stays private to members.',
                      actionLabel: 'BACK TO MENU',
                      onPressed: () => Navigator.of(context).pop(),
                    ),
                  ],
                );
              }
              return Column(
                children: [
                  const SlatePageHeader(title: 'GROUP CHALLENGES'),
                  const SizedBox(height: 8),
                  const Text(
                    'Create a group or join with a code from its owner.',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: _slateTextSecondary),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      Expanded(
                        child: SlatePillButton(
                          label: 'CREATE',
                          onPressed: () => _showNameDialog(join: false),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: SlatePillButton(
                          label: 'JOIN',
                          inverse: true,
                          onPressed: () => _showNameDialog(join: true),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Expanded(
                    child: StreamBuilder<List<QuizGroup>>(
                      stream: _service.myGroups(),
                      builder: (context, snapshot) {
                        if (snapshot.hasError) {
                          return const Center(
                            child: Text('Your groups could not be loaded.'),
                          );
                        }
                        if (!snapshot.hasData) {
                          return const Center(
                            child: CircularProgressIndicator(color: _gold),
                          );
                        }
                        final groups = snapshot.data!;
                        if (groups.isEmpty) {
                          return const Center(
                            child: Text(
                              'No groups yet. Create one for your church, class, or friends.',
                              textAlign: TextAlign.center,
                              style: TextStyle(color: _slateTextSecondary),
                            ),
                          );
                        }
                        return ListView.separated(
                          itemCount: groups.length,
                          separatorBuilder: (_, _) =>
                              const SizedBox(height: 10),
                          itemBuilder: (context, index) {
                            final group = groups[index];
                            return SlateSettingCard(
                              title: group.name,
                              subtitle:
                                  '${group.role == 'owner' ? 'Owner' : 'Member'} • ${group.id}',
                              icon: Icons.group_outlined,
                              onTap: () => Navigator.of(context).push(
                                MaterialPageRoute(
                                  builder: (_) => GroupDetailScreen(
                                    store: widget.store,
                                    group: group,
                                  ),
                                ),
                              ),
                            );
                          },
                        );
                      },
                    ),
                  ),
                ],
              );
            },
          ),
        ),
      ),
    ),
  );
}

class GroupDetailScreen extends StatefulWidget {
  const GroupDetailScreen({
    super.key,
    required this.store,
    required this.group,
  });
  final ProgressStore store;
  final QuizGroup group;

  @override
  State<GroupDetailScreen> createState() => _GroupDetailScreenState();
}

class _GroupDetailScreenState extends State<GroupDetailScreen> {
  final CloudChallengeService _service = CloudChallengeService();
  bool _publishing = false;

  Future<void> _publishToday() async {
    if (_publishing) return;
    setState(() => _publishing = true);
    try {
      final challenge = await _service.loadToday();
      if (challenge == null) throw StateError('No live cloud question');
      final id = await _service.createGroupChallenge(
        groupId: widget.group.id,
        challenge: challenge,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Group challenge published: $id')),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('The group challenge could not be published.'),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _publishing = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    body: DivineBackground(
      reduceMotion: widget.store.reduceMotion,
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(22, 16, 22, 40),
          child: Column(
            children: [
              SlatePageHeader(title: widget.group.name.toUpperCase()),
              const SizedBox(height: 8),
              SelectableText(
                'GROUP CODE: ${widget.group.id}',
                style: const TextStyle(
                  color: _slateTextSecondary,
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                ),
              ),
              if (widget.group.role == 'owner') ...[
                const SizedBox(height: 16),
                SlatePillButton(
                  label: _publishing
                      ? 'PUBLISHING…'
                      : 'PUBLISH TODAY\'S CLOUD QUESTION',
                  onPressed: _publishing ? null : _publishToday,
                ),
              ],
              const SizedBox(height: 18),
              Expanded(
                child: StreamBuilder<List<GroupChallenge>>(
                  stream: _service.groupChallenges(widget.group.id),
                  builder: (context, snapshot) {
                    if (snapshot.hasError) {
                      return const Center(
                        child: Text('Group challenges are unavailable.'),
                      );
                    }
                    if (!snapshot.hasData) {
                      return const Center(
                        child: CircularProgressIndicator(color: _gold),
                      );
                    }
                    final challenges = snapshot.data!;
                    if (challenges.isEmpty) {
                      return const Center(
                        child: Text(
                          'The owner has not published a group challenge yet.',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: _slateTextSecondary),
                        ),
                      );
                    }
                    return ListView.separated(
                      itemCount: challenges.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 10),
                      itemBuilder: (context, index) {
                        final challenge = challenges[index];
                        return SlateSettingCard(
                          title: 'Verified group challenge',
                          subtitle: challenge.scriptureReference,
                          icon: Icons.quiz_outlined,
                          onTap: () => Navigator.of(context).push(
                            MaterialPageRoute(
                              builder: (_) => GroupQuestionScreen(
                                store: widget.store,
                                groupId: widget.group.id,
                                challenge: challenge,
                              ),
                            ),
                          ),
                        );
                      },
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    ),
  );
}

class GroupQuestionScreen extends StatefulWidget {
  const GroupQuestionScreen({
    super.key,
    required this.store,
    required this.groupId,
    required this.challenge,
  });
  final ProgressStore store;
  final String groupId;
  final GroupChallenge challenge;

  @override
  State<GroupQuestionScreen> createState() => _GroupQuestionScreenState();
}

class _GroupQuestionScreenState extends State<GroupQuestionScreen> {
  final CloudChallengeService _service = CloudChallengeService();
  final Stopwatch _stopwatch = Stopwatch()..start();
  int _selected = -1;
  bool _submitting = false;
  bool _submitted = false;
  bool _wasCorrect = false;

  Future<void> _submit() async {
    if (_selected < 0 || _submitting || _submitted) return;
    setState(() => _submitting = true);
    try {
      final result = await _service.submitGroupChallenge(
        groupId: widget.groupId,
        challenge: widget.challenge,
        answerIndex: _selected,
        elapsedSeconds: _stopwatch.elapsed.inSeconds,
        displayName: widget.store.leaderboardName,
      );
      _stopwatch.stop();
      if (!mounted) return;
      setState(() {
        _submitted = true;
        _wasCorrect = result.correct;
      });
      unawaited(
        result.correct ? AnswerFeedback.correct() : AnswerFeedback.incorrect(),
      );
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Your group score was not verified. Please try again.',
            ),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  void dispose() {
    _stopwatch.stop();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    body: DivineBackground(
      reduceMotion: widget.store.reduceMotion,
      child: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(22, 16, 22, 40),
          children: [
            const SlatePageHeader(title: 'GROUP CHALLENGE'),
            const SizedBox(height: 20),
            SlateCard(
              child: Text(
                widget.challenge.question,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.bold,
                  height: 1.25,
                ),
              ),
            ),
            const SizedBox(height: 18),
            for (
              var index = 0;
              index < widget.challenge.options.length;
              index++
            )
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: SlateAnswerTile(
                  letter: String.fromCharCode(65 + index),
                  text: widget.challenge.options[index],
                  selected: _selected == index,
                  correct: _submitted && _selected == index && _wasCorrect,
                  feedback: _submitted && _selected == index,
                  onTap: () {
                    if (!_submitted && !_submitting) {
                      setState(() => _selected = index);
                    }
                  },
                ),
              ),
            if (_submitted)
              SlateCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _wasCorrect ? 'VERIFIED CORRECT' : 'ANSWER RECORDED',
                      style: TextStyle(
                        color: _wasCorrect ? _correct : _gold,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(widget.challenge.explanation),
                    const SizedBox(height: 8),
                    Text(
                      'SCRIPTURE: ${widget.challenge.scriptureReference}',
                      style: const TextStyle(
                        color: _slateTextSecondary,
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ),
            const SizedBox(height: 16),
            SlatePillButton(
              label: _submitted
                  ? 'BACK TO GROUP'
                  : _submitting
                  ? 'VERIFYING…'
                  : 'LOCK IN ANSWER',
              inverse: _submitted,
              onPressed: _submitted
                  ? () => Navigator.of(context).pop()
                  : _selected >= 0 && !_submitting
                  ? _submit
                  : null,
            ),
          ],
        ),
      ),
    ),
  );
}

class LeaderboardScreen extends StatelessWidget {
  const LeaderboardScreen({super.key, required this.store, this.challengeId});

  final ProgressStore store;
  final String? challengeId;

  @override
  Widget build(BuildContext context) {
    final service = CloudChallengeService();
    return Scaffold(
      body: DivineBackground(
        reduceMotion: store.reduceMotion,
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 16),
            child: ListView(
              children: [
                const SlatePageHeader(title: 'LEADERBOARD'),
                const SizedBox(height: 8),
                const Text(
                  'Only server-verified cloud challenge results are shown here.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: _slateTextSecondary),
                ),
                const SizedBox(height: 24),
                if (challengeId != null)
                  _VerifiedLeaderboard(challengeId: challengeId!)
                else
                  FutureBuilder<CloudChallenge?>(
                    future: service.loadToday(),
                    builder: (context, snapshot) {
                      if (snapshot.connectionState != ConnectionState.done) {
                        return const Padding(
                          padding: EdgeInsets.all(36),
                          child: Center(
                            child: CircularProgressIndicator(color: _gold),
                          ),
                        );
                      }
                      if (snapshot.hasError || snapshot.data == null) {
                        return _CloudMessageCard(
                          icon: Icons.leaderboard_outlined,
                          title: 'No live global ranking yet',
                          message:
                              'Once the verified cloud catalogue is published, today’s rankings will appear here.',
                          actionLabel: 'BACK TO MENU',
                          onPressed: () => Navigator.of(context).pop(),
                        );
                      }
                      return _VerifiedLeaderboard(
                        challengeId: snapshot.data!.challengeId,
                      );
                    },
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _VerifiedLeaderboard extends StatelessWidget {
  const _VerifiedLeaderboard({required this.challengeId});
  final String challengeId;

  @override
  Widget build(BuildContext context) => StreamBuilder<List<LeaderboardEntry>>(
    stream: CloudChallengeService().leaderboard(challengeId),
    builder: (context, snapshot) {
      if (snapshot.hasError) {
        return _CloudMessageCard(
          icon: Icons.cloud_off_outlined,
          title: 'Rankings are unavailable',
          message: 'Check your connection and try again.',
          actionLabel: 'CLOSE',
          onPressed: () => Navigator.of(context).pop(),
        );
      }
      if (!snapshot.hasData) {
        return const Padding(
          padding: EdgeInsets.all(36),
          child: Center(child: CircularProgressIndicator(color: _gold)),
        );
      }
      final entries = snapshot.data!;
      if (entries.isEmpty) {
        return _CloudMessageCard(
          icon: Icons.emoji_events_outlined,
          title: 'Be the first verified score',
          message: 'Complete today’s Cloud Challenge to take the first place.',
          actionLabel: 'CLOSE',
          onPressed: () => Navigator.of(context).pop(),
        );
      }
      return SlateCard(
        padding: const EdgeInsets.symmetric(vertical: 10),
        child: Column(
          children: [
            for (var index = 0; index < entries.length; index++)
              _LeaderboardRow(entry: entries[index], rank: index + 1),
          ],
        ),
      );
    },
  );
}

class _LeaderboardRow extends StatelessWidget {
  const _LeaderboardRow({required this.entry, required this.rank});
  final LeaderboardEntry entry;
  final int rank;

  @override
  Widget build(BuildContext context) {
    final accent = switch (rank) {
      1 => _gold,
      2 => const Color(0xFFE2E8F0),
      3 => const Color(0xFFC98D5B),
      _ => _slateTextSecondary,
    };
    return ListTile(
      leading: CircleAvatar(
        backgroundColor: accent.withValues(alpha: .16),
        child: Text(
          '$rank',
          style: TextStyle(color: accent, fontWeight: FontWeight.bold),
        ),
      ),
      title: Text(
        entry.displayName,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: Text(
        '${_formatTime(entry.elapsedSeconds)} • verified',
        style: const TextStyle(color: _slateTextSecondary),
      ),
      trailing: Text(
        '${entry.score}/1',
        style: TextStyle(
          color: accent,
          fontSize: 17,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key, required this.store});
  final ProgressStore store;

  Future<void> _choose(
    BuildContext context,
    String title,
    List<_Choice> choices,
  ) async {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        backgroundColor: _slateSurface,
        title: Text(title),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: choices
              .map(
                (choice) => ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: Icon(
                    choice.selected
                        ? Icons.radio_button_checked
                        : Icons.radio_button_off,
                    color: choice.selected ? _gold : _slateTextMuted,
                  ),
                  title: Text(choice.label),
                  onTap: () async {
                    await choice.onSelect();
                    if (dialogContext.mounted) {
                      Navigator.of(dialogContext).pop();
                    }
                  },
                ),
              )
              .toList(),
        ),
      ),
    );
  }

  Future<void> _editLeaderboardName(BuildContext context) async {
    final controller = TextEditingController(text: store.leaderboardName);
    final value = await showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        backgroundColor: _slateSurface,
        title: const Text('Leaderboard name'),
        content: TextField(
          controller: controller,
          maxLength: 24,
          textCapitalization: TextCapitalization.words,
          decoration: const InputDecoration(
            hintText: 'Faith learner',
            helperText: 'Shown only with verified cloud scores',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text('CANCEL'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(controller.text),
            child: const Text('SAVE'),
          ),
        ],
      ),
    );
    controller.dispose();
    if (value != null) await store.setLeaderboardName(value);
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    body: DivineBackground(
      reduceMotion: store.reduceMotion,
      child: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(22, 16, 22, 40),
          children: [
            const SlatePageHeader(title: 'SETTINGS'),
            const SizedBox(height: 24),
            const SlateSectionHeader('APPEARANCE'),
            const SlateSettingCard(
              title: 'Theme',
              subtitle: 'Slate Indigo Dark — the Faith Quiz visual style',
              icon: Icons.dark_mode,
            ),
            const SizedBox(height: 20),
            const SlateSectionHeader('ACCESSIBILITY'),
            SlateSettingCard(
              title: 'Text size',
              subtitle: store.textScale == 'large' ? 'Large' : 'Standard',
              icon: Icons.text_fields,
              onTap: () => _choose(context, 'Select Text Size', [
                _Choice(
                  'Standard',
                  'normal',
                  store.textScale == 'normal',
                  () => store.setTextScale('normal'),
                ),
                _Choice(
                  'Large',
                  'large',
                  store.textScale == 'large',
                  () => store.setTextScale('large'),
                ),
              ]),
            ),
            SlateSwitchSettingCard(
              title: 'Reduce motion',
              subtitle:
                  'Stops moving background particles and quiz-result animations',
              icon: Icons.motion_photos_off,
              value: store.reduceMotion,
              onChanged: store.setReduceMotion,
            ),
            const SizedBox(height: 20),
            const SlateSectionHeader('DATA & STORAGE'),
            SlateSwitchSettingCard(
              title: 'Secure cloud backup',
              subtitle: store.cloudSyncStatus,
              icon: Icons.cloud_sync_outlined,
              value: store.cloudBackupEnabled,
              onChanged: (enabled) async {
                if (enabled) {
                  await store.enableCloudBackup();
                } else {
                  await store.disableCloudBackup();
                }
              },
            ),
            const SizedBox(height: 20),
            const SlateSectionHeader('ONLINE PLAY'),
            SlateSettingCard(
              title: 'Leaderboard name',
              subtitle: store.leaderboardName,
              icon: Icons.badge_outlined,
              onTap: () => _editLeaderboardName(context),
            ),
            SlateSwitchSettingCard(
              title: 'Daily reminder',
              subtitle: store.reminderStatus,
              icon: Icons.notifications_outlined,
              value: store.remindersEnabled,
              onChanged: store.setRemindersEnabled,
            ),
            const SizedBox(height: 12),
            SlateSettingCard(
              title: 'Reset Progress',
              subtitle: 'Clear all stats and achievements',
              icon: Icons.delete_forever,
              iconColor: _wrong,
              onTap: () async {
                final confirmed = await showDialog<bool>(
                  context: context,
                  builder: (context) => AlertDialog(
                    backgroundColor: _slateSurface,
                    title: const Text('Reset Progress?'),
                    content: const Text(
                      'Are you sure you want to reset all your progress? This action cannot be undone.',
                    ),
                    actions: [
                      TextButton(
                        onPressed: () => Navigator.pop(context, false),
                        child: const Text('CANCEL'),
                      ),
                      FilledButton(
                        onPressed: () => Navigator.pop(context, true),
                        style: FilledButton.styleFrom(backgroundColor: _wrong),
                        child: const Text('RESET'),
                      ),
                    ],
                  ),
                );
                if (confirmed == true) {
                  await store.reset();
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text(
                          'Progress, scores, streaks, and saved sessions were reset.',
                        ),
                      ),
                    );
                  }
                }
              },
            ),
            const SlateSettingCard(
              title: 'Crash reporting',
              subtitle: 'Not enabled — this app does not send crash data',
              icon: Icons.privacy_tip_outlined,
            ),
            const SizedBox(height: 20),
            const SlateSectionHeader('ABOUT'),
            SlateSettingCard(
              title: 'About Faith Quiz',
              subtitle: 'Version 1.0.0',
              icon: Icons.info,
              onTap: () => showDialog<void>(
                context: context,
                builder: (context) => AlertDialog(
                  backgroundColor: _slateSurface,
                  title: const Text('About Faith Quiz'),
                  content: const Text(
                    'Faith Quiz is designed to help you master biblical knowledge through engaging quizzes and challenges.\n\nCreated with faith and code.',
                  ),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(context),
                      child: const Text('CLOSE'),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

class _Choice {
  const _Choice(this.label, this.value, this.selected, this.onSelect);
  final String label;
  final String value;
  final bool selected;
  final Future<void> Function() onSelect;
}

class SlateSectionHeader extends StatelessWidget {
  const SlateSectionHeader(this.title, {super.key});
  final String title;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 8),
    child: Text(
      title,
      style: const TextStyle(
        color: _gold,
        fontSize: 12,
        fontWeight: FontWeight.bold,
        letterSpacing: 1.5,
      ),
    ),
  );
}

class SlateSettingCard extends StatelessWidget {
  const SlateSettingCard({
    super.key,
    required this.title,
    required this.subtitle,
    required this.icon,
    this.onTap,
    this.iconColor = _gold,
  });
  final String title;
  final String subtitle;
  final IconData icon;
  final Color iconColor;
  final VoidCallback? onTap;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Material(
      color: _slateSurface,
      borderRadius: BorderRadius.circular(20),
      child: InkWell(
        borderRadius: BorderRadius.circular(20),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Icon(icon, color: iconColor),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                      ),
                    ),
                    Text(
                      subtitle,
                      style: const TextStyle(
                        color: _slateTextSecondary,
                        fontSize: 13,
                      ),
                    ),
                  ],
                ),
              ),
              if (onTap != null)
                const Icon(Icons.chevron_right, color: _slateTextMuted),
            ],
          ),
        ),
      ),
    ),
  );
}

class SlateSwitchSettingCard extends StatelessWidget {
  const SlateSwitchSettingCard({
    super.key,
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.value,
    required this.onChanged,
  });
  final String title;
  final String subtitle;
  final IconData icon;
  final bool value;
  final ValueChanged<bool> onChanged;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Container(
      decoration: BoxDecoration(
        color: _slateSurface,
        borderRadius: BorderRadius.circular(20),
      ),
      child: SwitchListTile(
        secondary: Icon(icon, color: _gold),
        title: Text(
          title,
          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
        ),
        subtitle: Text(
          subtitle,
          style: const TextStyle(color: _slateTextSecondary, fontSize: 13),
        ),
        value: value,
        activeThumbColor: _gold,
        onChanged: onChanged,
      ),
    ),
  );
}

extension<T> on Iterable<T> {
  T? get firstOrNull => isEmpty ? null : first;
}
