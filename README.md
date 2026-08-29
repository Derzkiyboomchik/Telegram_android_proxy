# TG WS Proxy Android

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/Derzkiyboomchik/Telegram_android_proxy?include_prereleases&color=blue)](https://github.com/Derzkiyboomchik/Telegram_android_proxy/releases)

Android-приложение для запуска локального TG WS Proxy прямо на смартфоне. Проксирует MTProto-трафик Telegram через WebSocket (`wss://kws*.web.telegram.org/apiws`), эффективно обходя DPI-фильтрацию. При недоступности прямого WebSocket автоматически переключается на Cloudflare CDN проксирование или TCP-fallback.

Встроенный высокопроизводительный Go-движок с CGO обеспечивает нативную скорость и минимальное энергопотребление.

## Возможности

- **Прямое WebSocket-подключение** к серверам Telegram с автоматическим MTProto packet splitting
- **Поддержка 16 клиентов Telegram** — автоопределение установленных приложений и быстрое подключение в один клик:
  * *Official Telegram, Telegram Beta, Telegram Direct (Web APK), Telegram X, Plus Messenger, AyuGram, NekoX, ForkClient, Forkgram, iMe Messenger, Kotatogram, BGram, Cherrygram, MDGram, Turrit, Teleplus*
- **Нативное подключение (MTProto Scheme)** — мгновенная активация прокси в Telegram без лишних окон
- **Плавная орбитальная анимация** — аппаратный бесшовный 360° рендеринг орбит, траекторий и звёздных частиц
- **Cloudflare CDN fallback** — автоматический обход блокировок с динамической балансировкой доменов и exponential backoff
- **DoH-резолвинг** через Cloudflare, Google, Quad9, AdGuard с кешированием
- **Умное энергосбережение** — адаптивный пинг радиомодема, освобождение WakeLock и оптимизация фоновой работы
- **Connection Pooling** — предварительно прогретый пул соединений для мгновенной отправки сообщений
- **Fake TLS** (ee-secret) — маскировка прокси-трафика под TLS
- **Quick Settings Tile** — быстрое включение/выключение прямо из шторки Android
- **Автозапуск** при старте системы
- **Уведомления о новых релизах** — проверка обновлений на GitHub

## Технический стек

| Компонент | Технология |
|---|---|
| UI | Jetpack Compose, Material 3, Single-Activity |
| Архитектура | MVVM (ViewModel + StateFlow) |
| Навигация | Compose Navigation + BottomBar |
| Фоновая служба | Foreground Service (`specialUse`) |
| Хранилище | DataStore Preferences |
| Движок | Go 1.23+ (CGO, нативная сборка `libtgwsproxy.so` для arm64, armv7, x86_64) |
| Сеть | Кастомный `RawWebSocket` + Go `crypto/tls` (TLS 1.2/1.3) |
| Шрифты | Inter |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 (Android 14) |

## Архитектура

```
┌─────────────────────────────────┐
│     Telegram / Fork Client      │
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

## Экраны приложения

| Таб | Описание |
|---|---|
| **Запуск** | Кнопка старта с орбитальной космической анимацией, статус, единая кнопка подключения к 16 клиентам Telegram и копирования ссылки, плашки статистики |
| **Настройки** | Настройка портов, ключей, Cloudflare CDN, пула соединений, Fake TLS, автозапуска и выбор тем оформления |
| **Логи** | Терминальный лог событий с фильтрацией (INFO / ERROR / NULL), автоскроллом и возможностью копирования |

## Темы оформления

- **Системная** — гармоничная адаптация под светлую или тёмную тему
- **Aurora** — космическая палитра с неоновыми оттенками
- **Sunset** — тёплые вечерние тона
- **Graphite** — строгая монохромная тема

## Разрешения Android

| Permission | Назначение |
|---|---|
| `INTERNET` | Работа сетевого прокси |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Фоновая работа службы |
| `POST_NOTIFICATIONS` | Системное уведомление статуса (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Автозапуск при включении устройства |

## Quick Settings Tile

1. Потяните шторку быстрых настроек Android вниз → нажмите значок редактирования (карандаш).
2. Найдите плитку **TG WS Proxy** и перетащите её в активную область.

## Лицензия

Проект распространяется под лицензией [MIT](LICENSE).

## Благодарности

- [Flowseal/tg-ws-proxy](https://github.com/Flowseal/tg-ws-proxy) — концепция WebSocket-прокси для Telegram
- [spatiumstas/tg-ws-proxy-go](https://github.com/spatiumstas/tg-ws-proxy-go) — базовая Go-реализация