# feature/contacts — Контакты

Просмотр, управление и перенос контактов устройства. Поддерживает чтение, добавление, редактирование и удаление контактов, а также импорт/экспорт в формате VCF.

## Функциональность

### Список контактов
- Загрузка всех контактов с устройства через `content query`
- Поиск по:
  - Имени (display name)
  - Номеру телефона
  - Email

**Команда чтения:**
```
adb shell content query --uri content://com.android.contacts/data \
  --projection display_name:data1:mimetype:...
```

---

### Детали контакта

**Отображаемые поля:**
- Отображаемое имя (display name)
- Имя / Фамилия / Отчество
- Телефоны (с типами: Mobile, Home, Work, …)
- Email-адреса (с типами)
- Организация / Должность
- Заметки
- Фото (аватар, если доступен)

---

### Добавление контакта
**Форма:**
- Имя, Фамилия (обязательно хотя бы одно)
- Несколько телефонных номеров (тип: Mobile / Home / Work / Other)
- Несколько email-адресов (тип: Home / Work / Other)
- Организация

**Команда записи:**
```
adb shell content insert --uri content://com.android.contacts/raw_contacts ...
adb shell content insert --uri content://com.android.contacts/data ...
```

---

### Редактирование контакта
- Та же форма, что и добавление, заполненная текущими данными
- Обновление через:
  ```
  adb shell content update --uri content://com.android.contacts/data --where ...
  ```

---

### Удаление контакта
- С подтверждением
- Команда:
  ```
  adb shell content delete --uri content://com.android.contacts/raw_contacts --where ...
  ```

---

### Экспорт (VCF)
- Экспорт выбранного контакта или всех контактов → `.vcf`-файл на хост
- Формат: vCard 3.0 / 4.0
- Реализован собственный `VcfSerializer` (без внешних зависимостей)
- Поддерживаемые поля в VCF:
  - `FN`, `N` — имя
  - `TEL` — телефон (с типами: CELL, HOME, WORK)
  - `EMAIL` — email (с типами)
  - `ORG`, `TITLE` — организация / должность
  - `NOTE` — заметка
  - `PHOTO` — фото (base64)

---

### Импорт (VCF)
- Импорт контактов из `.vcf`-файла с хоста
- Реализован собственный `VcfParser`
- Разбор стандартных полей vCard, множественные записи в одном файле
- Добавление на устройство через `content insert`

---

### Feedback
- Баннер успеха / ошибки с автоскрытием
- Индикатор загрузки при длительных операциях (для больших VCF-файлов)

## Архитектура модуля

```
feature/contacts/
├── ContactsComponent                       — публичный интерфейс
├── DefaultContactsComponent                — корневая оркестрация состояния
├── DefaultContactsComponentDevice          — операции чтения/записи на устройство
├── DefaultContactsComponentTransfer        — логика импорта/экспорта
├── DefaultContactsComponentSupport         — вспомогательные утилиты
├── DefaultContactsComponentFormAndDelete   — управление формой, удаление
├── ContactsState                           — главное состояние
├── ContactsListState                       — состояние списка
├── ContactDetailState                      — состояние детального просмотра
├── ContactsOperationState                  — состояние текущей операции
├── ContactFeedback                         — типы feedback-сообщений
├── AddContactFormState                     — состояние формы добавления
├── ContactsScreenSlices                    — UI-срезы для Compose
├── io/
│   ├── ContactIoService                    — интерфейс I/O операций
│   ├── VcfParser                           — парсер vCard формата
│   ├── VcfSerializer                       — генератор vCard формата
│   └── ContactJsonModel                    — JSON-модель для сериализации
└── ui/
    ├── ContactsScreen                      — главный layout
    ├── ContactsToolbar                     — тулбар: поиск, импорт/экспорт
    ├── ContactsListPane                    — список контактов
    ├── ContactDetailPanel                  — панель деталей
    ├── ContactDetailContent                — содержимое деталей
    ├── ContactDetailParts                  — секции деталей (телефоны, email, …)
    ├── AddContactDialog                    — диалог добавления контакта
    └── ContactsDialogs                     — прочие диалоги (подтверждение удаления, …)
```

## Связи с другими модулями
- Независимый модуль, не требует других фич.
- При необходимости открытия диалера / звонка — через `feature/deep-links` (`tel:` схема).
