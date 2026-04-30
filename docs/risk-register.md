# Risk register

| Риск | Вероятность | Влияние | Mitigation | Owner | Fallback |
| --- | --- | --- | --- | --- | --- |
| Docker Desktop не запущен | Medium | High | Pre-demo checklist | Admin | Показать recorded logs |
| kind cluster не создается | Medium | High | Проверить версии заранее | Admin | Docker Compose demo |
| Local registry недоступен | Medium | High | Smoke test до защиты | Admin | Использовать уже pulled image |
| Demo image не проходит validation | Medium | Medium | Хранить валидный tag | Developer | Показать сохраненный PASSED job |
| Worker зависает | Medium | Medium | Timeout и recovery policy | Developer | Manual status update в demo data |
| Недоверенный image пытается выйти за scope | Low | High | Pod Security, limits, NetworkPolicy | Admin | Остановить lab |
| Evidence содержит секреты | Medium | High | UI warning и review checklist | Curator | Удалить attachment |
| Scope creep | High | High | MVP boundaries | Project owner | Перенести в P1/P2 |
| Не хватает времени на full UI | Medium | Medium | P0 routes first | Project owner | Swagger + screenshots |
| CI нестабилен на Windows | Medium | Medium | Документировать local commands | Developer | Ручные проверки |

## Demo fallback package

Перед защитой подготовить:

- скриншоты key pages;
- validation job logs;
- audit trail sample;
- exported demo reports;
- список команд и ожидаемый вывод.
