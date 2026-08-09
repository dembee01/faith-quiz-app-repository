import 'package:flutter_test/flutter_test.dart';

import 'package:faithquiz/app.dart';
import 'package:faithquiz/progress_store.dart';

void main() {
  testWidgets('Faith Quiz home renders', (tester) async {
    final store = ProgressStore();
    await tester.pumpWidget(FaithQuizApp(store: store));

    expect(find.text('FAITH QUIZ'), findsOneWidget);
    expect(find.text('Continue level 1'), findsOneWidget);
    expect(find.text('Daily challenge  •  0 day streak'), findsOneWidget);
  });
}
