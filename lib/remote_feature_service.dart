import 'package:firebase_remote_config/firebase_remote_config.dart';

/// Remote Config changes safe presentation and catalogue-selection values.
/// Question bodies remain in Firestore, never in Remote Config.
class RemoteFeatureService {
  RemoteFeatureService._();

  static final instance = RemoteFeatureService._();
  FirebaseRemoteConfig get _config => FirebaseRemoteConfig.instance;

  static const _refreshInterval = Duration(minutes: 15);

  bool _prepared = false;
  bool _inFlight = false;
  DateTime _lastAttempt = DateTime.fromMillisecondsSinceEpoch(0);
  DateTime get lastFetchedAt => _lastAttempt;

  bool get cloudChallengesEnabled => _config.getBool('cloud_challenges_enabled');
  String get activeCloudCatalogue => _config.getString('active_cloud_catalogue');

  /// Fetches remote flags. Safe to call repeatedly: setup runs once, results
  /// are throttled to [_refreshInterval], and failures leave safe defaults in
  /// place so callers can decide what to do while offline.
  Future<void> refresh({bool force = false}) async {
    if (_inFlight) return;
    if (!force &&
        _lastAttempt.isAfter(
          DateTime.now().subtract(_refreshInterval),
        )) {
      return;
    }
    _inFlight = true;
    try {
      if (!_prepared) {
        await _config.setDefaults(const <String, Object>{
          'cloud_challenges_enabled': false,
          'active_cloud_catalogue': 'faith-quiz-global-v1',
        });
        await _config.setConfigSettings(
          RemoteConfigSettings(
            fetchTimeout: const Duration(seconds: 10),
            minimumFetchInterval: const Duration(hours: 12),
          ),
        );
        _prepared = true;
      }
      await _config.fetchAndActivate();
      _lastAttempt = DateTime.now();
    } catch (_) {
      // Keep the last known values; the next call retries after the throttle
      // window so features recover as soon as connectivity returns.
    } finally {
      _inFlight = false;
    }
  }

  /// Refreshes only when the gate is currently closed, so a player who opened
  /// the app offline sees cloud features appear without restarting.
  Future<void> refreshIfDisabled() async {
    if (cloudChallengesEnabled) return;
    await refresh();
  }
}
