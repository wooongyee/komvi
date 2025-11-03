# Komvi

Kotlin MVI (Model-View-Intent) library for Android with KSP-based code generation.

[![](https://jitpack.io/v/wooongyee/komvi.svg)](https://jitpack.io/#wooongyee/komvi)

## Features

- 🚀 MVI architecture made simple
- 🔧 KSP-based code generation
- 📱 Android & Jetpack Compose support
- 🔄 Coroutines-first design
- 🧪 Built-in testing utilities

## Installation

### Step 1: Add JitPack repository

Add JitPack to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2: Add dependencies

Add Komvi to your app module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
}

dependencies {
    // For Android projects
    implementation("com.github.wooongyee.komvi:komvi-android:0.1.0")

    // For Jetpack Compose
    implementation("com.github.wooongyee.komvi:komvi-compose:0.1.0")

    // KSP processor for code generation
    ksp("com.github.wooongyee.komvi:komvi-processor:0.1.0")
}
```

### Alternative: Individual modules

If you need more granular control:

```kotlin
dependencies {
    // Core library (pure Kotlin)
    implementation("com.github.wooongyee.komvi:komvi-core:0.1.0")

    // Annotations
    implementation("com.github.wooongyee.komvi:komvi-annotations:0.1.0")

    // Android ViewModel support
    implementation("com.github.wooongyee.komvi:komvi-android:0.1.0")

    // Compose integration
    implementation("com.github.wooongyee.komvi:komvi-compose:0.1.0")

    // KSP processor
    ksp("com.github.wooongyee.komvi:komvi-processor:0.1.0")

    // Testing utilities (testImplementation)
    testImplementation("com.github.wooongyee.komvi:komvi-test:0.1.0")
}
```

## Usage

### Basic Example

```kotlin
// Define your Intent
sealed interface CounterIntent {
    data object Increment : CounterIntent
    data object Decrement : CounterIntent
}

// Define your State
data class CounterState(
    val count: Int = 0
)

// Create ViewModel with MVI
@IntentHandler
class CounterViewModel : MviViewModel<CounterIntent, CounterState>(
    initialState = CounterState()
) {
    @HandleIntent
    fun handleIncrement(intent: CounterIntent.Increment) = intent {
        reduce { copy(count = count + 1) }
    }

    @HandleIntent
    fun handleDecrement(intent: CounterIntent.Decrement) = intent {
        reduce { copy(count = count - 1) }
    }
}

// Use in Compose
@Composable
fun CounterScreen(viewModel: CounterViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Column {
        Text("Count: ${state.count}")

        Button(onClick = { viewModel.intent(CounterIntent.Increment) }) {
            Text("Increment")
        }

        Button(onClick = { viewModel.intent(CounterIntent.Decrement) }) {
            Text("Decrement")
        }
    }
}
```

## Modules

- **komvi-core**: Core MVI logic (pure Kotlin)
- **komvi-annotations**: Annotations for code generation
- **komvi-processor**: KSP processor for generating boilerplate
- **komvi-android**: Android ViewModel integration
- **komvi-compose**: Jetpack Compose utilities
- **komvi-test**: Testing utilities

## License

```
Copyright 2024 wooongyee

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
