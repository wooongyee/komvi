<h1 align="center">Komvi</h1>

<p align="center">
  <a href="https://jitpack.io/#wooongyee/komvi"><img alt="JitPack" src="https://jitpack.io/v/wooongyee/komvi.svg"/></a>
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
  <a href="https://android-arsenal.com/api?level=24"><img alt="API" src="https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat"/></a>
</p>

<p align="center">
[<a href="README.md">English</a>] [<a href="README_ko.md">한국어</a>]
</p>

Kotlin MVI library for Android that minimizes boilerplate through KSP code generation and guides architectural best practices at compile-time.

## Why Komvi?

**Clear Intent Separation** – Distinguishes `ViewAction` (from View) and `Internal` (from ViewModel) intents, making data flow immediately traceable.

**Compile-Time Safety** – KSP validates your MVI architecture at build time, catching handler mismatches and visibility violations before runtime.

**Minimal Boilerplate** – Annotate your handlers with `@ViewActionHandler` or `@InternalHandler`, and Komvi generates the entire dispatch logic.

**Built for Android** – Integrates with ViewModel, SavedStateHandle, and Hilt/Dagger for production-ready apps.

## Installation

**Requirements:** Kotlin 2.1.21+ | KSP 2.1.21-2.0.2+ | minSdk 24+

> **Note**: Komvi is primarily designed for **Jetpack Compose**. View-only usage has not been tested.

Add JitPack repository in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add dependencies in your app module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.1.21-2.0.2"
}

dependencies {
    implementation("com.github.wooongyee.komvi:komvi-android:0.2.1")
    implementation("com.github.wooongyee.komvi:komvi-compose:0.2.1")
    ksp("com.github.wooongyee.komvi:komvi-processor:0.2.1")
}
```

## Quick Start

### 1. Define MVI Contract

```kotlin
sealed interface LoginIntent : Intent {
    // ViewAction: Dispatched from View (recommended pattern for annotation matching)
    sealed interface ViewAction : LoginIntent {
        data class EmailChanged(val email: String) : ViewAction
        data object LoginClicked : ViewAction
    }

    // Internal: Dispatched from ViewModel only
    sealed interface Internal : LoginIntent {
        data object OnLoginSuccess : Internal
        data class OnLoginFailure(val error: String) : Internal
    }
}

@Parcelize
data class LoginViewState(
    val email: String = "",
    val isLoading: Boolean = false
) : ViewState, Parcelable

sealed interface LoginSideEffect : SideEffect {
    data object NavigateToHome : LoginSideEffect
}
```

### 2. Create ViewModel

```kotlin
class LoginViewModel : MviViewModel<LoginViewState, LoginIntent, LoginSideEffect>(
    initialState = LoginViewState()
) {
    // Annotate handlers
    @ViewActionHandler
    internal fun handleEmailChanged(intent: LoginIntent.ViewAction.EmailChanged) = handler {
        reduce { copy(email = intent.email) }  // Update state
    }

    @ViewActionHandler(executionMode = ExecutionMode.DROP)
    internal fun handleLoginClicked(intent: LoginIntent.ViewAction.LoginClicked) = handler {
        reduce { copy(isLoading = true) }
        // API call...
        dispatch(LoginIntent.Internal.OnLoginSuccess)  // Dispatch internal intent
    }

    @InternalHandler
    internal fun handleOnLoginSuccess(intent: LoginIntent.Internal.OnLoginSuccess) = handler {
        reduce { copy(isLoading = false) }
        postSideEffect(LoginSideEffect.NavigateToHome)  // Emit side effect
    }
}
```

### 3. Use in Compose

```kotlin
@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    viewModel.collectSideEffect { effect ->
        when (effect) {
            LoginSideEffect.NavigateToHome -> navController.navigate("home")
        }
    }

    TextField(
        value = state.email,
        onValueChange = { viewModel.dispatch(LoginIntent.ViewAction.EmailChanged(it)) }
    )
}
```

## Features

### ExecutionMode Strategies
Control how concurrent intents are handled:
- `DROP` - Drop new while running (prevent duplicate clicks)
- `CANCEL_PREVIOUS` - Cancel previous, run latest (debounce search)
- `QUEUE` - Execute sequentially
- `PARALLEL` - Run all concurrently (default)

### Debug Mode
```kotlin
// Logging enabled by default
class MyViewModel : MviViewModel(
    initialState = State()
)

// Disable logging
class MyViewModel : MviViewModel(
    initialState = State(),
    debugMode = false
)

// Control per-handler logging
@ViewActionHandler(log = true)  // Logs when debugMode is true
internal fun handleAction(intent: MyIntent.Action) = handler { ... }
```

### SavedStateHandle Integration
```kotlin
class MyViewModel(savedStateHandle: SavedStateHandle) : MviViewModel(
    initialState = State(),
    savedStateHandle = savedStateHandle  // Survives process death
)
```

### Hilt Integration
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : MviViewModel(...)
```

## Sample App

For complete examples, see the **[sample app](sample/)**.

## License

```
Copyright 2025 wooongyee

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
