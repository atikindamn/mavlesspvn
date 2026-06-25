# MyVPN Client

Нативный Android VPN-клиент на Kotlin с поддержкой OpenVPN.

## Возможности

- Подключение к OpenVPN серверам по логину/паролю
- Сохранение нескольких профилей серверов
- Статус подключения в реальном времени
- Логи подключения
- Material 3 UI с тёмной темой
- Jetpack Compose + MVVM архитектура

## Структура проекта

```
app/src/main/java/com/myvpn/client/
├── MyVpnApp.kt                    # Application class
├── data/
│   ├── db/                         # Room database
│   ├── model/                      # Data classes
│   └── repository/                 # Repository pattern
├── service/
│   ├── VpnConnectionManager.kt    # VPN connection logic
│   └── MyOpenVpnService.kt        # Android VpnService
├── ui/
│   ├── MainActivity.kt            # Entry point + navigation
│   ├── theme/                      # Material 3 theme
│   ├── navigation/                 # Navigation routes
│   └── screens/
│       ├── home/                   # Main screen (profiles + connect)
│       ├── profile/                # Add/edit profile
│       └── logs/                   # Connection logs
└── utils/
    └── OpenVpnConfigBuilder.kt    # OpenVPN config generator
```

## Быстрый старт

### 1. Откройте проект
Откройте папку `MyVPNClient` в Android Studio (File → Open).

### 2. Sync Gradle
Android Studio предложит синхронизировать Gradle — нажмите Sync Now.

### 3. Запуск без OpenVPN (для тестирования UI)
Проект содержит **симуляцию подключения** — вы можете запустить приложение
и протестировать весь UI без настройки OpenVPN.

### 4. Интеграция настоящего OpenVPN

Это самый важный шаг. Есть **два варианта**:

---

## Вариант A: Использование OpenVPN library от Cake VPN (рекомендуется)

Это форк ics-openvpn, упакованный как AAR.

### Шаг 1: Скачайте библиотеку
```bash
git clone https://github.com/nickoala/AnyVPN.git
```
Или используйте JitPack:
```gradle
// В settings.gradle.kts уже добавлен JitPack
// В app/build.gradle.kts добавьте:
implementation("com.github.nickoala:AnyVPN:tag")
```

### Шаг 2: Альтернатива — ics-openvpn как модуль

```bash
# Клонируйте ics-openvpn
git clone https://github.com/nickoala/AnyVPN.git

# ИЛИ оригинальный
git clone https://github.com/nickoala/AnyVPN.git
```

Скопируйте модуль `vpnLib` в корень проекта и добавьте в `settings.gradle.kts`:
```kotlin
include(":vpnLib")
```

В `app/build.gradle.kts`:
```kotlin
implementation(project(":vpnLib"))
```

---

## Вариант B: Использование ics-openvpn напрямую

### Шаг 1: Клонируйте
```bash
git clone https://github.com/schwabe/ics-openvpn.git
```

### Шаг 2: Импортируйте как модуль
В Android Studio: File → New → Import Module → выберите `main` из ics-openvpn.

### Шаг 3: Добавьте зависимость
```kotlin
implementation(project(":main"))
```

### Шаг 4: NDK
Убедитесь, что у вас установлен NDK в Android Studio (SDK Manager → SDK Tools → NDK).

---

## Интеграция в код

После подключения библиотеки, замените симуляцию в `VpnConnectionManager.kt`:

```kotlin
// В методе connect() замените simulateConnection() на:

// 1. Запись конфига во временный файл
val configFile = File(context.cacheDir, "current.ovpn")
configFile.writeText(config)

// 2. Создание профиля OpenVPN
val vpnProfile = VpnProfile("MyVPN")
vpnProfile.mUsername = profile.username
vpnProfile.mPassword = profile.password

// 3. Импорт конфига
val configParser = ConfigParser()
configParser.parseConfig(StringReader(config))
val parsedProfile = configParser.convertProfile()
parsedProfile.mUsername = profile.username
parsedProfile.mPassword = profile.password
parsedProfile.mName = profile.name

// 4. Запуск
ProfileManager.getInstance(context).addProfile(parsedProfile)
VPNLaunchHelper.startOpenVpn(parsedProfile, context)
```

### Обработка колбэков состояния

```kotlin
// Зарегистрируйте слушатель в VpnConnectionManager:
VpnStatus.addStateListener(object : VpnStatus.StateListener {
    override fun updateState(
        state: String?,
        logmessage: String?,
        localizedResId: Int,
        level: ConnectionStatus.ConnectionLevel?
    ) {
        when (level) {
            ConnectionStatus.ConnectionLevel.LEVEL_CONNECTED -> {
                _connectionStatus.value = ConnectionStatus(
                    state = ConnectionState.CONNECTED,
                    // ...
                )
            }
            ConnectionStatus.ConnectionLevel.LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
            ConnectionStatus.ConnectionLevel.LEVEL_CONNECTING_SERVER_REPLIED -> {
                _connectionStatus.value = _connectionStatus.value.copy(
                    state = ConnectionState.CONNECTING
                )
            }
            ConnectionStatus.ConnectionLevel.LEVEL_NOTCONNECTED -> {
                _connectionStatus.value = ConnectionStatus(
                    state = ConnectionState.DISCONNECTED
                )
            }
            else -> {}
        }
        addLog(LogLevel.INFO, logmessage ?: state ?: "")
    }

    override fun setConnectedVPN(uuid: String?) {}
})
```

---

## О сертификатах (CA)

Для подключения к OpenVPN серверу нужен CA сертификат сервера.
Варианты:

1. **Встроить в конфиг** — отредактируйте `OpenVpnConfigBuilder.kt`,
   добавьте CA в метод `buildConfigWithCa()`
2. **Импорт .ovpn файла** — добавьте функцию импорта .ovpn в ProfileScreen
3. **Trust on first use** — для тестирования можно отключить проверку
   (НЕ для продакшена!)

## Минимальные требования

- Android 7.0 (API 24)
- Android Studio Iguana 2023.2.1+
- Kotlin 1.9.20+
- NDK (для сборки OpenVPN native)

## Лицензия

MIT
