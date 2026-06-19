# TG WS Proxy Android

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Android-приложение для запуска TG WS Proxy прямо на устройстве. Проксирует MTProto-трафик Telegram через WebSocket (`wss://kws*.web.telegram.org/apiws`), обходя DPI-фильтрацию. При прямом WS недоступности автоматически переключается на Cloudflare-прокси или TCP-fallback.

Встроенный Go-движок с CGO (JNI) обеспечивает нативную производительность. Приложение работает как VPN-подобный интерфейс с одной большой кнопкой включения/выключения.

## Возможности

- **Прямое WS-подключение** к Telegram DC с автоматическим MTProto-packet splitting
- **uTLS browser fingerprinting** — Chrome / Firefox / Safari / Random (режимы 0–3) для обхода TLS-отпечатков
- **Cloudflare-прокси fallback** — автоматическое переключение при блокировке WS с балансировкой доменов, кешированием списка из GitHub и exponential backoff при 429
- **DoH-резолвинг** через Cloudflare, Google, Quad9, AdGuard (с кешированием 15 мин)
- **Connection pooling** — пул предустановленных WS-соединений с периодическим probe (до 16 на DC)
- **Fake TLS** (ee-secret) — маскирование прокси-трафика под TLS-соединение
- **Адаптивный keepalive** — 30s → 15s при простое, восстановление при активности
- **Quick Settings Tile** — включение/выключение из шторки
- **Автозапуск** при загрузке устройства
- **DataStore** — персистентное хранение настроек

## Технический стек

| Компонент | Технология |
|---|---|
| UI | Jetpack Compose, Material 3, Single-Activity |
| Архитектура | MVVM (ViewModel + StateFlow) |
| Навигация | Compose Navigation + BottomBar |
| Фоновая работа | Foreground Service (`dataSync`) |
| Хранение | DataStore Preferences |
| Движок прокси | Go 1.24 (CGO/JNI, нативные `.so`) |
| WS-библиотека | Кастомный `RawWebSocket` (без gorilla) |
| TLS | `refraction-networking/utls` v1.8.2 |
| Шрифты | Inter (Regular, Medium, Semibold, Bold) |
| Локализация | Русский / English |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 |

## Оптимизации движка (последний коммит)

- **O(N) MTProto splitting** — zero-copy парсинг пакетов без реаллокаций буфера
- **Рандомизация CF-доменов** — `rand/v2.Shuffle` для равномерного распределения нагрузки
- **Обновлённый WS handshake** — актуальный Chrome UA, `Sec-WebSocket-Extensions`, `Accept-Encoding/Language`
- **DoH TTL 15 мин** — снижение задержки при CF fallback
- **Адаптивный keepalive** — быстрое обнаружение мёртвых соединений без лишнего трафика

## Архитектура

```
┌─────────────────────────────────┐
│         Telegram Client         │
│   (MTProto over local proxy)    │
└──────────────┬──────────────────┘
               │ 127.0.0.1:1443
               ▼
┌─────────────────────────────────┐
│      Go Proxy Engine (JNI)      │
│                                 │
│  ┌─────────┐  ┌──────────────┐  │
│  │  WS     │  │  CF Proxy    │  │
│  │  Direct │──│  Fallback    │  │
│  └────┬────┘  └──────┬───────┘  │
│       │              │          │
│  ┌────┴──────────────┴──────┐   │
│  │   Connection Pool        │   │
│  │   (per-DC, probe, TTL)   │   │
│  └──────────────────────────┘   │
│                                 │
│  ┌──────────┐  ┌─────────────┐  │
│  │  DoH     │  │  Fake TLS  │  │
│  │  4 prov. │  │  ee-secret │  │
│  └──────────┘  └─────────────┘  │
└─────────────────────────────────┘
               │
               ▼
     wss://kws*.web.telegram.org
     / CF proxy / TCP fallback
```

## Экраны

| Таб | Описание |
|---|---|
| **Запуск** | Главная кнопка с логотипом Telegram, статус подключения, кнопки запуска Telegram/Beta, панель CF/Пул/Порт, ссылка на прокси |
| **Настройки** | Порт, секрет, CF-прокси, пул, bypass-режим (uTLS), fake TLS, автовыгрузка, автозапуск, выбор палитры |
| **Логи** | Терминальный лог событий с фильтрами INFO/ERROR/NULL, моноширинный шрифт, автоскролл |

## Темы и палитры

- **System** — динамические цвета Material You (Android 12+)
- **Indigo** — фиолетовая светлая / тёмная
- **Espresso** — тёплая коричневая «раф на кокосовом молоке» / «эспрессо»
- **Forest** — зелёная приглушённая
- **Cyber** — тёмная неоновая «Telegram Neon» с `#2AABEE` акцентом

## Структура проекта

```
├── app/src/main/
│   ├── java/com/tgws/proxy/
│   │   ├── App.kt                     # Application, DataStore init
│   │   ├── MainActivity.kt            # Single Activity, navigation, bottom bar
│   │   ├── ProxyController.kt         # Start/stop прокси, JNI-вызовы
│   │   ├── ProxyService.kt            # Foreground service, notification
│   │   ├── SettingsStore.kt           # DataStore обёртка
│   │   ├── LogEntry.kt                # Модель лог-записи
│   │   ├── BootReceiver.kt            # Автозапуск
│   │   ├── ProxyTileService.kt        # Quick Settings tile
│   │   ├── NativeProxy.kt             # JNI bridge
│   │   └── ui/
│   │       ├── Theme.kt               # 5 палитр, Inter типография, AppColors
│   │       ├── ConnectionTab.kt       # Главная: кнопка, статус, Telegram launch
│   │       ├── SettingsTab.kt         # Настройки: порты, CF, bypass, fake TLS
│   │       ├── LogsTab.kt             # Терминальный лог
│   │       ├── ChatBackground.kt      # Анимированные чат-пузыри на фоне
│   │       ├── AppSectionCard.kt      # Переиспользуемая карточка с градиентом
│   │       ├── FloatingToolbar.kt     # Плавающая панель статистики
│   │       └── ExternalLinks.kt       # Ссылки на GitHub, донаты
│   ├── jniLibs/
│   │   ├── arm64-v8a/libtgwsproxy.so
│   │   ├── armeabi-v7a/libtgwsproxy.so
│   │   └── x86_64/libtgwsproxy.so
│   └── res/
│       ├── drawable/ic_telegram_logo.xml
│       ├── font/inter_*.ttf
│       └── ...
├── tg-ws-proxy.go                     # Go-исходник движка (CGO, ~3100 строк)
├── go.mod / go.sum
├── build-go.sh                        # Кросс-компиляция Go → Android
├── build-go-docker.ps1 / .bat         # Docker-сборка для Windows
└── gradle/libs.versions.toml
```

## Сборка

### Предварительные требования

- Go 1.24+
- Android Studio Hedgehog (2023.1.1)+ / JDK 17
- Gradle 8.4+

### 1. Клонирование

```bash
git clone https://github.com/Derzkiyboomchik/Telegram_android_proxy.git
cd Telegram_android_proxy
```

### 2. Сборка Go-бинарников

**Через Docker (рекомендуется):**

```bash
# PowerShell
.\build-go-docker.ps1

# Linux / macOS
docker run --rm -v "$PWD:/workspace" -w /workspace golang:1.24-alpine sh -c "
  apk add --no-cache git bash &&
  bash ./build-go.sh
"
```

**Локальный Go:**

```bash
./build-go.sh
```

**Android NDK (CGO):**

```bash
export ANDROID_NDK=/path/to/android-ndk
CC="$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang" \
  CGO_ENABLED=1 GOOS=android GOARCH=arm64 go build -o app/src/main/jniLibs/arm64-v8a/libtgwsproxy.so .
```

### 3. Сборка APK

```bash
./gradlew assembleDebug    # Debug
./gradlew assembleRelease  # Release (требуется keystore.properties)
```

### 4. GitHub Actions

При пуше в `main` или теге `v*` автоматически собираются Go-бинарники и APK:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Подписание Release APK

Создайте `app/keystore.properties`:

```properties
storeFile=../my-key.jks
storePassword=yourStorePassword
keyAlias=yourKeyAlias
keyPassword=yourKeyPassword
```

## Разрешения

| Permission | Назначение |
|---|---|
| `INTERNET` | Работа прокси |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | Фоновый сервис |
| `POST_NOTIFICATIONS` | Уведомление (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Автозапуск |

## Quick Settings Tile

1. Раскройте шторку → «Редактировать» (карандаш)
2. Перетащите **TG WS Proxy** в активную зону

## Лицензия

MIT License. См. [LICENSE](LICENSE).

## Благодарности

- [Flowseal/tg-ws-proxy](https://github.com/Flowseal/tg-ws-proxy) — оригинальная концепция WS-прокси для Telegram
- [spatiumstas/tg-ws-proxy-go](https://github.com/spatiumstas/tg-ws-proxy-go) — Go-реализация движка