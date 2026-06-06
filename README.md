# TG WS Proxy Android

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Android-приложение (форк) для запуска TG WS Proxy прямо на устройстве. Использует Go-реализацию [tg-ws-proxy-go](https://github.com/spatiumstas/tg-ws-proxy-go) как встроенный нативный бинарник.

## Архитектура

- **Single-Activity** с Jetpack Compose и Material 3
- **MVVM**: ViewModel + StateFlow
- **Compose Navigation** с BottomBar
- **Foreground Service** (`dataSync`) для работы в фоне
- **Quick Settings Tile** для быстрого включения/выключения
- **DataStore** для хранения настроек
- **Локализация**: Русский и Английский

## Скриншоты

*Главный экран*: VPN-подобный интерфейс с большой круглой кнопкой Connect/Disconnect, статусом и карточкой конфигурации.

## Требования

- Android 7.0+ (minSdk 24)
- targetSdk 34 / compileSdk 34
- Go 1.23+ (для сборки нативных бинарников)
- Gradle 8.4+ (или Android Studio со встроенным Gradle)
- Android Studio Hedgehog (2023.1.1) или новее
- JDK 17

## Структура проекта

```
├── app/
│   ├── src/main/
│   │   ├── assets/bin/<abi>/tg-ws-proxy   # Go-бинарники для Android ABIs
│   │   ├── java/com/tgws/proxy/
│   │   │   ├── App.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/
│   │   │   │   ├── local/ProxyDataStore.kt
│   │   │   │   └── repository/ProxyRepository.kt
│   │   │   ├── di/AppModule.kt
│   │   │   ├── service/ProxyForegroundService.kt
│   │   │   ├── tile/ProxyTileService.kt
│   │   │   ├── receiver/BootReceiver.kt
│   │   │   ├── ui/
│   │   │   │   ├── theme/
│   │   │   │   ├── navigation/
│   │   │   │   └── screens/
│   │   │   └── viewmodel/ProxyViewModel.kt
│   │   └── res/
│   └── build.gradle.kts
├── build-go.sh
├── .github/workflows/build.yml
└── gradle/libs.versions.toml
```

## Инструкция по сборке

### 1. Локальная сборка через Android Studio + Gradle

#### Шаг 1: Клонируйте репозиторий

```bash
git clone <your-repo-url>
cd tg-ws-proxy-android
```

#### Шаг 2: Сгенерируйте Gradle Wrapper (если отсутствует)

Если в проекте нет `gradle/wrapper/gradle-wrapper.jar`, сгенерируйте его:

```bash
gradle wrapper --gradle-version 8.4
```

Или используйте системный Gradle:

```bash
gradle assembleDebug
```

#### Шаг 3: Соберите Go-бинарники

##### Вариант A: Сборка через Docker (рекомендуется)

На Windows используйте готовый скрипт:

```powershell
# PowerShell
.\build-go-docker.ps1
```

Или через `cmd`:
```cmd
build-go-docker.bat
```

Или вручную одной строкой:
```bash
docker run --rm -v "$PWD:/workspace" -w /workspace golang:1.23-alpine sh -c "
apk add --no-cache git bash &&
bash ./build-go.sh
"
```

> **Важно**: в репозитории `tg-ws-proxy-go` исходники находятся в папке `src/`. Скрипт `build-go.sh` автоматически переходит в неё перед сборкой.

##### Вариант B: Сборка через локальный Go

```bash
# Убедитесь, что Go установлен (go version)
./build-go.sh
```

##### Вариант C: Сборка через Android NDK (если нужен CGO)

```bash
export ANDROID_NDK=/path/to/android-ndk

# arm64-v8a
CC="$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang" \
CGO_ENABLED=1 GOOS=android GOARCH=arm64 go build -o app/src/main/assets/bin/arm64-v8a/tg-ws-proxy .

# armeabi-v7a
CC="$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi24-clang" \
CGO_ENABLED=1 GOOS=android GOARCH=arm go build -o app/src/main/assets/bin/armeabi-v7a/tg-ws-proxy .

# x86_64
CC="$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/x86_64-linux-android24-clang" \
CGO_ENABLED=1 GOOS=android GOARCH=amd64 go build -o app/src/main/assets/bin/x86_64/tg-ws-proxy .

# x86
CC="$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/i686-linux-android24-clang" \
CGO_ENABLED=1 GOOS=android GOARCH=386 go build -o app/src/main/assets/bin/x86/tg-ws-proxy .
```

> **Примечание**: Поскольку `tg-ws-proxy-go` — чистый Go-проект без CGO, достаточно варианта A или B с `CGO_ENABLED=0`.

#### Шаг 4: Откройте проект в Android Studio

1. Запустите Android Studio
2. Выберите **Open** → укажите папку проекта
3. Дождитесь синхронизации Gradle
4. Выберите конфигурацию запуска `app`
5. Нажмите **Run** (Shift+F10) или соберите APK:
   - **Debug**: `Build → Build Bundle(s) / APK(s) → Build APK(s)`
   - **Release**: `Build → Generate Signed Bundle / APK...`

#### Шаг 5: Сборка через командную строку

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (требуется подпись)
./gradlew assembleRelease
```

Готовые APK будут находиться в:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`

### 2. Автоматическая сборка через GitHub Actions

В проекте настроен workflow `.github/workflows/build.yml`:

1. При пуше в `main` или создании тега `v*` автоматически:
   - Собираются Go-бинарники для всех ABI
   - Собираются Debug и Release APK
   - APK загружаются как артефакты

2. Для создания Release с APK:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

## Настройка подписи Release

Для подписи Release APK создайте файл `app/keystore.properties`:

```properties
storeFile=../my-key.jks
storePassword=yourStorePassword
keyAlias=yourKeyAlias
keyPassword=yourKeyPassword
```

И обновите `app/build.gradle.kts`:

```kotlin
val keystoreProperties = java.util.Properties().apply {
    load(file("keystore.properties").inputStream())
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"]!!)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## Permissions

Приложение запрашивает следующие разрешения:

- `INTERNET` — работа прокси
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` — фоновая работа
- `POST_NOTIFICATIONS` — уведомление о работающем сервисе (Android 13+)
- `RECEIVE_BOOT_COMPLETED` — автозапуск при загрузке

## Quick Settings Tile

После установки приложения добавьте плитку TG WS Proxy в шторку быстрых настроек:
1. Раскройте шторку уведомлений
2. Нажмите «Редактировать» (карандаш)
3. Перетащите плитку TG WS Proxy в активную зону

## Лицензия

MIT License. См. [LICENSE](LICENSE).

Оригинальные репозитории:
- [Flowseal/tg-ws-proxy](https://github.com/Flowseal/tg-ws-proxy) — оригинальная Node.js версия
- [spatiumstas/tg-ws-proxy-go](https://github.com/spatiumstas/tg-ws-proxy-go) — Go-реализация движка
