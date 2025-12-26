# Миграция с KAPT на KSP

## Проблема

KAPT (Kotlin Annotation Processing Tool) несовместим с Java 21 из-за модульной системы Java. KAPT использует внутренние API компилятора Java, которые стали недоступны.

Ошибка:
```
IllegalAccessError: superclass access check failed: class org.jetbrains.kotlin.kapt3.base.javac.KaptJavaCompiler 
cannot access class com.sun.tools.javac.main.JavaCompiler because module jdk.compiler does not export 
com.sun.tools.javac.main to unnamed module
```

## Решение: KSP вместо KAPT

KSP (Kotlin Symbol Processing) - это современная замена KAPT от JetBrains, которая:
- ✅ Полностью совместима с Java 17+
- ✅ Работает быстрее KAPT (в 2 раза)
- ✅ Лучше интегрируется с Kotlin
- ✅ Не требует Java компилятор

## Что было изменено

### 1. Заменен плагин в `app/build.gradle.kts`:
```kotlin
// Было:
id("kotlin-kapt")

// Стало:
id("com.google.devtools.ksp") version "1.9.20-1.0.14"
```

### 2. Заменены зависимости:
```kotlin
// Было:
kapt("androidx.room:room-compiler:$roomVersion")

// Стало:
ksp("androidx.room:room-compiler:$roomVersion")
```

## Что делать дальше

1. **Синхронизируйте проект** в Android Studio:
   - Нажмите **Sync Now** или **File → Sync Project with Gradle Files**
   - Gradle автоматически скачает KSP плагин

2. **Очистите проект** (рекомендуется):
   - **Build → Clean Project**
   - Затем **Build → Make Project** (или нажмите `Ctrl+F9`)
   
   **Альтернатива:** Можно использовать **Build → Clean and Assemble Project with Tests** для полной очистки и сборки

3. **Соберите проект**:
   - **Build → Make Project** или `Ctrl+F9`

4. Ошибка должна исчезнуть! ✅

## Преимущества KSP

- **Скорость**: KSP работает в 2 раза быстрее KAPT
- **Совместимость**: Полная поддержка Java 17, 19, 21
- **Интеграция**: Нативная поддержка Kotlin
- **Будущее**: KAPT устаревает, KSP - это будущее

## Совместимость версий

- Kotlin: 1.9.20
- KSP: 1.9.20-1.0.14 (соответствует версии Kotlin)
- Room: 2.6.1 (полностью поддерживает KSP)

## Если возникнут проблемы

1. Убедитесь, что версия KSP соответствует версии Kotlin
2. Очистите кэш: **File → Invalidate Caches / Restart**
3. Удалите папку `.gradle` и пересоберите проект

