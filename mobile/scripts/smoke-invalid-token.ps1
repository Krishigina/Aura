param(
    [string]$AppId = "com.aura.app",
    [string]$ApkPath = "..\app\build\outputs\apk\debug\app-debug.apk",
    [switch]$SkipInstall,
    [switch]$InjectInvalidToken
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-AdbPath {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd -and $cmd.Source) {
        return $cmd.Source
    }

    $candidates = @()
    if ($env:ANDROID_SDK_ROOT) {
        $candidates += (Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe")
    }
    if ($env:ANDROID_HOME) {
        $candidates += (Join-Path $env:ANDROID_HOME "platform-tools\adb.exe")
    }
    $candidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe")

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    throw "ADB not found. Add platform-tools to PATH or install Android SDK platform-tools."
}

function Ensure-DeviceConnected([string]$AdbPath) {
    $devices = & $AdbPath devices
    $online = @($devices | Where-Object { $_ -match "\tdevice$" })
    if ($online.Count -eq 0) {
        throw "No connected emulator/device detected. Start an emulator or connect a phone and enable USB debugging."
    }
}

function Install-Apk([string]$AdbPath, [string]$ScriptRoot, [string]$RelativeApkPath) {
    $apkFullPath = [System.IO.Path]::GetFullPath((Join-Path $ScriptRoot $RelativeApkPath))
    if (-not (Test-Path $apkFullPath)) {
        throw "APK not found at '$apkFullPath'. Build first: from mobile/ run '.\\gradlew :app:assembleDebug'."
    }

    Write-Host "Installing APK: $apkFullPath"
    & $AdbPath install -r $apkFullPath | Out-Host
}

function Clear-Session([string]$AdbPath, [string]$TargetAppId) {
    Write-Host "Clearing app data for $TargetAppId"
    & $AdbPath shell pm clear $TargetAppId | Out-Host
}

function Inject-InvalidToken([string]$AdbPath, [string]$TargetAppId) {
    $xml = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><string name='access_token'>invalid.token.for.smoke</string></map>"
    $base64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($xml))

    try {
        Write-Host "Injecting invalid token into shared prefs"
        & $AdbPath shell run-as $TargetAppId sh -c "echo $base64 | base64 -d > shared_prefs/aura_auth_prefs.xml" | Out-Host
    } catch {
        throw "Failed to inject invalid token via run-as. Ensure debug build is installed and appId is correct."
    }
}

function Launch-App([string]$AdbPath, [string]$TargetAppId) {
    Write-Host "Launching $TargetAppId"
    & $AdbPath shell monkey -p $TargetAppId -c android.intent.category.LAUNCHER 1 | Out-Host
}

try {
    $adb = Resolve-AdbPath
    Write-Host "Using adb: $adb"

    Ensure-DeviceConnected -AdbPath $adb

    if (-not $SkipInstall) {
        Install-Apk -AdbPath $adb -ScriptRoot $PSScriptRoot -RelativeApkPath $ApkPath
    }

    Clear-Session -AdbPath $adb -TargetAppId $AppId

    if ($InjectInvalidToken) {
        Inject-InvalidToken -AdbPath $adb -TargetAppId $AppId
    }

    Launch-App -AdbPath $adb -TargetAppId $AppId

    Write-Host ""
    Write-Host "Smoke setup complete."
    Write-Host "- If -InjectInvalidToken is used: app should redirect to AUTH (or show session-expired handling)."
    Write-Host "- Without -InjectInvalidToken: login and save survey manually to verify success path."
} catch {
    Write-Error $_
    exit 1
}
