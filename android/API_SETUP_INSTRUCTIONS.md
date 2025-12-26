# Инструкция по настройке Yandex GPT API

## Проблема с доступом (403 Forbidden)

Если вы видите ошибку **403 Forbidden** с сообщением:
```
Permission to [resource-manager.folder b1gdg5qqsp63ja0ct5mr] denied
```

Это означает, что ваш API ключ не имеет доступа к указанному Folder ID.

## Решение

### 1. Проверьте файл конфигурации

Убедитесь, что файл `android/app/src/main/res/values/config.xml` содержит правильные значения:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="yandex_api_key">ВАШ_API_КЛЮЧ</string>
    <string name="yandex_folder_id">b1gdg5qqsp63ja0ct5mr</string>
</resources>
```

### 2. Проверьте в Yandex Cloud Console

1. Откройте [Yandex Cloud Console](https://console.cloud.yandex.ru/)
2. Перейдите в раздел **IAM** (Identity and Access Management)
3. Найдите ваш API ключ
4. Убедитесь, что у API ключа есть права на:
   - `ai.languageModels.user` - для использования YandexGPT
   - Доступ к каталогу (folder) `b1gdg5qqsp63ja0ct5mr`

### 3. Создайте новый API ключ (если нужно)

1. В Yandex Cloud Console перейдите в **IAM → Service accounts**
2. Создайте или выберите сервисный аккаунт
3. Назначьте роли:
   - `ai.languageModels.user`
   - `viewer` (минимум) для каталога
4. Создайте API ключ для этого сервисного аккаунта
5. Обновите `yandex_api_key` в `config.xml`

### 4. Проверьте Folder ID

Убедитесь, что Folder ID `b1gdg5qqsp63ja0ct5mr`:
- Существует в вашем облаке
- Принадлежит вашему аккаунту
- Имеет доступ к сервису YandexGPT

### 5. Альтернатива: используйте другой Folder ID

Если у вас есть другой каталог с доступом к YandexGPT:
1. Скопируйте его Folder ID
2. Обновите `yandex_folder_id` в `config.xml`
3. Пересоберите приложение

## После настройки

1. **Пересоберите проект** в Android Studio
2. **Очистите кэш** (Build → Clean Project)
3. **Пересоберите** (Build → Rebuild Project)
4. **Запустите приложение** и попробуйте поиск снова

## Проверка работы

После настройки попробуйте найти текст, например:
- "Зимнее утро Пушкин"
- "Евгений Онегин Пушкин"

Если всё настроено правильно, текст должен быть найден и отображен для предпросмотра.

