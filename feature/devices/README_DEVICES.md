# feature/devices — Устройства

Экран управления подключёнными ADB-устройствами. Точка входа в работу с конкретным устройством: выбор активного девайса для всех остальных фич.

## Функциональность

### Список устройств
- Отображение всех подключённых ADB-устройств (USB + WiFi)
- Статус каждого устройства: `online` / `offline` / `unauthorized` / `recovery` / `fastboot`
- Бейдж транспорта: USB / WiFi
- Автообновление при изменении `adb devices`
- Секция ранее подключенных сетевых устройств (Wi-Fi history): имя, адрес, deviceId
- Быстрое переподключение и удаление записи из Wi-Fi history

### Детали устройства (правая панель)
- Модель, производитель, Android-версия, серийный номер, транспорт
- Навигация по секциям: открытие соответствующих фич (Device Info и т.д.)

### Действия над устройством
| Действие | ADB-команда |
|---|---|
| Reconnect | `adb reconnect <serial>` |
| Reboot — Normal | `adb reboot` |
| Reboot — Recovery | `adb reboot recovery` |
| Reboot — Bootloader | `adb reboot bootloader` |
| Reboot — Fastboot | `adb reboot fastboot` |
| Disconnect (WiFi) | `adb disconnect <host:port>` |

Деструктивные операции (reboot, disconnect) требуют подтверждения.

## Архитектура модуля

```
feature/devices/
├── DevicesComponent          — публичный интерфейс компонента
├── DefaultDevicesComponent   — реализация: список, выбор, действия
├── DevicesState              — DeviceListState, PendingDeviceActionType, ActionFeedback
└── ui/
    ├── DevicesScreen         — двухпанельный layout
    ├── DeviceCard            — карточка в списке (иконка транспорта, бейджи)
    └── DeviceDetailsPanel    — правая панель с деталями и кнопками действий
```

## Связи с другими модулями
- Управляет `selectedDevice` через `DeviceManager` — все остальные фичи читают выбранное устройство оттуда.
- Навигация из деталей: переход в `device-info`, `packages`, `logcat`.
