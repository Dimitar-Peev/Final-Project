# EventHub Notification Service

RESTful микросервис за управление на нотификации, част от EventHub системата. Изграден със Spring Boot 3.4.0 и Java 17.

## 📋 Съдържание

- [Общ преглед](#общ-преглед)
- [Технологии](#технологии)
- [Архитектура](#архитектура)
- [API Endpoints](#api-endpoints)
- [Инсталация](#инсталация)
- [Конфигурация](#конфигурация)
- [Docker](#docker)
- [Тестове](#тестове)
- [Swagger документация](#swagger-документация)

---

## 🎯 Общ преглед

Notification Service е микросервис, който управлява изпращането, съхранението и извличането на нотификации за потребителите на EventHub платформата. Предлага RESTful API за интеграция с главното EventHub приложение.

### Основни функционалности

- ✉️ Изпращане на нотификации до потребители
- 📋 Извличане на всички нотификации
- 👤 Извличане на нотификации по потребител
- 🗑️ Изтриване на отделни нотификации (soft delete)
- 🧹 Изчистване на цялата история на нотификации за потребител
- ⏰ Автоматично изчистване на стари нотификации (по-стари от 30 дни)
- 📊 Swagger UI документация

---

## 🛠 Технологии

- **Java 17**
- **Spring Boot 3.4.0**
  - Spring Data JPA
  - Spring Web
  - Spring Validation
  - Spring Scheduling
- **MySQL** - production база данни
- **H2 Database** - in-memory база за тестове
- **Lombok** - намаляване на boilerplate код
- **Gradle** - build инструмент
- **Swagger/OpenAPI 3** - API документация
- **JUnit 5 & Mockito** - unit и integration тестове
- **Docker** - контейнеризация

---

## 🏗 Архитектура

Проектът следва layered architecture:

```
com.exam.app
├── config/              # Конфигурационни класове
│   └── SwaggerConfig
├── exception/           # Custom exceptions
│   └── NotificationNotFoundException
├── model/              # Entity модели
│   ├── Notification
│   └── NotificationStatus (enum)
├── repository/         # Data access layer
│   └── NotificationRepository
├── scheduler/          # Scheduled tasks
│   └── NotificationCleanupScheduler
├── service/            # Business logic
│   └── NotificationService
└── web/                # Presentation layer
    ├── dto/            # Data Transfer Objects
    │   ├── ErrorResponse
    │   ├── NotificationRequest
    │   └── NotificationResponse
    ├── mapper/         # DTO mappers
    │   └── DtoMapper
    ├── ExceptionAdvice # Global exception handler
    ├── NotificationController
    └── Paths           # API path constants
```

---

### Модел на данни

**Notification Entity:**
```java
{
  "id": "UUID",
  "recipientId": "UUID",
  "recipientEmail": "String",
  "subject": "String",
  "message": "String (max 500 chars)",
  "status": "PENDING | SENT",
  "createdOn": "LocalDateTime",
  "deleted": "boolean"
}
```

---

## 🔌 API Endpoints

### Base Path: `/api/v1/notifications`

| Method | Endpoint | Описание |
|--------|----------|----------|
| POST | `/` | Изпрати нотификация |
| GET | `/` | Вземи всички нотификации |
| GET | `/{userId}` | Вземи нотификации по потребител |
| DELETE | `/{id}` | Изтрий нотификация |
| DELETE | `/?userId={userId}` | Изчисти историята на нотификации |

---

### Примерни Request/Response

**POST `/api/v1/notifications`** - Изпращане на нотификация

**Request:**

```json
{
    "recipientId": "8c69e438-21b3-4c9b-b10d-5a3f5c0a0a77",
    "recipientEmail": "user@example.com",
    "subject": "111 Booking Confirmed",
    "message": "Your booking for Java Conference 2025 was successfully confirmed!"
}
```

**Response (201 Created):**

```json
{
    "id": "41efd0b6-4ed4-46e0-9f1b-39aeec50b1fc",
    "recipientId": "8c69e438-21b3-4c9b-b10d-5a3f5c0a0a77",
    "recipientEmail": "user@example.com",
    "subject": "111 Booking Confirmed",
    "message": "Your booking for Java Conference 2025 was successfully confirmed!",
    "status": "SENT",
    "createdOn": "2025-12-08T20:55:33.619993"
}
```

**GET `/api/v1/notifications/{userId}`** - Извличане на нотификации

**Response (200 OK):**

```json
[
    {
        "id": "41efd0b6-4ed4-46e0-9f1b-39aeec50b1fc",
        "recipientId": "8c69e438-21b3-4c9b-b10d-5a3f5c0a0a77",
        "recipientEmail": "user@example.com",
        "subject": "111 Booking Confirmed",
        "message": "Your booking for Java Conference 2025 was successfully confirmed!",
        "status": "SENT",
        "createdOn": "2025-12-08T20:55:33.619993"
    }
]
```

---

## 📦 Инсталация

### Предпоставки

- Java 17+
- MySQL 8.0+
- Gradle 8.11+ (или използвайте gradle wrapper)
- Docker (по избор)

### Стъпки

1. **Клонирайте репозиторито:**
```bash
git clone <repository-url>
cd eventhub-notification-service
```

2. **Конфигурирайте базата данни:**

Създайте `.env` файл или задайте environment variables:
```bash
export MYSQL_USER=your_mysql_username
export MYSQL_PASSWORD=your_mysql_password
```

Или редактирайте `application-dev.yml` и задайте директно credentials.

3. **Build проекта:**
```bash
./gradlew build
```

4. **Стартирайте приложението:**
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Приложението ще стартира на `http://localhost:8081`

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

### application-dev.yaml
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/eventhub_notifications?createDatabaseIfNotExist=true
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
server:
  port: 8081
```

### application-prod.yaml
```yaml
spring:
  datasource:
    url: jdbc:mysql://host.docker.internal:3306/eventhub_notifications?createDatabaseIfNotExist=true
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
server:
  port: 8081
```

## 🐳 Docker

### Build Docker Image
```bash
gradle clean build
docker build -t eventhub-notification-service:1.0.0 .
```

### Run Container
```bash
docker run -d `
  -p 8081:8081 `
  -e SPRING_PROFILES_ACTIVE=prod `
  -e MYSQL_USER=root `
  -e MYSQL_PASSWORD=password `
  --name notification-service `
  eventhub-notification-service:1.0.0
```

### Docker Compose (пример)
```yaml
version: '3.8'
services:
  notification-service:
    image: eventhub-notification-service:1.0.0
    ports:
      - "8081:8081"
    environment:
      - MYSQL_USER=root
      - MYSQL_PASSWORD=password
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      - mysql
```

## 🧪 Тестове

### Структура на тестовете

```
src/test/java/com/exam/app
├── repository/
│   └── NotificationRepositoryTest.java
├── scheduler/
│   └── NotificationCleanupSchedulerUTest.java
├── service/
│   └── NotificationServiceUTest.java
└── util/
│   └── TestBuilder.java
├── web/
│   └── mapper/
│       └── DtoMapperUTest.java
│   ├── ExceptionAdviceUTest.java
│   ├── NotificationControllerApiTest.java
```

### Стартиране на тестовете

```bash
# Всички тестове
./gradlew test

# С coverage report
./gradlew test jacocoTestReport

# Само unit тестове
./gradlew test --tests "*UTest"

# Само integration тестове
./gradlew test --tests "*ApiTest"
```

### Test Coverage

- ✅ **Service Layer** - NotificationServiceUTest
- ✅ **Repository Layer** - NotificationRepositoryTest (@DataJpaTest)
- ✅ **Controller Layer** - NotificationControllerApiTest (@SpringBootTest)
- ✅ **Mapper Layer** - DtoMapperUTest
- ✅ **Exception Handling** - ExceptionAdviceUTest
- ✅ **Scheduler** - NotificationCleanupSchedulerUTest

---

## 📚 Swagger Документация

Swagger UI е достъпен на:
```
http://localhost:8081/swagger-ui/index.html
```

OpenAPI JSON schema:
```
http://localhost:8081/v3/api-docs
```

## 🔄 Scheduled Tasks

### Notification Cleanup Scheduler

Автоматично изтрива нотификации по-стари от 30 дни.

- **Честота:** На всеки час (3600000ms fixed delay)
- **Retention period:** 30 дни
- **Логика:** Изтрива нотификации с `createdOn < (now - 30 days)`

```java
@Scheduled(fixedDelay = 3600000)
public void cleanupOldNotifications()
```

---

## 🔐 Exception Handling

Глобален exception handler с три типа грешки:

1. **404 Not Found**
   - `NoResourceFoundException` - невалиден endpoint
   - `NotificationNotFoundException` - нотификация не е намерена

2. **500 Internal Server Error**
   - Всички други exceptions

### Error Response Format

```json
{
  "status": 404,
  "message": "Notification with ID [xxx] was not found."
}
```

---

## 📝 Допълнителни бележки

### Soft Delete Pattern

Приложението използва soft delete за нотификациите:
- `deleted` поле се маркира като `true`
- Физическо изтриване само от scheduler след 30 дни
- Всички query-та филтрират по `deleted = false`

### Logging

Структурирано логване с Slf4j:
- 📥 INFO - fetch операции
- ✉️ INFO - send операции
- 🗑️ INFO - delete операции
- ⚠️ ERROR - грешки (автоматично от Spring)

---

## 👥 Автор

Dimitar Peev - Spring Advanced October 2025 Retake Exam

## 📄 Лиценз

Educational project
