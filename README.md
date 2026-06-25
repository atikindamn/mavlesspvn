# MyVPN Client

Нативный Android VPN-клиент на Kotlin с поддержкой VLESS+REALITY

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
