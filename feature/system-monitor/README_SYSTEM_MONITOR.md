# feature/system-monitor — Системный монитор

Мониторинг процессов и хранилища устройства в реальном времени. Состоит из двух дочерних компонентов: Processes и Storage.

## Функциональность

---

### Processes — Процессы

#### Список процессов
**Источник:**
```
adb shell ps -A
# или
adb shell ps -ef
```

**Отображаемые поля:**

| Поле | Описание |
|---|---|
| PID | Идентификатор процесса |
| PPID | Идентификатор родительского процесса |
| Name | Имя процесса / команды |
| User | UID / имя пользователя |
| CPU% | Загрузка CPU (%) |
| RSS | Resident Set Size (используемая RAM) |
| Package | Связанный Android-пакет (если определяется) |

**Поиск:**
- По имени процесса
- По PID
- По package name

**Сортировка:**

| Поле | Направление |
|---|---|
| Name | A→Z |
| PID | Возрастание |
| Memory (RSS) | Убывание (наиболее прожорливые вверху) |
| CPU% | Убывание |

---

#### Мониторинг (живой режим)
- Кнопка **Start / Stop Monitoring** — периодическое обновление списка
- Интервал обновления: настраиваемый (или фиксированный)
- Бейдж в сайдбаре — показывает, что мониторинг активен
- График истории CPU / памяти для выбранного процесса

---

#### Детали процесса (правая панель)
- PID, PPID, User, Group
- Полный путь к исполняемому файлу
- Аргументы командной строки
- Статус процесса (Running / Sleeping / Zombie / …)
- Количество файловых дескрипторов
- Привязанный Android-пакет (если определяется по UID)
- История CPU / RSS (мини-график за последние N измерений)

---

#### Действия над процессом

| Действие | Команда | Примечание |
|---|---|---|
| **Kill** | `adb shell kill -9 <pid>` | Принудительное завершение по SIGKILL |
| **Force Stop** | `adb shell am force-stop <package>` | Только если есть привязанный пакет |
| **Open in Packages** | — | Переход в `feature/packages` с фокусом на пакете |

---

### Storage — Хранилище

**Источники данных:**
```
adb shell df -h
adb shell dumpsys diskstats
```

**Отображаемая информация:**

| Параметр | Описание |
|---|---|
| Internal Storage | Total / Used / Free для внутреннего хранилища |
| External Storage | Состояние и объём SD-карты (если подключена) |
| Emulated volumes | Раздел `/sdcard` и эмулированные тома |
| App Data | Суммарный объём данных приложений (из diskstats) |

**Визуализация:**
- Прогресс-бар заполненности для каждого раздела
- Цветовая индикация: зелёный (< 70%) / жёлтый (70-90%) / красный (> 90%)

---

### Общая навигация
- Две вкладки: **Processes** / **Storage**
- `isProcessMonitoring: StateFlow<Boolean>` — состояние мониторинга прокидывается в `SystemMonitorComponent` → бейдж в сайдбаре приложения

## Архитектура модуля

```
feature/system-monitor/
├── SystemMonitorComponent                     — корневой компонент: activeTab,
│                                                isProcessMonitoring, дочерние компоненты
├── processes/
│   ├── ProcessesComponent                     — интерфейс: start/stop monitoring,
│   │                                            search, sort, select, kill, force-stop
│   ├── DefaultProcessesComponent              — оркестрация
│   ├── DefaultProcessesComponentFilters       — фильтрация и сортировка
│   ├── DefaultProcessesComponentActions       — kill, force-stop, открытие в packages
│   ├── DefaultProcessesComponentMonitoring    — периодический polling, история
│   ├── DefaultProcessesComponentDetails       — загрузка деталей процесса
│   └── ProcessesState                         — ProcessListState, ProcessDetailState,
│                                                ProcessSortField, ProcessActionFeedback,
│                                                ProcessesState с историей
├── storage/
│   ├── StorageComponent                       — интерфейс: refresh, состояние разделов
│   └── DefaultStorageComponent               — реализация: df + diskstats parsing
└── ui/
    ├── SystemMonitorScreen                    — layout с вкладками
    ├── ProcessesScreen                        — список + детали процессов
    ├── ProcessListItem                        — строка процесса
    ├── ProcessDetailPanel                     — правая панель деталей
    ├── StorageScreen                          — экран хранилища
    └── StorageVolumeCard                      — карточка тома с прогресс-баром
```

## Связи с другими модулями
- **Kill / Force Stop** → немедленное действие, не требует перехода в другую фичу.
- **Open in Packages** → `feature/packages` (фокус на конкретном пакете по имени).
- Бейдж мониторинга отображается в `Sidebar` (`feature/dashboard` / `:app`).
