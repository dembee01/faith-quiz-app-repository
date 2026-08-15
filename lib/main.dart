import 'dart:async';

import 'package:firebase_app_check/firebase_app_check.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';

import 'app.dart';
import 'firebase_options.dart';
import 'progress_store.dart';
import 'remote_feature_service.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // Firebase is optional at launch: the complete offline quiz remains usable
  // if a device is offline or a cloud service is temporarily unavailable.
  try {
    await Firebase.initializeApp(
      options: DefaultFirebaseOptions.currentPlatform,
    );
    // App Check and Remote Config must never hold the launch screen while a
    // player is offline. Safe defaults keep cloud play disabled until the
    // background refresh completes.
    unawaited(_configureFirebaseServices());
  } catch (_) {}
  final store = ProgressStore();
  await store.load();
  runApp(FaithQuizApp(store: store));
}

Future<void> _configureFirebaseServices() async {
  try {
    await FirebaseAppCheck.instance.activate(
      providerAndroid: kDebugMode
          ? const AndroidDebugProvider()
          : const AndroidPlayIntegrityProvider(),
      providerApple: kDebugMode
          ? const AppleDebugProvider()
          : const AppleAppAttestWithDeviceCheckFallbackProvider(),
    );
    await RemoteFeatureService.instance.refresh();
  } catch (_) {}
}
