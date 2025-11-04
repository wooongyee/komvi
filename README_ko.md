<h1 align="center">Komvi</h1>

<p align="center">
  <a href="https://jitpack.io/#wooongyee/komvi"><img alt="JitPack" src="https://jitpack.io/v/wooongyee/komvi.svg"/></a>
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
  <a href="https://android-arsenal.com/api?level=24"><img alt="API" src="https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat"/></a>
</p>

<p align="center">
[<a href="README.md">English</a>] [<a href="README_ko.md">한국어</a>]
</p>

KSP 코드 생성을 통해 보일러플레이트를 최소화하고, 컴파일 타임에 MVI 아키텍처 패턴을 가이드하는 Kotlin 라이브러리입니다.

## Komvi를 선택하는 이유

**명확한 Intent 분리** – `ViewAction` (View에서 발생)과 `Internal` (ViewModel에서 발생)로 구분하여 데이터 흐름을 쉽게 추적할 수 있습니다.

**컴파일 타임 안정성** – KSP가 빌드 시점에 MVI 아키텍처를 검증하여, 핸들러 불일치나 가시성 문제를 런타임 전에 잡아냅니다.

**최소한의 보일러플레이트** – 핸들러에 `@ViewActionHandler` 또는 `@InternalHandler` 어노테이션만 추가하면, Komvi가 dispatch 로직을 자동 생성합니다.

**Android에 최적화** – ViewModel, SavedStateHandle, Hilt/Dagger와 통합되어 프로덕션 앱 개발에 바로 사용할 수 있습니다.

## 설치

**요구사항:** Kotlin 2.1.21+ | KSP 2.1.21-2.0.2+ | minSdk 24+

> **참고**: Komvi는 기본적으로 **Jetpack Compose** 환경을 위해 설계되었습니다. View만 사용하는 환경은 테스트되지 않았습니다.

`settings.gradle.kts`에 JitPack 저장소를 추가하세요:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

앱 모듈의 `build.gradle.kts`에 의존성을 추가하세요:

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

### 1. MVI Contract 정의

```kotlin
sealed interface LoginIntent : Intent {
    // ViewAction: View에서 호출 (어노테이션 매칭을 위해 권장되는 패턴)
    sealed interface ViewAction : LoginIntent {
        data class EmailChanged(val email: String) : ViewAction
        data object LoginClicked : ViewAction
    }

    // Internal: ViewModel 내부에서만 호출
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

### 2. ViewModel 작성

```kotlin
class LoginViewModel : MviViewModel<LoginViewState, LoginIntent, LoginSideEffect>(
    initialState = LoginViewState()
) {
    // 핸들러에 어노테이션 추가
    @ViewActionHandler
    internal fun handleEmailChanged(intent: LoginIntent.ViewAction.EmailChanged) = handler {
        reduce { copy(email = intent.email) }  // 상태 업데이트
    }

    @ViewActionHandler(executionMode = ExecutionMode.DROP)
    internal fun handleLoginClicked(intent: LoginIntent.ViewAction.LoginClicked) = handler {
        reduce { copy(isLoading = true) }
        // API 호출...
        dispatch(LoginIntent.Internal.OnLoginSuccess)  // 내부 Intent 전달
    }

    @InternalHandler
    internal fun handleOnLoginSuccess(intent: LoginIntent.Internal.OnLoginSuccess) = handler {
        reduce { copy(isLoading = false) }
        postSideEffect(LoginSideEffect.NavigateToHome)  // SideEffect 발행
    }
}
```

### 3. Compose에서 사용하기

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

## 주요 기능

### ExecutionMode 전략
동시 Intent 처리 방식 제어:
- `DROP` - 실행 중이면 새 요청 무시 (중복 클릭 방지)
- `CANCEL_PREVIOUS` - 이전 요청 취소하고 최신 요청 실행 (검색 디바운스)
- `QUEUE` - 순차적으로 실행
- `PARALLEL` - 동시 실행 (기본값)

### 디버그 모드
```kotlin
MviViewModel(
    initialState = State(),
    debugMode = true  // 로깅 활성화
)
```

### SavedStateHandle 통합
```kotlin
class MyViewModel(savedStateHandle: SavedStateHandle) : MviViewModel(
    initialState = State(),
    savedStateHandle = savedStateHandle  // Process death 복원
)
```

### Hilt 통합
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : MviViewModel(...)
```

## 샘플 앱

더 많은 예제는 **[샘플 앱](sample/)**을 참고하세요.

## 라이센스

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
