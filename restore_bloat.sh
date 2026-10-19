#!/bin/bash
set -e
ADB="${1:-adb}"

echo "=================================================="
echo "    Restore Debloated Apps on Philips TV          "
echo "=================================================="

PACKAGES=(
    "com.google.android.tvlauncher"
    "com.google.android.tvrecommendations"
    "com.google.android.videos"
    "com.google.android.tungsten.setupwraith"
    "com.mediatek.wwtv.setupwizard"
    "com.google.android.onetimeinitializer"
    "fusion.android.tv.demo"
    "tv.wuaki.apptv"
    "ru.ivi.client"
    "com.zdf.android.mediathek"
    "de.swr.avp.ard.tv"
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
    echo -n "Restoring $pkg... "
    $ADB shell pm enable "$pkg" 2>/dev/null || $ADB shell cmd package install-existing "$pkg" 2>/dev/null || true
    echo "Done"
done

echo ""
echo "All packages restored!"
