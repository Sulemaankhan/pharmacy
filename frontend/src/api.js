const TOKEN = 'pharmacy_token'
const USER = 'pharmacy_user'
const API_BASES = ['', 'http://localhost:8082']

export function getToken() {
  return localStorage.getItem(TOKEN)
}

export function getUser() {
  const raw = localStorage.getItem(USER)
  return raw ? JSON.parse(raw) : null
}

export function setAuth(data) {
  localStorage.setItem(TOKEN, data.token)
  localStorage.setItem(USER, JSON.stringify({ userId: data.userId, name: data.name, email: data.email, role: data.role }))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN)
  localStorage.removeItem(USER)
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
  const headers = { ...(options.headers || {}) }
  if (options.body) headers['Content-Type'] = 'application/json'
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  let lastError
  for (const base of API_BASES) {
    try {
      const res = await fetch(`${base}${path}`, { ...options, headers })
      if (!res.ok) {
        lastError = new Error(await readMessage(res, `Request failed (${res.status})`))
        continue
      }
      if (res.status === 204) return null
      const text = await res.text()
      if (!text) return null
      return JSON.parse(text)
    } catch (err) {
      lastError = err
    }
  }
  throw lastError || new Error('Request failed')
}

function fileNameFromDisposition(disposition, fallback) {
  if (!disposition) return fallback
  const utf = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf) return decodeURIComponent(utf[1])
  const ascii = disposition.match(/filename="?([^";]+)"?/i)
  return ascii ? ascii[1] : fallback
}

export async function downloadFile(path, fallbackName) {
  const headers = {
    Accept: 'application/pdf, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/json',
  }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  let lastError
  for (const base of API_BASES) {
    try {
      const res = await fetch(`${base}${path}`, { headers })
      const contentType = (res.headers.get('Content-Type') || '').toLowerCase()
      if (!res.ok) {
        lastError = new Error(await readMessage(res, `Download failed (${res.status})`))
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
      return
    } catch (err) {
      lastError = err
    }
  }
  throw lastError || new Error('Download failed')
}
