import { api, getToken } from './api'

export async function fetchAccount() {
  return api('/api/account')
}

export async function fetchPharmacyAccount() {
  return api('/api/account/pharmacy')
}

function accountSocketUrl(userId) {
  const token = getToken()
  const params = new URLSearchParams()
  if (token) params.set('token', token)
  if (userId) params.set('userId', String(userId))
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${window.location.host}/ws/accounts?${params.toString()}`
}

export function connectAccountSocket({ userId, onUpdate }) {
  if (!getToken() || typeof onUpdate !== 'function') {
    return () => {}
  }
  let closed = false
  let socket
  let retry
  const connect = () => {
    if (closed) return
    socket = new WebSocket(accountSocketUrl(userId))
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
