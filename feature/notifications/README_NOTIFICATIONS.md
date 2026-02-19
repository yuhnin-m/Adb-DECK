# feature/notifications — Уведомления

Мониторинг системных уведомлений с устройства: текущие + история + сохранённые. Встроенный конструктор для отправки тестовых уведомлений через `cmd notification post`.

## Функциональность

### Мониторинг уведомлений

**Источник данных:**
```
adb shell dumpsys notification
```

**Типы уведомлений:**

| Тип | Описание |
|---|---|
| Current | Активные уведомления на устройстве в данный момент |
| Historical | Накопленная история сессии (до 500 записей, не сбрасывается при обновлении) |
| Saved | Уведомления, сохранённые пользователем вручную (до 200, персистентны) |

**Фильтрация и поиск:**
- Источник: All / Current / Historical / Saved
- Текстовый поиск: по package name, заголовку, тексту
- Фильтр по пакету (выбирается из детали уведомления)
- Сортировка: Newest first / Oldest first / By package

---

### Детали уведомления

**Вкладка Details:**
- Package name, channel ID, notification ID, tag
- Priority, visibility, category
- Флаги: ongoing, auto-cancel, local-only, foreground service
- Extras: title, text, bigText, subText, infoText, progress
- Действия (кнопки): label, intent
- Иконки: small icon, large icon (превью если image)
- Звук, вибрация, lights

**Вкладка Raw Dump:**
- Полный текст блока из `dumpsys notification` в моноширинном виде
- Прокрутка, возможность копирования

**Вкладка Saved:**
- Список сохранённых уведомлений с заметками и временными метками
- Выбор для просмотра деталей
- Удаление записей

---

### Действия

| Действие | Описание |
|---|---|
| **Copy package name** | Package name → буфер обмена |
| **Copy title** | Title → буфер обмена |
| **Copy text** | Text → буфер обмена |
| **Copy raw dump** | Полный текст dumpsys блока → буфер обмена |
| **Save** | Сохранить уведомление с заметкой |
| **Delete saved** | Удалить из сохранённых |
| **Export JSON** | Все уведомления (current + historical + saved) → JSON-файл на хост |
| **Open in Packages** | Переход в Packages с фокусом на пакете уведомления |
| **Open in Deep Links** | Заполнение формы Deep Links content intent из уведомления |

---

### Конструктор уведомлений (Composer)

Отправка тестового уведомления через ADB.

**Базовая команда:**
```
adb shell cmd notification post [flags] <tag> <text>
```

#### Секция Source
| Поле | Флаг | Описание |
|---|---|---|
| Tag | (позиционный) | Идентификатор уведомления (строка) |
| Channel ID | — | Канал уведомления (если не указан — дефолтный) |
| Notification ID | — | Числовой ID (опционально) |

#### Секция Base
| Поле | Флаг | Описание |
|---|---|---|
| Title | `-t <title>` | Заголовок уведомления |
| Text | (позиционный) | Основной текст |

#### Секция Visual
| Поле | Флаг | Формат значения |
|---|---|---|
| Small icon | `-i <icon>` | Имя drawable ресурса: `ic_notification` |
| Large icon | `-I <icon>` | Имя drawable, `file:///path/to/image.png`, или base64 `data:image/png;base64,...` → staging |

**Large icon staging pipeline:**
1. base64 → decode → временный файл на хосте
2. `adb push` → `/data/local/tmp/adbdeck_notifications/<uuid>.png`
3. `adb shell chmod 0644 /data/local/tmp/adbdeck_notifications/<uuid>.png`
4. URI передаётся как `file:///data/local/tmp/adbdeck_notifications/<uuid>.png`

Файлы на устройстве не удаляются (накапливаются в `/data/local/tmp/adbdeck_notifications/`).

#### Секция Style
Выбор одного стиля (опционально):

| Стиль | Флаг | Дополнительные параметры |
|---|---|---|
| **BigText** | `-S bigtext` | Поле расширенного текста (подставляется как второй аргумент) |
| **BigPicture** | `-S bigpicture --picture <spec>` | Изображение: file path / base64 / data URI → тот же staging pipeline |
| **Inbox** | `-S inbox --line <line>...` | Строки списка, по одной на строку, каждая → отдельный `--line` |
| **Messaging** | `-S messaging [--conversation <title>] --message <who>:<text>...` | Заголовок беседы + сообщения в формате `Имя:текст`, по одному на строку |
| **Media** | `-S media` | Без дополнительных параметров (только стиль) |

#### Секция Intent (опционально)
| Поле | Флаг | Описание |
|---|---|---|
| Content Intent | `-c <intent-spec>` | Явный intent spec: `activity -a android.intent.action.VIEW -d "url"` |
| Deep Link | `-c activity -a android.intent.action.VIEW -d "<url>"` | Альтернативный ввод через URL (генерирует content intent автоматически) |
| Verbose | `-v` | Подробный лог выполнения команды |

> Примечание: Content Intent и Deep Link взаимно исключают друг друга — используется первый заполненный.

#### Примеры команд
```bash
# Простое уведомление
adb shell cmd notification post -t "Hello" my_tag "World"

# BigText с content intent
adb shell cmd notification post \
  -t "Title" \
  -S bigtext \
  -c "activity -a android.intent.action.VIEW -d https://example.com" \
  -v \
  my_tag "Big text content here"

# BigPicture с Large icon из файла
adb shell cmd notification post \
  -t "Photo" \
  -I "file:///data/local/tmp/adbdeck_notifications/icon.png" \
  -S bigpicture --picture "file:///data/local/tmp/adbdeck_notifications/pic.png" \
  my_tag "Check this out"

# Messaging style
adb shell cmd notification post \
  -t "Chat" \
  -S messaging \
  --conversation "Group chat" \
  --message "Alice:Hello" \
  --message "Bob:Hi there" \
  my_tag ""
```

> **Ограничение платформы:** `cmd notification post` не поддерживает action-кнопки ни на каких версиях Android. Секция кнопок в UI отсутствует намеренно.

---

### Персистентность
- Сохранённые уведомления: `~/.adbdeck/notifications-saved.json` (до 200 записей)
- История — только в памяти сессии (до 500 записей)

## Архитектура модуля

```
feature/notifications/
├── NotificationsComponent            — публичный интерфейс
├── DefaultNotificationsComponent     — оркестрация: refresh, фильтрация, сохранение, posting
├── NotificationsStorage              — персистентность сохранённых уведомлений
├── NotificationsState                — NotificationsListState, SavedNotification,
│                                       NotificationsFilter, NotificationsSortOrder,
│                                       NotificationFeedback
├── NotificationsFiltering            — логика фильтрации и сортировки
└── ui/
    ├── NotificationsScreen           — двухпанельный layout + статусбар
    ├── NotificationsListPane         — список с поиском и фильтрами
    ├── NotificationsComposerPanel    — конструктор уведомлений
    ├── NotificationDetailsTab        — вкладка Details
    ├── NotificationRawDumpTab        — вкладка Raw Dump
    └── NotificationsUiUtils          — NOTIFICATION_URI_REGEX, image file dialog, clipboard
```

```
core/adb-api/notifications/
├── NotificationsClient               — интерфейс: getNotifications, postNotification
└── NotificationPostRequest           — параметры POST-запроса

core/adb-impl/notifications/
└── SystemNotificationsClient         — реализация: dumpsys parsing, cmd notification post
```

## Связи с другими модулями
- «Open in Packages» → `feature/packages` (по package name).
- «Open in Deep Links» → `feature/deep-links` (заполнение формы content intent).
- Из `feature/logcat` возможен переход к пакету уведомления.
