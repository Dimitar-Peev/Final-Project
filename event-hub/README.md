# EventHub Main Application

RESTful основно приложение за управление на събития и интеграция с microservices, част от EventHub системата. Изградено със **Spring Boot 3.4.0** и **Java 17**.

## 📋 Съдържание

* [Общ преглед](#общ-преглед)
* [Технологии](#технологии)
* [Архитектура](#архитектура)
* [API Endpoints](#api-endpoints)
* [Инсталация](#инсталация)
* [Конфигурация](#конфигурация)
* [Docker](#docker)
* [Тестове](#тестове)
* [Swagger документация](#swagger-документация)
* [Security](#security)
* [Scheduled Tasks](#scheduled-tasks)
* [Logging](#logging)
* [Автор и лиценз](#автор-и-лиценз)

---

## 🎯 Общ преглед

`EventHub` е основното приложение на системата, което управлява събития, потребители и плащания. Използва RESTful комуникация с microservices за нотификации и плащания чрез Feign Client.

### Основни функционалности

* 📅 Управление на събития (създаване, редакция, изтриване)
* 👤 Управление на потребители и роли (USER, ADMIN)
* 💳 Обработка на плащания чрез `Payment Service`
* ✉️ Изпращане на нотификации чрез `Notification Service`
* 🔒 Authentication & Authorization 
* 🗄️ Логване на всички важни операции
* ⏰ Scheduled jobs (почистване на стари записи, синхронизация с microservices)
* 🌐 Swagger UI документация за API

---

## 🛠 Технологии

* **Java 17**
* **Spring Boot 3.4.0**
    * Spring Web, Spring Data JPA, Spring Security, Spring Validation, Spring Scheduling
* **MySQL** - production база данни
* **H2 Database** - in-memory база за тестове
* **Lombok** - boilerplate код
* **Gradle** - build инструмент
* **Swagger/OpenAPI 3** - API документация
* **JUnit 5 & Mockito** - unit и integration тестове
* **Docker** - контейнеризация
* **Feign Client** - междусервисна комуникация

---

## 🏗 Архитектура

Проектът следва layered architecture и feature-based структура:

```
com.exam.eventhub
├── config/              # Конфигурационни класове (Swagger, Security, JWT)
├── exception/           # Custom exceptions
├── model/               # Entity модели
│   ├── Event
│   ├── User
│   └── Payment
├── repository/          # Data access layer
├── service/             # Business logic
├── scheduler/           # Scheduled tasks
├── web/                 # Presentation layer
│   ├── dto/             # Data Transfer Objects
│   ├── mapper/          # DTO mappers
│   ├── advice/          # Global exception handlers
│   └── controller/      # REST controllers
└── security/            # JWT & Spring Security конфигурация
```

### Модел на данни (пример)

**Event Entity:**

```java
{
  "id": "UUID",
  "title": "String",
  "description": "String",
  "location": "String",
  "startDateTime": "LocalDateTime",
  "endDateTime": "LocalDateTime",
  "organizerId": "UUID",
  "createdOn": "LocalDateTime",
  "deleted": "boolean"
}
```

**User Entity:**

```java
{
  "id": "UUID",
  "username": "String",
  "email": "String",
  "password": "hashed String",
  "roles": ["USER", "ADMIN"],
  "blocked": "boolean"
}
```

---

## 🔌 API Endpoints

### Base Path: `/api/v1`

| Method | Endpoint         | Описание                                        |
| ------ | ---------------- | ----------------------------------------------- |
| POST   | `/events`        | Създай ново събитие                             |
| GET    | `/events`        | Вземи всички събития                            |
| GET    | `/events/{id}`   | Вземи събитие по ID                             |
| PUT    | `/events/{id}`   | Редактирай събитие                              |
| DELETE | `/events/{id}`   | Изтрий събитие (soft delete)                    |
| POST   | `/users`         | Регистрация на нов потребител                   |
| GET    | `/users/{id}`    | Вземи потребител                                |
| PUT    | `/users/{id}`    | Актуализиране на профил                         |
| POST   | `/payments`      | Извърши плащане                                 |
| POST   | `/notifications` | Изпрати нотификация (чрез Notification Service) |

---

## 📦 Инсталация

### Предпоставки

* Java 17+
* MySQL 8.0+
* Gradle 8.11+ (или използвайте Gradle wrapper)
* Docker (по избор)

### Стъпки

```bash
git clone <repository-url>
cd event-hub
./gradlew build
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Приложението стартира на `http://localhost:8080`

---

## ⚙️ Конфигурация

### Профили

- **dev** - Development профил (localhost MySQL)
- **prod** - Production профил (Docker MySQL)
- **test** - Test профил (H2 in-memory database)

### Environment Variables

| Variable | Описание | Default |
|----------|----------|---------|
| `MYSQL_USER` | MySQL потребителско име | - |
| `MYSQL_PASSWORD` | MySQL парола | - |
| `SPRING_PROFILES_ACTIVE` | Активен Spring профил | dev |

---

## 🐳 Docker

### Build Docker Image

```bash
./gradlew build
docker build -t eventhub-main:1.0.0 .
```

### Run Container

```bash
docker run -d \
  -p 8080:8080 \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=password \
  --name eventhub-main \
  eventhub-main:1.0.0
```

---

## 🧪 Тестове

```bash
# Всички тестове
./gradlew test

# С coverage report
./gradlew test jacocoTestReport
```

Структура на тестовете:

```
src/test/java/com/exam/eventhub
├── service/
├── repository/
├── web/
├── security/
└── scheduler/
```

---

## 📚 Swagger Документация

Swagger UI е достъпен на:

```
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON schema:

```
http://localhost:8080/v3/api-docs
```

---

## 🔐 Security

* JWT базирана Authentication & Authorization
* Роли: USER, ADMIN
* Open, Authenticated и Authorized endpoints
* CSRF защита активирана

---

## 🔄 Scheduled Tasks

* Почиства стари записи (събития и плащания) след определен период
* Изпълнява синхронизация с Notification и Payment microservices

---

## 📝 Logging

* Slf4j логване на всички ключови операции
* Уровни:

    * INFO - създаване/изтриване/актуализиране
    * ERROR - изключения и неуспешни операции

---

## 👥 Автор

Dimitar Peev - Spring Advanced October 2025 Retake Exam

## 📄 Лиценз

Educational project

---

