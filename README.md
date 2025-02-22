# rocket-action

## Used

### useiconic

https://useiconic.com/open

### Icons

https://iconizer.net/

https://icons8.com/icons

### image4j

https://github.com/imcdonagh/image4j

### batik

https://xmlgraphics.apache.org/batik/download.html

### FlatLaf

https://github.com/JFormDesigner/FlatLaf
https://www.formdev.com/flatlaf/themes/
https://www.formdev.com/flatlaf/customizing/
https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-swingx/README.md

### Swagger-UI

https://github.com/swagger-api/swagger-ui

## Infrastructure

|class| description                         |
|-----|-------------------------------------|
|NotificationService| Сервис уведомлений                  |
|IconRepository| Хранилище иконок                    |
| Cache | Кеширование файлов по URL           |
|UsedPropertiesName| Доступные свойства для конфигурации |

## Environment

Java 8

## Run

First argument is file with actions.

## Release

Change version in [application-ui-swing/pom.xml](application-ui-swing/pom.xml)

```bash
mvn clean package
```

