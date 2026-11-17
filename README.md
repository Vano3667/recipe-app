# Домашнее меню (Home Menu)

Уютное офлайн-приложение для Android: рецепты, меню на неделю и список покупок. Интерфейс только на русском.

## Возможности

- **Рецепты**: время, сложность, категория, калории, порции. Фото рецепта и каждого шага: из галереи (системный Photo Picker) или с камеры, сжатие до 1080 px.
  - Ингредиенты из каталога (124 продукта с отделами магазина, можно добавлять свои); единицы: г, мл, шт, ст. л., ч. л., щепотка.
  - Шаги, заметки, теги цветными чипами.
  - Поиск по названию или ингредиентам («курица, рис»), фильтры, сортировка.
  - Пересчёт порций: округление вверх, дробные результаты помечаются «~».
  - «Поделиться» текстом или файлом.
- **Меню**: неделя Пн–Вс или любой период; 4 приёма пищи в день; перетаскивание блюд (долгое нажатие); автозаполнение пустых слотов по тегам и категориям; кнопка «Готовить» открывает рецепт на запланированное число порций; сводка.
- **Покупки**: на день, неделю или период; суммирование по продукту и единице; группировка по отделам; галочки, прогресс, «Поделиться».
- **Резервная копия**: zip (`recipes.json` + `photos/`) через системный выбор файла. Импорт добавляет данные с новыми id и ничего не удаляет.
- **Темы**: светлая, тёмная «шоколадная», как в системе. Раздел «Оформление» в настройках: шесть готовых тем и свои цвета для основных элементов интерфейса.
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
.github/workflows/android.yml
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
3. После завершения задачи `build` скачайте артефакт **home-menu-debug-apk**. Внутри `app-debug.apk`.
4. Отчёты тестов и lint лежат в артефакте **reports**. Задачи `instrumented` прогоняют UI-тесты на эмуляторах API 29 (телефон) и API 34 (планшет).

Workflow сам создаёт Gradle wrapper 8.7 и скачивает шрифты (`scripts/fetch_fonts.sh`).

## Локальная сборка

Нужны JDK 17, Android SDK 34 и Gradle 8.7.

```bash
gradle wrapper --gradle-version 8.7
bash scripts/fetch_fonts.sh
./gradlew :app:assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest      # юнит + Robolectric
./gradlew :app:connectedDebugAndroidTest   # нужен эмулятор или устройство
```

Или откройте папку в Android Studio (Koala и новее), выполнив перед этим `scripts/fetch_fonts.sh`.