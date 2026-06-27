# O General AC IR Remote Control Android App

A premium, ad-free, secure, and offline-only infrared (IR) remote control Android application for **General**, **O General**, and **Fujitsu General** air conditioners.

This application is engineered specifically for Android devices with an built-in infrared transmitter (IR Blaster) like Xiaomi, Redmi, and POCO phones using Android's native `ConsumerIrManager` API.

---

## 📱 Features

- **Ad-Free & Tracker-Free**: No ads, no startup delays, no analytics, and no internet permission. Complete security and privacy.
- **Power Control**: Easily toggle Power ON/OFF.
- **Temperature Precision**: Control target temperatures from `16°C` to `30°C`.
- **Modes Supported**: Auto, Cool, Dry, Fan, and Heat.
- **Multi-level Fan Speeds**: Auto, Low, Medium, High speeds.
- **L/R & U/D Swings**: Seamlessly toggle active swing louvers.
- **Power Auxiliaries**: Quick toggle for **Turbo** cooling, **Sleep** eco-mode, and a cycler for **Hours Timer** (0-12h).
- **Physical Tactile Feel**: Modern spring-physics elastic animations on every button press with integrated haptic vibration.
- **Visual LCD Simulation**: An authentic simulated digital segment panel reproducing a physical remote's liquid crystal feedback, including dynamic speed bars and mode indicators.
- **Virtual Simulation Engine**: Automatic fallback to simulation mode when run on devices without physical IR emitters, allowing risk-free UI preview and internal logging.
- **State Persistence**: Remembers your last configured settings (mode, speed, swing, temp) across app restarts using Jetpack Preferences DataStore.

---

## 🛠️ Architecture

This app is built following standard Android modern development guidelines and **MVVM (Model-View-ViewModel)** Architecture:

```
                  ┌─────────────────────────────────┐
                  │          Jetpack Compose        │ (MainActivity.kt)
                  │          Material 3 UI          │
                  └────────────────┬────────────────┘
                                   │
                                   ▼
                  ┌─────────────────────────────────┐
                  │         RemoteViewModel         │ (RemoteViewModel.kt)
                  └───────┬─────────────────┬───────┘
                          │                 │
                          ▼                 ▼
  ┌─────────────────────────────────┐     ┌─────────────────────────────────┐
  │       IrRemoteManager (IR)      │     │    RemoteSettingsDataStore      │ (DataStore)
  └───────────────┬─────────────────┘     └─────────────────────────────────┘
                  │
                  ▼
  ┌─────────────────────────────────┐
  │      GeneralAcIrCodes (Data)    │ (GeneralAcIrCodes.kt)
  └─────────────────────────────────┘
```

- **`MainActivity.kt`**: Single-activity entry point hosting a responsive Compose Material 3 interface, featuring customizable dark/light schemes, bouncy click modifiers, and status display grids.
- **`RemoteViewModel.kt`**: State holder managing business logic. Collects configurations as hot `StateFlow` and exposes callbacks.
- **`IrRemoteManager.kt`**: Encapsulates Android's `ConsumerIrManager` API, query frequencies, asynchronous thread scheduling on `Dispatchers.IO`, and virtual logging.
- **`RemoteSettingsDataStore.kt`**: Low-latency file storage to persist and reload configurations using Jetpack Preferences DataStore.
- **`GeneralAcIrCodes.kt`**: Self-contained registry holding raw infrared pulse code arrays in microseconds and high-frequency wave configurations.

---

## 📡 Injecting Captured IR Codes

The application is structured to support instant expansion. If you capture or extract O General raw infrared pulses from a physical remote or reverse-engineer an APK, you can simply paste the raw pulse array into `GeneralAcIrCodes.kt` without changing any UI or ViewModel code.

### Step-by-Step Instructions

1. **Locate Code File**: Open `/app/src/main/java/com/example/GeneralAcIrCodes.kt`.
2. **Find Target Constant**: Scroll to the constant representing your command (e.g., `POWER` or `TEMP_24`).
3. **Format Pulse Arrays**: Ensure your captured codes are formatted as an `IntArray` representing alternating ON (transmission) and OFF (silence) durations in microseconds.
   
   *Example standard pattern:*
   ```kotlin
   val POWER = intArrayOf(3320, 1570, 430, 1180, 430, 380, ...)
   ```
4. **Update Frequency**: If your receiver/transmitter uses a frequency other than `38000 Hz` (38 kHz), modify the `CARRIER_FREQUENCY_HZ` constant at the top of the file:
   ```kotlin
   const val CARRIER_FREQUENCY_HZ = 38000
   ```
5. **Compile & Run**: The app will automatically read your new codes.

---

## 🚀 Build Instructions

### Prerequisites
- **Android Studio Ladybug** (or newer)
- **JDK 17** (or newer)
- Android SDK 24+

### Compiling on Android Studio
1. Clone / Extract the project.
2. Open Android Studio and select **File -> Open...**, then select the project root folder.
3. Allow Gradle to sync.
4. Connect an Android device (or launch an Emulator).
5. Click **Run (Shift + F10)** to install and launch the application.

### Building APK via Command Line
Run the following wrapper command from the project root to generate a debug APK:
```bash
gradle assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`
