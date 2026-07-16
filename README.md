# ☕ Cafe Diary

**Cafe Diary** is a premium, highly polished, offline-first Android application designed to serve as a digital companion for coffee enthusiasts, roastery explorers, and cafe bloggers. It allows users to log and rate visited cafes, analyze coffee quality and atmosphere, upload visual assets, view logs on a custom interactive map, and explore data trends over time.

---

## 🎨 Visual Identity & Architecture

Cafe Diary is designed around Material Design 3 guidelines, using a customized, eye-safe, cohesive color palette with modern typography, generous spacing, and rich vector illustrations. 

The application is structured using a robust **Model-View-ViewModel (MVVM)** clean architecture, ensuring a strict separation of concerns, high testability, and deterministic offline state handling:

```
                  ┌────────────────────────────────────────┐
                  │              UI layer                  │
                  │   (Jetpack Compose & Material 3)       │
                  └───────────────────┬────────────────────┘
                                      │
                                      ▼
                  ┌────────────────────────────────────────┐
                  │            ViewModel layer             │
                  │     (CafeViewModel, AuthViewModel)     │
                  └───────────────────┬────────────────────┘
                                      │
                                      ▼
                  ┌────────────────────────────────────────┐
                  │            Repository layer            │
                  │            (CafeRepository)            │
                  └───────────────────┬────────────────────┘
                                      │
                                      ▼
                  ┌────────────────────────────────────────┐
                  │            Local Database              │
                  │        (Room Database / SQLite)        │
                  └────────────────────────────────────────┘
```

---

## ✨ Features Breakdown

### 1. 📝 Comprehensive Cafe Logging
*   **Detailed Metrics**: Rate your experience using three separate dimensions: Overall Experience, Coffee Quality, and Atmosphere (each on an elegant 1-5 star scale).
*   **Media Preservation**: Take or upload multiple photos. The app clones images locally into private storage to prevent visual assets from becoming inaccessible if external gallery files are moved or deleted.
*   **Custom Annotations**: Capture personal notes, roast descriptions, and brewing methods.
*   **Maps Link Parsing**: Directly paste a Google Maps or Apple Maps share link (e.g., `maps.app.goo.gl` or `goo.gl/maps`). The app automatically parses coordinates and extracts geographic descriptors.

### 2. 🌍 Real-Time Online Geocoding & Address Suggestions
*   **OpenStreetMap Nominatim Integration**: When entering a cafe's name, the app makes real-time debounced requests to the Nominatim API to provide local address suggestions and autofill coordinates.
*   **Zero-Config Location Parsing**: No external billing or proprietary API keys are required to utilize real-time geocoding.

### 3. 🗺️ Custom Interactive Coffee Roastery Map
*   **Pure Canvas Rendering**: A lightweight, performant, fluid vector map rendered using custom Compose Canvas drawing. It supports multi-touch panning, inertia scrolling, and pinch-to-zoom gestures.
*   **Dynamic Centering**: Automatically calculates the geometric center of all logged cafes and positions the camera focus accordingly, falling back to a default location (e.g., San Francisco) if no entries exist.
*   **Interactive Spotlights**: Tapping pins on the map highlights them, bringing up visual summary cards showing address, ratings, and instant details.

### 4. 📊 Trends & Analytical Dashboard
Built completely from scratch using pure Jetpack Compose graphics APIs without heavy, web-view-based chart libraries, the **Trends** tab displays three interactive analytical views:
*   📈 **Cafes Visited Over Time**: A beautiful, gradient-filled cubic-bezier line chart showing cumulative cafe logs chronologically.
*   ⭐ **Experience Rating Trend**: A hybrid visualization depicting individual rating bars paired with a running-average cubic-spline trend line.
*   ☕ **Coffee Quality Insights**: A split dashboard featuring an animated vertical bar-chart breakdown of quality scores alongside a chronological average coffee rating trend.

### 5. 🗄️ Robust Offline-First Database
*   **Room Database**: Uses SQLite via Jetpack Room for data mapping, with clean migration path configurations.
*   **Auto-save Hooks**: Instantly writes new items to the local database, providing a bulletproof offline experience that guarantees zero data loss on application restarts.

---

## 🛠️ Technology Stack

*   **Language**: Kotlin 2.x
*   **UI Engine**: Jetpack Compose with Material 3 (supporting edge-to-edge screens via `enableEdgeToEdge()` and `WindowInsets`)
*   **Asynchronous Processing**: Kotlin Coroutines & Flow/StateFlow for state emission and stream pipelining
*   **Local Storage**: SQLite wrapped with Android Room
*   **Dependency Injection**: Simple constructor injection & custom ViewModel factories
*   **Image Loading**: Coil (Coroutine Image Loader) with private storage caching
*   **JSON Serialization**: kotlinx-serialization
*   **Build Automation**: Gradle 9.3.1 (Kotlin DSL) with Kotlin Symbol Processing (KSP) and AGP 8.12.0

---

## 🗂️ Project Directory Structure

```
/
├── app/
│   ├── build.gradle.kts                # App-level build configurations & dependencies
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml      # Application Manifest (Permissions & Activity registration)
│           ├── java/com/example/
│           │   ├── MainActivity.kt      # Main Entry Point & Screen Router
│           │   ├── auth/
│           │   │   └── AuthViewModel.kt # Mock/Sample Authentication State Machine
│           │   ├── data/
│           │   │   ├── CafeDatabase.kt  # Room database initialization
│           │   │   ├── CafeDao.kt       # Data Access Object (DAO) for SQL queries
│           │   │   ├── CafeEntity.kt    # SQLite database schema specification
│           │   │   └── CafeRepository.kt# Repository pattern implementation
│           │   └── ui/
│           │       ├── CafeViewModel.kt # State coordination & local file preservation
│           │       ├── screens/
│           │       │   ├── AddCafeScreen.kt# Cafe logger screen with online geocoding
│           │       │   ├── CafeDetailsDialog.kt # Modal detail viewer for cafe entries
│           │       │   ├── DashboardScreen.kt # Feed, search bar, & custom Canvas charts
│           │       │   ├── LoginScreen.kt # High-contrast welcome & login interface
│           │       │   ├── MainScreen.kt# Scaffold navigation container
│           │       │   └── MapScreen.kt # Canvas-rendered interactive map
│           │       └── theme/
│           │           ├── Color.kt     # Customized light/dark theme hex schemes
│           │           ├── Theme.kt     # M3 Theme Builder configurations
│           │           └── Type.kt      # Text styles, weights, and letter-spacings
│           └── res/
│               ├── values/
│               │   └── strings.xml      # String resource dictionary
│               └── drawable/
│                   └── ...              # Launch icons, vectors, and layouts
├── gradle/
│   ├── wrapper/                         # Gradle wrapper configuration files
│   └── libs.versions.toml               # Centrally managed project dependency catalogue
├── settings.gradle.kts                  # Gradle root settings file
├── gradle.properties                    # Android JVM & configuration properties
├── metadata.json                        # Platform metadata specification
└── README.md                            # Detailed documentation (This file)
```

---

## 🗃️ Database Schema

### Table: `cafes`

| Field Name | Data Type | Key Type | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY (Auto)` | Unique, auto-incrementing ID |
| `name` | `TEXT` | - | Name of the Cafe |
| `address` | `TEXT` | - | Formatted street address |
| `latitude` | `REAL` | - | Coordinate latitude (Double) |
| `longitude` | `REAL` | - | Coordinate longitude (Double) |
| `rating` | `INTEGER` | - | Overall experience rating (1 to 5) |
| `coffeeQualityRating`| `INTEGER` | - | Quality score of the coffee (1 to 5) |
| `atmosphereRating` | `INTEGER` | - | Ambience rating (1 to 5) |
| `notes` | `TEXT` | - | Personal diary notes, roasts, or techniques |
| `photoUri` | `TEXT (Nullable)`| - | Storage path for primary image |
| `photoUris` | `TEXT (Nullable)`| - | Semicolon-delimited paths for secondary photos|
| `mapShareLink` | `TEXT (Nullable)`| - | Google Maps shared URL link |
| `timestamp` | `INTEGER` | - | Epoch millisecond timestamp of log creation |

---

## 🚀 Getting Started (Run Locally)

To download, compile, and run Cafe Diary locally on your own machine:

### Prerequisites
1. **Android Studio** (Koala | 2024.1 or newer recommended)
2. **Java Development Kit (JDK) 21** configured in your environment/Android Studio
3. **Android Device or Emulator** running API 26 (Android 8.0) or higher

### Steps to Run
1.  **Clone the Repository**:
    ```bash
    git clone <your-repository-url>
    cd cafe-diary
    ```
2.  **Open in Android Studio**:
    *   Launch Android Studio.
    *   Select **Open**, then navigate to and select the `cafe-diary` folder.
    *   Allow Gradle to synchronize project dependencies automatically.
3.  **Run the Project**:
    *   Select a connected Android device or an active Emulator.
    *   Click the green **Run (Play)** button in the top toolbar of Android Studio, or use the keyboard shortcut `Shift + F10` (Windows/Linux) / `Control + R` (macOS).

### Local Terminal Build Commands
To run checks or build a debug APK using Gradle directly from your local terminal:

*   **Build the Debug APK**:
    ```bash
    ./gradlew assembleDebug
    ```
    The generated APK will be available in `app/build/outputs/apk/debug/app-debug.apk`.
    
*   **Run Unit and Robolectric Tests**:
    ```bash
    ./gradlew testDebugUnitTest
    ```
