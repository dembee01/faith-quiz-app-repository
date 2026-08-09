import 'package:flutter/widgets.dart';

import 'app.dart';
import 'progress_store.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final store = ProgressStore();
  await store.load();
  runApp(FaithQuizApp(store: store));
}
