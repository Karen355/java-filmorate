# Коллекция запросов для API Filmorate

Импорт в **Postman** или **Insomnia** для ручной проверки эндпоинтов.

## Как использовать

1. Запустите приложение: `mvn spring-boot:run` (по умолчанию порт 8080).
2. **Postman:** Import → Upload Files → выберите `Filmorate-API.postman_collection.json`.
3. **Insomnia:** Import/Export → Import Data → From File → выберите `Filmorate-API.postman_collection.json`.

Переменная `baseUrl` по умолчанию: `http://localhost:8080`. При другом порте измените её в настройках коллекции или
окружения.
