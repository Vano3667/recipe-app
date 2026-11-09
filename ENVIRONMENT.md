# Среда разработки

## Где писался код

Изолированная песочница ИИ-агента:

- Amazon Linux 2023, Python 3.13, Node 24, стандартные утилиты (jq, zip, ripgrep, ImageMagick и др.).
- **Нет:** JDK/javac, kotlinc, Gradle, Android SDK (sdkmanager, adb, emulator).
- **Интернета нет:** `dl.google.com` и `repo.maven.apache.org` недоступны (`curl: (6) Could not resolve host`), поэтому установить SDK и зависимости было нельзя.

Из-за этого сборка и тесты перенесены в GitHub Actions (`.github/workflows/android.yml`).

## Инструменты агента (skills и MCP)

| Инструмент | Тип | Использован |
|---|---|---|
| Computer (терминал, файлы) | встроенный модуль | да: написание кода, статическая проверка импортов, расчёт контраста, упаковка zip |
| Notion (страницы, базы) | встроенная интеграция | нет, для проекта не нужен |
| Search / Web | встроенные модули | нет (интернет в песочнице выключен) |
| Slack, Calendar, Mail | подключения агента | нет |
| Help docs | встроенный модуль | нет |
| Skills | модуль навыков | Android-специфичные навыки не использовались |
| MCP-серверы | — | **не подключены** |

## Где собирается и тестируется

GitHub Actions, `ubuntu-latest`:

- Temurin JDK 17, `android-actions/setup-android@v3` (SDK 34), Gradle 8.7 (wrapper создаётся в CI).
- Задача `build`: assembleDebug, testDebugUnitTest (JUnit + Robolectric), lintDebug.
- Задача `instrumented`: `reactivecircus/android-emulator-runner@v2`, google_apis x86_64, API 29 pixel_4 и API 34 pixel_tablet.

## Версии

AGP 8.5.2 · Kotlin 1.9.24 · KSP 1.9.24-1.0.20 · Compose BOM 2024.06.00 · Hilt 2.51.1 · Room 2.6.1 · Navigation 2.7.7 · minSdk 29 · target/compile 34.
