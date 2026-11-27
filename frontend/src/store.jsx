import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { api, asList, expireSession, getToken, getUser, getValidUser, isTokenValid, onAuthCleared, ownedByCurrentUser, sameUserId, setAuth, tokenExpiresAt } from './api'
import { isCatalogCacheFresh, readCatalogCache, writeCatalogCache } from './catalogCache'
import { clientLog } from './log'

const StoreContext = createContext(null)

function clearPrivateState(setCart, setWishlist, setOrders) {
  setCart([])
  setWishlist([])
  setOrders([])
}

export function StoreProvider({ children }) {
  const [user, setUser] = useState(getValidUser)
  const [products, setProducts] = useState(() => asList(readCatalogCache()?.products))
  const [categories, setCategories] = useState(() => asList(readCatalogCache()?.categories))
  const [cart, setCart] = useState([])
  const [wishlist, setWishlist] = useState([])
  const [orders, setOrders] = useState([])
  const [query, setQuery] = useState('')
  const [catalogError, setCatalogError] = useState('')
  const [loadingCatalog, setLoadingCatalog] = useState(() => !asList(readCatalogCache()?.products).length)
  const [profile, setProfile] = useState(null)
  const catalogLoad = useRef(null)
  const userLoad = useRef(null)
  const profileLoad = useRef(null)

  function refreshCatalog(options = {}) {
    const force = Boolean(options.force)
    if (catalogLoad.current) return catalogLoad.current
    const cached = readCatalogCache()
    if (!force && isCatalogCacheFresh(cached)) {
      setProducts(asList(cached.products))
      setCategories(asList(cached.categories))
      setCatalogError('')
      setLoadingCatalog(false)
      return Promise.resolve()
    }
    if (cached && asList(cached.products).length) {
      setProducts(asList(cached.products))
      setCategories(asList(cached.categories))
      setLoadingCatalog(false)
    }
    catalogLoad.current = (async () => {
      const showSpinner = !asList(cached?.products).length
      if (showSpinner) setLoadingCatalog(true)
      try {
        const cacheMode = force ? 'reload' : 'default'
        const [p, c] = await Promise.all([
          api('/api/products', { cache: cacheMode }),
          api('/api/categories', { cache: cacheMode }),
        ])
        const nextProducts = asList(p)
        const nextCategories = asList(c)
        setProducts(nextProducts)
        setCategories(nextCategories)
        writeCatalogCache(nextProducts, nextCategories)
        setCatalogError('')
      } finally {
        setLoadingCatalog(false)
        catalogLoad.current = null
      }
    })()
    return catalogLoad.current
  }

  function refreshUserData(expectedUserId = getUser()?.userId) {
    const uid = expectedUserId ?? getUser()?.userId
    if (uid == null || !isTokenValid()) {
      if (!isTokenValid() && (getToken() || getUser())) expireSession()
      else clearPrivateState(setCart, setWishlist, setOrders)
      return Promise.resolve()
    }
    if (userLoad.current?.uid === uid) return userLoad.current.promise
    const promise = (async () => {
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
    })()
    userLoad.current = { uid, promise }
    promise.finally(() => {
      if (userLoad.current?.promise === promise) userLoad.current = null
    })
    return promise
  }

  async function refreshCart(expectedUserId = getUser()?.userId) {
    const uid = expectedUserId ?? getUser()?.userId
    if (uid == null || !isTokenValid()) return
    const data = await api('/api/cart')
    if (!sameUserId(getUser()?.userId, uid)) return
    setCart(ownedByCurrentUser(data, uid))
  }

  async function refreshWishlist(expectedUserId = getUser()?.userId) {
    const uid = expectedUserId ?? getUser()?.userId
    if (uid == null || !isTokenValid()) return
    const data = await api('/api/wishlist')
    if (!sameUserId(getUser()?.userId, uid)) return
    setWishlist(ownedByCurrentUser(data, uid))
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

  function refreshProfile() {
    if (!isTokenValid() || !getUser()?.userId) {
      setProfile(null)
      return Promise.resolve(null)
    }
    const uid = getUser()?.userId
    if (profileLoad.current?.uid === uid) return profileLoad.current.promise
    const promise = (async () => {
      const data = await api('/api/profile')
      if (!sameUserId(getUser()?.userId, data.userId)) return null
      applyProfile(data)
      return data
    })()
    profileLoad.current = { uid, promise }
    promise.finally(() => {
      if (profileLoad.current?.promise === promise) profileLoad.current = null
    })
    return promise
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
    return undefined
  }, [user?.userId])

  useEffect(() => {
    let tries = 0
    let timer
    const load = (force = false) => {
      refreshCatalog({ force }).catch((err) => {
        setLoadingCatalog(false)
        if (asList(readCatalogCache()?.products).length) return
        setCatalogError(err.message || 'Could not load products')
        if (tries++ < 6) timer = setTimeout(() => load(true), 1500)
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
      clientLog('info', 'user.login.success', { userId: next?.userId, email: next?.email, name: next?.name, role: next?.role })
    },
    async register(payload) {
      clientLog('info', 'user.register.start', { email: payload.email, name: payload.name })
      const data = await api('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) })
      setAuth(data)
      const next = getUser()
      setUser(next)
      clientLog('info', 'user.register.success', { userId: next?.userId, email: next?.email, name: next?.name, role: next?.role })
    },
    logout() {
      const current = getUser()
      clientLog('info', 'user.logout', { userId: current?.userId, email: current?.email, name: current?.name })
      expireSession()
    },
    async addToCart(productId, quantity = 1) {
      if (!isTokenValid() || !getUser()?.userId) throw new Error('Please sign in to add items')
      clientLog('info', 'cart.add', { productId, quantity })
      const product = products.find((p) => p.id === productId)
      let snapshot
      setCart((prev) => {
        snapshot = prev
        if (!product) return prev
        const existing = prev.find((i) => i.product?.id === productId)
        if (existing) {
          return prev.map((i) => (i.product?.id === productId ? { ...i, quantity: (i.quantity || 0) + quantity } : i))
        }
        return [...prev, { id: `tmp-${productId}`, product, quantity, userId: getUser()?.userId }]
      })
      try {
        const saved = await api('/api/cart', { method: 'POST', body: JSON.stringify({ productId, quantity }) })
        setCart((prev) => {
          const rest = prev.filter((i) => i.id !== saved?.id && i.product?.id !== productId && i.id !== `tmp-${productId}`)
          const merged = saved?.product ? saved : { ...saved, product }
          return [...rest, merged]
        })
      } catch (err) {
        if (snapshot) setCart(snapshot)
        throw err
      }
    },
    async updateCart(id, quantity) {
      if (!isTokenValid() || !getUser()?.userId) throw new Error('Please sign in to update cart')
      clientLog('info', 'cart.update', { itemId: id, quantity })
      const saved = await api(`/api/cart/${id}`, { method: 'PATCH', body: JSON.stringify({ productId: 0, quantity }) })
      if (quantity < 1) {
        setCart((prev) => prev.filter((i) => i.id !== id))
      } else if (saved) {
        setCart((prev) => prev.map((i) => (i.id === id ? saved : i)))
      }
    },
    async removeCart(id) {
      if (!isTokenValid() || !getUser()?.userId) throw new Error('Please sign in to update cart')
      clientLog('info', 'cart.remove', { itemId: id })
      let snapshot
      setCart((prev) => {
        snapshot = prev
        return prev.filter((i) => i.id !== id)
      })
      try {
        await api(`/api/cart/${id}`, { method: 'DELETE' })
      } catch (err) {
        if (snapshot) setCart(snapshot)
        throw err
      }
    },
    refreshUserData,
    refreshProfile,
    applyProfile,
    async saveProfile(payload) {
      if (!isTokenValid() || !getUser()?.userId) throw new Error('Please sign in')
      clientLog('info', 'profile.save', { email: payload.email })
      const data = await api('/api/profile', { method: 'PATCH', body: JSON.stringify(payload) })
      applyProfile(data)
      clientLog('info', 'profile.saved', { userId: data.userId, email: data.email })
      return data
    },
    async toggleWish(productId) {
      if (!isTokenValid() || !getUser()?.userId) throw new Error('Please sign in to use wishlist')
      clientLog('info', 'wishlist.toggle', { productId })
      await api(`/api/wishlist/${productId}`, { method: 'POST' })
      await refreshWishlist(getUser()?.userId)
    }
  }), [user, profile, products, categories, cart, wishlist, orders, query, catalogError, loadingCatalog])

  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>
}

export function useStore() {
  return useContext(StoreContext)
}
