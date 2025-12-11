# EventHub Payment Service

RESTful микросервис за управление на плащания и възстановявания (refunds), част от EventHub екосистемата. Изграден със Spring Boot 3.4.0 и Java 17.

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
* [Exception Handling](#exception-handling)
* [Бележки](#бележки)

---

## 🎯 Общ преглед

Payment Service предоставя API за обработка на плащания и извършване на възстановявания към потребители на EventHub платформата. Работи като независим микросервис и може да бъде оркестриран от EventHub главното приложение.

### Основни функционалности

* 💳 Иницииране на плащане за поръчка или събитие
* ✔️ Потвърждение на успешно плащане
* ↩️ Извършване на refund по заявка на потребител или администратор
* 📄 История на всички плащания
* 🔍 Извличане на плащания по потребител
* 📊 Swagger UI документация
* 🧪 Покрити unit и integration тестове

---

## 🛠 Технологии

* **Java 17**
* **Spring Boot 3.4.0**

    * Spring Web
    * Spring Validation
    * Spring Data JPA
* **MySQL** — production база
* **H2 Database** — тестова база
* **Lombok**
* **Gradle**
* **JUnit 5 / Mockito**
* **Swagger/OpenAPI 3**
* **Docker**

---

## 🏗 Архитектура

Проектът следва ясно разделена слоеста структура:

```
com.exam.app
├── config/               # Swagger, app config
│   └── SwaggerConfig
├── exception/            # Custom Exceptions
│   └── PaymentNotFoundException
├── model/                # Persistence layer entities
│   ├── Payment
│   ├── PaymentStatus
│   ├── Transaction
│   ├── TransactionStatus
│   └── TransactionType
├── repository/
│   ├── PaymentRepository
│   └── TransactionRepository
├── service/
│   ├── PaymentGateway   # Payment Gateway abstraction
│   │   └── StripePaymentGateway (пример)
│   └── PaymentService
└── web/
    ├── dto/              # DTO слой
    │   ├── ErrorResponse 
    │   ├── PaymentRequest
    │   ├── PaymentResponse
    │   └── RefundRequest
    ├── mapper/           # DTO MapStruct/Manual mapper
    │   └── DtoMapper
    ├── ExceptionAdvice
    ├── Paths             # API path constants
    └── PaymentController
```

---

## 🗄 Модел на данни

**Payment Entity**

```java
{
  "id": "UUID",
  "userId": "UUID",
  "orderId": "UUID",
  "amount": "BigDecimal",
  "currency": "String",
  "status": "PENDING | SUCCESS | FAILED | REFUNDED",
  "createdOn": "LocalDateTime",
  "updatedOn": "LocalDateTime"
}
```

---

## 🔌 API Endpoints

### Base Path: `/api/v1/payments`

| Method | Endpoint          | Описание                     |
| ------ |-------------------|------------------------------|
| POST   | `/`               | Създаване на ново плащане    |
| GET    | `/`               | Извличане на всички плащания |
| GET    | `/users/{userId}` | Плащания по потребител       |
| GET    | `/bookings/{bookingId}`  | Плащания по резервация       |
| POST   | `/refund`         | Извършване на refund         |
| GET    | `/{paymentId}`    | Детайли за плащане           |

---

### Примерни заявки

**POST `/api/v1/payments`** - Иницииране на плащане

**Request:**

```json
{
    "bookingId": "b77c7b3d-5bcb-4e60-b1c1-b514edc88a1a",
    "userId": "a1e76d0a-7a3f-4d8c-9e34-9d635ba96c3b",
    "amount": 50.00
}
```

**Response (201 Created):**

```json
{
	"paymentId": "4174ee9b-1777-4587-83ac-efd86eccbb30",
	"bookingId": "b77c7b3d-5bcb-4e60-b1c1-b514edc88a1a",
	"userId": "a1e76d0a-7a3f-4d8c-9e34-9d635ba96c3b",
	"amount": 50.00,
	"status": "SUCCESS",
	"message": null,
	"createdOn": "2025-12-08T16:34:42.678596"
}
```

---

**POST `/api/v1/payments/4174ee9b-1777-4587-83ac-efd86eccbb30/refunds`** - Refund операция

**Request:**
```json
{
    "amount": 50.00
}
```

**Response:**

```json
{
    "paymentId": "4174ee9b-1777-4587-83ac-efd86eccbb30",
    "bookingId": "b77c7b3d-5bcb-4e60-b1c1-b514edc88a1a",
    "userId": "a1e76d0a-7a3f-4d8c-9e34-9d635ba96c3b",
    "amount": 50.00,
    "status": "REFUNDED",
    "message": null,
    "createdOn": "2025-12-08T16:34:42.678596"
}
```

---

## 📦 Инсталация

### Предпоставки

- Java 17+
- MySQL 8.0+
- Gradle 8.11+ (или използвайте gradle wrapper)
- Docker (по избор)


### Стъпки

1. **Клониране**

```bash
git clone <repo-url>
cd eventhub-payment-service
```

2. **Build**

```bash
./gradlew build
```

3. **Стартиране**

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Приложението стартира на:
👉 `http://localhost:8082`

---

## ⚙️ Конфигурация

### Профили

- **dev** - Development профил (localhost MySQL)
- **prod** - Production профил (Docker MySQL)
- **test** - Test профил (H2 in-memory database)

### Основни environment променливи

| Variable | Описание | Default |
|----------|----------|---------|
| `MYSQL_USER` | MySQL потребителско име | - |
| `MYSQL_PASSWORD` | MySQL парола | - |
| `SPRING_PROFILES_ACTIVE` | Активен Spring профил | dev |
---

### application-dev.yaml

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/eventhub_payments?createDatabaseIfNotExist=true
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
server:
  port: 8082
```

### application-prod.yaml

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/eventhub_payments?createDatabaseIfNotExist=true
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
server:
  port: 8082
```

---

## 🐳 Docker

### Build Image

```bash
gradle clean build
docker build -t mctrix87/eventhub-payment-service:1.0.0 .
```

### Run Container

```bash
docker run -d `
  -p 8082:8082 `
  -e SPRING_PROFILES_ACTIVE=prod `
  -e MYSQL_USER=root `
  -e MYSQL_PASSWORD=password `
  --name payment-service `
  mctrix87/eventhub-payment-service:1.0.0
```

### Docker Compose пример

```yaml
version: '3.8'

services:
  payment-service:
    image: eventhub-payment-service:1.0.0
    ports:
      - "8082:8082"
    environment:
      - MYSQL_USER=root
      - MYSQL_PASSWORD=password
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      - mysql
```

---

## 🧪 Тестове

Проектът има добре структурирани юнит и интеграционни тестове.

### Структура

```
src/test/java/com/exam/app
├── service/
│   └── PaymentServiceUTest
├── util/
│   └── TestBuilder
├── web/
│   ├── mapper/
│   │   └── DtoMapperUTest.java
│   ├── ExceptionAdviceUTest
│   ├── PaymentControllerApiTest
```

---

## 📚 Swagger документация

Swagger UI е достъпен на:
```
http://localhost:8082/swagger-ui/index.html
```

OpenAPI JSON schema:
```
http://localhost:8082/v3/api-docs
```

---

## 🔐 Exception Handling

Глобален Exception Handler (`ExceptionAdvice`) обработва:

### 1️⃣ PaymentNotFoundException

* Плащането не е намерено
* HTTP 404

### 2️⃣ RefundNotAllowedException

* Опит за refund на вече върнато или неуспешно плащане
* HTTP 400

### 3️⃣ MethodArgumentNotValidException

* Невалидни входни данни
* HTTP 400

### Error формат

```json
{
	"status": 404,
	"message": "Payment with ID [00000000-0000-0000-0000-000000000000] was not found.",
	"time": "2025-12-08T16:40:11.8805052"
}
```

---

## 📝 Бележки

### Payment Gateway Abstraction

В папка `service/gateway` има интерфейс за външен платежен доставчик.
Реализацията може да бъде Stripe, PayPal, банков gateway и др.

### Logging

* INFO — успешни операции
* WARN — отказани плащания
* ERROR — системни грешки

---

## 👥 Автор

Dimitar Peev — Spring Advanced October 2025 Retake Exam

## 📄 Лиценз

Educational project
