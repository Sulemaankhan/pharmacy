import { clientLog, newRequestId, redact } from './log'

const TOKEN = 'pharmacy_token'
const USER = 'pharmacy_user'
const API_BASES = ['', 'http://localhost:8082']

const authClearedListeners = new Set()

export function getToken() {
  return localStorage.getItem(TOKEN)
}

export function getUser() {
  const raw = localStorage.getItem(USER)
  return raw ? JSON.parse(raw) : null
}

export function setAuth(data) {
  if (data.token) localStorage.setItem(TOKEN, data.token)
  const prev = getUser() || {}
  const next = {
    userId: data.userId ?? data.id ?? prev.userId,
    name: data.name ?? prev.name,
    email: data.email ?? prev.email,
    role: data.role ?? prev.role,
    phone: data.phone ?? prev.phone ?? '',
    address: data.address ?? prev.address ?? '',
    city: data.city ?? prev.city ?? '',
    state: data.state ?? prev.state ?? '',
    pincode: data.pincode ?? prev.pincode ?? '',
    createdAt: data.createdAt ?? prev.createdAt,
    updatedAt: data.updatedAt ?? prev.updatedAt,
  }
  localStorage.setItem(USER, JSON.stringify(next))
  clientLog('info', 'auth.set', {
    userId: next.userId,
    email: next.email,
    name: next.name,
    role: next.role,
  })
}

export function clearAuth() {
  localStorage.removeItem(TOKEN)
  localStorage.removeItem(USER)
}

function decodeJwtPayload(token) {
  try {
    let part = token.split('.')[1]
    if (!part) return null
    part = part.replace(/-/g, '+').replace(/_/g, '/')
    const pad = part.length % 4
    if (pad) part += '='.repeat(4 - pad)
    return JSON.parse(atob(part))
  } catch {
    return null
  }
}

export function tokenExpiresAt(token = getToken()) {
  if (!token) return 0
  const payload = decodeJwtPayload(token)
  return payload?.exp ? payload.exp * 1000 : 0
}

export function isTokenValid(token = getToken()) {
  if (!token) return false
  const payload = decodeJwtPayload(token)
  if (!payload) return false
  if (!payload.exp) return true
  return payload.exp * 1000 > Date.now() + 1000
}

export function sameUserId(a, b) {
  if (a == null || b == null || a === '' || b === '') return false
  return Number(a) === Number(b)
}

export function ownedByCurrentUser(data, userId = getUser()?.userId) {
  if (userId == null || userId === '') return []
  return asList(data).filter((item) => item?.userId == null || sameUserId(item.userId, userId))
}

export function onAuthCleared(listener) {
  authClearedListeners.add(listener)
  return () => authClearedListeners.delete(listener)
}

export function expireSession() {
  if (!getToken() && !getUser()) return
  const current = getUser()
  clientLog('warn', 'auth.expired', {
    userId: current?.userId,
    email: current?.email,
    name: current?.name,
  })
  clearAuth()
  authClearedListeners.forEach((fn) => fn())
}

export function getValidUser() {
  if (!isTokenValid()) {
    clearAuth()
    return null
  }
  return getUser()
}

function isAuthRequest(path) {
  return path.startsWith('/api/auth')
}

export function asList(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.content)) return data.content
  if (Array.isArray(data?.value)) return data.value
  return []
}

async function readMessage(res, fallback) {
  const text = await res.text()
  if (!text) return fallback
  try {
    const body = JSON.parse(text)
    return body.message || body.error || fallback
  } catch {
    return fallback
  }
}

export async function api(path, options = {}) {
  const requestId = newRequestId()
  const method = (options.method || 'GET').toUpperCase()
  const headers = { ...(options.headers || {}), 'X-Request-Id': requestId }
  if (options.body) headers['Content-Type'] = 'application/json'
  let token = getToken()
  if (token && !isTokenValid(token)) {
    expireSession()
    token = null
  }
  if (token) headers.Authorization = `Bearer ${token}`
  let parsedBody
  try { parsedBody = options.body ? JSON.parse(options.body) : undefined } catch { parsedBody = undefined }
  clientLog('info', 'api.request', { requestId, method, path, body: redact(parsedBody) })

  let lastError
  let unauthorized = false
  for (const base of API_BASES) {
    try {
      const res = await fetch(`${base}${path}`, { ...options, headers })
      const backendRequestId = res.headers.get('X-Request-Id') || requestId
      clientLog(res.ok ? 'info' : 'warn', 'api.response', {
        requestId: backendRequestId,
        method,
        path,
        base: base || 'proxy',
        status: res.status,
      })
      if (!res.ok) {
        if (res.status === 401) unauthorized = true
        lastError = new Error(await readMessage(res, res.status === 401
          ? 'Session expired. Please sign in again.'
          : `Request failed (${res.status})`))
        continue
      }
      if (res.status === 204) return null
      const text = await res.text()
      if (!text) return null
      return JSON.parse(text)
    } catch (err) {
      lastError = err
      clientLog('warn', 'api.network', { requestId, method, path, base: base || 'proxy', error: err.message })
    }
  }
  if (unauthorized && token && !isAuthRequest(path)) expireSession()
  clientLog('error', 'api.failed', { requestId, method, path, error: lastError?.message })
  throw lastError || new Error('Request failed')
}

export async function apiUpload(path, file) {
  const requestId = newRequestId()
  const headers = { 'X-Request-Id': requestId }
  let token = getToken()
  if (token && !isTokenValid(token)) {
    expireSession()
    token = null
  }
  if (token) headers.Authorization = `Bearer ${token}`
  const body = new FormData()
  body.append('file', file)
  clientLog('info', 'api.upload', { requestId, path, file: file?.name, size: file?.size })

  let lastError
  let unauthorized = false
  for (const base of API_BASES) {
    try {
      const res = await fetch(`${base}${path}`, { method: 'POST', headers, body })
      const backendRequestId = res.headers.get('X-Request-Id') || requestId
      clientLog(res.ok ? 'info' : 'warn', 'api.response', {
        requestId: backendRequestId,
        method: 'POST',
        path,
        base: base || 'proxy',
        status: res.status,
      })
      if (!res.ok) {
        if (res.status === 401) unauthorized = true
        lastError = new Error(await readMessage(res, res.status === 401
          ? 'Session expired. Please sign in again.'
          : `Upload failed (${res.status})`))
        continue
      }
      const text = await res.text()
      return text ? JSON.parse(text) : null
    } catch (err) {
      lastError = err
      clientLog('warn', 'api.network', { requestId, method: 'POST', path, base: base || 'proxy', error: err.message })
    }
  }
  if (unauthorized && token) expireSession()
  clientLog('error', 'api.failed', { requestId, method: 'POST', path, error: lastError?.message })
  throw lastError || new Error('Upload failed')
}

function fileNameFromDisposition(disposition, fallback) {
  if (!disposition) return fallback
  const utf = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf) return decodeURIComponent(utf[1])
  const ascii = disposition.match(/filename="?([^";]+)"?/i)
  return ascii ? ascii[1] : fallback
}

export async function downloadFile(path, fallbackName) {
  const requestId = newRequestId()
  const headers = {
    Accept: 'application/pdf, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/json',
    'X-Request-Id': requestId,
  }
  let token = getToken()
  if (token && !isTokenValid(token)) {
    expireSession()
    token = null
  }
  if (token) headers.Authorization = `Bearer ${token}`
  clientLog('info', 'download.request', { requestId, path })

  let lastError
  let unauthorized = false
  for (const base of API_BASES) {
    try {
      const res = await fetch(`${base}${path}`, { headers })
      const contentType = (res.headers.get('Content-Type') || '').toLowerCase()
      if (!res.ok) {
        if (res.status === 401) unauthorized = true
        lastError = new Error(await readMessage(res, res.status === 401
          ? 'Session expired. Please sign in again.'
          : `Download failed (${res.status})`))
        continue
      }
      if (contentType.includes('application/json')) {
        lastError = new Error(await readMessage(res, 'Download failed'))
        continue
      }
      const blob = await res.blob()
      const filename = fileNameFromDisposition(res.headers.get('Content-Disposition'), fallbackName || 'order-history')
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
      clientLog('info', 'download.success', { requestId, path, filename })
      return
    } catch (err) {
      lastError = err
    }
  }
  if (unauthorized && token && !isAuthRequest(path)) expireSession()
  clientLog('error', 'download.failed', { requestId, path, error: lastError?.message })
  throw lastError || new Error('Download failed')
}
