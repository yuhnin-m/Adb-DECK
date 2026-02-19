# feature/file-explorer — Файловый менеджер

Двухпанельный файловый менеджер: левая панель — хост (macOS), правая панель — устройство. Поддерживает навигацию, базовые CRUD-операции и перенос файлов между сторонами.

## Функциональность

### Навигация

**Обе панели:**
- Просмотр содержимого директории
- Переход в поддиректорию (двойной клик)
- Переход назад (кнопка Up / хлебные крошки)
- История навигации (back/forward)
- Отображение скрытых файлов (toggle)

**Атрибуты файлов в списке:**

| Поле | Описание |
|---|---|
| Имя | Имя файла / папки |
| Тип | Directory / File / Symlink / Unknown |
| Размер | Человекочитаемый (KB / MB / GB) |
| Дата изменения | Время последнего изменения |

---

### Операции над файлами

Доступны на обеих панелях (хост и устройство):

| Операция | Хост | Устройство | Описание |
|---|---|---|---|
| **Создать директорию** | ✅ | ✅ | Диалог ввода имени |
| **Переименовать** | ✅ | ✅ | Диалог с текущим именем |
| **Удалить** | ✅ | ✅ | С подтверждением |

**Команды на стороне устройства:**
```
adb shell mkdir -p <path>
adb shell mv <old> <new>
adb shell rm -rf <path>
adb shell ls -la <path>
adb shell test -d <path>
```

---

### Перенос файлов

#### Push (хост → устройство)
```
adb push <host-path> <device-path>
```

#### Pull (устройство → хост)
```
adb pull <device-path> <host-path>
```

**Обработка конфликтов:**
- Детектирование: файл с таким именем уже существует в целевой директории
- Диалог с выбором:
  - **Overwrite** — перезаписать
  - **Skip** — пропустить

**Прогресс переноса:**
- Отображение статуса и прогресса (для больших файлов)
- Визуальный индикатор во время передачи

---

### Доступность директорий
- Проверка прав доступа перед входом в директорию устройства (`canAccessDirectory`)
- Корневые директории без доступа помечаются как недоступные
- На не-рутованных устройствах: `/data/`, `/system/` и другие системные директории — только для чтения или полностью закрыты

---

### Feedback
- Баннер успеха / ошибки с автоскрытием
- Сообщения об ошибках ADB (permission denied, no such file, etc.)

## Архитектура модуля

```
feature/file-explorer/
├── FileExplorerComponent           — публичный интерфейс: навигация,
│                                     delete, rename, mkdir, push, pull
├── DefaultFileExplorerComponent    — оркестрация двух панелей, конфликты, прогресс
├── FileExplorerState               — ExplorerSide (LOCAL/DEVICE), ExplorerFileType,
│                                     ExplorerFileItem, ExplorerPanelState,
│                                     TransferState, dialog states
├── services/
│   ├── DeviceFileService           — интерфейс: list, exists, canAccess,
│   │                                 mkdir, delete, rename, push, pull
│   ├── DefaultDeviceFileService    — реализация через ADB shell
│   ├── LocalFileService            — интерфейс: list, mkdir, delete, rename
│   ├── DefaultLocalFileService     — реализация через java.io / java.nio
│   ├── FileTransferService         — интерфейс push/pull с прогрессом
│   └── DefaultFileTransferService  — реализация adb push/pull
└── ui/
    ├── FileExplorerScreen          — двухпанельный layout
    ├── ExplorerPanel               — одна панель (хост или устройство)
    ├── FileListItem                — строка файла / папки
    ├── TransferProgressDialog      — диалог прогресса передачи
    └── ConflictDialog              — диалог обработки конфликтов
```

## Связи с другими модулями
- Используется для просмотра APK-файлов перед установкой (дополняет `feature/apk-install`).
- Позволяет вручную push/pull файлов, которые другие фичи создают на устройстве (скриншоты, temp-файлы нотификаций и т.д.).
