# Исправление ошибки JDK Image

## Проблема

Ошибка при сборке:
```
Could not resolve all files for configuration ':app:androidJdkImage'.
Failed to transform core-for-system-modules.jar
Error while executing process jlink.exe
```

Эта ошибка возникает из-за попытки Android Gradle Plugin создать JDK образ с помощью `jlink`, что может не работать с некоторыми версиями JDK.

## Решение

Добавлена настройка в `gradle.properties`:
```properties
android.experimental.disableJdkImage=true
```

Это отключает генерацию JDK образа и позволяет использовать стандартный JDK для компиляции.

## Что делать дальше

1. **Синхронизируйте проект**:
   - **File → Sync Project with Gradle Files**

2. **Очистите кэш Gradle** (если проблема сохраняется):
   ```bash
   # В Android Studio: File → Invalidate Caches / Restart
   # Или вручную удалите папку:
   # C:\Users\YourName\.gradle\caches\transforms-3\
   ```

3. **Соберите проект**:
   - **Build → Clean Project**
   - Затем **Build → Make Project** или `Ctrl+F9`

4. Ошибка должна исчезнуть! ✅

## Альтернативные решения

Если проблема сохраняется:

1. **Используйте JDK 17** вместо JDK 21:
   - В Android Studio: **File → Project Structure → SDK Location**
   - Убедитесь, что используется JDK 17

2. **Очистите кэш Gradle вручную**:
   - Закройте Android Studio
   - Удалите папку `C:\Users\YourName\.gradle\caches\transforms-3\`
   - Откройте проект заново

3. **Обновите Android Gradle Plugin** до последней версии (если доступно)

