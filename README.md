# STAGE 2 COMPLETED

**Stage 2 development tasks have been completed.**

All required Stage 2 tasks have been implemented, excluding Task 8, which was intentionally deferred.

> Status details (re-verified 2026-11-19): every Stage 2 item except Task 8 is implemented and passed CI on `main`
> (run 44225182905: build, unit + Robolectric incl. import of `recipes_from_budget_classic_1500kcal.json` into the
> database, lint, emulator tests API 29 / API 34). Manual re-checks on the user's device are still required for
> ingredient reordering, the compact theme selector, shopping-list sharing and the JSON import fix.
>
> **Task 8 (safe APK updates) started 2026-11-19 and is in progress:** increasing versionCode and signed release
> builds from a permanent key are implemented; the signed build and the update-over-install check are waiting for
> the signing key in GitHub Secrets (see below and `DEVELOPMENT_HANDOFF.md`, F2). Update compatibility is NOT yet verified.

---

# Домашнее меню (Home Menu)

Уютное офлайн-приложение для Android: рецепты, меню на неделю и список покупок. Интерфейс только на русском.

## Возможности

- **Рецепты**: время, сложность, категория, калории, порции. Фото рецепта и каждого шага: из галереи (системный Photo Picker) или с камеры, сжатие до 1080 px.
  - Ингредиенты из каталога (124 продукта с отделами магазина, можно добавлять свои); единицы: г, мл, шт, ст. л., ч. л., щепотка, по вкусу. Вставка списком, порядок меняется кнопками вверх/вниз.
  - Шаги, заметки, теги цветными чипами.
  - Поиск по названию или ингредиентам («курица, рис»), фильтры, сортировка.
  - Пересчёт порций: округление вверх, дробные результаты помечаются «~».
  - «Поделиться» текстом или файлом; импорт рецепта из текста.
- **Меню**: неделя Пн–Вс или любой период; 4 приёма пищи в день, в каждом несколько блюд; перетаскивание блюд (долгое нажатие); автозаполнение пустых слотов по тегам и категориям; кнопка «Готовить» открывает рецепт на запланированное число порций; сводка.
- **Покупки**: на день, неделю или период; суммирование по продукту и единице; группировка по отделам; галочки, прогресс; «Поделиться» (весь список или только оставшееся) в WhatsApp, Telegram и другие приложения. Символы ☐/☑ в сообщении не нажимаются: это обычный текст.
- **Резервная копия**: ZIP (`recipes.json` + `photos/` + `manifest.json`) и JSON (фото внутри). Перед импортом — предпросмотр и выбор, что делать с совпадающими рецептами; импорт в одной транзакции и проверка записанного. Файлы с пустыми (`null`) полями, например `recipes_from_budget_classic_1500kcal.json`, импортируются.
- **Темы**: раздел «Оформление» в настройках: компактный выпадающий выбор из шести готовых тем (по две в строке) и свои цвета для основных элементов интерфейса.
- **Адаптивность**: на телефоне — нижняя навигация; на планшете — боковая панель и режим «список + рецепт».

## Стек

Kotlin 1.9.24 · Jetpack Compose (BOM 2024.06) · Material 3 · MVVM (data / domain / ui) · ViewModel + StateFlow · Room 2.6 · Hilt 2.51 · Navigation Compose · Coroutines/Flow · kotlinx.serialization · Coil · minSdk 29, target/compile 34.

## Структура

```
app/src/main/java/com/example/homemenu/
  data/      db (Room), files (фото, бэкап), repo, seed
  domain/    модели, ScalingUseCase, ShoppingListUseCase, MenuGeneratorUseCase, RecipeSearch, backup
  ui/        theme, components, recipes, menu, shopping, settings, nav
app/src/test/         юнит-тесты и Robolectric-тесты UI
app/src/androidTest/  сценарные тесты на эмуляторе
.github/workflows/android.yml               сборка, тесты, подписанный APK, проверка обновления
.github/workflows/generate-signing-key.yml  одноразовое создание ключа подписи
scripts/update_check.sh                     установка новой версии поверх старой на эмуляторе
recipes_from_budget_classic_1500kcal.json   реальный файл импорта пользователя (тестовая фикстура, не менять)
```

## Сборка через GitHub Actions (рекомендуется)

1. Создайте пустой репозиторий на GitHub и загрузите туда содержимое папки `HomeMenu`:
   ```bash
   cd HomeMenu
   git init && git add . && git commit -m "Home Menu"
   git branch -M main
   git remote add origin https://github.com/<you>/home-menu.git
   git push -u origin main
   ```
2. Откройте вкладку **Actions**. Workflow «Build Home Menu APK» запустится сам (или нажмите **Run workflow**).
3. После завершения задачи `build` скачайте артефакт **home-menu-release-apk** (подписанный постоянным ключом, см. ниже). Пока ключа нет, собирается только **home-menu-debug-apk**.
4. Отчёты тестов и lint лежат в артефакте **reports**. Задачи `instrumented` прогоняют UI-тесты на эмуляторах API 29 (телефон) и API 34 (планшет); `update-check` ставит новую версию поверх старой и проверяет, что данные сохранились.

Workflow сам создаёт Gradle wrapper 8.7 и скачивает шрифты (`scripts/fetch_fonts.sh`). Номер сборки (versionCode) равен номеру запуска workflow и с каждой сборкой растёт.

### Постоянный ключ подписи (обновление без потери данных)

Android ставит новую версию поверх старой только если обе подписаны одним и тем же ключом. Раньше каждый APK из CI подписывался случайным debug-ключом (отсюда ошибка «подписи не совпадают»).

1. Actions → **Generate signing key (one time)** → **Run workflow** (только если своего ключа нет). Скачайте артефакт `signing-key-SAVE-THEN-DELETE` (хранится 3 дня).
2. Settings → Secrets and variables → Actions: создайте секреты `HOMEMENU_KEYSTORE_BASE64`, `HOMEMENU_KEYSTORE_PASSWORD`, `HOMEMENU_KEY_ALIAS`, `HOMEMENU_KEY_PASSWORD` (значение — содержимое одноимённого файла).
3. Сохраните `homemenu-release.jks` и пароль вне GitHub, затем удалите артефакт. Без этого ключа обновить установленное приложение больше не получится.
4. **Один раз** (новый ключ не совпадает со старыми случайными): сделайте экспорт ZIP, удалите приложение для всех пользователей телефона (включая рабочий профиль / «Второе пространство»), установите APK из `home-menu-release-apk` и импортируйте ZIP. Дальше новые версии из `home-menu-release-apk` ставятся поверх без удаления.

## Локальная сборка

Нужны JDK 17, Android SDK 34 и Gradle 8.7.

```bash
gradle wrapper --gradle-version 8.7
bash scripts/fetch_fonts.sh
./gradlew :app:assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest      # юнит + Robolectric
./gradlew :app:connectedDebugAndroidTest   # нужен эмулятор или устройство
```

Локальная сборка получает versionCode 3 и не ставится поверх сборки из CI (это было бы понижение версии); для телефона используйте APK из CI.

Или откройте папку в Android Studio (Koala и новее), выполнив перед этим `scripts/fetch_fonts.sh`.
