# Pharmacy

Medicine Drugstore is split into three backend apps plus the React storefront.

| App | Path | Port | Role |
|---|---|---|---|
| Drugstore API | `backend` | 8082 | Catalog, cart, orders, checkout orchestration |
| Payment Kafka service | `payment-service` | 8083 | Charges the selected payment mode |
| Notification Kafka service | `notification-service` | 8084 | Sends order and password-reset emails |
| Storefront | `frontend` | 5173 | Vite + React UI |

## Kafka topics

- `pharmacy.payment.commands` — drugstore asks the payment service to charge an order
- `pharmacy.payment.replies` — payment service returns success or failure
- `pharmacy.notification.commands` — drugstore asks the notification service to send email

## Run locally

1. Start Kafka:

```bash
docker compose up -d
```

2. Start MySQL with database `pharmacy`.

3. Start the three Spring Boot apps (separate terminals):

```bash
cd backend && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
```

4. Start the storefront:

```bash
cd frontend && npm install && npm run dev
```

Checkout still waits for the payment result over Kafka, then queues email asynchronously.
