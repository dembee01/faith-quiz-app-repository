import 'package:firebase_remote_config/firebase_remote_config.dart';

/// Remote Config changes safe presentation and catalogue-selection values.
/// Question bodies remain in Firestore, never in Remote Config.
class RemoteFeatureService {
  RemoteFeatureService._();

  static final instance = RemoteFeatureService._();
  final _config = FirebaseRemoteConfig.instance;

  bool get cloudChallengesEnabled => _config.getBool('cloud_challenges_enabled');
  String get activeCloudCatalogue => _config.getString('active_cloud_catalogue');

  Future<void> refresh() async {
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
    await _config.fetchAndActivate();
  }
}
