# feature/quick-toggles — Быстрые переключатели

Управление системными настройками устройства через ADB без необходимости взаимодействовать с UI девайса. Мгновенное переключение WiFi, Bluetooth, данных, режима полёта и других параметров.

## Функциональность

### Тогглы

#### WiFi
| Операция | Команда |
|---|---|
| Чтение состояния | `adb shell settings get global wifi_on` |
| Включить | `adb shell svc wifi enable` |
| Выключить | `adb shell svc wifi disable` |

#### Mobile Data
| Операция | Команда |
|---|---|
| Чтение | `adb shell settings get global mobile_data` |
| Включить | `adb shell svc data enable` |
| Выключить | `adb shell svc data disable` |

#### Bluetooth
| Операция | Команда |
|---|---|
| Чтение | `adb shell settings get global bluetooth_on` |
| Включить | `adb shell cmd bluetooth_manager enable` |
| Выключить | `adb shell cmd bluetooth_manager disable` |

#### Airplane Mode
| Операция | Команда |
|---|---|
| Чтение | `adb shell settings get global airplane_mode_on` |
| Включить | `adb shell settings put global airplane_mode_on 1` + broadcast |
| Выключить | `adb shell settings put global airplane_mode_on 0` + broadcast |

> ⚠️ Переключение Airplane Mode, WiFi и Bluetooth требует подтверждения — может прервать ADB-соединение.

#### Stay Awake (не гасить экран при зарядке)
| Операция | Команда |
|---|---|
| Чтение | `adb shell settings get global stay_on_while_plugged_in` |
| Включить | `adb shell settings put global stay_on_while_plugged_in 7` |
| Выключить | `adb shell settings put global stay_on_while_plugged_in 0` |

#### Animations
Независимое управление тремя шкалами анимации:

| Шкала | Чтение | Запись |
|---|---|---|
| Window | `settings get global window_animation_scale` | `settings put global window_animation_scale <value>` |
| Transition | `settings get global transition_animation_scale` | `settings put global transition_animation_scale <value>` |
| Animator | `settings get global animator_duration_scale` | `settings put global animator_duration_scale <value>` |

Доступные значения: `0` (off) / `0.5` / `1.0` / `1.5` / `2.0`

**UI:** три независимых слайдера / кнопки, черновик значений применяется единой кнопкой Apply.

---

### Состояния тогглов
- `ON` — включено
- `OFF` — выключено
- `CUSTOM` — специфическое значение (для Stay Awake: значение отличается от 0/7)
- `UNKNOWN` — не удалось определить (нет прав, нет данных)

---

### Дополнительно
- **Open Settings** — открыть системные настройки на устройстве как fallback:
  ```
  adb shell am start -a android.settings.SETTINGS
  ```
- Обновление всех тогглов — одна кнопка Refresh
- Обновление конкретного тоггла — индивидуальная кнопка рядом с переключателем
- Подтверждение для потенциально опасных действий (потеря ADB-соединения)

## Архитектура модуля

```
feature/quick-toggles/
├── QuickTogglesComponent           — публичный интерфейс
├── DefaultQuickTogglesComponent    — оркестрация, делегирование сервису
├── QuickTogglesState               — QuickToggleId, QuickToggleState (ON/OFF/CUSTOM/UNKNOWN),
│                                     AnimationScaleControl, ToggleItem,
│                                     PendingQuickToggleAction
├── services/
│   ├── QuickTogglesService         — интерфейс: readStatuses, readStatus,
│   │                                 setToggle, readAnimationScales,
│   │                                 setAnimationScale, openSettings
│   └── DefaultQuickTogglesService  — реализация shell-команд
└── ui/
    ├── QuickTogglesScreen          — layout с группами тогглов
    ├── ToggleRow                   — строка тоггла: иконка + label + switch + refresh
    └── AnimationScaleSection       — секция управления анимациями
```

## Связи с другими модулями
- Изменение анимационных шкал (0×) — стандартная практика при разработке/тестировании, дополняет `feature/screen-tools`.
