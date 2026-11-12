# Pharmacy

Medicine Drugstore is split into seven backend apps plus the React storefront.

| App | Path | Port | Role | Communication |
|---|---|---|---|---|
| Pharmacy Service | `backend` | 8082 | BFF, cart, orders, checkout saga | HTTP + Kafka orchestrator |
| Customer service | `customer-service` | 8087 | Register, login, profile, password reset | Sync HTTP |
| Inventory service | `inventory-service` | 8088 | Catalog HTTP; stock reserve/release | HTTP + Kafka request-reply |
| Payment Kafka service | `payment-service` | 8083 | Validates UPI/card/wallet/COD then charges the gateway | Kafka request-reply |
| Gateway service | `gateway-service` | 8086 | Customer accounts, debit transactions, live balance | HTTP + WebSocket |
| Notification Kafka service | `notification-service` | 8084 | Sends order and password-reset emails | Kafka fire-and-forget |
| Shipment Kafka service | `shipment-service` | 8085 | Advances delivery and pushes live tracking | Kafka request-reply + WebSocket |
| Pharmacy UI | `frontend` | 5173 | Vite + React UI | HTTP to pharmacy-service 8082 |

## Kafka topics

Commands (partition key = `orderNumber`):

- `pharmacy.stock.commands` — reserve or release stock
- `pharmacy.payment.commands` — charge or refund (idempotency key = `transactionRef`)
- `pharmacy.shipment.commands` — pack, confirm, or cancel a delivery
- `pharmacy.notification.commands` — send customer email

Domain events (partition key = `orderNumber`):

- `pharmacy.order.events` — `ORDER_CREATED`, `ORDER_CONFIRMED`, `ORDER_DELIVERED`, `ORDER_CANCELLED`
- `pharmacy.inventory.events` — `INVENTORY_RESERVED`, `INVENTORY_FAILED`, `INVENTORY_RELEASED`
- `pharmacy.payment.events` — `PAYMENT_SUCCESS`, `PAYMENT_FAILED`
- `pharmacy.fulfilment.events` — `PACKAGE_READY`
- `pharmacy.shipment.events` — `SHIPMENT_CREATED`, `SHIPMENT_PICKED_UP`, `SHIPMENT_DELIVERED`, `SHIPMENT_FAILED`
- `pharmacy.shipment.updates` — live tracking snapshots
- `pharmacy.saga.dlt` — dead-letter topic after Kafka retries

Checkout is an **orchestrated saga** in pharmacy-service. The HTTP checkout writes `ORDER_CREATED` plus a stock command through a **transactional outbox**, then waits for payment. The workflow is:

`ORDER_CREATED` → `INVENTORY_RESERVED` → `PAYMENT_SUCCESS` → `ORDER_CONFIRMED` → `PACKAGE_READY` → `SHIPMENT_CREATED` → `SHIPMENT_PICKED_UP` (customer notified) → `SHIPMENT_DELIVERED` → order `DELIVERED`.

If payment fails: `PAYMENT_FAILED` → `RELEASE_INVENTORY` → `CANCEL_ORDER`.

Production reliability:

| Problem | Solution |
|---|---|
| Duplicate Kafka messages | Idempotency (`processed_events`, payment `transactionRef`) |
| Event lost after DB commit | Transactional outbox |
| Consumer crashes | Kafka retry with exponential backoff |
| Repeated failures | Dead letter topic `pharmacy.saga.dlt` |
| Duplicate payment request | Idempotency key `CHARGE:{transactionRef}` |
| Concurrent medicine booking | Pessimistic row lock + `@Version` on inventory |
| Payment status mismatch | Gateway reconciliation (`GET /internal/accounts/transactions/{ref}`) |
| Event ordering | Partition by `orderNumber` |

Login and profile stay **synchronous HTTP** (the UI needs the JWT immediately). Catalog listing is **synchronous HTTP**. Stock mutation is **Kafka request-reply** so the saga can wait and compensate.

Live tracking uses **WebSocket** from the browser to `shipment-service` (`/ws/shipments`).
Live wallet balance uses **WebSocket** from the browser to `gateway-service` (`/ws/accounts`).

Customer money lives in MySQL tables `customer_accounts` and `account_transactions` (owned by gateway-service). New wallets open with ₹10,000. Checkout **debits the logged-in customer** and **credits the pharmacy account** (`pharmacy@pharmacy.com`) in the same transaction. Insufficient funds fail the payment without charging.

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
cd gateway-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd shipment-service && mvn spring-boot:run
```

Start `customer-service`, `inventory-service`, and `gateway-service` before using the storefront. Profile → Account shows available balance and every CREDIT/DEBIT row.

4. Start the storefront:

```bash
cd frontend && npm install && npm run dev
```
