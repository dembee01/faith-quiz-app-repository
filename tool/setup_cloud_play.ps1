# One-time server setup that turns on Faith Quiz online play.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File tool\setup_cloud_play.ps1
#
# What it does:
#   1. Signs you in to Firebase (opens a browser).
#   2. Selects the Faith Quiz project.
#   3. Deploys the callable functions with App Check enforcement relaxed so
#      sideloaded APKs can submit verified answers.
#
# Re-run after any change to functions\index.js.

$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

firebase --version | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Firebase CLI is not installed. Run: npm install -g firebase-tools' }

firebase login
if ($LASTEXITCODE -ne 0) { throw 'firebase login failed.' }

firebase use faith-quiz-app-119653
if ($LASTEXITCODE -ne 0) { throw 'Could not select the project.' }

firebase deploy --only functions
if ($LASTEXITCODE -ne 0) { throw 'Function deployment failed.' }

Write-Host ''
Write-Host 'Online play is live. Cloud Challenge, groups, and leaderboards can now verify answers.'
Write-Host 'If you later publish on Google Play, re-enable enforceAppCheck in functions\index.js and deploy again.'
