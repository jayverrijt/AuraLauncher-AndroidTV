#!/bin/bash
set -e
ADB="${1:-adb}"

echo "=================================================="
echo "    Safe Android TV Debloater for Philips TV      "
echo "=================================================="

# List of safe-to-disable packages (bloatware, ads, telemetry, UK/RU/DE preinstalls)
PACKAGES=(
    # Stock Launcher & Setup Wizards
    "com.google.android.tvlauncher"
    "com.google.android.tvrecommendations"
    "com.google.android.videos"
    "com.google.android.tungsten.setupwraith"
    "com.mediatek.wwtv.setupwizard"
    "com.google.android.onetimeinitializer"

    # Store Demo Mode & Third-party Bloat
    "fusion.android.tv.demo"
    "tv.wuaki.apptv"
    "ru.ivi.client"
    "com.zdf.android.mediathek"
    "de.swr.avp.ard.tv"

    # UK Freeview suite (unused outside UK)
    "uk.co.freeview.amc_catchup"
    "uk.co.freeview.amc_horror"
    "uk.co.freeview.bbc"
    "uk.co.freeview.bbc_sounds"
    "uk.co.freeview.ch4_vod"
    "uk.co.freeview.ch5"
    "uk.co.freeview.explore"
    "uk.co.freeview.fvpconfigauth"
    "uk.co.freeview.itv"
    "uk.co.freeview.mdsclient"
    "uk.co.freeview.onnow"
    "uk.co.freeview.stv"
    "uk.co.freeview.tifbridge"
    "uk.co.freeview.uktv"
    "com.mediatek.tv.freeviewplay"

    # Telemetry, Diagnostics & Crash Loggers
    "com.google.android.feedback"
    "com.google.android.partnersetup"
    "com.google.android.syncadapters.calendar"
    "com.google.android.marvin.talkback"
    "com.google.android.play.games"
    "com.tpv.xmic.ots.cms.client"
    "org.droidtv.candeebug"
    "org.droidtv.tv.frameworklogger"
    "org.droidtv.nettvregistration"
    "org.droidtv.eum"
    "com.mstar.android.tv.disclaimercustomization"
)

for pkg in "${PACKAGES[@]}"; do
    echo -n "Disabling $pkg... "
    if $ADB shell pm disable-user --user 0 "$pkg" 2>/dev/null; then
        echo "OK (disabled)"
    elif $ADB shell pm uninstall -k --user 0 "$pkg" 2>/dev/null; then
        echo "OK (uninstalled user 0)"
    else
        echo "Skipped / not found"
    fi
done

echo ""
echo "Debloat completed successfully!"
