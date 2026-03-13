# feature/file-system — Файловые системы устройства

Экран отображает информацию о разделах файловой системы Android-устройства на основе `adb shell df`.

## Функциональность
- Автозагрузка данных при выборе/смене активного устройства.
- Ручное обновление через кнопку `Refresh`.
- Сводка по релевантным разделам: `Used / Free / Total / %`.
- Список разделов с прогрессом заполнения и категорией (`System`, `Data`, `External`, `Other`).

## Архитектура
- `FileSystemState.kt` — state-модели экрана.
- `FileSystemComponent.kt` — публичный контракт.
- `DefaultFileSystemComponent.kt` — бизнес-логика загрузки и обновления.
- `ui/FileSystemScreen.kt` — Compose UI.
- `ui/FileSystemPreviews.kt` — preview-стабы.

## Зависимости
- `DeviceManager` — активное устройство.
- `SystemMonitorClient` — получение информации о хранилище.
- `SettingsRepository` — путь к `adb`.
