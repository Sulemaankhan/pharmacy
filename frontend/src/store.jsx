import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { api, asList, expireSession, getToken, getUser, getValidUser, isTokenValid, onAuthCleared, ownedByCurrentUser, sameUserId, setAuth, tokenExpiresAt } from './api'
import { clientLog } from './log'

const StoreContext = createContext(null)

function clearPrivateState(setCart, setWishlist, setOrders) {
  setCart([])
  setWishlist([])
  setOrders([])
}

export function StoreProvider({ children }) {
  const [user, setUser] = useState(getValidUser)
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [cart, setCart] = useState([])
  const [wishlist, setWishlist] = useState([])
  const [orders, setOrders] = useState([])
  const [query, setQuery] = useState('')
  const [catalogError, setCatalogError] = useState('')
  const [loadingCatalog, setLoadingCatalog] = useState(true)
  const [profile, setProfile] = useState(null)

  async function refreshCatalog() {
    setLoadingCatalog(true)
    const [p, c] = await Promise.all([api('/api/products'), api('/api/categories')])
    setProducts(asList(p))
    setCategories(asList(c))
    setCatalogError('')
    setLoadingCatalog(false)
  }

  async function refreshUserData(expectedUserId = getUser()?.userId) {
    const uid = expectedUserId ?? getUser()?.userId
    if (uid == null || !isTokenValid()) {
      if (!isTokenValid() && (getToken() || getUser())) expireSession()
      else clearPrivateState(setCart, setWishlist, setOrders)
      return
    }
    const [cartRes, wishRes, orderRes] = await Promise.allSettled([
      api('/api/cart'),
      api('/api/wishlist'),
      api('/api/orders'),
    ])
    if (!sameUserId(getUser()?.userId, uid)) return
    if (cartRes.status === 'fulfilled') setCart(ownedByCurrentUser(cartRes.value, uid))
    if (wishRes.status === 'fulfilled') setWishlist(ownedByCurrentUser(wishRes.value, uid))
    if (orderRes.status === 'fulfilled') setOrders(ownedByCurrentUser(orderRes.value, uid))
    clientLog('info', 'user.data.refresh', {
      userId: uid,
      cart: cartRes.status === 'fulfilled' ? ownedByCurrentUser(cartRes.value, uid).length : cartRes.reason?.message,
      wishlist: wishRes.status === 'fulfilled' ? ownedByCurrentUser(wishRes.value, uid).length : wishRes.reason?.message,
      orders: orderRes.status === 'fulfilled' ? ownedByCurrentUser(orderRes.value, uid).length : orderRes.reason?.message,
    })
    if (cartRes.status === 'rejected') throw cartRes.reason
  }

  function applyProfile(data) {
    if (!data) return
    setAuth({ ...data, token: data.token || getToken() })
    const next = getUser()
    setUser((prev) => {
      if (prev?.name === next?.name && prev?.email === next?.email && prev?.phone === next?.phone
        && prev?.address === next?.address && prev?.city === next?.city && prev?.state === next?.state
        && prev?.pincode === next?.pincode) {
        return prev
      }
      return next
    })
    setProfile((prev) => {
      if (prev && prev.orderCount === data.orderCount && prev.cartCount === data.cartCount
        && prev.wishlistCount === data.wishlistCount && prev.lastOrderNumber === data.lastOrderNumber
        && prev.name === data.name && prev.email === data.email && prev.phone === data.phone
        && prev.address === data.address && prev.updatedAt === data.updatedAt) {
        return { ...prev, refreshedAt: data.refreshedAt }
      }
      return data
    })
  }

  async function refreshProfile() {
    if (!isTokenValid() || !getUser()?.userId) {
      setProfile(null)
      return null
    }
    const data = await api('/api/profile')
    if (!sameUserId(getUser()?.userId, data.userId)) return null
    applyProfile(data)
    return data
  }

  useEffect(() => {
    const current = getValidUser()
    clientLog('info', 'app.start', current
      ? { userId: current.userId, email: current.email, name: current.name, role: current.role }
      : { userId: 'anonymous' })
  }, [])

  useEffect(() => {
    return onAuthCleared(() => {
      setUser(null)
      setProfile(null)
      clearPrivateState(setCart, setWishlist, setOrders)
    })
  }, [])

  useEffect(() => {
    if (!user) return undefined
    const exp = tokenExpiresAt()
    if (!exp) return undefined
    const delay = exp - Date.now()
    if (delay <= 0) {
      expireSession()
      return undefined
    }
    const timer = setTimeout(() => expireSession(), Math.min(delay, 2_147_483_647))
    return () => clearTimeout(timer)
  }, [user])

  useEffect(() => {
    const userId = user?.userId
    if (userId == null) {
      setProfile(null)
      clearPrivateState(setCart, setWishlist, setOrders)
      return undefined
    }
    clearPrivateState(setCart, setWishlist, setOrders)
    refreshUserData(userId).catch(() => {})
    refreshProfile().catch(() => {})
    const timer = setInterval(() => refreshProfile().catch(() => {}), 8000)
    return () => clearInterval(timer)
  }, [user?.userId])

  useEffect(() => {
    let tries = 0
    let timer
    const load = () => {
      refreshCatalog().catch((err) => {
        setCatalogError(err.message || 'Could not load products')
        setLoadingCatalog(false)
        if (tries++ < 6) timer = setTimeout(load, 1500)
      })
    }
    load()
    return () => clearTimeout(timer)
  }, [])

  const value = useMemo(() => ({
    user, profile, products, categories, cart, wishlist, orders, query, setQuery, catalogError, loadingCatalog, refreshCatalog,
    deals: products.filter((p) => p.dealOfTheDay && !p.labPanel),
    featured: products.filter((p) => p.featured && !p.labPanel),
    cartCount: cart.reduce((n, i) => n + (i?.quantity || 0), 0),
    cartTotal: cart.reduce((n, i) => n + Number(i?.product?.price || 0) * (i?.quantity || 0), 0),
    wishCount: wishlist.length,
    isWished: (id) => wishlist.some((w) => w?.product?.id === id),
    async login(payload) {
      clientLog('info', 'user.login.start', { email: payload.email })
      const data = await api('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) })
      setAuth(data)
      const next = getUser()
      setUser(next)
      await refreshUserData(next?.userId).catch(() => {})
      clientLog('info', 'user.login.success', { userId: next?.userId, email: next?.email, name: next?.name, role: next?.role })
    },
    async register(payload) {
      clientLog('info', 'user.register.start', { email: payload.email, name: payload.name })
      const data = await api('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) })
      setAuth(data)
      const next = getUser()
      setUser(next)
      await refreshUserData(next?.userId).catch(() => {})
      clientLog('info', 'user.register.success', { userId: next?.userId, email: next?.email, name: next?.name, role: next?.role })
    },
    logout() {
      const current = getUser()
      clientLog('info', 'user.logout', { userId: current?.userId, email: current?.email, name: current?.name })
      expireSession()
      refreshCatalog().catch(() => {})
    },
    async addToCart(productId, quantity = 1) {
      if (!isTokenValid() || !getUser()?.userId) throw new Error('Please sign in to add items')
      clientLog('info', 'cart.add', { productId, quantity })
      await api('/api/cart', { method: 'POST', body: JSON.stringify({ productId, quantity }) })
      await refreshUserData(getUser()?.userId)
    },
    async updateCart(id, quantity) {
      if (!isTokenValid() || !getUser()?.userId) throw new Error('Please sign in to update cart')
      clientLog('info', 'cart.update', { itemId: id, quantity })
      await api(`/api/cart/${id}`, { method: 'PATCH', body: JSON.stringify({ productId: 0, quantity }) })
      await refreshUserData(getUser()?.userId)
    },
    async removeCart(id) {
      if (!isTokenValid() || !getUser()?.userId) throw new Error('Please sign in to update cart')
      clientLog('info', 'cart.remove', { itemId: id })
      await api(`/api/cart/${id}`, { method: 'DELETE' })
      await refreshUserData(getUser()?.userId)
    },
    refreshUserData,
    refreshProfile,
    applyProfile,
    async saveProfile(payload) {
      if (!isTokenValid() || !getUser()?.userId) throw new Error('Please sign in')
      clientLog('info', 'profile.save', { email: payload.email })
      const data = await api('/api/profile', { method: 'PATCH', body: JSON.stringify(payload) })
      applyProfile(data)
      await refreshUserData(getUser()?.userId).catch(() => {})
      clientLog('info', 'profile.saved', { userId: data.userId, email: data.email })
      return data
    },
    async toggleWish(productId) {
      if (!isTokenValid() || !getUser()?.userId) throw new Error('Please sign in to use wishlist')
      clientLog('info', 'wishlist.toggle', { productId })
      await api(`/api/wishlist/${productId}`, { method: 'POST' })
      await refreshUserData(getUser()?.userId)
    }
  }), [user, profile, products, categories, cart, wishlist, orders, query, catalogError, loadingCatalog])

  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>
}

export function useStore() {
  return useContext(StoreContext)
}
