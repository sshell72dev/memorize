# Диагностика падения приложения

## Проблема
Приложение сразу закрывается при запуске.

## Что было исправлено

1. ✅ Добавлена обработка ошибок в `MainActivity`
2. ✅ Добавлен `.allowMainThreadQueries()` для Room Database
3. ✅ Добавлена обработка ошибок в `SearchViewModel`
4. ✅ Добавлено логирование для диагностики

## Как получить логи ошибки

### Способ 1: Android Studio Logcat

1. Откройте Android Studio
2. Откройте вкладку **Logcat** (внизу экрана)
3. В фильтре введите: `tag:Memorize` или `package:com.memorize`
4. Очистите логи (кнопка очистки)
5. Запустите приложение через Run
6. Скопируйте все строки с `Memorize` или `ERROR`/`FATAL`

### Способ 2: ADB (если доступен)

```powershell
# Очистить логи
adb logcat -c

# Запустить приложение и получить логи
adb logcat | Select-String "Memorize|FATAL|AndroidRuntime"
```

### Способ 3: Фильтр по ошибкам

В Logcat используйте фильтр:
- `level:error` - только ошибки
- `tag:AndroidRuntime` - системные ошибки
- `package:com.memorize` - логи приложения

## Что искать в логах

Ищите строки, содержащие:
- `FATAL EXCEPTION`
- `RuntimeException`
- `Memorize: Error`
- `Memorize: Exception`

## Следующие шаги

1. Пересоберите приложение: **Build → Rebuild Project**
2. Запустите приложение
3. Скопируйте логи с тегом `Memorize` или `FATAL EXCEPTION`
4. Отправьте логи для дальнейшей диагностики

## Возможные причины

1. **Проблема с базой данных** - Room не может создать/открыть БД
2. **Проблема с ViewModel** - ошибка при инициализации
3. **Проблема с ресурсами** - отсутствует `config.xml` или неправильные ключи
4. **Проблема с разрешениями** - отсутствуют необходимые разрешения

## Проверка конфигурации

Убедитесь, что файл существует:
- `android/app/src/main/res/values/config.xml`

И содержит:
```xml
<string name="yandex_api_key">YOUR_KEY</string>
<string name="yandex_folder_id">YOUR_FOLDER_ID</string>
```

