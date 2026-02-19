# ADB Deck — Гайд по созданию feature-модуля

Эталонный документ для создания новых экранов. Примеры взяты из реально существующего кода проекта (`feature/packages`, `feature/logcat`, `feature/notifications`).

---

## Содержание

1. [Структура модуля](#1-структура-модуля)
2. [Атомарность файлов](#2-атомарность-файлов)
3. [Слои и ответственности](#3-слои-и-ответственности)
4. [build.gradle.kts](#4-buildgradlekts)
5. [Локализация и строки](#5-локализация-и-строки)
6. [Дизайн-система: цвета, размеры, скругления](#6-дизайн-система-цвета-размеры-скругления)
7. [core/ui — готовые компоненты](#7-coreui--готовые-компоненты)
8. [core/utils — утилиты](#8-coreutils--утилиты)
9. [ADB API: core/adb-api и core/adb-impl](#9-adb-api-coreadb-api-и-coreadb-impl)
10. [DeviceManager](#10-devicemanager)
11. [DI — внедрение зависимостей](#11-di--внедрение-зависимостей)
12. [Интеграция нового экрана в app](#12-интеграция-нового-экрана-в-app)
13. [Кросс-фичевая навигация](#13-кросс-фичевая-навигация)
14. [Запрет `!!` — безопасная работа с nullable](#14-запрет----безопасная-работа-с-nullable)
15. [Compose: оптимизация и паттерны](#15-compose-оптимизация-и-паттерны)
16. [KDoc и комментарии](#16-kdoc-и-комментарии)
17. [Превью](#17-превью)
18. [Чеклист](#18-чеклист)

---

## 1. Структура модуля

```
feature/<name>/
├── build.gradle.kts
└── src/main/
    ├── composeResources/
    │   ├── values/strings.xml          # Строки EN (базовые)
    │   └── values-ru/strings.xml       # Строки RU
    └── kotlin/com/adbdeck/feature/<name>/
        ├── <Name>State.kt              # Модели данных + enums + sealed interfaces
        ├── <Name>Component.kt          # Публичный интерфейс компонента
        ├── Default<Name>Component.kt   # Реализация компонента
        ├── storage/
        │   └── <Name>Storage.kt        # Персистенция (если нужна)
        └── ui/
            ├── <Name>Screen.kt         # Корневой экран (вход в UI)
            ├── <Name>DetailPanel.kt    # Правая панель деталей (если есть master-detail)
            └── <Name>Previews.kt       # @Preview-функции
```

**Эталон**: `feature/packages` — наиболее полная реализация всех паттернов.

---

## 2. Атомарность файлов

### Правило: один файл — одна ответственность

| Файл | Содержит | Лимит |
|---|---|---|
| `<Name>State.kt` | data class состояния, enums, sealed interfaces, вспомогательные data class | — |
| `<Name>Component.kt` | `interface` с методами; KDoc на каждый метод | — |
| `Default<Name>Component.kt` | Только реализация компонента | ~600 строк |
| `<Name>Screen.kt` | Корневой `@Composable`, toolbar, список, status bar | ~300 строк |
| `<Name>DetailPanel.kt` | Панель деталей (tabs, секции, действия) | ~400 строк |
| `<Name>Previews.kt` | Preview-стаб + @Preview функции | — |

### Когда выносить в отдельный файл

- Если composable-функция > 150 строк и логически самостоятельна — выносить.
- Если панель настроек сложная (как `LogcatSettingsPanel`) — выносить в `<Name>SettingsPanel.kt`.
- Секции внутри детали (Permissions, Flags) — выносить только если > 200 строк со своей логикой.
- Вспомогательные sealed class ошибок (`<Name>Error`) — можно в отдельный `<Name>Error.kt`.

### Чего не дробить

Маленькие composable-функции (< 50 строк), которые используются только в одном файле, остаются в нём же как `private fun`.

---

## 3. Слои и ответственности

```
UI Layer          <Name>Screen.kt / <Name>DetailPanel.kt
                  ↓ читает state, вызывает методы компонента
Business Layer    Default<Name>Component.kt
                  ↓ использует клиентов, менеджеры
Data Layer        core/adb-impl (SystemXxxClient)
                  ↓ выполняет adb-команды через ProcessRunner
Infrastructure    core/process (ProcessRunner)
```

### `<Name>State.kt`

Содержит **только данные**, никакой логики.

```kotlin
// Enums — фильтры, вкладки, режимы
enum class FooFilter { ALL, ACTIVE, SAVED }
enum class FooTab { DETAILS, RAW }

// Sealed interface состояния загрузки
sealed interface FooListState {
    data object NoDevice  : FooListState
    data object Loading   : FooListState
    data class  Success(val items: List<FooItem>) : FooListState
    data class  Error(val message: String) : FooListState
}

// Корневое состояние — одно на экран
data class FooState(
    val activeDeviceId: String? = null,
    val listState: FooListState = FooListState.NoDevice,
    val filter: FooFilter = FooFilter.ALL,
    val selectedItem: FooItem? = null,
    val isRefreshing: Boolean = false,
    val feedback: FooFeedback? = null,
)
```

### `<Name>Component.kt`

Только `interface`. Методы разбивать на смысловые группы с KDoc.

```kotlin
interface FooComponent {
    val state: StateFlow<FooState>

    // ── Загрузка ─────────────────────────────────────────────────
    /** Выполнить запрос и обновить список. */
    fun onRefresh()

    // ── Фильтрация ───────────────────────────────────────────────
    fun onFilterChanged(filter: FooFilter)

    // ── Выбор ────────────────────────────────────────────────────
    fun onSelectItem(item: FooItem)
    fun onCloseDetail()
}
```

### `Default<Name>Component.kt`

```kotlin
class DefaultFooComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val fooClient: FooClient,
    private val settingsRepository: SettingsRepository,
    private val onOpenInOtherFeature: (String) -> Unit = {},
) : FooComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()           // Essenty, Dispatchers.Main
    private val _state = MutableStateFlow(FooState())
    override val state: StateFlow<FooState> = _state.asStateFlow()

    private var loadJob: Job? = null               // отменять при повторном вызове
    private var feedbackJob: Job? = null           // авто-скрытие feedback через 3 сек

    init {
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                // реагировать на смену устройства
            }
        }
    }

    override fun onRefresh() {
        loadJob?.cancel()
        loadJob = scope.launch {
            _state.update { it.copy(isRefreshing = true, listState = FooListState.Loading) }
            // ...
        }
    }

    // Авто-скрытие feedback
    private fun showFeedback(message: String, isError: Boolean = false) {
        feedbackJob?.cancel()
        _state.update { it.copy(feedback = FooFeedback(message, isError)) }
        feedbackJob = scope.launch {
            delay(3_000)
            _state.update { it.copy(feedback = null) }
        }
    }
}
```

**Важно**: `coroutineScope()` от Essenty использует `Dispatchers.Main`. Файловые операции всегда оборачивать в `withContext(Dispatchers.IO)`.

---

## 4. build.gradle.kts

Эталон — `feature/packages/build.gradle.kts`:

```kotlin
/** Модуль feature:<name> — <описание одной строкой>. */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    // alias(libs.plugins.kotlin.serialization)  // только если нужна персистенция
}

dependencies {
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)   // ВСЕГДА — для Icons.Outlined.*
    implementation(compose.ui)
    implementation(compose.components.uiToolingPreview)
    implementation(compose.components.resources)    // ВСЕГДА — для строковых ресурсов

    implementation(libs.decompose)
    implementation(libs.decompose.extensions.compose)
    implementation(libs.coroutines.core)
    implementation(libs.essenty.lifecycle.coroutines)
    // implementation(libs.serialization.json)     // только если нужна персистенция

    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:adb-api"))
    implementation(project(":core:settings"))
    // implementation(project(":core:i18n"))       // для доступа к common-строкам
    // implementation(project(":core:utils"))      // для StringExtensions и др.
}
```

**Порядок** при добавлении фичи в `settings.gradle.kts`:
```kotlin
include(":feature:<name>")
```

---

## 5. Локализация и строки

### Структура файлов

```
src/main/composeResources/
├── values/strings.xml        # Базовые строки (EN) — ОБЯЗАТЕЛЬНО
└── values-ru/strings.xml     # Переводы (RU)
```

Оба файла должны содержать **одинаковые ключи**.

### Соглашение об именовании ключей

```
<feature>_<section>_<element>
```

Примеры из `feature/packages`:
```xml
<string name="packages_toolbar_refresh">Refresh</string>
<string name="packages_filter_all">All</string>
<string name="packages_empty_no_device">No device selected.</string>
<string name="packages_status_count_total">%1$d packages</string>
<string name="packages_feedback_app_launched">App launched</string>
<string name="packages_error_unknown">Unknown error</string>
<string name="packages_dialog_uninstall_title">Uninstall app</string>
```

**Секции**: `toolbar`, `filter`, `empty`, `loading`, `status`, `action`, `feedback`, `error`, `dialog`, `detail`, `meta`.

### Форматирование строк с аргументами

```xml
<string name="foo_status_count">%1$d items</string>
<string name="foo_feedback_copied">Copied: %1$s</string>
<string name="foo_error_details">Error: %1$s</string>
```

Использование в коде:
```kotlin
stringResource(Res.string.foo_status_count, state.items.size)
```

### Common strings — `core:i18n`

Модуль `core/i18n` содержит общие строки для **повторяющихся действий**. Подключать через `implementation(project(":core:i18n"))`.

Доступные ключи:
```
common_action_start         → "Старт" / "Start"
common_action_stop          → "Стоп" / "Stop"
common_action_clear         → "Очистить" / "Clear"
common_action_confirm       → "Подтвердить" / "Confirm"
common_action_cancel        → "Отмена" / "Cancel"
common_action_refresh       → "Обновить" / "Refresh"
common_action_close         → "Закрыть" / "Close"
common_action_settings      → "Настройки" / "Settings"
common_placeholder_search   → "Поиск..." / "Search..."
common_error_generic        → "Ошибка" / "Error"
common_error_unknown        → "Неизвестная ошибка" / "Unknown error"
common_error_with_details   → "Ошибка: %1$s" / "Error: %1$s"
```

Перед добавлением строки в свою фичу — проверить, нет ли её в `core/i18n`.

### Получение строк в компоненте (suspend)

```kotlin
// В корутине компонента — для feedback-сообщений
val message = getString(Res.string.foo_feedback_success)
showFeedback(message)
```

### Правило: ноль хардкода строк

Любая строка, видимая пользователю, должна быть в XML-ресурсе. Исключение — технические строки в логах/отладке.

---

## 6. Дизайн-система: цвета, размеры, скругления

### Цвета — `AdbTheme`

**Никогда не писать `Color(0xFF...)` в feature-модулях.** Всегда использовать токены.

```kotlin
import com.adbdeck.core.designsystem.AdbTheme

// Material3 роли (светлая/тёмная тема автоматически)
AdbTheme.colorScheme.primary
AdbTheme.colorScheme.onPrimary
AdbTheme.colorScheme.surface
AdbTheme.colorScheme.onSurface
AdbTheme.colorScheme.surfaceVariant
AdbTheme.colorScheme.onSurfaceVariant
AdbTheme.colorScheme.error
AdbTheme.colorScheme.outline

// Семантические цвета (info/success/warning)
AdbTheme.semanticColors.info      // синий
AdbTheme.semanticColors.success   // зелёный
AdbTheme.semanticColors.warning   // оранжевый
```

`MaterialTheme.colorScheme` — то же самое, что `AdbTheme.colorScheme`; можно использовать напрямую как короткую запись.

### Базовая палитра (для иконок, chip-цветов)

```kotlin
import com.adbdeck.core.designsystem.*

AdbDeckBlue   // 0xFF1976D2 — primary акцент
AdbDeckTeal   // 0xFF00897B — secondary
AdbDeckRed    // 0xFFD32F2F — ошибка
AdbDeckGreen  // 0xFF388E3C — успех
AdbDeckAmber  // 0xFFF57C00 — предупреждение
```

### Отступы — `Dimensions`

```kotlin
import com.adbdeck.core.designsystem.Dimensions

Dimensions.paddingXSmall  // 4.dp
Dimensions.paddingSmall   // 8.dp
Dimensions.paddingMedium  // 12.dp
Dimensions.paddingDefault // 16.dp
Dimensions.paddingLarge   // 24.dp
Dimensions.paddingXLarge  // 32.dp

Dimensions.iconSizeNav    // 20.dp
Dimensions.iconSizeCard   // 24.dp
Dimensions.iconSizeLarge  // 48.dp (пустые состояния)
```

### Скругления — `AdbCornerRadius`

```kotlin
import com.adbdeck.core.designsystem.AdbCornerRadius

AdbCornerRadius.NONE.value    // 0.dp
AdbCornerRadius.SMALL.value   // 6.dp
AdbCornerRadius.MEDIUM.value  // 10.dp  ← кнопки, чипы
AdbCornerRadius.LARGE.value   // 14.dp  ← карточки
AdbCornerRadius.XLARGE.value  // 18.dp
```

Также доступны через `Dimensions`:
```kotlin
Dimensions.cardCornerRadius   // AdbCornerRadius.LARGE.value
Dimensions.buttonCornerRadius // AdbCornerRadius.MEDIUM.value
```

### Типографика — `MaterialTheme.typography`

```
headlineLarge   28sp / SemiBold   — заголовки страниц
titleLarge      16sp / Medium     — заголовки секций
titleMedium     14sp / Medium     — подзаголовки
bodyLarge       14sp / Normal     — основной текст
bodyMedium      13sp / Normal     — вторичный текст ✓ ЧАСТО
bodySmall       12sp / Normal     — мелкий текст, статус-бар
labelLarge      13sp / Medium     — кнопки
labelMedium     12sp / Medium     — чипы, метки
labelSmall      11sp / Medium     — бейджи, теги
```

---

## 7. core/ui — готовые компоненты

Подключение: `implementation(project(":core:ui"))` — уже есть в стандартном `build.gradle.kts`.

### Состояния загрузки

```kotlin
// Загрузка
LoadingView(message = stringResource(Res.string.foo_loading))

// Пустое состояние
EmptyView(message = stringResource(Res.string.foo_empty_no_device))

// Ошибка
ErrorView(
    message = error.message,
    onRetry = component::onRefresh,  // кнопка "Повторить" опциональна
)
```

### AdbBanner — feedback-баннер

Для временных сообщений об успехе/ошибке после действий:

```kotlin
// В State
data class FooFeedback(val message: String, val isError: Boolean = false)

// В UI (поверх контента)
Box(modifier = Modifier.fillMaxSize()) {
    // ... основной контент ...

    state.feedback?.let { fb ->
        AdbBanner(
            message = fb.message,
            type = if (fb.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
            onDismiss = component::onDismissFeedback,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(Dimensions.paddingDefault),
        )
    }
}
```

### AdbButtons, AdbTextFields, AdbSegmentedButtons

Используются для кастомных кнопок и полей ввода в стиле проекта. Смотри примеры в `core/ui/src/main/kotlin/com/adbdeck/core/ui/buttons/` и `textfields/`.

---

## 8. core/utils — утилиты

Подключение: `implementation(project(":core:utils"))`.

### StringExtensions

```kotlin
import com.adbdeck.core.utils.*

// Обрезать строку с многоточием
"very long package name".truncate(30)  // "very long package na…"

// null если пустая строка
"".nullIfBlank()   // null
"foo".nullIfBlank() // "foo"

// Форматирование размеров
1_048_576L.formatKb()     // "1.0 GB"
2_048L.formatKb()          // "2.0 MB"
1_073_741_824L.formatBytes() // "1.0 GB"
```

### ResultExtensions — корутинная безопасность

**Всегда** использовать вместо `runCatching` в suspend-функциях:

```kotlin
import com.adbdeck.core.utils.runCatchingPreserveCancellation

// Правильно — не подавляет CancellationException
val result = runCatchingPreserveCancellation {
    fooClient.getData(deviceId, adbPath)
}

// Неправильно — может сломать cooperative cancellation
val result = runCatching { fooClient.getData(deviceId, adbPath) }
```

---

## 9. ADB API: core/adb-api и core/adb-impl

### Добавление нового клиента

**Шаг 1** — интерфейс в `core/adb-api`:

```
core/adb-api/src/main/kotlin/com/adbdeck/core/adb/api/<domain>/
├── FooItem.kt           # Domain-модели (data class)
└── FooClient.kt         # Интерфейс с suspend fun
```

```kotlin
// FooItem.kt
data class FooItem(
    val id: String,
    val name: String,
    val value: Int,
)

// FooClient.kt
interface FooClient {
    /** Получить список элементов через `adb shell ...`. */
    suspend fun getItems(deviceId: String, adbPath: String = "adb"): Result<List<FooItem>>
}
```

**Шаг 2** — реализация в `core/adb-impl`:

```
core/adb-impl/src/main/kotlin/com/adbdeck/core/adb/impl/<domain>/
└── SystemFooClient.kt
```

```kotlin
class SystemFooClient(
    private val processRunner: ProcessRunner,
) : FooClient {

    override suspend fun getItems(deviceId: String, adbPath: String): Result<List<FooItem>> =
        runCatchingPreserveCancellation {
            val result = processRunner.run(
                listOf(adbPath, "-s", deviceId, "shell", "your-command")
            )
            parseOutput(result.stdout)
        }

    private fun parseOutput(output: String): List<FooItem> {
        // Парсинг — best-effort, не кидать исключения на плохих данных
        return output.lines()
            .mapNotNull { line -> parseLine(line) }
    }
}
```

### Принципы парсинга

- Все поля nullable — плохие данные → `null`, не краш
- `rawBlock` (если есть) хранить для debug view
- Парсинг полностью в `Dispatchers.IO` (processRunner уже это делает внутри)

---

## 10. DeviceManager

`DeviceManager` — синглтон, предоставляет активное устройство.

```kotlin
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState

class DefaultFooComponent(
    private val deviceManager: DeviceManager,
    // ...
) {
    init {
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                val deviceId = device
                    ?.takeIf { it.state == DeviceState.DEVICE }
                    ?.deviceId

                if (deviceId != null) {
                    loadData(deviceId)
                } else {
                    _state.update { it.copy(listState = FooListState.NoDevice) }
                }
            }
        }
    }

    private fun loadData(deviceId: String) {
        loadJob?.cancel()
        loadJob = scope.launch {
            val adbPath = settingsRepository.getSettings().adbPath.ifBlank { "adb" }
            _state.update { it.copy(listState = FooListState.Loading) }

            fooClient.getItems(deviceId, adbPath)
                .onSuccess { items ->
                    _state.update { it.copy(listState = FooListState.Success(items)) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(listState = FooListState.Error(e.message ?: "Unknown error"))
                    }
                }
        }
    }
}
```

### Проверка доступности устройства перед действием

```kotlin
override fun onDoAction() {
    val device = deviceManager.selectedDeviceFlow.value
    if (device == null || device.state != DeviceState.DEVICE) {
        showFeedback(/* deviceUnavailable string */, isError = true)
        return
    }
    val adbPath = settingsRepository.getSettings().adbPath.ifBlank { "adb" }
    scope.launch {
        fooClient.doSomething(device.deviceId, adbPath)
            .onSuccess { showFeedback(/* success string */) }
            .onFailure { showFeedback(/* error string */, isError = true) }
    }
}
```

---

## 11. DI — внедрение зависимостей

### Регистрация клиента в `AppModule.kt`

```kotlin
// app/src/main/kotlin/com/adbdeck/app/di/AppModule.kt
singleOf(::SystemFooClient) bind FooClient::class
```

Все зависимости — **singletons** (`singleOf` / `single<Interface> { ... }`).

### Получение зависимости в `Main.kt`

```kotlin
// app/src/main/kotlin/com/adbdeck/app/Main.kt
import org.koin.java.KoinJavaComponent.get

val fooClient = get<FooClient>(FooClient::class.java)
```

### Компонент создаётся вручную в `DefaultRootComponent`

Feature-компоненты **не регистрируются в Koin** — они создаются в `DefaultRootComponent.createChild()`:

```kotlin
is Screen.Foo -> RootComponent.Child.Foo(
    DefaultFooComponent(
        componentContext    = componentContext,
        deviceManager       = deviceManager,
        fooClient           = fooClient,
        settingsRepository  = settingsRepository,
        onOpenInOtherFeature = ::openSomethingFromFoo,
    )
)
```

---

## 12. Интеграция нового экрана в app

### Полный список изменяемых файлов

| Файл | Изменение |
|---|---|
| `settings.gradle.kts` | `include(":feature:<name>")` |
| `app/build.gradle.kts` | `implementation(project(":feature:<name>"))` |
| `navigation/Screen.kt` | `data object Foo : Screen` |
| `navigation/RootComponent.kt` | `class Foo(val component: FooComponent) : Child()` |
| `navigation/DefaultRootComponent.kt` | параметр `fooClient`, case `Screen.Foo` в `createChild()` |
| `di/AppModule.kt` | `singleOf(::SystemFooClient) bind FooClient::class` |
| `Main.kt` | `val fooClient = get(FooClient::class.java)` |
| `ui/AppContent.kt` | два места: `activeScreen` и рендер экрана |
| `ui/AppContent.kt` — `Screen.title()` | заголовок для TopBar |
| `ui/Sidebar.kt` | `SidebarNavItem` с иконкой и `Screen.Foo` |
| `ui/AppUiPreviews.kt` | `PreviewFooComponent` stub + wire в `PreviewRootComponent` |

### Как устроен child stack

Полный поток при переходе на новый экран:

```
Sidebar.onNavigate(Screen.Foo)
  → RootComponent.navigate(Screen.Foo)
    → navigation.bringToFront(Screen.Foo)       ← Decompose
      → если компонент уже в стеке:
          переиспользует существующий (состояние сохранено)
        если компонента нет:
          вызывает createChild(Screen.Foo, componentContext)
            → DefaultFooComponent создаётся, init запускается
  → childStack обновляется
    → AppContent.subscribeAsState() получает обновление
      → when(activeChild) is Child.Foo → FooScreen(it.component)
```

#### `Screen.kt` — только конфигурация, без данных

```kotlin
// navigation/Screen.kt
sealed interface Screen {
    data object Dashboard   : Screen
    data object Packages    : Screen
    data object Foo         : Screen   // ← добавить
    // ...
}
```

#### `RootComponent.kt` — обёртка компонента

```kotlin
// navigation/RootComponent.kt
sealed class Child {
    class Dashboard(val component: DashboardComponent) : Child()
    class Packages(val component: PackagesComponent)   : Child()
    class Foo(val component: FooComponent)             : Child()  // ← добавить
}
```

#### `DefaultRootComponent.kt` — фабрика

```kotlin
private fun createChild(screen: Screen, componentContext: ComponentContext): RootComponent.Child =
    when (screen) {
        is Screen.Foo -> RootComponent.Child.Foo(
            DefaultFooComponent(
                componentContext   = componentContext,
                deviceManager      = deviceManager,
                fooClient          = fooClient,
                settingsRepository = settingsRepository,
            )
        )
        // ...
    }
```

#### `AppContent.kt` — рендер активного экрана

В `AppContent` нужно обновить **два места**:

```kotlin
// 1. Определение activeScreen для Sidebar
val activeScreen: Screen = when (activeChild) {
    is RootComponent.Child.Foo -> Screen.Foo       // ← добавить
    // ...
    else -> Screen.Dashboard
}

// 2. Рендер экрана
when (val instance = activeChild) {
    is RootComponent.Child.Foo -> FooScreen(instance.component)  // ← добавить
    // ...
}

// 3. Заголовок в TopBar
private fun Screen.title(): String = when (this) {
    is Screen.Foo -> "Foo"   // ← добавить
    // ...
}
```

---

## 13. Кросс-фичевая навигация

Фичи не знают друг о друге напрямую. Вся кросс-фичевая навигация идёт **только через `DefaultRootComponent`** посредством callback-ов.

### Паттерн: callback в конструкторе компонента

```kotlin
// В DefaultFooComponent — объявить callback-параметр
class DefaultFooComponent(
    // ...
    private val onOpenInPackages: (String) -> Unit = {},
    private val onOpenInLogcat: (String) -> Unit = {},
) : FooComponent, ComponentContext by componentContext {

    override fun onTrackInLogcat(item: FooItem) {
        onOpenInLogcat(item.packageName)
    }
}

// В DefaultRootComponent — передать реализацию
is Screen.Foo -> RootComponent.Child.Foo(
    DefaultFooComponent(
        // ...
        onOpenInPackages = ::openPackageFromFoo,
        onOpenInLogcat   = ::openPackageInLogcat,
    )
)
```

### Два сценария при переходе

#### Сценарий А — целевой компонент уже создан

Компонент живёт в стеке → можно обратиться к нему напрямую:

```kotlin
// DefaultRootComponent.kt
private fun openPackageInLogcat(packageName: String) {
    val normalized = packageName.trim().ifBlank { return }

    // Ищем существующий компонент в стеке
    val existingLogcat = childStack.value.items
        .asSequence()
        .map { it.instance }
        .filterIsInstance<RootComponent.Child.Logcat>()
        .map { it.component }
        .firstOrNull()

    if (existingLogcat != null) {
        // Компонент уже есть — вызываем метод напрямую
        existingLogcat.onPackageFilterChanged(normalized)
    } else {
        // Компонента нет — сохраняем в pending
        pendingPackageForLogcat = normalized
    }

    navigate(Screen.Logcat)
}
```

#### Сценарий Б — целевой компонент ещё не создан (pending)

Значение сохраняется в поле `DefaultRootComponent` и применяется при создании компонента:

```kotlin
// DefaultRootComponent.kt — поле-хранилище
private var pendingPackageForLogcat: String? = null

// В createChild() — прочитать и сбросить через .also { }
is Screen.Logcat -> RootComponent.Child.Logcat(
    DefaultLogcatComponent(
        componentContext = componentContext,
        // ...
    ).also { component ->
        // Применить pending и сразу сбросить
        pendingPackageForLogcat?.also(component::onPackageFilterChanged)
        pendingPackageForLogcat = null
    }
)
```

Альтернатива через `initialXxx`-параметр (когда компонент принимает значение при создании):

```kotlin
// Компонент принимает начальное значение
class DefaultDeepLinksComponent(
    // ...
    initialDeepLinkUri: String? = null,
) {
    init {
        initialDeepLinkUri?.let { onDlUriChanged(it) }
    }
}

// В createChild() — передать и сразу обнулить
is Screen.DeepLinks -> RootComponent.Child.DeepLinks(
    DefaultDeepLinksComponent(
        // ...
        initialDeepLinkUri = pendingDeepLinkUri.also { pendingDeepLinkUri = null },
    )
)
```

### Какой вариант выбрать

| Ситуация | Подход |
|---|---|
| Компонент stateless или не хранит введённые данные | `initialXxx`-параметр |
| Компонент имеет сложное состояние, которое нельзя терять | pending + `component.onXxx()` |
| Нужно передать данные в уже живущий компонент | всегда через `filterIsInstance` + метод |

### Что запрещено

- Импортировать классы одной фичи в другую фичу — зависимость только через `:core:adb-api` интерфейсы
- Хранить ссылку на компонент другой фичи внутри feature-компонента
- Навигировать напрямую из фичи — только через callback

---

## 14. Запрет `!!` — безопасная работа с nullable

**`!!` запрещён** во всём проекте. Оператор маскирует логические ошибки и заменяет понятный контекст на безымянный `NullPointerException`.

### Замены для каждого случая

#### Выполнить действие если не null → `?.let`

```kotlin
// Запрещено
if (record.title != null) Text(record.title!!)

// Правильно
record.title?.let { Text(it) }
```

#### Ранний выход из функции → `?: return`

```kotlin
// Запрещено
val device = deviceManager.selectedDeviceFlow.value!!

// Правильно
val device = deviceManager.selectedDeviceFlow.value ?: return
val deviceId = device.takeIf { it.state == DeviceState.DEVICE }?.deviceId ?: return
```

#### Ранний выход из корутины → `?: return@launch`

```kotlin
scope.launch {
    val item = state.value.selectedItem ?: return@launch
    fooClient.doAction(item)
}
```

#### Цепочка операций → elvis с дефолтом

```kotlin
// Запрещено
val name = record.title!!.trim()

// Правильно
val name = record.title?.trim() ?: ""
val name = record.title.orEmpty().trim()
```

#### Smart cast из другого модуля → локальная `val`

Kotlin не может делать smart cast на `open val` из другого модуля даже после null-check. Решение — скопировать в локальную переменную:

```kotlin
// Не компилируется (smart cast impossible)
if (record.title != null) Text(record.title)

// Правильно — через локальную val (компилятор видит, что она не меняется)
val title = record.title
if (title != null) Text(title)

// Или через ?.let (короче)
record.title?.let { Text(it) }
```

#### Обработка результата из sealed class → `when` или `as?`

```kotlin
// Запрещено
val items = (state.listState as FooListState.Success).items

// Правильно — через as? + elvis
val items = (state.listState as? FooListState.Success)?.items ?: return

// Или через when
when (val ls = state.listState) {
    is FooListState.Success -> ls.items
    else -> return
}
```

#### Когда состояние теоретически невозможно → `error()`

В редких случаях, когда `null` действительно означает баг в логике (не пользовательский ввод):

```kotlin
// Не !! — но явное сообщение об ошибке
val component = childStack.value.active.instance as? RootComponent.Child.Foo
    ?: error("Ожидался Foo, но получен ${childStack.value.active.instance::class.simpleName}")
```

### Быстрая шпаргалка

| Паттерн | Запрещено | Правильно |
|---|---|---|
| Действие если не null | `value!!.doIt()` | `value?.doIt()` |
| Выход из функции | `value!!` | `val v = value ?: return` |
| Выход из корутины | `value!!` | `val v = value ?: return@launch` |
| Дефолт | `value!! ?: ""` | `value ?: ""` или `value.orEmpty()` |
| Smart cast cross-module | `if (x != null) use(x!!)` | `val local = x; if (local != null) use(local)` или `x?.let { use(it) }` |
| Приведение типа | `(x as Foo).bar` | `(x as? Foo)?.bar ?: fallback` |

---

## 15. Compose: оптимизация и паттерны

### `remember` для дорогих вычислений

```kotlin
// Плохо — пересчитывается на каждой рекомпозиции
val savedKeys = state.savedItems.map { it.key }.toSet()
val detectedUri = extractUri(record)  // regex

// Хорошо
val savedKeys = remember(state.savedItems) {
    state.savedItems.map { it.key }.toSet()
}
val detectedUri = remember(record) { extractUri(record) }
```

### `key` в `LazyColumn`

Всегда указывать `key` для корректной анимации и переиспользования:

```kotlin
LazyColumn {
    items(items = state.displayedItems, key = { it.id }) { item ->
        FooRow(item = item)
    }
}
```

### Composable-функции как top-level, не как local fun

```kotlin
// Плохо — local fun, Compose не может отслеживать стабильность
@Composable
fun SomeComposable() {
    fun ActionButton(label: String, onClick: () -> Unit) { ... }
}

// Хорошо — top-level private fun вне родительского composable
@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick) { Text(label) }
}
```

### IO-операции — обязательно `withContext(Dispatchers.IO)`

`coroutineScope()` от Essenty работает на `Dispatchers.Main`. Файловые операции:

```kotlin
// Плохо — блокирует Main thread
scope.launch { File(path).writeText(content) }

// Хорошо
scope.launch {
    val content = json.encodeToString(data)
    withContext(Dispatchers.IO) { File(path).writeText(content) }
}
```

### `O(1)` поиск вместо `O(n)` `.any { }`

```kotlin
// Плохо — O(n) на каждую строку списка
isSaved = state.savedItems.any { it.record.key == record.key }

// Хорошо — O(1) благодаря Set
val savedKeys = remember(state.savedItems) { state.savedItems.map { it.key }.toSet() }
isSaved = savedKeys.contains(record.key)
```

### `HorizontalDivider`, не `Divider`

`Divider` deprecated в Material3:
```kotlin
HorizontalDivider()  // ✓
Divider()            // ✗ deprecated
```

### Единый стиль правых дочерних панелей (master-detail)

Если у экрана есть несколько правых панелей (например, `DetailPanel` и `ComposerPanel`), их shell-оформление должно быть одинаковым.

**Обязательные правила:**
- Разделитель между списком и правой панелью — только `VerticalDivider()`, без кастомных `Box` с `outline.copy(alpha = ...)`.
- Корневой контейнер каждой правой панели должен иметь явный фон `MaterialTheme.colorScheme.surface`.
- Ширина панелей в одном экране должна быть одинаковой (`Dimensions.sidebarWidth * N`), чтобы не было «скачка» компоновки при переключении.

Рекомендуемый паттерн:
```kotlin
// Screen.kt
Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
    // основной контент

    if (isRightPanelOpen) {
        Row {
            VerticalDivider()
            RightPanel(
                modifier = Modifier
                    .width(Dimensions.sidebarWidth * 2)
                    .fillMaxHeight(),
            )
        }
    }
}

// RightPanel.kt
@Composable
fun RightPanel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        // header
        HorizontalDivider()
        // content
    }
}
```

---

## 16. KDoc и комментарии

### Язык: русский

Весь KDoc и inline-комментарии — на **русском языке**.

### KDoc на классах и интерфейсах

```kotlin
/**
 * Реализация [FooComponent].
 *
 * Архитектура взаимодействия с устройством:
 * 1. `init` — подписывается на [DeviceManager.selectedDeviceFlow]
 * 2. При смене устройства запускает загрузку данных
 * 3. Деструктивные действия требуют двухшагового подтверждения
 *
 * @param componentContext   Контекст Decompose (lifecycle, корутин-скоуп).
 * @param deviceManager      Менеджер устройств — источник активного устройства.
 * @param fooClient          ADB-клиент для работы с Foo.
 * @param settingsRepository Репозиторий настроек (путь к adb).
 */
class DefaultFooComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    // ...
```

### KDoc на методах интерфейса

```kotlin
interface FooComponent {
    /**
     * Выполнить `adb shell ...` и обновить список.
     * Вызывается при открытии экрана, смене устройства или нажатии кнопки Refresh.
     */
    fun onRefresh()

    /**
     * Сохранить элемент в локальную коллекцию (персистентно).
     * Если элемент с таким key уже сохранён — игнорируется.
     */
    fun onSaveItem(item: FooItem)
}
```

### KDoc на полях State

```kotlin
/**
 * Полное состояние экрана Foo.
 *
 * @param activeDeviceId  Серийный номер активного устройства или null.
 * @param listState       Состояние загрузки основного списка.
 * @param filter          Текущий фильтр источника записей.
 * @param isRefreshing    `true` во время выполнения refresh.
 */
data class FooState(
    val activeDeviceId: String? = null,
    val listState: FooListState = FooListState.NoDevice,
    // ...
)
```

### Секционные разделители в файлах

Для разбивки длинных файлов на логические группы:

```kotlin
// ── Загрузка ─────────────────────────────────────────────────────────────────

override fun onRefresh() { ... }

// ── Фильтрация ───────────────────────────────────────────────────────────────

override fun onFilterChanged(filter: FooFilter) { ... }

// ── Действия ─────────────────────────────────────────────────────────────────

override fun onCopyItem(item: FooItem) { ... }
```

### Когда НЕ писать комментарий

Если код самоочевиден — комментарий не нужен:

```kotlin
// Не нужно — и так понятно
_state.update { it.copy(isRefreshing = false) }

// Нужно — неочевидное поведение
// При переполнении trySend отбрасывает строки: лучше drop, чем OOM
private val pendingChannel = Channel<Entry>(capacity = 4096)
```

---

## 17. Превью

Каждая фича должна иметь файл `<Name>Previews.kt`.

### Preview-стаб компонента

```kotlin
/**
 * Стаб [FooComponent] для использования в превью и тестах.
 * Все методы — no-op.
 */
class PreviewFooComponent(
    initialState: FooState = FooState(),
) : FooComponent {
    override val state: StateFlow<FooState> = MutableStateFlow(initialState)
    override fun onRefresh() = Unit
    override fun onFilterChanged(filter: FooFilter) = Unit
    // ... все методы интерфейса
}
```

### @Preview функции

Минимум: light, dark, состояние без устройства.

```kotlin
@Preview
@Composable
private fun FooScreenLightPreview() {
    AdbDeckTheme(isDarkTheme = false) {
        FooScreen(
            component = PreviewFooComponent(
                FooState(
                    activeDeviceId = "emulator-5554",
                    listState = FooListState.Success(previewItems),
                )
            )
        )
    }
}

@Preview
@Composable
private fun FooScreenDarkPreview() {
    AdbDeckTheme(isDarkTheme = true) {
        FooScreen(component = PreviewFooComponent(...))
    }
}

@Preview
@Composable
private fun FooNoDevicePreview() {
    AdbDeckTheme(isDarkTheme = false) {
        FooScreen(
            component = PreviewFooComponent(
                FooState(listState = FooListState.NoDevice)
            )
        )
    }
}
```

---

## 18. Чеклист

При создании новой фичи проверить каждый пункт:

### Структура
- [ ] Файлы: `State`, `Component`, `DefaultComponent`, `Screen`, `Previews`
- [ ] `DetailPanel.kt` если есть master-detail layout
- [ ] Ни один файл не превышает 400 строк (экран) / 600 строк (компонент)

### Строки
- [ ] Все UI-строки в `values/strings.xml` + `values-ru/strings.xml`
- [ ] Ключи по конвенции `<feature>_<section>_<element>`
- [ ] Проверены common strings в `core/i18n` — дубли не добавлены

### Дизайн
- [ ] Нет `Color(0xFF...)` хардкода — только `AdbTheme` / `MaterialTheme`
- [ ] Отступы через `Dimensions.*`, не магические числа
- [ ] Скругления через `AdbCornerRadius.*` или `Dimensions.*`
- [ ] `HorizontalDivider` вместо `Divider`

### Компонент
- [ ] `coroutineScope()` от Essenty (не `CoroutineScope(Dispatchers.Main)`)
- [ ] Файловые операции в `withContext(Dispatchers.IO)`
- [ ] `runCatchingPreserveCancellation` вместо `runCatching` в suspend-коде
- [ ] Job-переменные для отменяемых операций: `loadJob`, `detailJob`, `feedbackJob`
- [ ] Feedback auto-dismiss через `delay(3_000)` в `feedbackJob`
- [ ] Подписка на `deviceManager.selectedDeviceFlow` в `init`

### Compose
- [ ] Нет `!!` — только `?.let`, `?: return`, локальные `val`
- [ ] Дорогие вычисления в `remember(key) { ... }`
- [ ] `key = { it.id }` в `LazyColumn`/`LazyRow`
- [ ] Вспомогательные composable — top-level `private fun`, не local fun

### Навигация
- [ ] Кросс-фичевая навигация только через callback в DefaultRootComponent
- [ ] Pending-поле объявлено в DefaultRootComponent, сбрасывается в `createChild()` через `.also { }`
- [ ] `Screen`, `RootComponent.Child`, `createChild()`, `AppContent` (два места + `title()`), `Sidebar` — все обновлены

### DI и интеграция
- [ ] Клиент зарегистрирован в `AppModule.kt` (`singleOf(::SystemFooClient) bind FooClient::class`)
- [ ] Получен в `Main.kt` через `KoinJavaComponent.get()`
- [ ] Добавлен в `settings.gradle.kts` и `app/build.gradle.kts`

### Документация
- [ ] KDoc на классах, интерфейсах, методах интерфейса — на русском
- [ ] Секционные разделители `// ── Название ──────` в длинных файлах
- [ ] `PreviewFooComponent` в `AppUiPreviews.kt`

### Сборка
- [ ] `./gradlew :app:build` → **BUILD SUCCESSFUL**
