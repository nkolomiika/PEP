# Accessibility и compatibility

## Поддерживаемые браузеры

Для защиты MVP поддерживаются актуальные версии:

- Google Chrome;
- Microsoft Edge.

Firefox и Safari относятся к расширенной production-ready проверке.

## Layout

- Основной сценарий оптимизирован для desktop/laptop.
- Минимальная ширина для комфортной защиты: 1280px.
- Таблицы должны иметь горизонтальный scroll или адаптивное представление.

## Keyboard navigation

- Все формы доступны через `Tab`.
- Focus state виден на кнопках, ссылках, input и textarea.
- Submit доступен с клавиатуры.
- Modal dialogs не должны терять focus.

## Forms

- Каждый input имеет label.
- Ошибка связана с полем и написана на русском.
- Обязательные поля отмечены текстом, а не только цветом.
- Поля image reference, port и health path имеют examples.

## Contrast и text

- Текст читаем на светлой и темной теме, если темная тема включена.
- Статус нельзя передавать только цветом.
- Размер шрифта не ниже 14px для основного текста.

## Checklist основных страниц

- Login.
- Course module.
- Submission form.
- Validation details.
- Report editor.
- Curator review.
- Admin labs.
- Audit page.
