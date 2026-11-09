import { api, asList, getToken } from './api'

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
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${window.location.host}/ws/shipments?${params.toString()}`
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
    socket.onclose = () => {
      if (!closed) retry = window.setTimeout(connect, 4000)
    }
    socket.onerror = () => {
      try { socket.close() } catch { /* already closed */ }
    }
  }
  connect()
  return () => {
    closed = true
    window.clearTimeout(retry)
    if (socket && socket.readyState < 2) socket.close()
  }
}
