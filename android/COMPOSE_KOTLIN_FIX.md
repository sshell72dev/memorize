# Исправление несовместимости Compose Compiler и Kotlin

## Проблема

Версия Compose Compiler 1.5.3 требует Kotlin 1.9.10, но в проекте используется Kotlin 1.9.20, что вызывает предупреждение о несовместимости.

## Решение

Обновлен Compose Compiler до версии 1.5.5, которая полностью совместима с Kotlin 1.9.20.

### Изменения в `app/build.gradle.kts`:
```kotlin
composeOptions {
    kotlinCompilerExtensionVersion = "1.5.5"  // было 1.5.3
}
```

## Совместимость версий

- ✅ Kotlin: 1.9.20
- ✅ Compose Compiler: 1.5.5
- ✅ Совместимость подтверждена официальной таблицей совместимости

## Что делать дальше

1. **Синхронизируйте проект**:
   - **File → Sync Project with Gradle Files**

2. **Соберите проект**:
   - **Build → Make Project** или `Ctrl+F9`

3. Предупреждение должно исчезнуть! ✅

## Дополнительно: Индексы для внешних ключей

Также добавлены индексы для всех внешних ключей в Room entities, чтобы улучшить производительность запросов:

- `SectionEntity`: индекс на `textId`
- `ParagraphEntity`: индекс на `sectionId`
- `PhraseEntity`: индекс на `paragraphId`
- `LearningSessionEntity`: индекс на `textId`

Это устраняет предупреждения KSP о полных сканированиях таблиц.

