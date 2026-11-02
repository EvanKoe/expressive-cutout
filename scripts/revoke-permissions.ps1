# Revokes every grant the Permissions tab manages, so the Shizuku shortcut can be tested from scratch.
# Shizuku's own grant is not touched: revoke it in the Shizuku app if needed.

$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$pkg = 'com.ekoehler.expressivecutout'

& $adb shell cmd notification disallow_listener "$pkg/.service.CutoutNotificationListenerService"

# The accessibility list is colon-separated and shared with other services, so drop only ours.
$enabled = (& $adb shell settings get secure enabled_accessibility_services).Trim()
if ($enabled -ne 'null' -and $enabled -ne '') {
    $kept = ($enabled -split ':') | Where-Object { $_ -notlike "$pkg/*" }
    if ($kept) {
        & $adb shell settings put secure enabled_accessibility_services "'$($kept -join ':')'"
    } else {
        & $adb shell settings delete secure enabled_accessibility_services
    }
}

& $adb shell dumpsys deviceidle whitelist "-$pkg"
& $adb shell pm revoke $pkg android.permission.READ_PHONE_STATE

Write-Host "Revoked notification access, accessibility, battery exemption and phone state for $pkg"
