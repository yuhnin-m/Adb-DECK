# feature/screen-tools — Экранные инструменты

Снятие скриншотов и запись экрана устройства с сохранением на хост. Две вкладки: Screenshot и Screen Record.

## Функциональность

### Вкладка Screenshot

**Настройки:**
- Выходная директория на хосте (диалог выбора)
- Качество изображения — 6 пресетов:

| Пресет | Формат | Описание |
|---|---|---|
| PNG Lossless | PNG | Без потерь, максимальное качество |
| JPEG 95% | JPEG | Почти без потерь |
| JPEG 85% | JPEG | Высокое качество |
| JPEG 75% | JPEG | Среднее качество |
| JPEG 60% | JPEG | Компактный размер |
| JPEG 45% | JPEG | Минимальный размер |

**Действия:**

| Действие | Описание |
|---|---|
| **Take Screenshot** | Снять скриншот и сохранить на хост |
| **Copy to Clipboard** | Скопировать последний скриншот в системный буфер обмена |
| **Open File** | Открыть последний сохранённый файл |
| **Open Folder** | Открыть папку с сохранёнными скриншотами |

**Пайплайн скриншота:**
```
adb shell screencap -p /data/local/tmp/adbdeck_screenshot.png
→ adb pull /data/local/tmp/adbdeck_screenshot.png <host-dir>/<timestamp>.jpg/png
```
Конвертация в JPEG с заданным качеством — на стороне хоста.

---

### Вкладка Screen Record

**Настройки:**
- Выходная директория на хосте (диалог выбора)
- Качество записи — 6 пресетов:

| Пресет | Битрейт | Описание |
|---|---|---|
| Ultra | 20 Mbps | Максимальное качество |
| High | 12 Mbps | Высокое качество |
| Medium | 8 Mbps | Сбалансированный |
| Standard | 5 Mbps | Стандартный |
| Low | 4 Mbps | Экономия места |
| Eco | 3 Mbps | Минимальный размер |

**Действия:**

| Действие | Описание |
|---|---|
| **Start Recording** | Начать запись экрана |
| **Stop Recording** | Остановить запись, сохранить на хост |
| **Open File** | Открыть последний записанный файл |
| **Open Folder** | Открыть папку с записями |

**Фазы записи:**
- `IDLE` → `STARTING` → `RECORDING` → `STOPPING` → `IDLE`
- Прогресс-индикатор: null = indeterminate (STARTING / STOPPING), число = реальный прогресс

**Пайплайн записи:**
```
adb shell screenrecord --bit-rate <bitrate> /data/local/tmp/adbdeck_screenrecord.mp4
# (в фоне, пока пользователь не нажмёт Stop)
→ (остановка процесса)
→ adb pull /data/local/tmp/adbdeck_screenrecord.mp4 <host-dir>/<timestamp>.mp4
```

> **Ограничение платформы:** `screenrecord` поддерживается на Android 4.4+. Максимальная длительность без принудительного указания — 3 минуты (ограничение `screenrecord`). На эмуляторах может не работать в зависимости от конфигурации GPU.

---

### Статус и прогресс
- Строка статуса показывает текущее состояние: Idle / Starting… / Recording (таймер) / Stopping…
- Ошибки — баннер с описанием проблемы

## Архитектура модуля

```
feature/screen-tools/
├── ScreenToolsComponent          — публичный интерфейс
├── DefaultScreenToolsComponent   — оркестрация состояния, делегирование сервисам
├── ScreenToolsState              — ScreenToolsTab, ScreenshotQualityPreset,
│                                   ScreenrecordQualityPreset, RecordingPhase,
│                                   ScreenshotSectionState, ScreenrecordSectionState,
│                                   ScreenToolsStatus
├── services/
│   ├── ScreenshotService         — интерфейс снятия скриншота
│   ├── DefaultScreenshotService  — реализация: screencap + pull + конвертация
│   ├── ScreenrecordService       — интерфейс записи экрана
│   ├── DefaultScreenrecordService — реализация: screenrecord + pull
│   ├── HostFileService           — работа с локальными файлами (открытие, директории)
│   └── ScreenrecordStopError     — типы ошибок остановки записи
└── ui/
    ├── ScreenToolsScreen         — layout с вкладками
    ├── ScreenshotSection         — UI скриншота
    ├── ScreenrecordSection       — UI записи экрана
    └── ScreenToolsStatusBar      — строка статуса
```
