# КРИТИЧЕСКИ ВАЖНО: Как получить логи ошибки

## Проблема
Приложение падает, но в логах нет `FATAL EXCEPTION`. Это означает, что нужно использовать **ПРАВИЛЬНЫЙ ФИЛЬТР**.

## ⚠️ ВАЖНО: Используйте этот фильтр

В Android Studio Logcat:

1. **Уберите все существующие фильтры** (очистите поле фильтра)
2. В поле фильтра введите **ТОЧНО** это:
   ```
   package:com.memorize
   ```
3. Нажмите Enter
4. **Очистите логи** (кнопка с иконкой мусорного ведра или `Ctrl+L`)
5. **Запустите приложение** (Run → Run 'app')
6. **СРАЗУ после запуска** (в течение 1-2 секунд) скопируйте **ВСЕ** строки, которые появятся

## Альтернативный способ (если первый не работает)

1. В Logcat выберите фильтр: **"Show only selected application"** (если есть)
2. Или используйте фильтр: `tag:AndroidRuntime`
3. Очистите логи
4. Запустите приложение
5. Скопируйте **ВСЕ** строки с `FATAL` или `Exception`

## Что искать

Ищите строки, которые содержат:
- `FATAL EXCEPTION`
- `AndroidRuntime`
- `com.memorize`
- `Process: com.memorize`
- `PID: 9808` (или другой номер процесса)

## Если логов все еще нет

Попробуйте:

1. **В Logcat выберите уровень**: `Error` или `Verbose`
2. **Убедитесь, что выбрано правильное устройство** (вверху Logcat)
3. **Попробуйте без фильтра** - просто очистите логи и запустите приложение, затем найдите строки с `com.memorize`

## Пример того, что должно быть в логах

```
AndroidRuntime: FATAL EXCEPTION: main
AndroidRuntime: Process: com.memorize, PID: 9808
AndroidRuntime: java.lang.RuntimeException: ...
AndroidRuntime:     at com.memorize.MainActivity.onCreate(...)
```

Или:

```
E/AndroidRuntime: FATAL EXCEPTION: main
E/AndroidRuntime: Process: com.memorize, PID: 9808
E/AndroidRuntime: java.lang.ClassNotFoundException: ...
```

## После получения логов

Скопируйте **ВСЕ** строки с ошибкой (особенно stack trace) и отправьте.

