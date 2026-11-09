# Pharmacy

Medicine Drugstore is split into four backend apps plus the React storefront.

| App | Path | Port | Role |
|---|---|---|---|
| Pharmacy Service | `backend` | 8082 | Catalog, cart, orders, checkout orchestration |
| Payment Kafka service | `payment-service` | 8083 | Charges the selected payment mode |
| Notification Kafka service | `notification-service` | 8084 | Sends order and password-reset emails |
| Shipment Kafka service | `shipment-service` | 8085 | Advances delivery and pushes live tracking |
| Pharmacy UI | `frontend` | 5173 | Vite + React UI |

## Kafka topics

- `pharmacy.payment.commands` — pharmacy-service asks the payment service to charge an order
- `pharmacy.payment.replies` — payment service returns success or failure
- `pharmacy.notification.commands` — pharmacy-service asks the notification service to send email
- `pharmacy.shipment.commands` — pharmacy-service asks the shipment service to start or confirm a delivery
- `pharmacy.shipment.updates` — shipment service publishes status changes

Live tracking uses **WebSocket** from the browser to `shipment-service` (`/ws/shipments`).

## Run locally

1. Start Kafka:

```bash
docker compose up -d
```

2. Start MySQL with database `pharmacy`.

3. Start the Spring Boot apps (separate terminals):

```bash
cd backend && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd shipment-service && mvn spring-boot:run
```

4. Start the storefront:

```bash
cd frontend && npm install && npm run dev
```
