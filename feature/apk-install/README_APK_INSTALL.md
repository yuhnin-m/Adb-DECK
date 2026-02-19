# feature/apk-install — Установка APK

Установка APK-файлов на устройство через drag & drop или диалог выбора файла.

## Функциональность

### Выбор файла

**Два способа:**
1. **Drag & Drop** — перетащить `.apk`-файл в зону на экране
2. **Browse** — открыть диалог выбора файла на хосте (фильтр: `*.apk`)

После выбора отображается имя файла, путь и размер.

---

### Параметры установки

| Параметр | Флаг `adb install` | Описание |
|---|---|---|
| Replace existing | `-r` | Переустановить, если приложение уже есть (сохраняет данные) |
| Grant all permissions | `-g` | Автоматически выдать все запрошенные разрешения |

---

### Установка

**Команда:**
```
adb install [-r] [-g] <path-to-apk>
```

**Пайплайн:**
1. Выбор / получение APK-файла на хосте
2. `adb install` с выбранными параметрами
3. Отображение прогресса установки
4. Результат: успех / ошибка с текстом от ADB

**Типичные ошибки ADB:**
- `INSTALL_FAILED_ALREADY_EXISTS` — приложение установлено, нужен флаг `-r`
- `INSTALL_FAILED_VERSION_DOWNGRADE` — попытка установить старую версию
- `INSTALL_FAILED_INVALID_APK` — повреждённый или невалидный APK
- `INSTALL_FAILED_INSUFFICIENT_STORAGE` — не хватает места

---

### Зона Drag & Drop
- Визуальная обратная связь при наведении файла (подсветка зоны)
- Валидация расширения: принимаются только `.apk`

---

### Состояния UI
- `Idle` — ожидание файла
- `FileSelected` — файл выбран, кнопка Install доступна
- `Installing` — установка в процессе (индикатор прогресса)
- `Success` — установка успешна (с именем пакета)
- `Error` — ошибка (с сообщением)

## Архитектура модуля

```
feature/apk-install/
├── ApkInstallComponent             — публичный интерфейс
├── DefaultApkInstallComponent      — оркестрация состояния
├── ApkInstallState                 — состояния: Idle/FileSelected/Installing/Success/Error
├── ApkInstallError                 — типы ошибок
├── ApkInstallFactory               — фабрика компонента
├── services/
│   ├── ApkInstallService           — интерфейс установки APK
│   ├── DefaultApkInstallService    — реализация через adb install
│   ├── ApkInstallHostFileService   — интерфейс работы с файлом на хосте
│   └── DefaultApkInstallHostFileService — реализация (чтение, валидация)
└── ui/
    ├── ApkInstallScreen            — основной экран
    ├── ApkInstallContent           — содержимое: файл + параметры + кнопка
    ├── ApkInstallDropZone          — зона drag & drop
    └── ApkInstallPreviews          — preview-composables
```

## Связи с другими модулями
- После успешной установки — возможен переход в `feature/packages` для просмотра установленного пакета.
- Дополняет `feature/file-explorer`: если APK найден через файловый менеджер, можно установить его отсюда.
