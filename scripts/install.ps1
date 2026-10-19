# Builds the debug APK, installs it on the first physical device adb can see, and grants every
# permission the Permissions tab manages. Shizuku's own grant is not touched: it lives in Shizuku.
# Run from the repo root: .\script\install.ps1

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$pkg = 'com.ekoehler.expressivecutout'
$listener = "$pkg/.service.CutoutNotificationListenerService"
$accessibility = "$pkg/$pkg.service.CutoutAccessibilityService"

# First connected device that isn't an emulator
$serial = (& $adb devices) |
    Where-Object { $_ -match '^(\S+)\s+device$' -and $Matches[1] -notlike 'emulator-*' } |
    ForEach-Object { $Matches[1] } |
    Select-Object -First 1
if (-not $serial) { throw 'No physical device connected.' }
Write-Host "Device: $serial"

& "$root\gradlew.bat" -p $root assembleDebug -q
if ($LASTEXITCODE -ne 0) { throw 'Build failed.' }
& $adb -s $serial install -r "$root\app\build\outputs\apk\debug\app-debug.apk"
if ($LASTEXITCODE -ne 0) { throw 'Install failed.' }

& $adb -s $serial shell cmd notification allow_listener $listener

# Always remove then re-add our service: a reinstall leaves it listed but silently unbound, and
# only a toggle brings it back.
$enabled = (& $adb -s $serial shell settings get secure enabled_accessibility_services).Trim()
$kept = @()
if ($enabled -ne 'null' -and $enabled -ne '') {
    $kept = @(($enabled -split ':') | Where-Object { $_ -notlike "$pkg/*" })
}
if ($kept) {
    & $adb -s $serial shell settings put secure enabled_accessibility_services "'$($kept -join ':')'"
} else {
    & $adb -s $serial shell settings delete secure enabled_accessibility_services
}
& $adb -s $serial shell settings put secure enabled_accessibility_services "'$(($kept + $accessibility) -join ':')'"
& $adb -s $serial shell settings put secure accessibility_enabled 1

& $adb -s $serial shell dumpsys deviceidle whitelist "+$pkg"
& $adb -s $serial shell pm grant $pkg android.permission.READ_PHONE_STATE

Write-Host "Installed and granted notification access, accessibility, battery exemption and phone state on $serial"
