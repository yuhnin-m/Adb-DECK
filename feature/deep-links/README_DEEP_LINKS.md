# feature/deep-links — Диплинки и интенты

Отправка `am start` команд на устройство в двух режимах: простой диплинк и полная форма интента. Включает историю запусков, шаблоны и автодополнение.

## Функциональность

### Режим Deep Link
Упрощённый ввод URL с параметрами.

**Поля:**
- URL — адрес с автодополнением по схемам (`http://`, `https://`, кастомные схемы)
- Query-параметры — таблица ключ → значение, автоматически подставляются в URL
- Intent Flags — выбор через диалог (см. ниже)

**Результирующая команда:**
```
adb shell am start -a android.intent.action.VIEW -d "<url>[?key=value&...]" [flags]
```

---

### Режим Intent (полная форма)
Полный контроль над всеми полями `am start`.

**Поля:**

| Поле | Параметр `am start` | Описание |
|---|---|---|
| Action | `-a <action>` | `android.intent.action.VIEW`, `MAIN`, `SEND`, … (autocomplete) |
| Data URI | `-d <uri>` | Произвольный URI |
| MIME Type | `-t <type>` | `text/plain`, `image/*`, … |
| Component | `-n <pkg/.Class>` | Явный компонент: `com.example/.MainActivity` |
| Categories | `-c <category>` | `LAUNCHER`, `BROWSABLE`, … (мультивыбор) |
| Extras | `--es/--ei/--el/--ef/--ez/--eu` | Ключ + тип + значение |
| Intent Flags | `-f <hex>` | Выбор через диалог |

**Типы Extras:**

| Тип | Параметр |
|---|---|
| String | `--es key value` |
| Int | `--ei key value` |
| Long | `--el key value` |
| Float | `--ef key value` |
| Double | `--ef key value` |
| Boolean | `--ez key true/false` |
| URI | `--eu key value` |

**Результирующая команда:**
```
adb shell am start \
  -a android.intent.action.SEND \
  -d content://... \
  -t text/plain \
  -n com.example/.ShareActivity \
  -c android.intent.category.DEFAULT \
  --es subject "Hello" \
  --ei count 42 \
  --ez silent true \
  -f 0x10000000
```

---

### Диалог Intent Flags
- Полный каталог флагов `FLAG_ACTIVITY_*` и `FLAG_*` с чекбоксами
- **Пресеты:**
  - Clear Task + New Task
  - Single Top
  - No Animation
  - и другие типовые комбинации
- Валидация конфликтующих флагов — подсветка с предупреждением
- Итоговое значение показывается в hex (`-f 0x…`) и десятичном виде

---

### История запусков
- Автоматически сохраняется каждый успешный запуск
- Поля записи: timestamp, режим (DeepLink / Intent), все параметры формы
- Восстановление из истории: клик → заполнение формы
- Удаление отдельных записей
- Очистка всей истории

### Шаблоны
- Сохранить текущую форму как именованный шаблон (диалог ввода имени)
- Загрузить шаблон → заполнение формы
- Удалить шаблон
- Персистентность: `~/.adbdeck/deep-links.json`

### Результат запуска
- Вкладка с итоговой командой (текст для копирования)
- Статус: успех / ошибка + сообщение от ADB
- Feedback-баннер с автоскрытием

### Autocomplete
- Поле Action: часто используемые action-строки
- Поле Data: типовые схемы и примеры URI
- Поле Package: список установленных пакетов с устройства
- Поле Component: классы из выбранного пакета

## Архитектура модуля

```
feature/deep-links/
├── DeepLinksComponent              — публичный интерфейс
├── DefaultDeepLinksComponent       — оркестрация состояния, делегирование хендлерам
├── DeepLinksStorage                — персистентность (~/.adbdeck/deep-links.json)
├── DeepLinksState                  — полная модель UI-состояния
├── DeepLinksUiStateSlices          — иммутабельные срезы для Compose
├── IntentFlagsCatalog              — каталог флагов, пресеты, валидация конфликтов
├── DeepLinksSuggestions            — данные для автодополнения
├── handlers/
│   ├── DeepLinksLaunchHandler      — построение команды, выполнение, запись истории
│   ├── DeepLinksHistoryHandler     — restore/delete/clear истории
│   ├── DeepLinksTemplatesHandler   — CRUD шаблонов
│   └── DeepLinksStateMappers       — конвертация state ↔ параметры команды
└── ui/
    ├── DeepLinksScreen             — layout: левая форма + правые вкладки
    ├── DeepLinksFormPanel          — форма Deep Link / Intent с редактором extras
    ├── DeepLinksRightPanel         — вкладки: Command / History / Templates
    ├── DeepLinksStatusBar          — статусбар: устройство, счётчики
    ├── DeepLinksDialogs            — диалог сохранения шаблона
    ├── DeepLinksIntentFlagsDialog  — диалог выбора флагов с пресетами и валидацией
    └── DeepLinksSuggestionTextField — TextField с autocomplete-дропдауном
```

## Связи с другими модулями
- Из `feature/notifications`: кнопка «Открыть в Deep Links» заполняет форму content intent из уведомления.
- Из `feature/packages`: кнопка «Launch» использует ту же `am start` логику.
