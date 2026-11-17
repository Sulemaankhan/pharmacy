import { api, asList, GATEWAY_BASE, getToken } from './api'

function fromOrders(orders) {
  return asList(orders).map((order) => order.shipment).filter(Boolean)
}

export async function fetchShipments() {
  try {
    return asList(await api('/api/shipments'))
  } catch {
    return fromOrders(await api('/api/orders'))
  }
}

export async function fetchShipmentByTracking(trackingNumber) {
  try {
    return await api(`/api/shipments/${trackingNumber}`)
  } catch {
    return (await fetchShipments()).find((item) => item.trackingNumber === trackingNumber) || null
  }
}

export async function fetchOrderShipment(orderNumber) {
  try {
    return await api(`/api/orders/${orderNumber}/shipment`)
  } catch {
    const order = await api(`/api/orders/${orderNumber}`)
    return order?.shipment || null
  }
}

function shipmentSocketUrl({ trackingNumber, userId }) {
  const token = getToken()
  const params = new URLSearchParams()
  if (token) params.set('token', token)
  if (trackingNumber) params.set('trackingNumber', trackingNumber)
  if (userId) params.set('userId', String(userId))
  const wsBase = GATEWAY_BASE.replace(/^http/, 'ws')
  return `${wsBase}/ws/shipments?${params.toString()}`
}

export function connectShipmentSocket({ trackingNumber, userId, onUpdate }) {
  if (!getToken() || typeof onUpdate !== 'function') {
    return () => {}
  }
  let closed = false
  let socket
  let retry
  const connect = () => {
    if (closed) return
    socket = new WebSocket(shipmentSocketUrl({ trackingNumber, userId }))
    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data) onUpdate(data)
      } catch {
        // ignore malformed frames
      }
    }
    socket.onclose = (event) => {
      if (!closed && event.code !== 1000) retry = window.setTimeout(connect, 4000)
    }
  }
  connect()
  return () => {
    closed = true
    window.clearTimeout(retry)
    if (socket && socket.readyState < 2) socket.close(1000, 'done')
  }
}
