# core:adb-api

Модуль с публичными контрактами и моделями для ADB-слоя.

## Принцип структуры

`adb-api` организован по доменам, а не по типам файлов.
Каждый подпакет содержит и интерфейсы, и модели, относящиеся к одному контексту.

## Пакеты

- `com.adbdeck.core.adb.api.adb` — базовый ADB-клиент и проверка доступности adb.
- `com.adbdeck.core.adb.api.device` — устройства, состояние, выбор активного устройства, control/info.
- `com.adbdeck.core.adb.api.contacts` — контракты и модели контактов.
- `com.adbdeck.core.adb.api.files` — файловая система устройства.
- `com.adbdeck.core.adb.api.intents` — deep links и explicit intents.
- `com.adbdeck.core.adb.api.logcat` — поток logcat и парсинг.
- `com.adbdeck.core.adb.api.monitoring` — контракты мониторинга и общие системные метрики.
- `com.adbdeck.core.adb.api.monitoring.process` — модели и состояния процессов.
- `com.adbdeck.core.adb.api.monitoring.storage` — модели хранилища и сводные метрики.
- `com.adbdeck.core.adb.api.notifications` — уведомления.
- `com.adbdeck.core.adb.api.packages` — пакеты приложений и операции с ними.
- `com.adbdeck.core.adb.api.screen` — screenshot/screenrecord/install APK.

## Правила добавления нового API

1. Добавляй тип в доменный пакет, которому он принадлежит.
2. Если тип нужен в нескольких доменах, выделяй отдельный поддомен (например, `common`) только при реальной необходимости.
3. Предпочитай один публичный тип на файл (исключение: тесно связанные маленькие enum/data-классы).
4. Пиши KDoc на русском для всех публичных типов и публичных методов.
5. Не складывай новые модели обратно в `com.adbdeck.core.adb.api` без подпакета.
