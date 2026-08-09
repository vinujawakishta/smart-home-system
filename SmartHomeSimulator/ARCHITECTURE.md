# Lumen Home Architecture Documentation

Lumen Home is built using **Clean Architecture** principles combined with the **MVVM (Model-View-ViewModel)** design pattern. This ensures the application is scalable, maintainable, and easy to test.

## Architectural Overview

The application is divided into three primary layers, following the principle of separation of concerns.

### 1. Domain Layer (Pure Kotlin)
This is the core of the application. It contains the business logic and is completely independent of other layers, frameworks, or databases.
- **Models**: Pure Kotlin data classes (e.g., `Device`, `Alert`) used throughout the app.
- **Repository Interface**: Defines the contract for data operations. The UI depends on this interface, not the implementation.
- **Constants**: Static configurations like floor plans and grid layouts.

### 2. Data Layer
Responsible for fulfilling the data requirements of the domain layer.
- **SmartHomeRepositoryImpl**: The implementation of the Domain's Repository interface. It handles all communication with **Firebase Realtime Database**.
- **Data Source**: Manages connection lifecycles and maps Firebase snapshots into clean Domain Models using Kotlin Flows.

### 3. UI Layer (Presentation)
Handles everything related to showing data to the user and capturing user interactions.
- **Compose Screens**: Declarative UI components that observe state and emit events.
- **ViewModels**: Manage UI state using `StateFlow`. They communicate with the Repository and handle screen-specific logic.
- **Navigation**: Uses Jetpack Navigation Compose to manage screen transitions and deep linking.
- **Theme**: A centralized luxury design system defining colors, typography, and shapes.

---

## Technical Stack

- **UI Framework**: Jetpack Compose
- **Concurrency**: Kotlin Coroutines & Flows
- **Dependency Injection**: Hilt (Dagger)
- **Database**: Firebase Realtime Database
- **Navigation**: Navigation Compose
- **Lifecycle**: ViewModel, StateFlow

---

## Folder Structure

```text
com.example.smarthomesimulator
├── data
│   └── repository       # Firebase implementation of the repository
├── di                   # Hilt Modules (Dependency Injection setup)
├── domain
│   ├── model            # Clean data classes and constants
│   └── repository       # Data access interfaces (contracts)
├── ui
│   ├── navigation       # Bottom bar and routing logic
│   ├── screens          # Modular Compose screens and UI components
│   ├── theme            # Luxury design system (Color, Type, Theme)
│   └── viewmodel        # MVVM state management
├── LumenApplication.kt  # Hilt Application entry point
└── MainActivity.kt      # Main entry point & NavHost setup
```

---

## Key Design Patterns

1. **Reactive Programming**: The app uses a reactive stream (Flow) from Firebase all the way to the UI. When data changes in the database, the UI updates automatically without manual polling.
2. **Repository Pattern**: Abstracts the data source. If you decide to switch from Firebase to a REST API, you only need to change the implementation in the `data` package.
3. **Luxury Design System**: A custom Material 3 theme that prioritizes high-end aesthetics (Midnight & Gold) while maintaining accessibility and readability.
