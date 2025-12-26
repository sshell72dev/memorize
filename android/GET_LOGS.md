# Как получить логи ошибки приложения

## Важно!
Приложение падает, но логи с тегом "Memorize" не видны. Это означает, что нужно использовать правильный фильтр.

## Способ 1: Фильтр по AndroidRuntime (РЕКОМЕНДУЕТСЯ)

В Android Studio Logcat:

1. Откройте вкладку **Logcat**
2. В поле фильтра введите: `tag:AndroidRuntime`
3. Очистите логи (кнопка очистки)
4. Запустите приложение
5. Скопируйте **ВСЕ** строки, которые появятся (особенно те, что содержат `FATAL EXCEPTION`)

## Способ 2: Фильтр по уровню ошибок

В Logcat используйте фильтр:
- `level:error` - покажет все ошибки
- Или: `package:com.memorize level:error`

## Способ 3: Без фильтра (показать все)

1. Уберите все фильтры в Logcat
2. Очистите логи
3. Запустите приложение
4. Ищите строки с:
   - `FATAL EXCEPTION`
   - `AndroidRuntime`
   - `com.memorize`
   - `Process: com.memorize`

## Что искать

Ищите блоки вида:
```
FATAL EXCEPTION: main
Process: com.memorize, PID: XXXXX
java.lang.RuntimeException: ...
    at com.memorize...
```

Или:
```
AndroidRuntime: FATAL EXCEPTION: main
AndroidRuntime: Process: com.memorize, PID: XXXXX
AndroidRuntime: java.lang.ClassNotFoundException: ...
```

## Пример правильного фильтра

В Logcat введите:
```
tag:AndroidRuntime | package:com.memorize | level:error
```

Это покажет:
- Все ошибки AndroidRuntime
- Все логи приложения com.memorize
- Все ошибки уровня ERROR

## После получения логов

Скопируйте **ВСЕ** строки с ошибкой (особенно stack trace) и отправьте для анализа.

