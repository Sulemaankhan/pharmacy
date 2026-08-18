import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { api, asList, clearAuth, getToken, getUser, setAuth } from './api'

const StoreContext = createContext(null)

export function StoreProvider({ children }) {
  const [user, setUser] = useState(getUser())
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [cart, setCart] = useState([])
  const [wishlist, setWishlist] = useState([])
  const [query, setQuery] = useState('')
  const [catalogError, setCatalogError] = useState('')
  const [loadingCatalog, setLoadingCatalog] = useState(true)

  async function refreshCatalog() {
    setLoadingCatalog(true)
    const [p, c] = await Promise.all([api('/api/products'), api('/api/categories')])
    setProducts(asList(p))
    setCategories(asList(c))
    setCatalogError('')
    setLoadingCatalog(false)
  }

  async function refreshUserData() {
    if (!getToken()) {
      setCart([])
      setWishlist([])
      return
    }
    const [c, w] = await Promise.all([api('/api/cart'), api('/api/wishlist')])
    setCart(asList(c))
    setWishlist(asList(w))
  }

  useEffect(() => {
    let tries = 0
    let timer
    const load = () => {
      refreshCatalog()
        .then(() => refreshUserData().catch(() => {}))
        .catch((err) => {
          setCatalogError(err.message || 'Could not load products')
          setLoadingCatalog(false)
          if (tries++ < 6) timer = setTimeout(load, 1500)
        })
    }
    load()
    return () => clearTimeout(timer)
  }, [])

  const value = useMemo(() => ({
    user, products, categories, cart, wishlist, query, setQuery, catalogError, loadingCatalog, refreshCatalog,
    deals: products.filter((p) => p.dealOfTheDay),
    featured: products.filter((p) => p.featured),
    cartCount: cart.reduce((n, i) => n + (i?.quantity || 0), 0),
    cartTotal: cart.reduce((n, i) => n + Number(i?.product?.price || 0) * (i?.quantity || 0), 0),
    wishCount: wishlist.length,
    isWished: (id) => wishlist.some((w) => w?.product?.id === id),
    async login(payload) {
      const data = await api('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) })
      setAuth(data)
      setUser(getUser())
      await Promise.all([refreshCatalog().catch(() => {}), refreshUserData()])
    },
    async register(payload) {
      const data = await api('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) })
      setAuth(data)
      setUser(getUser())
      await Promise.all([refreshCatalog().catch(() => {}), refreshUserData()])
    },
    logout() {
      clearAuth()
      setUser(null)
      setCart([])
      setWishlist([])
      refreshCatalog().catch(() => {})
    },
    async addToCart(productId, quantity = 1) {
      if (!getToken()) throw new Error('Please sign in to add items')
      await api('/api/cart', { method: 'POST', body: JSON.stringify({ productId, quantity }) })
      await refreshUserData()
    },
    async updateCart(id, quantity) {
      await api(`/api/cart/${id}`, { method: 'PATCH', body: JSON.stringify({ productId: 0, quantity }) })
      await refreshUserData()
    },
    async removeCart(id) {
      await api(`/api/cart/${id}`, { method: 'DELETE' })
      await refreshUserData()
    },
    refreshUserData,
    async toggleWish(productId) {
      if (!getToken()) throw new Error('Please sign in to use wishlist')
      await api(`/api/wishlist/${productId}`, { method: 'POST' })
      await refreshUserData()
    }
  }), [user, products, categories, cart, wishlist, query, catalogError, loadingCatalog])

  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>
}

export function useStore() {
  return useContext(StoreContext)
}
