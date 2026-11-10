# Pharmacy

Medicine Drugstore is split into seven backend apps plus the React storefront.

| App | Path | Port | Role | Communication |
|---|---|---|---|---|
| Pharmacy Service | `backend` | 8082 | BFF, cart, orders, checkout saga | HTTP + Kafka orchestrator |
| Customer service | `customer-service` | 8087 | Register, login, profile, password reset | Sync HTTP |
| Inventory service | `inventory-service` | 8088 | Catalog HTTP; stock reserve/release | HTTP + Kafka request-reply |
| Payment Kafka service | `payment-service` | 8083 | Validates UPI/card/wallet/COD then charges the gateway | Kafka request-reply |
| Payment gateway | `payment-gateway` | 8086 | Customer accounts, debit transactions, live balance | HTTP + WebSocket |
| Notification Kafka service | `notification-service` | 8084 | Sends order and password-reset emails | Kafka fire-and-forget |
| Shipment Kafka service | `shipment-service` | 8085 | Advances delivery and pushes live tracking | Kafka request-reply + WebSocket |
| Pharmacy UI | `frontend` | 5173 | Vite + React UI | HTTP to pharmacy-service 8082 |

## Kafka topics

- `pharmacy.stock.commands` — pharmacy-service asks inventory to reserve or release stock
- `pharmacy.stock.replies` — inventory returns reserved or insufficient
- `pharmacy.payment.commands` — pharmacy-service asks the payment service to charge an order
- `pharmacy.payment.replies` — payment service returns success or failure
- `pharmacy.notification.commands` — pharmacy-service or customer-service asks the notification service to send email
- `pharmacy.shipment.commands` — pharmacy-service asks the shipment service to reserve, confirm, or cancel a delivery
- `pharmacy.shipment.replies` — shipment service returns reserved or unavailable
- `pharmacy.shipment.updates` — shipment service publishes status changes

Checkout is a **saga orchestrator** in pharmacy-service: Kafka-reserve stock → charge wallet → Kafka-reserve shipment. If any step fails, the orchestrator compensates in reverse: refunds the customer, reverses the pharmacy credit, Kafka-releases stock, and cancels the order.

Login and profile stay **synchronous HTTP** (the UI needs the JWT immediately). Catalog listing is **synchronous HTTP**. Stock mutation is **Kafka request-reply** so the saga can wait and compensate.

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
cd customer-service && mvn spring-boot:run
cd inventory-service && mvn spring-boot:run
cd backend && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
cd payment-gateway && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd shipment-service && mvn spring-boot:run
```

Start `customer-service`, `inventory-service`, and `payment-gateway` before using the storefront. Profile → Account shows available balance and every CREDIT/DEBIT row.

4. Start the storefront:

```bash
cd frontend && npm install && npm run dev
```
