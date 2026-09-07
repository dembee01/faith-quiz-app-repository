# One-time server setup that turns on Faith Quiz online play.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File tool\setup_cloud_play.ps1
#
# What it does:
#   1. Signs you in to Firebase (opens a browser).
#   2. Selects the Faith Quiz project.
#   3. Deploys callable Functions and Firestore security rules.
#   4. Uploads and verifies the cloud catalogue.
#   5. Enables the matching Remote Config feature flag and verifies it.
#
# Re-run after any change to functions\index.js.

$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

firebase --version | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Firebase CLI is not installed. Run: npm install -g firebase-tools' }

$accounts = firebase login:list 2>&1
if ($LASTEXITCODE -ne 0 -or ($accounts -join "`n") -match 'No Firebase CLI users logged in') {
  firebase login
  if ($LASTEXITCODE -ne 0) { throw 'firebase login failed.' }
}

firebase use faith-quiz-app-119653
if ($LASTEXITCODE -ne 0) { throw 'Could not select the project.' }

firebase deploy --only functions,firestore:rules
if ($LASTEXITCODE -ne 0) { throw 'Functions or Firestore rules deployment failed.' }

node tool\upload_cloud_catalogue_with_firebase_cli.js
if ($LASTEXITCODE -ne 0) { throw 'Cloud catalogue upload or verification failed.' }

node tool\publish_remote_config_defaults.js
if ($LASTEXITCODE -ne 0) { throw 'Remote Config publication or verification failed.' }

Write-Host ''
Write-Host 'Online play is live and verified. Cloud Challenge, groups, and leaderboards can now verify answers.'
Write-Host 'If you later publish on Google Play, re-enable enforceAppCheck in functions\index.js and deploy again.'
