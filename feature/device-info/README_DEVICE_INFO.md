# feature/device-info — Информация об устройстве

Детальная техническая информация об устройстве, собранная из множества системных источников. Данные загружаются параллельно по секциям, кешируются на 3 секунды.

## Функциональность

### Секции (12 штук)

#### Overview
- Модель, производитель, серийный номер
- Версия Android, API level
- IMEI (если доступен)
- Источники: `getprop ro.product.*`, `getprop ro.build.version.*`, `service call iphonesubinfo`

#### Build
- Build number, Build fingerprint
- Дата патча безопасности (`ro.build.version.security_patch`)
- Версия baseband (`ro.baseband`)
- Компилятор (`ro.dalvik.vm.isa.*`)
- Источники: `getprop`

#### Display
- Физическое разрешение (`wm size`)
- Логическое разрешение (с учётом DPI override)
- Плотность пикселей — физическая и логическая (`wm density`)
- Refresh rate, HDR capabilities
- Источники: `wm size`, `wm density`, `dumpsys display`

#### CPU / RAM
- Количество ядер, архитектура (ARMv8, x86_64, …), ABI-список
- Частоты по кластерам (big/little/prime) в MHz/GHz, диапазоны
- RAM: total / available / used / zram
- Источники: `/proc/cpuinfo`, `/sys/devices/system/cpu/cpu*/cpufreq/scaling_*`, `dumpsys meminfo`

#### Battery
- Уровень заряда (%)
- Статус: charging / discharging / full / not charging
- Источник питания: USB / AC / Wireless / None
- Здоровье аккумулятора: good / overheat / dead / over voltage / …
- Температура (°C), напряжение (mV), технология (Li-ion, …)
- Источник: `dumpsys battery`

#### Network
- IP-адреса всех сетевых интерфейсов (IPv4 + IPv6)
- Таблица маршрутов (шлюзы, интерфейсы)
- MAC-адрес
- WiFi: SSID, BSSID, RSSI (dBm), канал, частота (2.4/5/6 GHz), режим безопасности
- Источники: `ip addr`, `ip route`, `dumpsys wifi`, `dumpsys connectivity`

#### Cellular
- Оператор (имя, MCC, MNC)
- Тип сети: GSM / WCDMA / LTE / NR (5G) / …
- Сигнал: RSRP, RSRQ, RSSI, SNR (для LTE/5G), RSCP/Ec-No (для 3G)
- Роуминг, состояние SIM
- Источник: `dumpsys telephony.registry` (best-effort parsing с vendor-вариациями)

#### Modem
- Версия baseband firmware
- Состояние радиомодуля
- Источники: `getprop`, `dumpsys phone`

#### IMS / RCS
- Состояние IMS-регистрации (registered / not registered)
- RCS-возможности (chat, file transfer, …)
- Источник: `dumpsys ims`

#### Storage
- Внутреннее хранилище: total / used / free
- Внешнее хранилище / SD-карта (если есть)
- Эмулированные разделы
- Источники: `df -h`, `dumpsys diskstats`

#### Security
- SELinux: enforcing / permissive / disabled
- Verified Boot state: verified / self_signed / unverified / failed
- Шифрование хранилища: encrypted / unencrypted / unsupported
- Источник: `getprop`, `dumpsys device_policy`

#### System
- Uptime устройства
- Версия ядра (`uname -r`)
- Часовой пояс, язык системы
- Источник: `/proc/uptime`, `uname`, `getprop persist.sys.*`

### Экспорт
- Все 12 секций → JSON-файл на хост (сохранить через диалог выбора пути)

## Архитектура модуля

```
feature/device-info/
├── DeviceInfoComponent           — публичный интерфейс: refresh, export
├── DefaultDeviceInfoComponent    — параллельная загрузка секций, кеш 3с
├── DeviceInfoState               — модель состояния (12 секций)
├── DeviceInfoService             — интерфейс загрузки секций
├── DefaultDeviceInfoService      — реализация (~970 строк)
└── parsers/
    ├── BatteryParsers            — dumpsys battery
    ├── BatteryStatsParsers       — dumpsys batterystats
    ├── CpuMemoryParsers          — /proc/cpuinfo, dumpsys meminfo
    ├── CpuFrequencyLoader        — /sys/devices/system/cpu/*/cpufreq
    ├── DisplayParsers            — wm size/density, dumpsys display
    ├── NetworkParsers            — ip addr/route, dumpsys wifi
    ├── CellularParsers           — dumpsys telephony.registry
    ├── ImsRcsParsers             — dumpsys ims
    ├── StorageParsers            — df -h, dumpsys diskstats
    ├── TextParsers               — parseColonKeyValueLines, normalizeForUi
    └── PackageUidParsers         — маппинг uid → package
```

## Связи с другими модулями
- Открывается из `feature/devices` (кнопка в деталях устройства).
