import 'dart:io' show Platform;

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';

/// Opt-in device reminder registration. No token is collected until the player
/// explicitly enables this setting, and the server uses it only for the daily
/// Faith Quiz reminder scheduled by the Firebase backend.
class ReminderService {
  ReminderService({
    FirebaseAuth? auth,
    FirebaseFirestore? firestore,
    FirebaseMessaging? messaging,
  }) : _auth = auth ?? FirebaseAuth.instance,
       _firestore = firestore ?? FirebaseFirestore.instance,
       _messaging = messaging ?? FirebaseMessaging.instance;

  final FirebaseAuth _auth;
  final FirebaseFirestore _firestore;
  final FirebaseMessaging _messaging;

  Future<void> enable() async {
    final settings = await _messaging.requestPermission(
      alert: true,
      badge: true,
      sound: true,
    );
    if (settings.authorizationStatus == AuthorizationStatus.denied) {
      throw StateError('Notifications were not allowed on this device.');
    }
    await _messaging.setAutoInitEnabled(true);
    final token = await _messaging.getToken();
    if (token == null || token.isEmpty) {
      throw StateError('A notification token is not available yet.');
    }
    final user = _auth.currentUser ?? (await _auth.signInAnonymously()).user!;
    await _firestore
        .collection('users')
        .doc(user.uid)
        .collection('private')
        .doc('notifications')
        .set(<String, dynamic>{
          'remindersEnabled': true,
          'fcmToken': token,
          'platform': _platform,
          'updatedAt': FieldValue.serverTimestamp(),
        });
  }

  Future<void> disable() async {
    await _messaging.setAutoInitEnabled(false);
    await _messaging.deleteToken();
    final user = _auth.currentUser;
    if (user == null) return;
    await _firestore
        .collection('users')
        .doc(user.uid)
        .collection('private')
        .doc('notifications')
        .set(<String, dynamic>{
          'remindersEnabled': false,
          'fcmToken': '',
          'platform': _platform,
          'updatedAt': FieldValue.serverTimestamp(),
        });
  }

  String get _platform {
    if (kIsWeb) return 'web';
    if (Platform.isIOS) return 'ios';
    if (Platform.isAndroid) return 'android';
    return 'other';
  }
}
