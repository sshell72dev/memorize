# Исправление ошибки: "resource mipmap/ic_launcher not found"

## Проблема

Android приложение требует иконку запуска (launcher icon), которая указана в `AndroidManifest.xml`:
- `android:icon="@mipmap/ic_launcher"`
- `android:roundIcon="@mipmap/ic_launcher_round"`

Но папки `mipmap-*` с иконками отсутствовали, что вызывало ошибку сборки:
```
AAPT: error: resource mipmap/ic_launcher (aka com.memorize:mipmap/ic_launcher) not found.
```

## Решение

### Что было сделано:

1. **Изменен AndroidManifest.xml**:
   - Заменено `@mipmap/ic_launcher` на `@drawable/ic_launcher`
   - Заменено `@mipmap/ic_launcher_round` на `@drawable/ic_launcher`

2. **Создана векторная иконка**:
   - Файл: `android/app/src/main/res/drawable/ic_launcher.xml`
   - Простая векторная иконка с символом книги/запоминания

## Что делать дальше

1. **Синхронизируйте проект** в Android Studio:
   - Нажмите **Sync Now** или **File → Sync Project with Gradle Files**

2. **Соберите проект**:
   - **Build → Make Project** или `Ctrl+F9`

3. Ошибка должна исчезнуть! ✅

## Создание профессиональной иконки (опционально)

Если вы хотите создать полноценную иконку приложения:

### Вариант 1: Через Android Studio
1. **File → New → Image Asset**
2. Выберите **Launcher Icons (Adaptive and Legacy)**
3. Настройте иконку
4. Android Studio автоматически создаст все необходимые размеры

### Вариант 2: Онлайн генераторы
- [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html)
- Загрузите ваше изображение
- Скачайте сгенерированные иконки
- Поместите их в папки `mipmap-*`

### Вариант 3: Вручную
Создайте папки:
- `res/mipmap-mdpi/` (48x48 px)
- `res/mipmap-hdpi/` (72x72 px)
- `res/mipmap-xhdpi/` (96x96 px)
- `res/mipmap-xxhdpi/` (144x144 px)
- `res/mipmap-xxxhdpi/` (192x192 px)

И поместите в каждую папку файл `ic_launcher.png` соответствующего размера.

После создания иконок обновите `AndroidManifest.xml` обратно на `@mipmap/ic_launcher`.

## Текущее решение

Сейчас используется простая векторная иконка из `drawable`, которая:
- ✅ Работает на всех устройствах
- ✅ Масштабируется без потери качества
- ✅ Не требует дополнительных ресурсов
- ⚠️ Простая по дизайну (можно заменить позже)

