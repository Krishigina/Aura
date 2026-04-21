# Invalid Token Smoke Check (2–3 minutes)

This verifies the latest mobile fixes around token persistence, startup routing, and survey save behavior.

## Preconditions

- Backend is running on `http://10.0.2.2:3002`.
- Debug APK is built (`:app:assembleDebug`) or can be built on your machine.
- Emulator/device is connected.

## One-command setup

From repo root:

```powershell
cd mobile
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-invalid-token.ps1
```

What it does:

1. Finds `adb` automatically (PATH / Android SDK).
2. Installs debug APK (unless `-SkipInstall`).
3. Clears app data (fresh session).
4. Launches the app.

## Scenario A — valid flow (must pass)

1. Login normally.
2. Fill the survey.
3. Tap save/complete.

Expected:

- Save succeeds (no `invalid token`).
- You see normal success/next-step flow.

## Scenario B — forced invalid token (must pass)

Run:

```powershell
cd mobile
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-invalid-token.ps1 -SkipInstall -InjectInvalidToken
```

Expected:

- App does **not** continue with stale/bad session.
- User is sent to AUTH flow (or gets explicit session-expired handling).
- On survey save attempt with invalid token, app shows session-expired style error and clears local token.

## Optional quick log check

```powershell
"$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -d | Select-String -Pattern "invalid token|401|unauthorized|session"
```

## Pass criteria

- Valid login + survey save works.
- Invalid token path is handled gracefully (clear session + auth recovery), without false-success save.
