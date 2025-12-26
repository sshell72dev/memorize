# Исправление ошибки jlink.exe и androidJdkImage

## Проблема
```
Execution failed for task ':app:compileDebugJavaWithJavac'.
> Could not resolve all files for configuration ':app:androidJdkImage'.
> Error while executing process jlink.exe
```

## Причина
Android Gradle Plugin пытается создать JDK образ с помощью `jlink.exe`, что не работает с некоторыми версиями JDK или конфигурациями.

## Решение

### Шаг 1: Очистка кеша Gradle (КРИТИЧЕСКИ ВАЖНО!)

1. **Закройте Android Studio**

2. **Удалите кеш Gradle**:
   - Откройте Проводник Windows
   - Перейдите в: `C:\Users\hp_ps\.gradle\caches\`
   - Удалите папку `transforms-3` (или всю папку `caches`)

   Или через PowerShell:
   ```powershell
   Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches\transforms-3\"
   ```

3. **Откройте Android Studio снова**

### Шаг 2: Invalidate Caches

1. **File → Invalidate Caches / Restart**
2. Выберите **"Invalidate and Restart"**
3. Дождитесь перезапуска

### Шаг 3: Sync Project

1. **File → Sync Project with Gradle Files**
2. Дождитесь завершения синхронизации

### Шаг 4: Clean Project

1. **Build → Clean Project**
2. Дождитесь завершения

### Шаг 5: Rebuild Project

1. **Build → Rebuild Project**
2. Дождитесь завершения

## Что было исправлено

1. ✅ Добавлено отключение задач `androidJdkImage` в `build.gradle.kts`
2. ✅ Настройка `android.experimental.disableJdkImage=true` в `gradle.properties`

## Альтернативное решение (если проблема сохраняется)

### Вариант 1: Использовать JDK 17 вместо JDK 21

1. В Android Studio: **File → Project Structure → SDK Location**
2. Убедитесь, что используется **JDK 17** (не 21)
3. Если JDK 17 не установлен, скачайте его с [adoptium.net](https://adoptium.net/)

### Вариант 2: Обновить Android Gradle Plugin

В `android/build.gradle.kts`:
```kotlin
id("com.android.application") version "8.3.0" apply false
```

### Вариант 3: Временно отключить Java компиляцию (НЕ РЕКОМЕНДУЕТСЯ)

Это решит проблему с jlink, но Room не будет работать. Используйте только если другие методы не помогли:

В `app/build.gradle.kts`:
```kotlin
tasks.withType<JavaCompile> {
    enabled = false
}
```

## Проверка

После выполнения шагов 1-5 попробуйте собрать проект:
- **Build → Make Project** или `Ctrl+F9`

Если ошибка исчезла, запустите приложение:
- **Run → Run 'app'**

