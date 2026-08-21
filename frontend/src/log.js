const USER_KEY = 'pharmacy_user'

function userInfo() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    if (!raw) return { userId: 'anonymous', email: '-', name: '-', role: '-' }
    const user = JSON.parse(raw)
    return {
      userId: user.userId ?? 'anonymous',
      email: user.email || '-',
      name: user.name || '-',
      role: user.role || '-',
    }
  } catch {
    return { userId: 'anonymous', email: '-', name: '-', role: '-' }
  }
}

export function newRequestId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID()
  return `fe-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function redact(value) {
  if (value == null) return value
  if (typeof value !== 'object') return value
  const hidden = ['password', 'token', 'cvv', 'cardNumber', 'cardHolder']
  const copy = Array.isArray(value) ? [...value] : { ...value }
  for (const key of Object.keys(copy)) {
    if (hidden.includes(key) && copy[key]) copy[key] = '***'
  }
  return copy
}

export function clientLog(level, event, details = {}) {
  const payload = {
    ts: new Date().toISOString(),
    event,
    ...userInfo(),
    ...details,
  }
  const line = `[pharmacy] ${event}`
  if (level === 'error') console.error(line, payload)
  else if (level === 'warn') console.warn(line, payload)
  else console.info(line, payload)
}
