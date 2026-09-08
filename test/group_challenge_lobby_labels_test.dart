import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:firebase_auth/firebase_auth.dart';

import 'package:faithquiz/app.dart';
import 'package:faithquiz/cloud_challenge_service.dart';
import 'package:faithquiz/progress_store.dart';

class _LobbyGateway extends Fake implements CloudGroupGateway {
  _LobbyGateway({
    required this.group,
    required this.challenge,
    required this.members,
    this.memberStream,
  });

  final QuizGroup group;
  final GroupChallenge challenge;
  final List<GroupMember> members;
  final Stream<List<GroupMember>>? memberStream;

  @override
  User? get currentUser => null;

  @override
  Future<bool> get isAvailable async => true;

  @override
  Stream<GroupChallenge> streamChallenge(String groupId, String challengeId) =>
      Stream.value(challenge);

  @override
  Stream<QuizGroup> streamGroup(String groupId) => Stream.value(group);

  @override
  Stream<List<GroupMember>> streamGroupMembers(String groupId) =>
      memberStream ?? Stream.value(members);

  @override
  Future<void> startGroupChallenge({
    required String groupId,
    required String challengeId,
  }) async {}
}

QuizGroup _group({
  required int participantCount,
  int maximumParticipants = 4,
}) => QuizGroup(
  id: 'lobby-group',
  name: 'Faith Night Challenge',
  role: 'owner',
  joinCode: '482731',
  maximumParticipants: maximumParticipants,
  participantCount: participantCount,
);

GroupChallenge _challenge({
  required String mode,
  required int count,
  int participantCount = 2,
}) => GroupChallenge(
  id: 'lobby-challenge',
  title: 'Faith Night Challenge',
  mode: mode,
  status: 'lobby',
  questionCount: count,
  question: '',
  options: const [],
  explanation: '',
  scriptureReference: '',
  participantUids: const ['host', 'member'],
  participantCount: participantCount,
);

Future<void> _pumpLobby(
  WidgetTester tester, {
  required String mode,
  required int count,
  required int participantCount,
  Stream<List<GroupMember>>? memberStream,
}) async {
  final group = _group(participantCount: participantCount);
  final members = participantCount == 1
      ? const [GroupMember(uid: 'host', displayName: 'Host', role: 'owner')]
      : const [
          GroupMember(uid: 'host', displayName: 'Host', role: 'owner'),
          GroupMember(uid: 'member', displayName: 'John', role: 'member'),
        ];
  await tester.pumpWidget(
    MaterialApp(
      home: GroupLobbyScreen(
        store: ProgressStore(),
        groupId: group.id,
        challenge: _challenge(
          mode: mode,
          count: count,
          participantCount: participantCount,
        ),
        group: group,
        service: _LobbyGateway(
          group: group,
          challenge: _challenge(
            mode: mode,
            count: count,
            participantCount: participantCount,
          ),
          members: members,
          memberStream: memberStream,
        ),
      ),
    ),
  );
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 100));
}

void main() {
  testWidgets('Competitive lobby explains individual play and shows room count', (
    tester,
  ) async {
    await _pumpLobby(
      tester,
      mode: 'competitive',
      count: 20,
      participantCount: 2,
    );

    expect(find.text('Faith Night Challenge'), findsOneWidget);
    expect(find.text('Competitive • 20 Questions'), findsOneWidget);
    expect(find.text('2 / 4 joined'), findsOneWidget);
    expect(find.text('Host'), findsOneWidget);
    expect(find.text('John'), findsOneWidget);
    expect(
      find.text(
        'Everyone answers the same Bible questions individually. Accuracy is most important, and speed helps decide rankings when needed. Results and rankings are shown when the challenge finishes.',
      ),
      findsOneWidget,
    );
  });

  testWidgets('Fellowship lobby explains host-led synchronized play and count', (
    tester,
  ) async {
    await _pumpLobby(
      tester,
      mode: 'fellowship',
      count: 30,
      participantCount: 2,
    );

    expect(find.text('Fellowship • 30 Questions'), findsOneWidget);
    expect(find.text('2 / 4 joined'), findsOneWidget);
    expect(
      find.text(
        'Everyone goes through the questions together. Players answer first, then the host reveals the correct answer and Scripture for discussion before moving to the next question.',
      ),
      findsOneWidget,
    );
  });

  testWidgets('solo lobby explains why the start action is unavailable', (
    tester,
  ) async {
    await _pumpLobby(
      tester,
      mode: 'competitive',
      count: 10,
      participantCount: 1,
    );

    expect(find.text('1 / 4 joined'), findsOneWidget);
    expect(
      find.text(
        'At least one other player must join before you can start a Group Challenge.',
      ),
      findsOneWidget,
    );
  });

  testWidgets('lobby start action unlocks when a second member joins', (
    tester,
  ) async {
    final members = StreamController<List<GroupMember>>();
    addTearDown(members.close);
    await _pumpLobby(
      tester,
      mode: 'competitive',
      count: 10,
      participantCount: 1,
      memberStream: members.stream,
    );

    expect(find.text('1 / 4 joined'), findsOneWidget);
    final startBefore = tester.widget<FilledButton>(
      find.widgetWithText(FilledButton, 'START CHALLENGE'),
    );
    expect(startBefore.onPressed, isNull);

    members.add(const [
      GroupMember(uid: 'host', displayName: 'Host', role: 'owner'),
      GroupMember(uid: 'member', displayName: 'John', role: 'member'),
    ]);
    await tester.pump();

    expect(find.text('2 / 4 joined'), findsOneWidget);
    final startAfter = tester.widget<FilledButton>(
      find.widgetWithText(FilledButton, 'START CHALLENGE'),
    );
    expect(startAfter.onPressed, isNotNull);
  });
}
