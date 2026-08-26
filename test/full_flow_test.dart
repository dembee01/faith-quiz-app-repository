import 'package:faithquiz/app.dart';
import 'package:faithquiz/question_bank.dart';
import 'package:faithquiz/progress_store.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues(<String, Object>{});
  });

  testWidgets('playing level 1 to a passing score opens level 2 next',
      (tester) async {
    final store = ProgressStore();
    await tester.pumpWidget(
      MaterialApp(home: QuizScreen(store: store, level: 1)),
    );
    await tester.pump();

    final total = QuestionBank.forLevel(1).length;
    for (var question = 0; question < total; question++) {
      final tiles = tester
          .widgetList<SlateAnswerTile>(find.byType(SlateAnswerTile))
          .toList();
      final correctTile =
          tiles.firstWhere((tile) => tile.correct, orElse: () => tiles.first);
      await tester.tap(find.byWidget(correctTile));
      await tester.pump();
      await tester.ensureVisible(find.text('SUBMIT ANSWER'));
      await tester.tap(find.text('SUBMIT ANSWER'));
      await tester.pump();
      final isFinal = question == total - 1;
      await tester.ensureVisible(
        find.text(isFinal ? 'FINISH QUIZ' : 'NEXT QUESTION'),
      );
      await tester.tap(find.text(isFinal ? 'FINISH QUIZ' : 'NEXT QUESTION'));
      await tester.pump();
    }

    await tester.pump(const Duration(milliseconds: 50));
    expect(store.highestUnlocked, 2);
    expect(find.text('LEVEL 1 COMPLETE'), findsOneWidget);
    expect(find.text('NEXT'), findsOneWidget);

    await tester.tap(find.text('NEXT'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));
    expect(store.session, isNull);
  });

  testWidgets('covenant journey locks every node above highestUnlocked',
      (tester) async {
    tester.view.physicalSize = const Size(1080, 6200);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final store = ProgressStore();
    store.highestUnlocked = 8;
    await tester.pumpWidget(MaterialApp(home: JourneyScreen(store: store)));
    await tester.pump();
    final tiles = tester.widgetList<InkWell>(
      find.descendant(of: find.byType(ListView), matching: find.byType(InkWell)),
    ).toList();
    expect(tiles.length, 30);
    for (var i = 0; i < 30; i++) {
      expect(tiles[i].onTap != null, i < 8,
          reason: 'journey node ${i + 1} should be ${i < 8 ? 'open' : 'locked'}');
    }
  });

  testWidgets('an interrupted level resumes instead of restarting',
      (tester) async {
    final store = ProgressStore();
    await store.saveQuizSession(
      const QuizSession(
        level: 3,
        mode: 'classic',
        index: 2,
        score: 2,
        elapsedSeconds: 30,
        seed: 42,
        lives: 0,
        remainingSeconds: 0,
      ),
    );
    await tester.pumpWidget(
      MaterialApp(home: QuizScreen(store: store, level: 3)),
    );
    await tester.pump();
    expect(find.text('RESUMED SESSION'), findsOneWidget);
  });
}
