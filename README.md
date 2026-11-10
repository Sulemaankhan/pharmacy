# Pharmacy

Medicine Drugstore is split into five backend apps plus the React storefront.

| App | Path | Port | Role |
|---|---|---|---|
| Pharmacy Service | `backend` | 8082 | Catalog, cart, orders, checkout orchestration |
| Payment Kafka service | `payment-service` | 8083 | Validates UPI/card/wallet/COD then charges the gateway |
| Payment gateway | `payment-gateway` | 8086 | Customer accounts, debit transactions, live balance |
| Notification Kafka service | `notification-service` | 8084 | Sends order and password-reset emails |
| Shipment Kafka service | `shipment-service` | 8085 | Advances delivery and pushes live tracking |
| Pharmacy UI | `frontend` | 5173 | Vite + React UI |

## Kafka topics

- `pharmacy.payment.commands` — pharmacy-service asks the payment service to charge an order
- `pharmacy.payment.replies` — payment service returns success or failure
- `pharmacy.notification.commands` — pharmacy-service asks the notification service to send email
- `pharmacy.shipment.commands` — pharmacy-service asks the shipment service to reserve, confirm, or cancel a delivery
- `pharmacy.shipment.replies` — shipment service returns reserved or unavailable
- `pharmacy.shipment.updates` — shipment service publishes status changes

Checkout is a **saga orchestrator** in pharmacy-service: reserve stock → charge wallet → reserve shipment. If shipment is unavailable (service down, or demo pincode `999999` / `000000`), the orchestrator compensates in reverse: refunds the customer, reverses the pharmacy credit, restores stock, and cancels the order.

Live tracking uses **WebSocket** from the browser to `shipment-service` (`/ws/shipments`).
Live wallet balance uses **WebSocket** from the browser to `payment-gateway` (`/ws/accounts`).

Customer money lives in MySQL tables `customer_accounts` and `account_transactions` (owned by payment-gateway). New wallets open with ₹10,000. Checkout **debits the logged-in customer** and **credits the pharmacy account** (`pharmacy@pharmacy.com`) in the same transaction. Insufficient funds fail the payment without charging.

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
cd payment-gateway && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd shipment-service && mvn spring-boot:run
```

Start `payment-gateway` before checkout. Profile → Account shows available balance and every CREDIT/DEBIT row.

4. Start the storefront:

```bash
cd frontend && npm install && npm run dev
```
