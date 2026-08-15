# Faith Quiz cloud catalogues

`cloud_prophets_witnesses_v1.json` contains 500 cloud-only challenge questions.
It is deliberately separate from the bundled offline packs:

- 400 questions from the sixteen Old Testament writing prophets.
- 100 questions from New Testament prophetic witnesses and prophecy passages.
- Every question has a unique verse reference, four choices, and a server-side
  correct-answer index for verified leaderboard submissions.

The catalogue is generated from the public-domain KJV verse index named in its
`source` field. Regenerate and verify it with:

```powershell
node tool/build_cloud_prophet_catalogue.js
node tool/verify_cloud_prophet_catalogue.js
```

To upload the reviewed catalogue from this signed-in development machine (it
does not print or save a credential), run:

```powershell
node tool/upload_cloud_catalogue_with_firebase_cli.js
node tool/publish_remote_config_defaults.js
```

The uploader writes public questions under `content/` and their answer indexes
under the Firestore-rule-protected `contentPrivate/` collection. Keep the
Remote Config flag off until callable Functions are deployed, so no client can
start an online challenge before its server-side grading is available.

The validator confirms the exact count, 400/100 Testament split, unique IDs,
unique verse references, four unique options, correct source-book answer,
excerpt/source match, and a Scripture reference in every explanation.
