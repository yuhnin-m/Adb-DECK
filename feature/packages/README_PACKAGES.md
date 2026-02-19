# feature/packages — Менеджер пакетов

Просмотр, фильтрация и управление установленными приложениями на устройстве. Поддерживает работу с разрешениями, экспорт APK и деструктивные действия с подтверждением.

## Функциональность

### Список пакетов

**Загрузка:**
```
adb shell pm list packages -f [-s|-3] [-d] [--user 0]
```
- Получение всех установленных пакетов с путями к APK

**Фильтры:**

| Фильтр | Описание |
|---|---|
| All | Все пакеты |
| User | Только пользовательские (`pm list packages -3`) |
| System | Только системные (`pm list packages -s`) |
| Disabled only | Только отключённые (`pm list packages -d`) |
| Debuggable only | Только с флагом `FLAG_DEBUGGABLE` |

**Сортировка:**
- По имени пакета (по умолчанию)
- По label (отображаемому имени)

**Поиск:**
- По package name
- По label приложения

---

### Детали пакета (правая панель)

**Основная информация:**
- Package name, label (отображаемое имя)
- Version name, version code
- Target SDK, Min SDK
- Путь к APK / APKs
- Флаги: debuggable, system app, enabled/disabled
- UID

**Разрешения:**
- Полный список запрошенных разрешений
- Состояние каждого: granted / denied
- Группировка по категории (если доступно)

---

### Действия над пакетом

| Действие | Команда | Примечание |
|---|---|---|
| **Launch** | `adb shell am start -n <pkg>/.MainActivity` | Запуск главной активности |
| **Force Stop** | `adb shell am force-stop <pkg>` | Принудительная остановка |
| **App Info** | `adb shell am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:<pkg>` | Открыть настройки приложения на устройстве |
| **Grant permission** | `adb shell pm grant <pkg> <permission>` | Выдача разрешения |
| **Revoke permission** | `adb shell pm revoke <pkg> <permission>` | Отзыв разрешения |
| **Export APK** | `adb pull <apk-path> <host-dir>` | Сохранить APK на хост |
| **Clear Data** | `adb shell pm clear <pkg>` | ⚠️ С подтверждением |
| **Uninstall** | `adb shell pm uninstall <pkg>` | ⚠️ С подтверждением |
| **Track in Logcat** | — | Переход в Logcat с фильтром по пакету |

**Экспорт APK:**
- Автодетект формата: `.apk` для обычных пакетов, `.apks` для split-пакетов
- Путь назначения — диалог выбора директории на хосте

---

### Feedback
- Баннер успеха / ошибки с автоскрытием (~3 сек)
- Подтверждение для деструктивных операций (Clear Data, Uninstall)

## Архитектура модуля

```
feature/packages/
├── PackagesComponent           — публичный интерфейс
├── DefaultPackagesComponent    — список, детали, действия
├── PackagesState               — PackagesListState, PackageDetailState,
│                                 PackageTypeFilter, PackageSortOrder,
│                                 PendingPackageAction, ActionFeedback
└── ui/
    ├── PackagesScreen          — двухпанельный layout
    ├── PackageListPane         — список с фильтрами и поиском
    └── PackageDetailPanel      — детали + кнопки действий + список разрешений
```

## Связи с другими модулями
- «Track in Logcat» → `feature/logcat` с предустановленным фильтром по пакету.
- «App Info» использует `am start` аналогично `feature/deep-links`.
- Из `feature/system-monitor` (Processes): переход в Packages по package name процесса.
- Из `feature/notifications`: кнопка «Открыть в Packages» по package name уведомления.
