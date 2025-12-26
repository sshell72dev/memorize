# Полная пересборка проекта для исправления ошибки Room

## Проблема
```
Cannot find implementation for com.memorize.database.MemorizeDatabase. MemorizeDatabase_Impl does not exist
```

## ✅ Хорошая новость
KSP **УЖЕ СГЕНЕРИРОВАЛ** классы! Файл `MemorizeDatabase_Impl.java` существует в:
```
android/app/build/generated/ksp/debug/java/com/memorize/database/
```

Проблема в том, что эти классы не включены в APK или кеш устарел.

## 🔧 Решение: Полная пересборка

### Шаг 1: Invalidate Caches (ОБЯЗАТЕЛЬНО!)
1. **File → Invalidate Caches / Restart**
2. Выберите **"Invalidate and Restart"**
3. Дождитесь перезапуска Android Studio

### Шаг 2: Clean Project
1. **Build → Clean Project**
2. Дождитесь завершения (30-60 секунд)

### Шаг 3: Rebuild Project
1. **Build → Rebuild Project**
2. Дождитесь завершения (1-3 минуты)

### Шаг 4: Sync Project with Gradle Files
1. **File → Sync Project with Gradle Files**
2. Дождитесь завершения синхронизации

### Шаг 5: Запуск
1. **Run → Run 'app'**
2. Приложение должно запуститься без ошибок

## ⚠️ Важно

**НЕ ПРОПУСКАЙТЕ ШАГ 1** (Invalidate Caches) - это критически важно! Без этого Android Studio может использовать старый кеш.

## Проверка после пересборки

После пересборки проверьте, что файлы существуют:
- `android/app/build/generated/ksp/debug/java/com/memorize/database/MemorizeDatabase_Impl.java`
- `android/app/build/generated/ksp/debug/java/com/memorize/database/dao/TextDao_Impl.java`
- И другие `*_Impl.java` файлы

## Если проблема сохраняется

1. Удалите папку `android/app/build` вручную
2. Выполните шаги 1-5 снова
3. Проверьте, что в `build.gradle.kts` есть:
   ```kotlin
   id("com.google.devtools.ksp") version "1.9.20-1.0.14"
   ```
   И:
   ```kotlin
   ksp("androidx.room:room-compiler:$roomVersion")
   ```

