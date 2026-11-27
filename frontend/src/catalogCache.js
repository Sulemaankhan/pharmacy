const KEY = 'pharmacy_catalog_v1'
export const CATALOG_TTL_MS = 60_000

export function readCatalogCache() {
  try {
    const raw = sessionStorage.getItem(KEY)
    if (!raw) return null
    const data = JSON.parse(raw)
    if (!Array.isArray(data?.products) || !Array.isArray(data?.categories) || typeof data.fetchedAt !== 'number') {
      return null
    }
    return data
  } catch {
    return null
  }
}

export function writeCatalogCache(products, categories) {
  try {
    sessionStorage.setItem(KEY, JSON.stringify({
      products,
      categories,
      fetchedAt: Date.now(),
    }))
  } catch {
    // Ignore quota or private-mode failures.
  }
}

export function isCatalogCacheFresh(cache, now = Date.now()) {
  return Boolean(cache && now - cache.fetchedAt < CATALOG_TTL_MS)
}
