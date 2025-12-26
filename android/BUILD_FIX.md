# Исправление ошибки сборки: "The file name must end with .xml"

## Проблема

Android Gradle Plugin обрабатывает **все файлы** в папке `res/values/` как XML ресурсы. Файл `config.xml.example` не является валидным XML ресурсом, поэтому сборка падала с ошибкой:

```
The file name must end with .xml
```

## Решение

Файл `config.xml.example` был перемещен из:
- ❌ `android/app/src/main/res/values/config.xml.example` (обрабатывается как ресурс)

В:
- ✅ `android/config.xml.example` (не обрабатывается как ресурс)

## Что делать дальше

1. **Синхронизируйте проект** в Android Studio:
   - Нажмите **Sync Now** или **File → Sync Project with Gradle Files**

2. **Попробуйте собрать проект снова**:
   - **Build → Make Project** или `Ctrl+F9`

3. Ошибка должна исчезнуть! ✅

## Настройка API ключей

Если вам нужно настроить API ключи:
1. Скопируйте `android/config.xml.example` 
2. Вставьте в `android/app/src/main/res/values/config.xml`
3. Замените `YOUR_YANDEX_API_KEY` и `YOUR_FOLDER_ID` на ваши значения

**Важно:** Файл `config.xml` уже существует и настроен, так что этот шаг нужен только если вы хотите пересоздать его.

