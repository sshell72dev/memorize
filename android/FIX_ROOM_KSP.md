# Исправление ошибки Room KSP

## Проблема
```
Cannot find implementation for com.memorize.database.MemorizeDatabase. MemorizeDatabase_Impl does not exist
```

## Причина
KSP (Kotlin Symbol Processing) не сгенерировал классы Room. Это происходит, когда:
1. Проект не был пересобран после добавления KSP
2. KSP не обработал аннотации Room

## Решение

### Шаг 1: Clean Project
В Android Studio:
1. **Build → Clean Project**
2. Дождитесь завершения

### Шаг 2: Rebuild Project
1. **Build → Rebuild Project**
2. Дождитесь завершения (может занять 1-2 минуты)

### Шаг 3: Проверка
После пересборки KSP должен сгенерировать:
- `MemorizeDatabase_Impl`
- Все DAO реализации
- Все Entity классы

### Шаг 4: Запуск
Запустите приложение снова - ошибка должна исчезнуть.

## Что было исправлено

1. ✅ Добавлена конфигурация KSP в `build.gradle.kts`
2. ✅ KSP правильно настроен для Room

## Если проблема сохраняется

1. **File → Invalidate Caches / Restart**
2. Выберите **Invalidate and Restart**
3. После перезапуска: **Build → Rebuild Project**

## Проверка

После пересборки проверьте, что в папке:
```
android/app/build/generated/ksp/debug/kotlin/com/memorize/database/
```

Появились файлы:
- `MemorizeDatabase_Impl.kt`
- И другие сгенерированные классы

