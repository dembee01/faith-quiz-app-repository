import 'dart:async';
import 'dart:math';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:cloud_functions/cloud_functions.dart'
    show FirebaseFunctionsException;

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

/// Turns a failed server verification into a message a player can act on.
String cloudSubmitErrorMessage(Object error) {
  if (error is FirebaseFunctionsException) {
    switch (error.code) {
      case 'already-exists':
        return 'You already locked in this answer.';
      case 'not-found':
        return 'This challenge is no longer available.';
      case 'unauthenticated':
      case 'permission-denied':
        return 'The quiz server could not verify your sign-in. Try again shortly.';
      case 'unavailable':
      case 'deadline-exceeded':
      case 'cancelled':
        return 'The quiz server is unreachable right now. Check your connection and try again.';
    }
  }
  return 'Your answer was not verified, so no score was recorded. Please try again.';
}

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
  Widget build(BuildContext context) => MaterialApp(
    title: 'Faith Quiz',
    debugShowCheckedModeBanner: false,
    theme: _theme(),
    darkTheme: _theme(),
    themeMode: ThemeMode.dark,
    builder: (context, child) => ListenableBuilder(
      listenable: store,
      builder: (context, _) {
        final media =
            MediaQuery.maybeOf(context) ??
            MediaQueryData.fromView(View.of(context));
        return MediaQuery(
          data: media.copyWith(
            textScaler: TextScaler.linear(store.textScale == 'large' ? 1.2 : 1),
          ),
          child: child ?? const SizedBox.shrink(),
        );
      },
    ),
    home: SplashScreen(store: store),
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

  Future<void> _openCloudChallenge(BuildContext context) async {
    final service = CloudChallengeService();
    final user = service.currentUser;
    if (user == null || user.isAnonymous) {
      final signedIn = await showModalBottomSheet<bool>(
        context: context,
        backgroundColor: Colors.transparent,
        isScrollControlled: true,
        builder: (sheetContext) =>
            _GoogleSignInSheet(service: service, store: store),
      );
      if (signedIn != true) return;
    }

    final claimed = await service.getClaimedUsername();
    if (claimed == null || claimed.isEmpty) {
      if (!context.mounted) return;
      final usernameClaimed = await showDialog<bool>(
        context: context,
        barrierDismissible: false,
        builder: (dialogContext) =>
            _ClaimUsernameDialog(service: service, store: store),
      );
      if (usernameClaimed != true) return;
    }

    if (context.mounted) {
      _open(context, CloudChallengeScreen(store: store));
    }
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: store,
    builder: (context, _) => Scaffold(
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
                onTap: () => _openCloudChallenge(context),
              ),
              SlateMenuCard(
                title: 'The Covenant Journey',
                subtitle: 'Your Biblical Adventure Map',
                icon: Icons.map_outlined,
                onTap: () => _open(context, JourneyScreen(store: store)),
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
    this.loading = false,
  });
  final String label;
  final VoidCallback? onPressed;
  final bool inverse;
  final IconData? icon;
  final bool loading;

  @override
  Widget build(BuildContext context) => SizedBox(
    height: 56,
    width: double.infinity,
    child: FilledButton.icon(
      onPressed: loading ? null : onPressed,
      icon: loading
          ? SizedBox(
              width: 18,
              height: 18,
              child: CircularProgressIndicator(
                strokeWidth: 2.2,
                color: inverse ? _slateButtonText : _gold,
              ),
            )
          : (icon == null ? const SizedBox.shrink() : Icon(icon)),
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

class JourneyScreen extends StatelessWidget {
  const JourneyScreen({super.key, required this.store});
  final ProgressStore store;

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: store,
    builder: (context, _) => Scaffold(
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
  Timer? _timer;
  int _questionSeconds = 0;
  int _totalSeconds = 0;
  CloudChallenge? _challenge;
  String? _error;
  int _selected = -1;
  bool _loading = true;
  bool _submitting = false;
  bool _feedback = false;
  bool _wasCorrect = false;
  bool _alreadySubmitted = false;
  String? _challengeId;
  int? _serverCorrectAnswer;
  int _currentIndex = 0;
  int _questionNumber = 1;
  int _sessionScore = 0;
  int _sessionStreak = 0;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? CloudChallengeService();
    if (_service is CloudChallengeService) {
      unawaited(_service.warmUp());
    }
    unawaited(_load());
  }

  void _startTimer() {
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted) return;
      setState(() {
        _questionSeconds++;
        _totalSeconds++;
      });
    });
  }

  void _stopTimer() {
    _timer?.cancel();
    _timer = null;
    _stopwatch.stop();
  }

  Future<void> _load({int? index}) async {
    _stopTimer();
    setState(() {
      _loading = true;
      _error = null;
      _challenge = null;
      _selected = -1;
      _feedback = false;
      _wasCorrect = false;
      _alreadySubmitted = false;
      _serverCorrectAnswer = null;
      _questionSeconds = 0;
    });
    try {
      final targetIndex = index ?? 0;
      _currentIndex = targetIndex;
      _challenge = await _service.loadQuestion(index: targetIndex);
      if (_challenge != null) {
        _stopwatch
          ..reset()
          ..start();
        _startTimer();
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
      final elapsed = _questionSeconds > 0
          ? _questionSeconds
          : _stopwatch.elapsed.inSeconds;
      final result = await _service.submit(
        challenge: challenge,
        answerIndex: _selected,
        elapsedSeconds: elapsed,
        displayName: widget.store.leaderboardName,
      );
      _stopTimer();
      if (!mounted) return;
      setState(() {
        _wasCorrect = result.correct;
        _challengeId = result.challengeId;
        _alreadySubmitted = result.alreadySubmitted;
        _serverCorrectAnswer = result.correctAnswer;
        _feedback = true;
        if (result.correct) {
          _sessionScore++;
          _sessionStreak++;
        } else {
          _sessionStreak = 0;
        }
      });
      unawaited(
        result.correct ? AnswerFeedback.correct() : AnswerFeedback.incorrect(),
      );
    } on FirebaseFunctionsException catch (error) {
      _stopTimer();
      if (!mounted) return;
      if (error.code == 'already-exists') {
        final correctIdx = _resolvedCorrectAnswerIndex(challenge);
        final matches = correctIdx != null && correctIdx == _selected;
        setState(() {
          _wasCorrect = matches;
          _challengeId = challenge.challengeId;
          _feedback = true;
          _alreadySubmitted = true;
        });
      } else {
        setState(() => _error = cloudSubmitErrorMessage(error));
      }
    } catch (_) {
      _stopTimer();
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

  void _nextQuestion() {
    setState(() {
      _questionNumber++;
    });
    unawaited(_load(index: _currentIndex + 1));
  }

  int? _resolvedCorrectAnswerIndex(CloudChallenge challenge) {
    if (_wasCorrect && _selected >= 0 && _selected < challenge.options.length) {
      return _selected;
    }
    if (_serverCorrectAnswer != null &&
        _serverCorrectAnswer! >= 0 &&
        _serverCorrectAnswer! < challenge.options.length) {
      return _serverCorrectAnswer;
    }
    final ref = challenge.scriptureReference.trim().toLowerCase();
    for (var i = 0; i < challenge.options.length; i++) {
      final opt = challenge.options[i].trim().toLowerCase();
      if (ref.startsWith(opt) ||
          RegExp(r'\b' + RegExp.escape(opt) + r'\b').hasMatch(ref)) {
        return i;
      }
    }
    return null;
  }

  @override
  void dispose() {
    _stopTimer();
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
                onPressed: () => _load(index: _currentIndex),
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

  String _categorySubtitle(CloudChallenge challenge) {
    final isOldTestament = challenge.testament.toLowerCase().contains('old');
    // Never spoil the answer if propheticFocus matches one of the multiple choice options!
    final focusLeaksOption = challenge.options.any(
      (option) =>
          option.trim().toLowerCase() ==
          challenge.propheticFocus.trim().toLowerCase(),
    );
    if (focusLeaksOption) {
      return isOldTestament
          ? 'Prophetic Scripture & Books'
          : 'Prophetic Witness';
    }
    return challenge.propheticFocus.isNotEmpty
        ? challenge.propheticFocus
        : (isOldTestament ? 'Prophetic Scripture' : 'Prophetic Witness');
  }

  Widget _challengeBody(BuildContext context, CloudChallenge challenge) {
    final correctIndex = _resolvedCorrectAnswerIndex(challenge);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Wrap(
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          runSpacing: 8,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              decoration: BoxDecoration(
                color: _slateSurface,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: Colors.white.withValues(alpha: .12)),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.quiz_outlined, size: 16, color: _gold),
                  const SizedBox(width: 6),
                  Text(
                    'QUESTION $_questionNumber',
                    style: const TextStyle(
                      color: _gold,
                      fontWeight: FontWeight.bold,
                      fontSize: 12,
                      letterSpacing: 1,
                    ),
                  ),
                ],
              ),
            ),
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: _slateSurface,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(
                      color: Colors.white.withValues(alpha: .12),
                    ),
                  ),
                  child: Text(
                    'SCORE: $_sessionScore${_sessionStreak > 1 ? '  🔥 $_sessionStreak' : ''}',
                    style: const TextStyle(
                      color: _slateTextPrimary,
                      fontWeight: FontWeight.bold,
                      fontSize: 12,
                      letterSpacing: .8,
                    ),
                  ),
                ),
                const SizedBox(width: 4),
                IconButton(
                  tooltip: 'Leaderboard',
                  icon: const Icon(
                    Icons.leaderboard_outlined,
                    color: _gold,
                    size: 22,
                  ),
                  onPressed: () => Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => LeaderboardScreen(
                        store: widget.store,
                        challengeId: _challengeId,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
        const SizedBox(height: 14),
        Center(
          child: DivineTimerHud(
            questionSeconds: _questionSeconds,
            totalSeconds: _totalSeconds,
          ),
        ),
        const SizedBox(height: 16),
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
                _categorySubtitle(challenge),
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
              correct: _feedback && (correctIndex == index),
              feedback: _feedback,
              onTap: () {
                if (!_feedback && !_submitting) {
                  setState(() => _selected = index);
                }
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
                  _wasCorrect
                      ? (_alreadySubmitted
                            ? 'VERIFIED CORRECT (ALREADY RECORDED)'
                            : 'VERIFIED CORRECT')
                      : (_alreadySubmitted
                            ? 'ANSWER RECORDED (PREVIOUSLY ATTEMPTED)'
                            : 'INCORRECT'),
                  style: TextStyle(
                    color: _wasCorrect ? _correct : _wrong,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 1,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  challenge.explanation,
                  style: const TextStyle(height: 1.35),
                ),
                const SizedBox(height: 8),
                Text(
                  'SCRIPTURE: ${challenge.scriptureReference}',
                  style: const TextStyle(
                    color: _slateTextSecondary,
                    fontWeight: FontWeight.bold,
                    fontSize: 12,
                  ),
                ),
                if (challenge.propheticFocus.isNotEmpty &&
                    challenge.propheticFocus.trim().toLowerCase() !=
                        challenge.testament.trim().toLowerCase()) ...[
                  const SizedBox(height: 6),
                  Text(
                    'FOCUS: ${challenge.propheticFocus}',
                    style: const TextStyle(
                      color: _gold,
                      fontWeight: FontWeight.bold,
                      fontSize: 12,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
        const SizedBox(height: 14),
        if (_feedback) ...[
          SlatePillButton(
            label: 'NEXT QUESTION',
            icon: Icons.arrow_forward,
            onPressed: _nextQuestion,
          ),
          const SizedBox(height: 8),
          Center(
            child: TextButton.icon(
              onPressed: () => Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => LeaderboardScreen(
                    store: widget.store,
                    challengeId: _challengeId,
                  ),
                ),
              ),
              icon: const Icon(
                Icons.leaderboard_outlined,
                size: 16,
                color: _slateTextSecondary,
              ),
              label: const Text(
                'View today\'s leaderboard',
                style: TextStyle(color: _slateTextSecondary, fontSize: 13),
              ),
            ),
          ),
        ] else ...[
          SlatePillButton(
            label: _submitting ? 'VERIFYING ANSWER…' : 'LOCK IN ANSWER',
            loading: _submitting,
            onPressed: _selected >= 0 && !_submitting ? _submit : null,
          ),
        ],
      ],
    );
  }
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
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: store,
    builder: (context, _) {
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
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: _slateTextSecondary,
                    ),
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
                                const Text(
                                  'DUE QUESTIONS',
                                  style: TextStyle(
                                    color: _gold,
                                    fontSize: 12,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  '${store.dueReviewCount} questions need review today',
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
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
                                    level: dueLevel ?? 1,
                                    mode: 'review',
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                const Padding(
                  padding: EdgeInsets.fromLTRB(24, 0, 24, 8),
                  child: SlateSectionHeader('MISTAKE NOTEBOOK'),
                ),
                Expanded(
                  child: store.detailedMistakes.isEmpty
                      ? const Center(
                          child: Text(
                            'No recorded mistakes. Take a quiz to begin studying here.',
                            textAlign: TextAlign.center,
                            style: TextStyle(color: _slateTextMuted),
                          ),
                        )
                      : ListView.separated(
                          padding: const EdgeInsets.fromLTRB(22, 0, 22, 32),
                          itemCount: store.detailedMistakes.length,
                          separatorBuilder: (_, _) =>
                              const SizedBox(height: 14),
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
    },
  );
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
  const GroupsScreen({super.key, required this.store, this.service});
  final ProgressStore store;
  final CloudGroupGateway? service;

  @override
  State<GroupsScreen> createState() => _GroupsScreenState();
}

class _GroupsScreenState extends State<GroupsScreen> {
  late final CloudGroupGateway _service;
  late Future<bool> _available;
  late final Stream<List<QuizGroup>> _groupsStream;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? CloudChallengeService();
    _available = _service.isAvailable;
    _groupsStream = _service.myGroups();
  }

  Future<void> _showNameDialog({required bool join}) async {
    final controller = TextEditingController();
    int selectedDuration = 10;
    final result = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          backgroundColor: _slateSurface,
          title: Text(join ? 'Join Group' : 'Create Group'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextField(
                controller: controller,
                maxLength: join ? 6 : 40,
                keyboardType: join ? TextInputType.number : TextInputType.text,
                autofocus: true,
                decoration: InputDecoration(
                  hintText: join
                      ? 'Enter the 6-digit join code'
                      : 'e.g. Grace Fellowship',
                ),
              ),
              if (!join) ...[
                const SizedBox(height: 8),
                const Text(
                  'JOIN WINDOW',
                  style: TextStyle(
                    color: _slateTextSecondary,
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                const Text(
                  'People can join this group for 10 minutes. Extend the window later if needed; this does not limit challenge time.',
                  style: TextStyle(
                    color: _slateTextSecondary,
                    fontSize: 12,
                    height: 1.3,
                  ),
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: [
                    ChoiceChip(
                      label: const Text(
                        '10 min',
                        style: TextStyle(fontSize: 11),
                      ),
                      selected: selectedDuration == 10,
                      selectedColor: _gold,
                      labelStyle: TextStyle(
                        color: selectedDuration == 10
                            ? _slateButtonText
                            : _slateTextPrimary,
                        fontWeight: FontWeight.bold,
                      ),
                      onSelected: (selected) {
                        if (selected) {
                          setDialogState(() => selectedDuration = 10);
                        }
                      },
                    ),
                  ],
                ),
              ],
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('CANCEL'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(dialogContext, true),
              child: Text(join ? 'JOIN' : 'CREATE'),
            ),
          ],
        ),
      ),
    );
    final text = controller.text.trim();
    controller.dispose();
    if (result != true || text.isEmpty || !mounted) return;
    if (join && !RegExp(r'^\d{6}$').hasMatch(text)) {
      _notice('Enter the 6-digit join code shared by the host.');
      return;
    }
    try {
      if (join) {
        await _service.joinGroup(text);
        if (mounted) _notice('You joined the group!');
      } else {
        final code = await _service.createGroup(
          text,
          durationMinutes: selectedDuration,
        );
        if (mounted) _notice('Group created! Share code: $code');
      }
    } catch (e) {
      if (mounted) {
        final msg = e is FirebaseFunctionsException && e.message != null
            ? e.message!
            : 'That group action could not be completed. Try again.';
        _notice(msg);
      }
    }
  }

  void _notice(String message) {
    if (!mounted) return;
    ScaffoldMessenger.maybeOf(
      context,
    )?.showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _signInWithGoogle() async {
    try {
      final user = await _service.signInWithGoogle();
      if (user != null && mounted) {
        final username = await _service.getClaimedUsername();
        if (username == null || username.isEmpty) {
          if (mounted) {
            await showDialog<bool>(
              context: context,
              barrierDismissible: false,
              builder: (dContext) =>
                  _ClaimUsernameDialog(service: _service, store: widget.store),
            );
          }
        }
        if (mounted) {
          setState(() {});
          _notice(
            'Signed in as ${user.displayName ?? user.email ?? 'learner'}',
          );
        }
      }
    } catch (_) {
      if (mounted) _notice('Google Sign-in was cancelled or unavailable.');
    }
  }

  Future<void> _signOut() async {
    await _service.signOut();
    if (mounted) {
      setState(() {});
      _notice('Signed out');
    }
  }

  Widget _accountBar() {
    final user = _service.currentUser;
    final isGoogleUser = user != null && !user.isAnonymous;
    if (isGoogleUser) {
      return FutureBuilder<String?>(
        future: _service.getClaimedUsername(),
        builder: (context, snapshot) {
          final claimed = snapshot.data;
          final handle = claimed != null && claimed.isNotEmpty
              ? '@$claimed'
              : (user.displayName ?? user.email ?? 'Google Account');
          return Container(
            margin: const EdgeInsets.only(bottom: 14),
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            decoration: BoxDecoration(
              color: _slateSurface,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: Colors.white.withValues(alpha: .12)),
            ),
            child: Row(
              children: [
                const Icon(Icons.account_circle, color: _gold, size: 22),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        handle,
                        style: const TextStyle(
                          color: _slateTextPrimary,
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                      if (claimed != null && claimed.isNotEmpty)
                        Text(
                          user.email ?? '',
                          style: const TextStyle(
                            color: _slateTextSecondary,
                            fontSize: 10,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                    ],
                  ),
                ),
                TextButton(
                  onPressed: _signOut,
                  style: TextButton.styleFrom(
                    visualDensity: VisualDensity.compact,
                    padding: EdgeInsets.zero,
                  ),
                  child: const Text(
                    'Sign out',
                    style: TextStyle(color: _slateTextSecondary, fontSize: 11),
                  ),
                ),
              ],
            ),
          );
        },
      );
    }
    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
      decoration: BoxDecoration(
        color: _slateSurface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withValues(alpha: .12)),
      ),
      child: Row(
        children: [
          const Icon(Icons.account_circle_outlined, color: _gold, size: 22),
          const SizedBox(width: 10),
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Google Account',
                  style: TextStyle(
                    color: _slateTextPrimary,
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Text(
                  'Sign in to host & sync group quizzes',
                  style: TextStyle(color: _slateTextSecondary, fontSize: 10),
                ),
              ],
            ),
          ),
          FilledButton.tonal(
            onPressed: _signInWithGoogle,
            style: FilledButton.styleFrom(
              visualDensity: VisualDensity.compact,
              backgroundColor: _slateSurfaceVariant,
            ),
            child: const Text(
              'SIGN IN',
              style: TextStyle(
                color: _gold,
                fontSize: 11,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    );
  }

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
                  const SizedBox(height: 14),
                  _accountBar(),
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
                      stream: _groupsStream,
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
                            final isExpired = group.isExpired;
                            final codeDisplay =
                                !isExpired && group.joinCode != null
                                ? 'Join code: ${group.joinCode}'
                                : isExpired
                                ? 'Join window closed'
                                : 'Invite code unavailable';
                            final String timeStatus;
                            if (group.expiresAt == null) {
                              timeStatus = 'Open session';
                            } else if (isExpired) {
                              timeStatus = 'Expired';
                            } else {
                              final mins = group.remainingTime?.inMinutes ?? 0;
                              timeStatus = mins > 0
                                  ? '${mins}m left'
                                  : '<1m left';
                            }
                            final roleDisplay = group.role == 'owner'
                                ? 'HOST'
                                : 'MEMBER';
                            return SlateSettingCard(
                              title: group.name,
                              subtitle:
                                  '$roleDisplay • $codeDisplay • $timeStatus',
                              icon: isExpired
                                  ? Icons.timer_off_outlined
                                  : Icons.group_outlined,
                              onTap: () => Navigator.of(context).push(
                                MaterialPageRoute(
                                  builder: (_) => GroupDetailScreen(
                                    store: widget.store,
                                    group: group,
                                    service: _service,
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
    this.service,
  });
  final ProgressStore store;
  final QuizGroup group;
  final CloudGroupGateway? service;

  @override
  State<GroupDetailScreen> createState() => _GroupDetailScreenState();
}

class _GroupDetailScreenState extends State<GroupDetailScreen> {
  late final CloudGroupGateway _service;
  late final Stream<List<GroupChallenge>> _challengesStream;
  StreamSubscription<QuizGroup>? _groupSub;
  late QuizGroup _currentGroup;
  bool _publishing = false;
  bool _extending = false;
  Timer? _countdownTimer;
  late DateTime? _expiresAt;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? CloudChallengeService();
    _currentGroup = widget.group;
    _challengesStream = _service.groupChallenges(widget.group.id);
    _expiresAt = widget.group.expiresAt;
    _groupSub = _service.streamGroup(widget.group.id).listen((updated) {
      if (mounted) {
        setState(() {
          _currentGroup = updated;
          _expiresAt = updated.expiresAt;
        });
      }
    });
    if (_expiresAt != null) {
      _countdownTimer = Timer.periodic(const Duration(seconds: 1), (_) {
        if (mounted) setState(() {});
      });
    }
  }

  @override
  void dispose() {
    _countdownTimer?.cancel();
    _groupSub?.cancel();
    super.dispose();
  }

  Future<void> _extendGroup() async {
    if (_extending) return;
    setState(() => _extending = true);
    try {
      await _service.extendGroup(widget.group.id, additionalMinutes: 10);
      final base = (_expiresAt != null && _expiresAt!.isAfter(DateTime.now()))
          ? _expiresAt!
          : DateTime.now();
      setState(() {
        _expiresAt = base.add(const Duration(minutes: 10));
      });
      if (mounted) {
        ScaffoldMessenger.maybeOf(context)?.showSnackBar(
          const SnackBar(content: Text('Session extended by 10 minutes!')),
        );
      }
    } catch (e) {
      if (mounted) {
        final message = e is FirebaseFunctionsException && e.message != null
            ? e.message!
            : 'Could not extend session. Please try again.';
        ScaffoldMessenger.maybeOf(
          context,
        )?.showSnackBar(SnackBar(content: Text(message)));
      }
    } finally {
      if (mounted) setState(() => _extending = false);
    }
  }

  void _copyCode(String code) {
    Clipboard.setData(ClipboardData(text: code));
    ScaffoldMessenger.maybeOf(
      context,
    )?.showSnackBar(SnackBar(content: Text('Copied "$code" to clipboard!')));
  }

  Future<void> _createQuiz(int count, String mode) async {
    if (_publishing) return;
    setState(() => _publishing = true);
    try {
      final modeLabel = mode == 'fellowship' ? 'Fellowship' : 'Competitive';
      await _service.createGroupQuiz(
        groupId: widget.group.id,
        questionCount: count,
        mode: mode,
        title: '${_currentGroup.name} $modeLabel ($count Qs)',
      );
      if (mounted) {
        ScaffoldMessenger.maybeOf(context)?.showSnackBar(
          SnackBar(
            content: Text(
              '$modeLabel challenge with $count questions published!',
            ),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        final message = e is FirebaseFunctionsException && e.message != null
            ? e.message!
            : 'The group challenge could not be published.';
        ScaffoldMessenger.maybeOf(
          context,
        )?.showSnackBar(SnackBar(content: Text(message)));
      }
    } finally {
      if (mounted) setState(() => _publishing = false);
    }
  }

  void _showCreateQuizDialog() {
    String selectedMode = 'competitive';
    int selectedCount = 10;
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: _slateSurface,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (sheetContext) => StatefulBuilder(
        builder: (context, setSheetState) => SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 20, 24, 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'NEW GROUP CHALLENGE',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: _gold,
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 1.2,
                  ),
                ),
                const SizedBox(height: 16),
                const Text(
                  'SELECT CHALLENGE MODE',
                  style: TextStyle(
                    color: _slateTextSecondary,
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 1.0,
                  ),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: InkWell(
                        onTap: () =>
                            setSheetState(() => selectedMode = 'competitive'),
                        borderRadius: BorderRadius.circular(12),
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            vertical: 12,
                            horizontal: 8,
                          ),
                          decoration: BoxDecoration(
                            color: selectedMode == 'competitive'
                                ? _gold.withValues(alpha: .18)
                                : _slateSurfaceVariant,
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(
                              color: selectedMode == 'competitive'
                                  ? _gold
                                  : Colors.white12,
                              width: selectedMode == 'competitive' ? 1.5 : 1.0,
                            ),
                          ),
                          child: Column(
                            children: [
                              Icon(
                                Icons.speed,
                                color: selectedMode == 'competitive'
                                    ? _gold
                                    : _slateTextSecondary,
                                size: 24,
                              ),
                              const SizedBox(height: 6),
                              Text(
                                'COMPETITIVE',
                                style: TextStyle(
                                  color: selectedMode == 'competitive'
                                      ? _gold
                                      : _slateTextPrimary,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 12,
                                ),
                              ),
                              const SizedBox(height: 2),
                              const Text(
                                'Race through the same Bible questions. Accuracy wins; speed breaks ties.',
                                textAlign: TextAlign.center,
                                style: TextStyle(
                                  color: _slateTextSecondary,
                                  fontSize: 10,
                                  height: 1.25,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: InkWell(
                        onTap: () =>
                            setSheetState(() => selectedMode = 'fellowship'),
                        borderRadius: BorderRadius.circular(12),
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            vertical: 12,
                            horizontal: 8,
                          ),
                          decoration: BoxDecoration(
                            color: selectedMode == 'fellowship'
                                ? _gold.withValues(alpha: .18)
                                : _slateSurfaceVariant,
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(
                              color: selectedMode == 'fellowship'
                                  ? _gold
                                  : Colors.white12,
                              width: selectedMode == 'fellowship' ? 1.5 : 1.0,
                            ),
                          ),
                          child: Column(
                            children: [
                              Icon(
                                Icons.groups_outlined,
                                color: selectedMode == 'fellowship'
                                    ? _gold
                                    : _slateTextSecondary,
                                size: 24,
                              ),
                              const SizedBox(height: 6),
                              Text(
                                'FELLOWSHIP',
                                style: TextStyle(
                                  color: selectedMode == 'fellowship'
                                      ? _gold
                                      : _slateTextPrimary,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 12,
                                ),
                              ),
                              const SizedBox(height: 2),
                              const Text(
                                'Answer together, reveal Scripture, discuss, then let the host move everyone forward.',
                                textAlign: TextAlign.center,
                                style: TextStyle(
                                  color: _slateTextSecondary,
                                  fontSize: 10,
                                  height: 1.25,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 18),
                const Text(
                  'SELECT QUESTION COUNT',
                  style: TextStyle(
                    color: _slateTextSecondary,
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 1.0,
                  ),
                ),
                const SizedBox(height: 8),
                for (final count in const [10, 20, 30]) ...[
                  InkWell(
                    onTap: () => setSheetState(() => selectedCount = count),
                    borderRadius: BorderRadius.circular(12),
                    child: Container(
                      margin: const EdgeInsets.only(bottom: 8),
                      padding: const EdgeInsets.symmetric(
                        vertical: 12,
                        horizontal: 16,
                      ),
                      decoration: BoxDecoration(
                        color: selectedCount == count
                            ? _gold.withValues(alpha: .15)
                            : _slateSurfaceVariant,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(
                          color: selectedCount == count
                              ? _gold
                              : Colors.white10,
                          width: selectedCount == count ? 1.5 : 1.0,
                        ),
                      ),
                      child: Row(
                        children: [
                          Icon(
                            selectedCount == count
                                ? Icons.radio_button_checked
                                : Icons.radio_button_off,
                            color: selectedCount == count
                                ? _gold
                                : _slateTextSecondary,
                            size: 18,
                          ),
                          const SizedBox(width: 12),
                          Text(
                            '$count QUESTIONS',
                            style: TextStyle(
                              color: selectedCount == count
                                  ? _gold
                                  : _slateTextPrimary,
                              fontWeight: FontWeight.bold,
                              fontSize: 14,
                            ),
                          ),
                          const Spacer(),
                          Text(
                            count == 10
                                ? 'Quick'
                                : count == 20
                                ? 'Standard'
                                : 'Deep Study',
                            style: const TextStyle(
                              color: _slateTextSecondary,
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
                const SizedBox(height: 12),
                SlatePillButton(
                  label: 'CREATE CHALLENGE',
                  onPressed: () {
                    Navigator.of(sheetContext).pop();
                    _createQuiz(selectedCount, selectedMode);
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isExpired = _expiresAt != null && DateTime.now().isAfter(_expiresAt!);
    final joinCode = isExpired ? null : _currentGroup.joinCode;
    return Scaffold(
      body: DivineBackground(
        reduceMotion: widget.store.reduceMotion,
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(22, 16, 22, 40),
            child: Column(
              children: [
                SlatePageHeader(title: _currentGroup.name.toUpperCase()),
                const SizedBox(height: 10),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 12,
                  ),
                  decoration: BoxDecoration(
                    color: _slateSurface,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(
                      color: Colors.white.withValues(alpha: .12),
                    ),
                  ),
                  child: Column(
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                joinCode != null
                                    ? 'JOIN CODE'
                                    : 'JOIN CODE UNAVAILABLE',
                                style: const TextStyle(
                                  color: _slateTextSecondary,
                                  fontSize: 10,
                                  letterSpacing: 1.2,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 2),
                              SelectableText(
                                joinCode ??
                                    'Ask the host to share the 6-digit code',
                                style: TextStyle(
                                  color: joinCode == null
                                      ? _slateTextSecondary
                                      : _gold,
                                  fontSize: joinCode == null ? 13 : 20,
                                  fontWeight: FontWeight.w900,
                                  letterSpacing: joinCode == null ? 0 : 3.0,
                                ),
                              ),
                            ],
                          ),
                          if (joinCode != null)
                            FilledButton.tonalIcon(
                              onPressed: () => _copyCode(joinCode),
                              icon: const Icon(
                                Icons.copy,
                                size: 16,
                                color: _gold,
                              ),
                              label: const Text(
                                'COPY',
                                style: TextStyle(
                                  color: _gold,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 12,
                                ),
                              ),
                              style: FilledButton.styleFrom(
                                visualDensity: VisualDensity.compact,
                                backgroundColor: _slateSurfaceVariant,
                              ),
                            ),
                        ],
                      ),
                      if (_expiresAt != null) ...[
                        const Divider(color: Colors.white12, height: 16),
                        Builder(
                          builder: (context) {
                            final diff = _expiresAt!.difference(DateTime.now());
                            final remainingStr = isExpired
                                ? 'JOIN WINDOW CLOSED'
                                : 'JOIN WINDOW • ${diff.inMinutes}:${(diff.inSeconds % 60).toString().padLeft(2, '0')} remaining';
                            return Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Row(
                                  children: [
                                    Icon(
                                      isExpired
                                          ? Icons.timer_off_outlined
                                          : Icons.timer_outlined,
                                      size: 16,
                                      color: isExpired ? _wrong : _gold,
                                    ),
                                    const SizedBox(width: 6),
                                    Text(
                                      remainingStr,
                                      style: TextStyle(
                                        color: isExpired
                                            ? _wrong
                                            : _slateTextPrimary,
                                        fontSize: 12,
                                        fontWeight: FontWeight.w600,
                                      ),
                                    ),
                                  ],
                                ),
                                if (_currentGroup.isOwner)
                                  TextButton.icon(
                                    onPressed: _extending ? null : _extendGroup,
                                    icon: const Icon(
                                      Icons.add_alarm,
                                      size: 16,
                                      color: _gold,
                                    ),
                                    label: Text(
                                      _extending
                                          ? 'EXTENDING…'
                                          : '+10 MIN WINDOW',
                                      style: const TextStyle(
                                        color: _gold,
                                        fontSize: 12,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                    style: TextButton.styleFrom(
                                      visualDensity: VisualDensity.compact,
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: 8,
                                      ),
                                    ),
                                  ),
                              ],
                            );
                          },
                        ),
                      ],
                    ],
                  ),
                ),
                if (_currentGroup.isOwner) ...[
                  const SizedBox(height: 12),
                  SlatePillButton(
                    label: _publishing
                        ? 'PUBLISHING QUIZ…'
                        : isExpired
                        ? 'JOIN WINDOW CLOSED (EXTEND TO HOST)'
                        : 'CREATE GROUP CHALLENGE',
                    loading: _publishing,
                    onPressed: _publishing || isExpired
                        ? null
                        : _showCreateQuizDialog,
                  ),
                ],
                const SizedBox(height: 18),
                Expanded(
                  child: StreamBuilder<List<GroupChallenge>>(
                    stream: _challengesStream,
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
                          final count = challenge.questionCount;
                          final countLabel = count > 1
                              ? '$count Questions'
                              : '1 Question';
                          final modeLabel = challenge.isFellowship
                              ? 'Fellowship'
                              : 'Competitive';
                          final statusLabel = challenge.isLobby
                              ? 'Waiting for host to start'
                              : challenge.isCompleted
                              ? 'Challenge complete'
                              : 'Active • Resume';
                          return SlateSettingCard(
                            title: challenge.title.isNotEmpty
                                ? challenge.title
                                : '$modeLabel Bible Challenge',
                            subtitle: '$modeLabel • $countLabel • $statusLabel',
                            icon: challenge.isFellowship
                                ? Icons.groups_outlined
                                : Icons.speed,
                            onTap: () {
                              if (challenge.isLobby) {
                                Navigator.of(context).push(
                                  MaterialPageRoute(
                                    builder: (_) => GroupLobbyScreen(
                                      store: widget.store,
                                      groupId: widget.group.id,
                                      challenge: challenge,
                                      group: _currentGroup,
                                      service: _service,
                                    ),
                                  ),
                                );
                              } else if (challenge.isFellowship) {
                                Navigator.of(context).push(
                                  MaterialPageRoute(
                                    builder: (_) => FellowshipQuestionScreen(
                                      store: widget.store,
                                      groupId: widget.group.id,
                                      challenge: challenge,
                                      service: _service,
                                    ),
                                  ),
                                );
                              } else {
                                Navigator.of(context).push(
                                  MaterialPageRoute(
                                    builder: (_) => GroupQuestionScreen(
                                      store: widget.store,
                                      groupId: widget.group.id,
                                      challenge: challenge,
                                      service: _service,
                                    ),
                                  ),
                                );
                              }
                            },
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
}

class GroupLobbyScreen extends StatefulWidget {
  const GroupLobbyScreen({
    super.key,
    required this.store,
    required this.groupId,
    required this.challenge,
    required this.group,
    this.service,
  });

  final ProgressStore store;
  final String groupId;
  final GroupChallenge challenge;
  final QuizGroup group;
  final CloudGroupGateway? service;

  @override
  State<GroupLobbyScreen> createState() => _GroupLobbyScreenState();
}

class _GroupLobbyScreenState extends State<GroupLobbyScreen> {
  late final CloudGroupGateway _service;
  late final Stream<GroupChallenge> _challengeStream;
  late final Stream<QuizGroup> _groupStream;
  bool _starting = false;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? CloudChallengeService();
    _challengeStream = _service.streamChallenge(
      widget.groupId,
      widget.challenge.id,
    );
    _groupStream = _service.streamGroup(widget.groupId);
  }

  Future<void> _startChallenge() async {
    if (_starting) return;
    setState(() => _starting = true);
    try {
      await _service.startGroupChallenge(
        groupId: widget.groupId,
        challengeId: widget.challenge.id,
      );
    } catch (e) {
      if (mounted) {
        final message = e is FirebaseFunctionsException && e.message != null
            ? e.message!
            : 'Could not start challenge. Please try again.';
        ScaffoldMessenger.maybeOf(
          context,
        )?.showSnackBar(SnackBar(content: Text(message)));
      }
    } finally {
      if (mounted) setState(() => _starting = false);
    }
  }

  void _copyCode(String code) {
    Clipboard.setData(ClipboardData(text: code));
    ScaffoldMessenger.maybeOf(
      context,
    )?.showSnackBar(SnackBar(content: Text('Copied "$code" to clipboard!')));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: DivineBackground(
        reduceMotion: widget.store.reduceMotion,
        child: SafeArea(
          child: StreamBuilder<GroupChallenge>(
            stream: _challengeStream,
            initialData: widget.challenge,
            builder: (context, challengeSnap) {
              final challenge = challengeSnap.data ?? widget.challenge;

              if (!challenge.isLobby) {
                WidgetsBinding.instance.addPostFrameCallback((_) {
                  if (!mounted) return;
                  if (challenge.isFellowship) {
                    Navigator.of(context).pushReplacement(
                      MaterialPageRoute(
                        builder: (_) => FellowshipQuestionScreen(
                          store: widget.store,
                          groupId: widget.groupId,
                          challenge: challenge,
                          service: _service,
                        ),
                      ),
                    );
                  } else {
                    Navigator.of(context).pushReplacement(
                      MaterialPageRoute(
                        builder: (_) => GroupQuestionScreen(
                          store: widget.store,
                          groupId: widget.groupId,
                          challenge: challenge,
                          service: _service,
                        ),
                      ),
                    );
                  }
                });
              }

              return StreamBuilder<QuizGroup>(
                stream: _groupStream,
                initialData: widget.group,
                builder: (context, groupSnap) {
                  final group = groupSnap.data ?? widget.group;
                  final isHost =
                      group.isOwner ||
                      (challenge.ownerId != null &&
                          challenge.ownerId == _service.currentUser?.uid);
                  final remaining = group.remainingTime;
                  final remainingStr = remaining != null
                      ? 'JOIN WINDOW • ${remaining.inMinutes}:${(remaining.inSeconds % 60).toString().padLeft(2, '0')} remaining'
                      : 'JOIN WINDOW ACTIVE';

                  return Padding(
                    padding: const EdgeInsets.fromLTRB(22, 16, 22, 40),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        const SlatePageHeader(title: 'CHALLENGE LOBBY'),
                        const SizedBox(height: 16),
                        Container(
                          padding: const EdgeInsets.all(18),
                          decoration: BoxDecoration(
                            color: _slateSurface,
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(color: Colors.white12),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          group.name.toUpperCase(),
                                          style: const TextStyle(
                                            color: _slateTextPrimary,
                                            fontWeight: FontWeight.bold,
                                            fontSize: 16,
                                          ),
                                        ),
                                        const SizedBox(height: 4),
                                        Text(
                                          remainingStr,
                                          style: const TextStyle(
                                            color: _gold,
                                            fontSize: 12,
                                            fontWeight: FontWeight.w600,
                                          ),
                                        ),
                                        const SizedBox(height: 8),
                                        Container(
                                          padding: const EdgeInsets.symmetric(
                                            horizontal: 8,
                                            vertical: 3,
                                          ),
                                          decoration: BoxDecoration(
                                            color: isHost
                                                ? _gold.withValues(alpha: .18)
                                                : _slateSurfaceVariant,
                                            borderRadius: BorderRadius.circular(
                                              8,
                                            ),
                                          ),
                                          child: Text(
                                            isHost
                                                ? 'YOU • HOST'
                                                : 'MEMBER • HOST-LED LOBBY',
                                            style: TextStyle(
                                              color: isHost
                                                  ? _gold
                                                  : _slateTextSecondary,
                                              fontSize: 10,
                                              fontWeight: FontWeight.bold,
                                              letterSpacing: .5,
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                  if (group.joinCode != null &&
                                      !group.isExpired)
                                    FilledButton.tonalIcon(
                                      onPressed: () =>
                                          _copyCode(group.joinCode!),
                                      icon: const Icon(
                                        Icons.copy,
                                        size: 14,
                                        color: _gold,
                                      ),
                                      label: Text(
                                        group.joinCode!,
                                        style: const TextStyle(
                                          color: _gold,
                                          fontWeight: FontWeight.bold,
                                          letterSpacing: 1.5,
                                        ),
                                      ),
                                      style: FilledButton.styleFrom(
                                        backgroundColor: _slateSurfaceVariant,
                                        visualDensity: VisualDensity.compact,
                                      ),
                                    ),
                                ],
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),
                        SlateCard(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Icon(
                                    challenge.isFellowship
                                        ? Icons.groups_outlined
                                        : Icons.speed,
                                    color: _gold,
                                    size: 24,
                                  ),
                                  const SizedBox(width: 10),
                                  Expanded(
                                    child: Text(
                                      challenge.isFellowship
                                          ? 'FELLOWSHIP / HOST-LED MODE'
                                          : 'COMPETITIVE MODE',
                                      style: const TextStyle(
                                        color: _gold,
                                        fontWeight: FontWeight.bold,
                                        fontSize: 15,
                                        letterSpacing: 0.8,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 10),
                              Text(
                                challenge.isFellowship
                                    ? 'Everyone stays synchronized on the same question. The host controls answer reveals and question advancement, allowing Scripture discussion.'
                                    : 'Players begin together and progress independently through the questions. Accuracy comes first; completion time breaks ties.',
                                style: const TextStyle(
                                  color: _slateTextSecondary,
                                  fontSize: 13,
                                  height: 1.35,
                                ),
                              ),
                              const Divider(color: Colors.white12, height: 24),
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  const Text(
                                    'Question Count',
                                    style: TextStyle(
                                      color: _slateTextSecondary,
                                      fontSize: 13,
                                    ),
                                  ),
                                  Text(
                                    '${challenge.questionCount} Questions',
                                    style: const TextStyle(
                                      color: _slateTextPrimary,
                                      fontWeight: FontWeight.bold,
                                      fontSize: 14,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 6),
                              const Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Text(
                                    'Question Order',
                                    style: TextStyle(
                                      color: _slateTextSecondary,
                                      fontSize: 13,
                                    ),
                                  ),
                                  Text(
                                    'Identical for all players',
                                    style: TextStyle(
                                      color: _slateTextPrimary,
                                      fontWeight: FontWeight.w600,
                                      fontSize: 13,
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                        const Spacer(),
                        if (isHost) ...[
                          const Text(
                            'Waiting for players',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: _gold,
                              fontSize: 16,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            group.joinCode != null
                                ? 'Share code ${group.joinCode} with your players, then start when ready.'
                                : 'Invite players, then start when ready.',
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              color: _slateTextSecondary,
                              fontSize: 12,
                            ),
                          ),
                          const SizedBox(height: 12),
                          SlatePillButton(
                            label: _starting
                                ? 'STARTING…'
                                : challenge.isFellowship
                                ? 'START FELLOWSHIP'
                                : 'START CHALLENGE',
                            loading: _starting,
                            onPressed: _starting ? null : _startChallenge,
                          ),
                        ] else ...[
                          const Center(
                            child: Column(
                              children: [
                                CircularProgressIndicator(color: _gold),
                                SizedBox(height: 16),
                                Text(
                                  'Waiting for the host to start',
                                  style: TextStyle(
                                    color: _gold,
                                    fontWeight: FontWeight.bold,
                                    fontSize: 16,
                                  ),
                                ),
                                SizedBox(height: 6),
                                Text(
                                  'The host controls when this lobby opens for everyone.',
                                  textAlign: TextAlign.center,
                                  style: TextStyle(
                                    color: _slateTextSecondary,
                                    fontSize: 13,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const Spacer(),
                        ],
                      ],
                    ),
                  );
                },
              );
            },
          ),
        ),
      ),
    );
  }
}

class FellowshipQuestionScreen extends StatefulWidget {
  const FellowshipQuestionScreen({
    super.key,
    required this.store,
    required this.groupId,
    required this.challenge,
    this.service,
  });

  final ProgressStore store;
  final String groupId;
  final GroupChallenge challenge;
  final CloudGroupGateway? service;

  @override
  State<FellowshipQuestionScreen> createState() =>
      _FellowshipQuestionScreenState();
}

class _FellowshipQuestionScreenState extends State<FellowshipQuestionScreen> {
  late final CloudGroupGateway _service;
  late final Stream<GroupChallenge> _challengeStream;
  late final List<GroupChallengeItem> _questions;
  final Map<int, int> _myAnswers = <int, int>{};
  bool _submittingAnswer = false;
  bool _revealing = false;
  bool _advancing = false;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? CloudChallengeService();
    _challengeStream = _service.streamChallenge(
      widget.groupId,
      widget.challenge.id,
    );
    _questions = widget.challenge.items.isNotEmpty
        ? widget.challenge.items
        : [
            GroupChallengeItem(
              id: widget.challenge.id,
              question: widget.challenge.question,
              options: widget.challenge.options,
              scriptureReference: widget.challenge.scriptureReference,
              testament: '',
              propheticFocus: '',
            ),
          ];
  }

  Future<void> _submitAnswer(
    int qIndex,
    String qId,
    int optionIndex,
    DateTime? openedAt,
  ) async {
    if (_submittingAnswer || _myAnswers.containsKey(qIndex)) return;
    setState(() {
      _myAnswers[qIndex] = optionIndex;
      _submittingAnswer = true;
    });
    final latency = openedAt != null
        ? DateTime.now().difference(openedAt).inMilliseconds
        : 0;
    try {
      await _service.submitFellowshipAnswer(
        groupId: widget.groupId,
        challengeId: widget.challenge.id,
        questionIndex: qIndex,
        questionId: qId,
        selectedOptionIndex: optionIndex,
        responseLatencyMs: latency,
        username: widget.store.leaderboardName,
      );
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.maybeOf(context)?.showSnackBar(
          const SnackBar(
            content: Text('Could not record answer. Please try again.'),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _submittingAnswer = false);
    }
  }

  Future<void> _revealAnswer() async {
    if (_revealing) return;
    setState(() => _revealing = true);
    try {
      await _service.revealFellowshipAnswer(
        groupId: widget.groupId,
        challengeId: widget.challenge.id,
      );
    } catch (e) {
      if (mounted) {
        final msg = e is FirebaseFunctionsException && e.message != null
            ? e.message!
            : 'Could not reveal answer.';
        ScaffoldMessenger.maybeOf(
          context,
        )?.showSnackBar(SnackBar(content: Text(msg)));
      }
    } finally {
      if (mounted) setState(() => _revealing = false);
    }
  }

  Future<void> _advanceQuestion() async {
    if (_advancing) return;
    setState(() => _advancing = true);
    try {
      await _service.advanceFellowshipQuestion(
        groupId: widget.groupId,
        challengeId: widget.challenge.id,
      );
    } catch (e) {
      if (mounted) {
        final msg = e is FirebaseFunctionsException && e.message != null
            ? e.message!
            : 'Could not advance question.';
        ScaffoldMessenger.maybeOf(
          context,
        )?.showSnackBar(SnackBar(content: Text(msg)));
      }
    } finally {
      if (mounted) setState(() => _advancing = false);
    }
  }

  String _formatTime(int seconds) {
    final m = seconds ~/ 60;
    final s = seconds % 60;
    return '${m.toString().padLeft(2, '0')}:${s.toString().padLeft(2, '0')}';
  }

  Widget _buildResultsView() {
    return ListView(
      padding: const EdgeInsets.fromLTRB(22, 16, 22, 40),
      children: [
        const SlatePageHeader(title: 'FELLOWSHIP RESULTS'),
        const SizedBox(height: 16),
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: _slateSurface,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: _gold.withValues(alpha: .5), width: 1.5),
          ),
          child: const Column(
            children: [
              Icon(Icons.groups_outlined, color: _gold, size: 48),
              SizedBox(height: 8),
              Text(
                'STUDY COMPLETED',
                style: TextStyle(
                  color: _gold,
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 1.1,
                ),
              ),
              SizedBox(height: 4),
              Text(
                'Rankings sorted by Scripture accuracy, then answer response time',
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: _slateTextSecondary,
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 24),
        const SlateSectionHeader('FELLOWSHIP LEADERBOARD'),
        const SizedBox(height: 10),
        StreamBuilder<List<LeaderboardEntry>>(
          stream: _service.groupLeaderboard(
            widget.groupId,
            widget.challenge.id,
          ),
          builder: (context, snapshot) {
            if (snapshot.hasError) {
              return const Center(child: Text('Leaderboard unavailable.'));
            }
            if (!snapshot.hasData) {
              return const Center(
                child: Padding(
                  padding: EdgeInsets.all(16),
                  child: CircularProgressIndicator(color: _gold),
                ),
              );
            }
            final entries = snapshot.data!;
            if (entries.isEmpty) {
              return const Text(
                'Gathering fellowship scores…',
                textAlign: TextAlign.center,
                style: TextStyle(color: _slateTextSecondary),
              );
            }
            return ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: entries.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final entry = entries[index];
                final isTop3 = index < 3;
                final isYou =
                    entry.displayName == widget.store.leaderboardName ||
                    entry.id == _service.currentUser?.uid;
                return Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 14,
                    vertical: 10,
                  ),
                  decoration: BoxDecoration(
                    color: _slateSurface,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                      color: isYou
                          ? _gold
                          : isTop3
                          ? _gold.withValues(alpha: .4)
                          : Colors.white.withValues(alpha: .08),
                      width: isYou ? 1.5 : 1.0,
                    ),
                  ),
                  child: Row(
                    children: [
                      Text(
                        '#${index + 1}',
                        style: TextStyle(
                          color: isTop3 ? _gold : _slateTextSecondary,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Row(
                          children: [
                            Flexible(
                              child: Text(
                                entry.displayName,
                                style: TextStyle(
                                  color: isYou ? _gold : _slateTextPrimary,
                                  fontWeight: isYou
                                      ? FontWeight.bold
                                      : FontWeight.w600,
                                ),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            if (isYou) ...[
                              const SizedBox(width: 6),
                              Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 6,
                                  vertical: 2,
                                ),
                                decoration: BoxDecoration(
                                  color: _gold.withValues(alpha: .2),
                                  borderRadius: BorderRadius.circular(6),
                                ),
                                child: const Text(
                                  'YOU',
                                  style: TextStyle(
                                    color: _gold,
                                    fontSize: 9,
                                    fontWeight: FontWeight.w900,
                                    letterSpacing: 0.8,
                                  ),
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                      Text(
                        '${entry.score} pts • ${_formatTime(entry.elapsedSeconds)}',
                        style: const TextStyle(
                          color: _slateTextSecondary,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                );
              },
            );
          },
        ),
        const SizedBox(height: 24),
        SlatePillButton(
          label: 'BACK TO GROUP',
          onPressed: () => Navigator.of(context).pop(),
        ),
      ],
    );
  }

  Widget _buildActiveQuestionView(GroupChallenge challenge) {
    final currentIndex = challenge.currentQuestionIndex.clamp(
      0,
      _questions.length - 1,
    );
    final currentQ = _questions[currentIndex];
    final isHost =
        challenge.ownerId != null &&
        challenge.ownerId == _service.currentUser?.uid;
    final isRevealed = challenge.isQuestionRevealed;
    final mySelection = _myAnswers[currentIndex];
    final hasAnswered =
        mySelection != null ||
        challenge.answeredUids.contains(_service.currentUser?.uid);
    final isLast = currentIndex == _questions.length - 1;
    final correctAnswer = challenge.revealedAnswer ?? 0;

    return ListView(
      padding: const EdgeInsets.fromLTRB(22, 16, 22, 40),
      children: [
        const SlatePageHeader(title: 'FELLOWSHIP STUDY'),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          decoration: BoxDecoration(
            color: _slateSurface,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: Colors.white.withValues(alpha: .12)),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  const Icon(Icons.people_alt_outlined, color: _gold, size: 18),
                  const SizedBox(width: 6),
                  Text(
                    '${challenge.answeredUids.length} answered',
                    style: const TextStyle(
                      color: _slateTextPrimary,
                      fontWeight: FontWeight.bold,
                      fontSize: 13,
                    ),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: _gold.withValues(alpha: .15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  'QUESTION ${currentIndex + 1} OF ${_questions.length}',
                  style: const TextStyle(
                    color: _gold,
                    fontWeight: FontWeight.bold,
                    fontSize: 12,
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 10),
        ClipRRect(
          borderRadius: BorderRadius.circular(4),
          child: LinearProgressIndicator(
            value: (currentIndex + 1) / _questions.length,
            backgroundColor: Colors.white.withValues(alpha: .08),
            valueColor: const AlwaysStoppedAnimation<Color>(_gold),
            minHeight: 5,
          ),
        ),
        const SizedBox(height: 18),
        SlateCard(
          child: Text(
            currentQ.question,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.titleLarge?.copyWith(
              fontWeight: FontWeight.bold,
              height: 1.25,
            ),
          ),
        ),
        const SizedBox(height: 18),
        for (var i = 0; i < currentQ.options.length; i++)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: SlateAnswerTile(
              letter: String.fromCharCode(65 + i),
              text: currentQ.options[i],
              selected: mySelection == i,
              correct: isRevealed && i == correctAnswer,
              feedback: isRevealed,
              onTap: () {
                if (!hasAnswered && !isRevealed && !_submittingAnswer) {
                  _submitAnswer(
                    currentIndex,
                    currentQ.id,
                    i,
                    challenge.currentQuestionOpenedAt,
                  );
                }
              },
            ),
          ),
        if (isRevealed && challenge.revealedExplanation != null) ...[
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: _slateSurface,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                color: _correct.withValues(alpha: .4),
                width: 1.2,
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(
                      Icons.menu_book_outlined,
                      color: _gold,
                      size: 20,
                    ),
                    const SizedBox(width: 8),
                    Text(
                      challenge.revealedScriptureReference?.isNotEmpty == true
                          ? challenge.revealedScriptureReference!
                          : (currentQ.scriptureReference.isNotEmpty
                                ? currentQ.scriptureReference
                                : 'Scripture Insight'),
                      style: const TextStyle(
                        color: _gold,
                        fontWeight: FontWeight.bold,
                        fontSize: 14,
                      ),
                    ),
                  ],
                ),
                if (challenge.revealedExplanation!.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Text(
                    challenge.revealedExplanation!,
                    style: const TextStyle(
                      color: _slateTextPrimary,
                      fontSize: 13,
                      height: 1.4,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
        const SizedBox(height: 20),
        if (isHost) ...[
          if (!isRevealed)
            SlatePillButton(
              label: _revealing ? 'REVEALING…' : 'REVEAL ANSWER',
              loading: _revealing,
              onPressed: _revealing ? null : _revealAnswer,
            )
          else
            SlatePillButton(
              label: _advancing
                  ? 'ADVANCING…'
                  : (isLast ? 'COMPLETE FELLOWSHIP' : 'NEXT QUESTION'),
              loading: _advancing,
              onPressed: _advancing ? null : _advanceQuestion,
            ),
        ] else ...[
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: _slateSurfaceVariant,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                if (!isRevealed && hasAnswered) ...[
                  const Icon(
                    Icons.check_circle_outline,
                    color: _gold,
                    size: 18,
                  ),
                  const SizedBox(width: 8),
                  const Text(
                    'Answer locked. Waiting for host to reveal…',
                    style: TextStyle(
                      color: _slateTextPrimary,
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ] else if (!isRevealed) ...[
                  const Icon(Icons.touch_app_outlined, color: _gold, size: 18),
                  const SizedBox(width: 8),
                  const Text(
                    'Select your answer before host reveals!',
                    style: TextStyle(color: _slateTextSecondary, fontSize: 13),
                  ),
                ] else ...[
                  const Icon(Icons.forum_outlined, color: _gold, size: 18),
                  const SizedBox(width: 8),
                  const Text(
                    'Host discussing Scripture. Ready for next!',
                    style: TextStyle(
                      color: _slateTextPrimary,
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildFinalizingView(GroupChallenge challenge) {
    final isHost =
        challenge.ownerId != null &&
        challenge.ownerId == _service.currentUser?.uid;
    return ListView(
      padding: const EdgeInsets.fromLTRB(22, 16, 22, 40),
      children: [
        const SlatePageHeader(title: 'FELLOWSHIP STUDY'),
        const SizedBox(height: 24),
        SlateCard(
          child: Column(
            children: [
              const Icon(Icons.hourglass_top, color: _gold, size: 48),
              const SizedBox(height: 12),
              const Text(
                'FINALIZING RESULTS…',
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: _gold,
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 1,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                isHost
                    ? 'The server is preparing the fellowship leaderboard. You can retry if this remains stuck.'
                    : 'The host is finishing the fellowship results. Please wait…',
                textAlign: TextAlign.center,
                style: const TextStyle(
                  color: _slateTextSecondary,
                  fontSize: 13,
                  height: 1.4,
                ),
              ),
            ],
          ),
        ),
        if (isHost) ...[
          const SizedBox(height: 20),
          SlatePillButton(
            label: _advancing ? 'RETRYING…' : 'RETRY FINALIZATION',
            loading: _advancing,
            onPressed: _advancing ? null : _advanceQuestion,
          ),
        ],
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: DivineBackground(
        reduceMotion: widget.store.reduceMotion,
        child: SafeArea(
          child: StreamBuilder<GroupChallenge>(
            stream: _challengeStream,
            initialData: widget.challenge,
            builder: (context, snapshot) {
              final challenge = snapshot.data ?? widget.challenge;
              if (challenge.isCompleted) {
                return _buildResultsView();
              }
              if (challenge.isFinalizing) {
                return _buildFinalizingView(challenge);
              }
              return _buildActiveQuestionView(challenge);
            },
          ),
        ),
      ),
    );
  }
}

class GroupQuestionScreen extends StatefulWidget {
  const GroupQuestionScreen({
    super.key,
    required this.store,
    required this.groupId,
    required this.challenge,
    this.service,
  });
  final ProgressStore store;
  final String groupId;
  final GroupChallenge challenge;
  final CloudGroupGateway? service;

  @override
  State<GroupQuestionScreen> createState() => _GroupQuestionScreenState();
}

class _GroupQuestionScreenState extends State<GroupQuestionScreen> {
  static const _questionLimitSeconds = 30;
  late final CloudGroupGateway _service;
  late final List<GroupChallengeItem> _questions;
  late final List<int> _answers;
  late final DateTime _sessionStartedAt;
  final Map<int, int> _questionElapsed = <int, int>{};
  final Set<int> _expiredQuestions = <int>{};
  int _currentIndex = 0;
  Timer? _timer;
  int _questionSeconds = 0;
  int _totalSeconds = 0;
  bool _submitting = false;
  GroupQuizResult? _result;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? CloudChallengeService();
    _questions = widget.challenge.items.isNotEmpty
        ? widget.challenge.items
        : [
            GroupChallengeItem(
              id: widget.challenge.id,
              question: widget.challenge.question,
              options: widget.challenge.options,
              scriptureReference: widget.challenge.scriptureReference,
              testament: '',
              propheticFocus: '',
            ),
          ];
    _answers = List.filled(_questions.length, -1);
    // Reopening a completed challenge is read-only: show its leaderboard
    // instead of starting question one again.
    if (widget.challenge.isCompleted) {
      _result = GroupQuizResult(
        challengeId: widget.challenge.id,
        score: 0,
        total: _questions.length,
        elapsedSeconds: 0,
        breakdown: const [],
      );
    }
    // The challenge's server timestamp anchors the total duration after a
    // reconnect. Per-question countdowns are local because competitive
    // challenges do not persist a question-open timestamp for each player.
    _sessionStartedAt = widget.challenge.startedAt ?? DateTime.now();
    _totalSeconds = max(
      0,
      DateTime.now().difference(_sessionStartedAt).inSeconds,
    );
    _questionElapsed[0] = 0;
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (mounted && _result == null) {
        // Tick counters keep the countdown deterministic even when the app
        // is rebuilt/offline. On a reconnect, _totalSeconds starts from the
        // server's startedAt timestamp above.
        final elapsed = (_questionElapsed[_currentIndex] ?? 0) + 1;
        _questionElapsed[_currentIndex] = elapsed;
        final reachedLimit = elapsed >= _questionLimitSeconds;
        final shouldAdvance =
            reachedLimit &&
            !_expiredQuestions.contains(_currentIndex) &&
            _currentIndex < _questions.length - 1;
        final expiredIndex = _currentIndex;
        setState(() {
          _questionSeconds = min(_questionLimitSeconds, max(0, elapsed));
          _totalSeconds++;
          if (reachedLimit) _expiredQuestions.add(_currentIndex);
        });
        if (shouldAdvance) {
          // Give the player a brief visual indication that the question
          // expired, then continue without allowing a late answer.
          Future<void>.delayed(const Duration(milliseconds: 350), () {
            if (!mounted || _result != null || _currentIndex != expiredIndex) {
              return;
            }
            setState(() {
              _currentIndex++;
              _questionSeconds = _questionElapsed[_currentIndex] ?? 0;
            });
          });
        }
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  String _formatTime(int seconds) {
    final m = seconds ~/ 60;
    final s = seconds % 60;
    return '${m.toString().padLeft(2, '0')}:${s.toString().padLeft(2, '0')}';
  }

  Future<void> _submitAll() async {
    if (_submitting || _result != null) return;
    setState(() => _submitting = true);
    _timer?.cancel();
    _totalSeconds = max(
      _totalSeconds,
      DateTime.now().difference(_sessionStartedAt).inSeconds,
    );
    try {
      final res = await _service.submitGroupQuiz(
        groupId: widget.groupId,
        challengeId: widget.challenge.id,
        answers: _answers,
        elapsedSeconds: _totalSeconds,
        displayName: widget.store.leaderboardName,
      );
      if (!mounted) return;
      setState(() {
        _result = res;
      });
      unawaited(
        res.score >= (res.total / 2).ceil()
            ? AnswerFeedback.correct()
            : AnswerFeedback.incorrect(),
      );
    } on FirebaseFunctionsException catch (error) {
      if (!mounted) return;
      if (error.code == 'already-exists') {
        setState(() {
          _result = GroupQuizResult(
            challengeId: widget.challenge.id,
            score: 0,
            total: _questions.length,
            elapsedSeconds: _totalSeconds,
            breakdown: const [],
          );
        });
        ScaffoldMessenger.maybeOf(context)?.showSnackBar(
          const SnackBar(
            content: Text(
              'You already submitted this challenge. Showing group leaderboard.',
            ),
          ),
        );
      } else {
        ScaffoldMessenger.maybeOf(context)?.showSnackBar(
          SnackBar(content: Text(cloudSubmitErrorMessage(error))),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.maybeOf(context)?.showSnackBar(
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

  Widget _buildQuizView() {
    final currentQ = _questions[_currentIndex];
    final hasAnswered = _answers[_currentIndex] >= 0;
    final isExpired = _expiredQuestions.contains(_currentIndex);
    final isLast = _currentIndex == _questions.length - 1;
    final answeredCount = _answers.where((a) => a >= 0).length;
    final completedCount = min(
      _questions.length,
      answeredCount +
          _expiredQuestions.where((index) => _answers[index] < 0).length,
    );
    final remainingSeconds = max(0, _questionLimitSeconds - _questionSeconds);

    return ListView(
      padding: const EdgeInsets.fromLTRB(22, 16, 22, 40),
      children: [
        const SlatePageHeader(title: 'GROUP CHALLENGE'),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          decoration: BoxDecoration(
            color: _slateSurface,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: Colors.white.withValues(alpha: .12)),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  const Icon(Icons.timer_outlined, color: _gold, size: 18),
                  const SizedBox(width: 6),
                  Text(
                    isExpired
                        ? 'TIME EXPIRED • Total: ${_formatTime(_totalSeconds)}'
                        : 'Q LEFT: ${_formatTime(remainingSeconds)} • Total: ${_formatTime(_totalSeconds)}',
                    style: const TextStyle(
                      color: _slateTextPrimary,
                      fontWeight: FontWeight.bold,
                      fontSize: 13,
                    ),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: _gold.withValues(alpha: .15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  'QUESTION ${_currentIndex + 1} OF ${_questions.length}',
                  style: const TextStyle(
                    color: _gold,
                    fontWeight: FontWeight.bold,
                    fontSize: 12,
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 10),
        ClipRRect(
          borderRadius: BorderRadius.circular(4),
          child: LinearProgressIndicator(
            value: (_currentIndex + 1) / _questions.length,
            backgroundColor: Colors.white.withValues(alpha: .08),
            valueColor: const AlwaysStoppedAnimation<Color>(_gold),
            minHeight: 5,
          ),
        ),
        const SizedBox(height: 18),
        SlateCard(
          child: Text(
            currentQ.question,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.titleLarge?.copyWith(
              fontWeight: FontWeight.bold,
              height: 1.25,
            ),
          ),
        ),
        const SizedBox(height: 18),
        for (var i = 0; i < currentQ.options.length; i++)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: SlateAnswerTile(
              letter: String.fromCharCode(65 + i),
              text: currentQ.options[i],
              selected: _answers[_currentIndex] == i,
              correct: false,
              feedback: false,
              onTap: () {
                if (!_submitting && !isExpired) {
                  setState(() {
                    _answers[_currentIndex] = i;
                  });
                }
              },
            ),
          ),
        const SizedBox(height: 16),
        Row(
          children: [
            if (_currentIndex > 0) ...[
              Expanded(
                child: SlatePillButton(
                  label: 'PREVIOUS',
                  inverse: true,
                  onPressed: () {
                    setState(() {
                      _currentIndex--;
                      _questionSeconds = _questionElapsed[_currentIndex] ?? 0;
                    });
                  },
                ),
              ),
              const SizedBox(width: 12),
            ],
            if (!isLast)
              Expanded(
                child: SlatePillButton(
                  label: 'NEXT QUESTION',
                  onPressed: hasAnswered || isExpired
                      ? () {
                          setState(() {
                            _currentIndex++;
                            _questionSeconds =
                                _questionElapsed[_currentIndex] ?? 0;
                          });
                        }
                      : null,
                ),
              )
            else
              Expanded(
                child: SlatePillButton(
                  label: _submitting
                      ? 'SUBMITTING…'
                      : 'SUBMIT GROUP QUIZ ($completedCount/${_questions.length})',
                  loading: _submitting,
                  onPressed: completedCount == _questions.length && !_submitting
                      ? _submitAll
                      : null,
                ),
              ),
          ],
        ),
      ],
    );
  }

  Widget _buildResultsView() {
    final res = _result!;
    final pct = ((res.score / res.total) * 100).round();

    return ListView(
      padding: const EdgeInsets.fromLTRB(22, 16, 22, 40),
      children: [
        const SlatePageHeader(title: 'CHALLENGE RESULTS'),
        const SizedBox(height: 16),
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: _slateSurface,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: _gold.withValues(alpha: .5), width: 1.5),
          ),
          child: Column(
            children: [
              const Icon(Icons.stars, color: _gold, size: 48),
              const SizedBox(height: 8),
              Text(
                res.breakdown.isEmpty
                    ? 'CHALLENGE COMPLETE'
                    : '${res.score} / ${res.total} CORRECT',
                style: const TextStyle(
                  color: _gold,
                  fontSize: 26,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 1.1,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                res.breakdown.isEmpty
                    ? 'Waiting for final rankings…'
                    : '$pct% Score • Total Time: ${_formatTime(res.elapsedSeconds)}',
                style: const TextStyle(
                  color: _slateTextSecondary,
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 24),
        const SlateSectionHeader('GROUP LEADERBOARD'),
        const SizedBox(height: 10),
        StreamBuilder<List<LeaderboardEntry>>(
          stream: _service.groupLeaderboard(
            widget.groupId,
            widget.challenge.id,
          ),
          builder: (context, snapshot) {
            if (snapshot.hasError) {
              return const Center(child: Text('Leaderboard unavailable.'));
            }
            if (!snapshot.hasData) {
              return const Center(
                child: Padding(
                  padding: EdgeInsets.all(16),
                  child: CircularProgressIndicator(color: _gold),
                ),
              );
            }
            final entries = snapshot.data!;
            if (entries.isEmpty) {
              return const Text(
                'No scores submitted yet. Your score has been recorded!',
                textAlign: TextAlign.center,
                style: TextStyle(color: _slateTextSecondary),
              );
            }
            return ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: entries.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final entry = entries[index];
                final isTop3 = index < 3;
                return Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 14,
                    vertical: 10,
                  ),
                  decoration: BoxDecoration(
                    color: _slateSurface,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                      color: isTop3
                          ? _gold.withValues(alpha: .4)
                          : Colors.white.withValues(alpha: .08),
                    ),
                  ),
                  child: Row(
                    children: [
                      Text(
                        '#${index + 1}',
                        style: TextStyle(
                          color: isTop3 ? _gold : _slateTextSecondary,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          entry.displayName,
                          style: const TextStyle(
                            color: _slateTextPrimary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                      Text(
                        '${entry.score} pts • ${_formatTime(entry.elapsedSeconds)}',
                        style: const TextStyle(
                          color: _slateTextSecondary,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                );
              },
            );
          },
        ),
        if (res.breakdown.isNotEmpty) ...[
          const SizedBox(height: 28),
          SlateSectionHeader(
            'QUESTION REVIEW (${_questions.length} QUESTIONS)',
          ),
          const SizedBox(height: 12),
          for (var i = 0; i < _questions.length; i++) ...[
            _buildQuestionReviewCard(i, res),
            const SizedBox(height: 14),
          ],
        ],
        const SizedBox(height: 16),
        SlatePillButton(
          label: 'BACK TO GROUP',
          onPressed: () => Navigator.of(context).pop(),
        ),
      ],
    );
  }

  Widget _buildQuestionReviewCard(int index, GroupQuizResult res) {
    final q = _questions[index];
    QuestionReview? review;
    if (index < res.breakdown.length) {
      review = res.breakdown[index];
    }
    final isCorrect = review?.correct ?? false;
    final userAns = review != null ? review.userAnswer : _answers[index];
    final correctAns = review?.correctAnswer ?? 0;
    final explanation = review != null && review.explanation.isNotEmpty
        ? review.explanation
        : (index == 0 ? widget.challenge.explanation : '');
    final scripture = review != null && review.scriptureReference.isNotEmpty
        ? review.scriptureReference
        : q.scriptureReference;

    final userAnsText = (userAns >= 0 && userAns < q.options.length)
        ? q.options[userAns]
        : 'No answer';
    final correctAnsText = (correctAns >= 0 && correctAns < q.options.length)
        ? q.options[correctAns]
        : '';

    return SlateCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                  color: (isCorrect ? _correct : _wrong).withValues(alpha: .2),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                    color: (isCorrect ? _correct : _wrong).withValues(
                      alpha: .5,
                    ),
                  ),
                ),
                child: Text(
                  isCorrect ? 'CORRECT' : 'INCORRECT',
                  style: TextStyle(
                    color: isCorrect ? _correct : _wrong,
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const Spacer(),
              Text(
                'QUESTION ${index + 1}',
                style: const TextStyle(
                  color: _slateTextMuted,
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            q.question,
            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _ReviewAnswerBox(
                  label: 'YOUR ANSWER',
                  text: userAnsText,
                  color: isCorrect ? _correct : _wrong,
                ),
              ),
              if (!isCorrect && correctAnsText.isNotEmpty) ...[
                const SizedBox(width: 8),
                Expanded(
                  child: _ReviewAnswerBox(
                    label: 'CORRECT ANSWER',
                    text: correctAnsText,
                    color: _correct,
                  ),
                ),
              ],
            ],
          ),
          if (explanation.isNotEmpty) ...[
            const SizedBox(height: 10),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(Icons.info_outline, size: 15, color: _gold),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    explanation,
                    style: const TextStyle(
                      color: _slateTextSecondary,
                      fontSize: 12,
                    ),
                  ),
                ),
              ],
            ),
          ],
          if (scripture.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(
              'SCRIPTURE: $scripture',
              style: const TextStyle(
                color: _gold,
                fontSize: 11,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    body: DivineBackground(
      reduceMotion: widget.store.reduceMotion,
      child: SafeArea(
        child: _result != null ? _buildResultsView() : _buildQuizView(),
      ),
    ),
  );
}

class _GoogleSignInSheet extends StatefulWidget {
  const _GoogleSignInSheet({required this.service, required this.store});
  final CloudChallengeService service;
  final ProgressStore store;

  @override
  State<_GoogleSignInSheet> createState() => _GoogleSignInSheetState();
}

class _GoogleSignInSheetState extends State<_GoogleSignInSheet> {
  bool _loading = false;
  String? _error;

  Future<void> _signIn() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final user = await widget.service.signInWithGoogle();
      if (!mounted) return;
      if (user != null) {
        if (user.displayName != null && user.displayName!.isNotEmpty) {
          unawaited(widget.store.setLeaderboardName(user.displayName!));
        }
        Navigator.of(context).pop(true);
      } else {
        setState(() {
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _loading = false;
          _error = 'Google Sign-In failed. Please try again.';
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: _slateSurface,
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 36),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: .2),
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(height: 24),
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              color: _gold.withValues(alpha: .15),
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.public_outlined, color: _gold, size: 28),
          ),
          const SizedBox(height: 16),
          const Text(
            'Online Faith Challenge',
            style: TextStyle(
              color: _slateTextPrimary,
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Sign in with your Google account to track your verified progress, level up, and rank on the global leaderboard with your unique username.',
            textAlign: TextAlign.center,
            style: TextStyle(color: _slateTextSecondary, height: 1.35),
          ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            Text(
              _error!,
              textAlign: TextAlign.center,
              style: const TextStyle(color: _wrong, fontSize: 13),
            ),
          ],
          const SizedBox(height: 24),
          SlatePillButton(
            label: _loading ? 'SIGNING IN…' : 'SIGN IN WITH GOOGLE',
            icon: Icons.login_rounded,
            loading: _loading,
            onPressed: _loading ? null : _signIn,
          ),
          const SizedBox(height: 10),
          TextButton(
            onPressed: _loading ? null : () => Navigator.of(context).pop(false),
            child: const Text(
              'Cancel',
              style: TextStyle(color: _slateTextSecondary),
            ),
          ),
        ],
      ),
    );
  }
}

class _ClaimUsernameDialog extends StatefulWidget {
  const _ClaimUsernameDialog({required this.service, required this.store});
  final CloudGroupGateway service;
  final ProgressStore store;

  @override
  State<_ClaimUsernameDialog> createState() => _ClaimUsernameDialogState();
}

class _ClaimUsernameDialogState extends State<_ClaimUsernameDialog> {
  late final TextEditingController _controller;
  bool _submitting = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    final current = widget.store.leaderboardName;
    final initial = current != 'Faith learner' && current.isNotEmpty
        ? current.replaceAll(RegExp(r'[^a-zA-Z0-9_]'), '')
        : (widget.service.currentUser?.displayName ?? '').replaceAll(
            RegExp(r'[^a-zA-Z0-9_]'),
            '',
          );
    _controller = TextEditingController(text: initial);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _claim() async {
    final raw = _controller.text.trim();
    if (raw.length < 3 || raw.length > 20) {
      setState(() => _error = 'Username must be between 3 and 20 characters.');
      return;
    }
    if (!RegExp(r'^[a-zA-Z0-9_]+$').hasMatch(raw)) {
      setState(
        () => _error = 'Only letters, numbers, and underscores allowed.',
      );
      return;
    }

    setState(() {
      _submitting = true;
      _error = null;
    });

    try {
      await widget.service.claimUsername(raw);
      await widget.store.setLeaderboardName(raw);
      if (mounted) Navigator.of(context).pop(true);
    } on FirebaseFunctionsException catch (e) {
      if (mounted) {
        setState(() {
          _submitting = false;
          _error =
              e.message ??
              'This username is already taken. Please choose another.';
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _submitting = false;
          _error = 'Could not claim username. Please try another.';
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: _slateSurface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(24),
        side: BorderSide(color: _gold.withValues(alpha: .3)),
      ),
      title: const Row(
        children: [
          Icon(Icons.badge_outlined, color: _gold, size: 24),
          SizedBox(width: 10),
          Text(
            'Choose Username',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
        ],
      ),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Your unique username will appear on the global leaderboard. No other player can use your username.',
            style: TextStyle(
              color: _slateTextSecondary,
              fontSize: 13,
              height: 1.3,
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _controller,
            maxLength: 20,
            style: const TextStyle(
              color: _slateTextPrimary,
              fontWeight: FontWeight.bold,
            ),
            decoration: InputDecoration(
              prefixText: '@ ',
              prefixStyle: const TextStyle(
                color: _gold,
                fontWeight: FontWeight.bold,
              ),
              hintText: 'username',
              hintStyle: const TextStyle(color: _slateTextMuted),
              filled: true,
              fillColor: _slateSurfaceVariant,
              counterText: '',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: BorderSide(
                  color: Colors.white.withValues(alpha: .1),
                ),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: const BorderSide(color: _gold, width: 2),
              ),
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 8),
            Text(_error!, style: const TextStyle(color: _wrong, fontSize: 12)),
          ],
        ],
      ),
      actions: [
        TextButton(
          onPressed: _submitting
              ? null
              : () => Navigator.of(context).pop(false),
          child: const Text(
            'Cancel',
            style: TextStyle(color: _slateTextSecondary),
          ),
        ),
        FilledButton(
          onPressed: _submitting ? null : _claim,
          style: FilledButton.styleFrom(
            backgroundColor: _gold,
            foregroundColor: _slateBottom,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
          child: _submitting
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: _slateBottom,
                  ),
                )
              : const Text(
                  'CLAIM USERNAME',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
        ),
      ],
    );
  }
}

class LeaderboardScreen extends StatefulWidget {
  const LeaderboardScreen({
    super.key,
    required this.store,
    this.challengeId,
    this.service,
  });

  final ProgressStore store;
  final String? challengeId;
  final CloudGroupGateway? service;

  @override
  State<LeaderboardScreen> createState() => _LeaderboardScreenState();
}

class _LeaderboardScreenState extends State<LeaderboardScreen> {
  late final CloudGroupGateway _service;
  int _activeTab = 0;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? CloudChallengeService();
    if (widget.challengeId != null) {
      _activeTab = 1;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: DivineBackground(
        reduceMotion: widget.store.reduceMotion,
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 16),
            child: ListView(
              children: [
                const SlatePageHeader(title: 'LEADERBOARD'),
                const SizedBox(height: 8),
                const Text(
                  'Verified rankings, levels, and achievements across online challenges.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: _slateTextSecondary),
                ),
                const SizedBox(height: 20),
                Container(
                  padding: const EdgeInsets.all(4),
                  decoration: BoxDecoration(
                    color: _slateSurface,
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(
                      color: Colors.white.withValues(alpha: .08),
                    ),
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: GestureDetector(
                          onTap: () => setState(() => _activeTab = 0),
                          child: Container(
                            padding: const EdgeInsets.symmetric(vertical: 10),
                            decoration: BoxDecoration(
                              color: _activeTab == 0
                                  ? _gold
                                  : Colors.transparent,
                              borderRadius: BorderRadius.circular(16),
                            ),
                            child: Text(
                              'GLOBAL CHALLENGE',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                color: _activeTab == 0
                                    ? _slateBottom
                                    : _slateTextSecondary,
                                fontWeight: FontWeight.bold,
                                fontSize: 12,
                                letterSpacing: .8,
                              ),
                            ),
                          ),
                        ),
                      ),
                      Expanded(
                        child: GestureDetector(
                          onTap: () => setState(() => _activeTab = 1),
                          child: Container(
                            padding: const EdgeInsets.symmetric(vertical: 10),
                            decoration: BoxDecoration(
                              color: _activeTab == 1
                                  ? _gold
                                  : Colors.transparent,
                              borderRadius: BorderRadius.circular(16),
                            ),
                            child: Text(
                              'DAILY QUESTION',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                color: _activeTab == 1
                                    ? _slateBottom
                                    : _slateTextSecondary,
                                fontWeight: FontWeight.bold,
                                fontSize: 12,
                                letterSpacing: .8,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),
                if (_activeTab == 0)
                  _GlobalLeaderboard(service: _service)
                else if (widget.challengeId != null)
                  _VerifiedLeaderboard(challengeId: widget.challengeId!)
                else
                  FutureBuilder<CloudChallenge?>(
                    future: _service.loadToday(),
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
                          title: 'No question ranking yet',
                          message:
                              'Once today’s question is answered, rankings will appear here.',
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

class _GlobalLeaderboard extends StatelessWidget {
  const _GlobalLeaderboard({required this.service});
  final CloudGroupGateway service;

  @override
  Widget build(BuildContext context) {
    final currentUid = service.currentUser?.uid;
    return StreamBuilder<List<LeaderboardEntry>>(
      stream: service.globalLeaderboard(),
      builder: (context, snapshot) {
        if (snapshot.hasError) {
          return _CloudMessageCard(
            icon: Icons.cloud_off_outlined,
            title: 'Rankings unavailable',
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
            title: 'Be the First Global Champion',
            message:
                'Play the Cloud Challenge online questions to claim the #1 spot on the global leaderboard!',
            actionLabel: 'CLOSE',
            onPressed: () => Navigator.of(context).pop(),
          );
        }
        return SlateCard(
          padding: const EdgeInsets.symmetric(vertical: 10),
          child: Column(
            children: [
              for (var index = 0; index < entries.length; index++)
                _GlobalLeaderboardRow(
                  entry: entries[index],
                  rank: index + 1,
                  isCurrentUser: entries[index].id == currentUid,
                ),
            ],
          ),
        );
      },
    );
  }
}

class _GlobalLeaderboardRow extends StatelessWidget {
  const _GlobalLeaderboardRow({
    required this.entry,
    required this.rank,
    required this.isCurrentUser,
  });
  final LeaderboardEntry entry;
  final int rank;
  final bool isCurrentUser;

  @override
  Widget build(BuildContext context) {
    final accent = switch (rank) {
      1 => _gold,
      2 => const Color(0xFFE2E8F0),
      3 => const Color(0xFFC98D5B),
      _ => _slateTextSecondary,
    };
    return Container(
      decoration: isCurrentUser
          ? BoxDecoration(
              color: _gold.withValues(alpha: .08),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: _gold.withValues(alpha: .3)),
            )
          : null,
      margin: isCurrentUser
          ? const EdgeInsets.symmetric(horizontal: 6, vertical: 2)
          : EdgeInsets.zero,
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: accent.withValues(alpha: .16),
          child: Text(
            '$rank',
            style: TextStyle(color: accent, fontWeight: FontWeight.bold),
          ),
        ),
        title: Row(
          children: [
            Expanded(
              child: Text(
                entry.displayName,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: isCurrentUser ? _gold : _slateTextPrimary,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            if (isCurrentUser)
              Container(
                margin: const EdgeInsets.only(left: 6),
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                decoration: BoxDecoration(
                  color: _gold,
                  borderRadius: BorderRadius.circular(6),
                ),
                child: const Text(
                  'YOU',
                  style: TextStyle(
                    color: _slateBottom,
                    fontSize: 9,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
          ],
        ),
        subtitle: Text(
          'Level ${entry.level} • ${entry.totalAnswered} Questions${entry.accuracy > 0 ? ' • ${entry.accuracy}% Acc' : ''}',
          style: const TextStyle(color: _slateTextSecondary, fontSize: 11),
        ),
        trailing: Text(
          '${entry.score} pts',
          style: TextStyle(
            color: accent,
            fontSize: 16,
            fontWeight: FontWeight.bold,
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
        '${entry.score} pt',
        style: TextStyle(
          color: accent,
          fontSize: 16,
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
    final service = CloudChallengeService();
    final user = service.currentUser;
    if (user == null || user.isAnonymous) {
      final signedIn = await showModalBottomSheet<bool>(
        context: context,
        backgroundColor: Colors.transparent,
        isScrollControlled: true,
        builder: (sheetContext) =>
            _GoogleSignInSheet(service: service, store: store),
      );
      if (signedIn != true || !context.mounted) return;
    }
    await showDialog<bool>(
      context: context,
      builder: (dialogContext) =>
          _ClaimUsernameDialog(service: service, store: store),
    );
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: store,
    builder: (context, _) => Scaffold(
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
                icon: Icons.palette_outlined,
              ),
              SlateSettingCard(
                title: 'Text Size',
                subtitle: store.textScale == 'large' ? 'Large text' : 'Normal',
                icon: Icons.format_size,
                onTap: () => _choose(context, 'Text Size', [
                  _Choice(
                    'Normal',
                    'normal',
                    store.textScale == 'normal',
                    () => store.setTextScale('normal'),
                  ),
                  _Choice(
                    'Large text',
                    'large',
                    store.textScale == 'large',
                    () => store.setTextScale('large'),
                  ),
                ]),
              ),
              SlateSwitchSettingCard(
                title: 'Reduced Motion',
                subtitle: 'Minimise background and transition animations',
                icon: Icons.motion_photos_off_outlined,
                value: store.reduceMotion,
                onChanged: (value) => store.setReduceMotion(value),
              ),
              const SizedBox(height: 20),
              const SlateSectionHeader('STUDY & PROGRESS'),
              SlateSettingCard(
                title: 'Cloud Backup',
                subtitle: store.cloudSyncStatus,
                icon: Icons.cloud_sync_outlined,
                onTap: () => store.cloudBackupEnabled
                    ? store.disableCloudBackup()
                    : store.enableCloudBackup(),
              ),
              SlateSettingCard(
                title: 'Quiz Username',
                subtitle:
                    store.leaderboardName != 'Faith learner' &&
                        store.leaderboardName.isNotEmpty
                    ? '@${store.leaderboardName}'
                    : 'Claim unique username',
                icon: Icons.badge_outlined,
                onTap: () => _editLeaderboardName(context),
              ),
              SlateSwitchSettingCard(
                title: 'Daily Reminders',
                subtitle: store.reminderStatus,
                icon: Icons.alarm_outlined,
                value: store.remindersEnabled,
                onChanged: (value) => store.setRemindersEnabled(value),
              ),
              const SizedBox(height: 20),
              const SlateSectionHeader('DATA'),
              SlateSettingCard(
                title: 'Reset Progress',
                subtitle: 'Clear unlocks, scores, streaks, and saved sessions',
                icon: Icons.delete_outline,
                onTap: () async {
                  final confirmed = await showDialog<bool>(
                    context: context,
                    builder: (context) => AlertDialog(
                      backgroundColor: _slateSurface,
                      title: const Text('Reset All Progress?'),
                      content: const Text(
                        'This deletes unlocked levels, high scores, streaks, saved sessions, and mistake logs on this device.',
                      ),
                      actions: [
                        TextButton(
                          onPressed: () => Navigator.pop(context, false),
                          child: const Text('CANCEL'),
                        ),
                        FilledButton(
                          onPressed: () => Navigator.pop(context, true),
                          style: FilledButton.styleFrom(
                            backgroundColor: _wrong,
                          ),
                          child: const Text('RESET'),
                        ),
                      ],
                    ),
                  );
                  if (confirmed == true) {
                    await store.reset();
                    if (context.mounted) {
                      ScaffoldMessenger.maybeOf(context)?.showSnackBar(
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
              const SizedBox(height: 20),
              const SlateSectionHeader('ABOUT'),
              SlateSettingCard(
                title: 'Version',
                subtitle: 'Faith Quiz 1.0.0 (Build 1)',
                icon: Icons.info_outline,
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
