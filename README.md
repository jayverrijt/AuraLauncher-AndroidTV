# Aura Launcher • Android TV

A lightweight, modern, ad-free Android TV launcher crafted for performance, smooth D-pad remote navigation, smart speaker media integration, and system optimization.

---

## ✨ Features

- **Blazing Fast & Clean Interface**: Minimalist dashboard designed specifically for 1080p and 4K Android TV remote navigation.
- **Smart Speaker & Spotify Hub**:
  - Live now-playing metadata and album art display via Google Cast v2 protocol.
  - Direct remote controls: Play/Pause, Next, Previous, Stop, and Favorite.
  - Granular volume control (±5%) and quick presets (20%, 50%, 75%) with D-pad / remote number shortcuts (1, 2, 3).
- **Smart Home Quick Controls**: Instant toggle dialog for bedroom lights, TV status, and audio playback.
- **Integrated Private DNS AdBlocker**:
  - Global ad & tracker blocking via encrypted Private DNS (`dns.adguard-dns.com`).
  - Toggleable on-the-fly from the launcher settings hub.
- **Pinned Web Apps & Bookmarks**:
  - Built-in full-screen Leanback web browser with D-pad remote scrolling and virtual cursor.
  - Pin web shortcuts with custom color cards.
- **One-Click RAM Optimization**: View real-time memory usage and free up system memory with a single click.
- **Safe TV Debloater**: Included scripts to safely disable manufacturer bloatware, telemetry, and background services on Android TVs (tested on Philips Android TVs).

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (or newer) / JDK 17+ or JDK 21.
- Android SDK 34 (`compileSdk 34`, `minSdk 26`).

### Configuration

Copy `local.properties.example` to `local.properties` (this file is ignored by git to keep your private network configuration safe):

```bash
cp local.properties.example local.properties
```

Configure your local environment and optional smart speaker settings in `local.properties`:

```properties
sdk.dir=/path/to/your/android/sdk

# Optional: Smart Speaker IP (Google Cast / Nest Speaker)
speaker.ip=192.168.1.100
speaker.name=Living Room Speaker
speaker.room=Living Room
home.name=My Home
```

### Build & Install

Build the debug APK with Gradle:

```bash
./gradlew assembleDebug
```

Install directly to your Android TV over ADB:

```bash
adb connect <TV_IP_ADDRESS>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧹 TV Debloating Utilities

Two helper shell scripts are included for debloating and restoring system packages:

- `debloat_tv.sh`: Safely disables unused store demo apps, telemetry, crash loggers, and foreign broadcast suites for user 0 without root.
- `restore_bloat.sh`: Re-enables any disabled packages if needed.

Usage:
```bash
./debloat_tv.sh adb
```

---

## 📄 License

This project is licensed under the MIT License.
