import { api, asList } from './api'

function fromOrders(orders) {
  return asList(orders).map((order) => order.shipment).filter(Boolean)
}

export async function fetchShipments() {
  return fromOrders(await api('/api/orders'))
}

export async function fetchShipmentByTracking(trackingNumber) {
  return (await fetchShipments()).find((item) => item.trackingNumber === trackingNumber) || null
}

export async function fetchOrderShipment(orderNumber) {
  const order = await api(`/api/orders/${orderNumber}`)
  return order?.shipment || null
}
