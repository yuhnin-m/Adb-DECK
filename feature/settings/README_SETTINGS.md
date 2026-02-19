# feature/settings — Настройки приложения

Конфигурация самого ADB Deck: путь к ADB, тема оформления и прочие глобальные параметры. Изменения применяются немедленно без перезапуска.

## Функциональность

### ADB Path
- Путь к бинарнику `adb` (кастомный или системный)
- Поле ввода с кнопкой «Browse» (диалог выбора файла)
- Если поле пустое — используется `adb` из `$PATH`
- Валидация: проверка наличия файла и его исполняемости
- Применяется немедленно — следующий ADB-вызов уже использует новый путь

### Тема оформления
- **Light** — светлая тема
- **Dark** — тёмная тема
- **System** — следовать системной теме macOS

Переключение происходит мгновенно — `SettingsRepository.settingsFlow` эмитит новое значение, `Main.kt` подхватывает изменение и передаёт в `AdbDeckTheme`.

### Прочие настройки
(Расширяется по мере роста приложения — добавляются новые параметры в `AppSettings`)

---

### Персистентность
- Все настройки сохраняются в `~/.adbdeck/settings.json`
- Формат: JSON через Kotlinx Serialization
- Читаются при старте приложения

## Архитектура модуля

```
feature/settings/
├── SettingsComponent           — публичный интерфейс: обновление полей
├── DefaultSettingsComponent    — чтение/запись через SettingsRepository
├── SettingsUiState             — модель UI-состояния (adb path, theme, draft values)
└── ui/
    ├── SettingsScreen          — UI настроек
    └── SettingsPreviews        — preview-composables
```

```
core/settings/
├── AppSettings                 — data class: adbPath, appTheme
├── AppTheme                    — enum: LIGHT / DARK / SYSTEM
├── SettingsRepository          — интерфейс: settingsFlow, updateSettings
└── FileSettingsRepository      — реализация: ~/.adbdeck/settings.json
```

## Связи с другими модулями
- `SettingsRepository` используется во всех модулях — через него ADB-клиент получает путь к бинарнику.
- Тема читается в `:app` → `Main.kt` → передаётся в `AppContent` → `AdbDeckTheme`.
