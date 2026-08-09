import 'package:faithquiz/app.dart';
import 'package:faithquiz/models.dart';
import 'package:faithquiz/progress_store.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('Faith Quiz starts with the original slate splash and opens the menu', (tester) async {
    final store = ProgressStore();
    await tester.pumpWidget(FaithQuizApp(store: store));

    expect(find.text('GET STARTED'), findsOneWidget);
    await tester.tap(find.text('GET STARTED'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));

    expect(find.text('Daily Challenge'), findsOneWidget);
    expect(find.text('The Covenant Journey'), findsOneWidget);
    expect(find.text('Adaptive Levels'), findsOneWidget);
  });

  testWidgets('quiz selection is confirmed before correctness feedback is shown', (tester) async {
    const question = QuizQuestion(
      question: 'What is the first book of the Bible?',
      options: ['Genesis', 'Exodus', 'Psalms', 'Matthew'],
      correctAnswer: 0,
      explanation: 'Genesis begins the biblical story.',
    );
    final store = ProgressStore();
    await tester.pumpWidget(MaterialApp(home: QuizScreen(store: store, level: 1, questions: const [question])));

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
  });

  testWidgets('quiz timer tracks total elapsed time while an answer is selected', (tester) async {
    const question = QuizQuestion(
      question: 'What is the first book of the Bible?',
      options: ['Genesis', 'Exodus', 'Psalms', 'Matthew'],
      correctAnswer: 0,
      explanation: 'Genesis begins the biblical story.',
    );
    final store = ProgressStore();
    await tester.pumpWidget(MaterialApp(home: QuizScreen(store: store, level: 1, questions: const [question])));

    await tester.tap(find.text('Genesis'));
    await tester.pump(const Duration(seconds: 2));

    expect(find.text('00:02'), findsOneWidget);
    expect(find.text('2s'), findsOneWidget);
  });
}
