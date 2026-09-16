import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useStore } from '../store'

function formatWhen(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

function initials(name) {
  return (name || 'U').trim().split(/\s+/).slice(0, 2).map((p) => p[0]?.toUpperCase() || '').join('') || 'U'
}

function fromProfile(source, user) {
  return {
    name: source?.name || user?.name || '',
    email: source?.email || user?.email || '',
    phone: source?.phone || user?.phone || '',
    address: source?.address || user?.address || '',
    city: source?.city || user?.city || '',
    state: source?.state || user?.state || '',
    pincode: source?.pincode || user?.pincode || '',
  }
}

export default function Profile() {
  const { user, profile, cartCount, wishCount, saveProfile, refreshProfile } = useStore()
  const [form, setForm] = useState(() => fromProfile(profile, user))
  const [dirty, setDirty] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!dirty) setForm(fromProfile(profile, user))
  }, [profile, user, dirty])

  if (!user) {
    return (
      <div className="container page">
        <div className="empty">
          <h2>Sign in to view your profile</h2>
          <p className="muted">Your name, contact and live order stats stay on this page.</p>
          <Link className="btn" to="/auth" style={{ marginTop: 16 }}>Sign in</Link>
        </div>
      </div>
    )
  }

  const set = (key) => (e) => {
    setDirty(true)
    setForm((prev) => ({ ...prev, [key]: e.target.value }))
  }

  async function submit(e) {
    e.preventDefault()
    setError('')
    setMessage('')
    const phone = String(form.phone || '').replace(/\D/g, '')
    const pin = String(form.pincode || '').replace(/\D/g, '')
    if (!form.name.trim()) {
      setError('Enter your name')
      return
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      setError('Enter a valid email ID')
      return
    }
    if (phone && phone.length !== 10) {
      setError('Enter a 10-digit contact number')
      return
    }
    if (pin && pin.length !== 6) {
      setError('Enter a 6-digit pincode')
      return
    }
    setBusy(true)
    try {
      await saveProfile({
        ...form,
        phone,
        pincode: pin,
      })
      setDirty(false)
      setMessage('Profile updated. Header and stats refresh automatically.')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const live = profile || {}
  const orders = live.orderCount ?? 0
  const carts = live.cartCount ?? cartCount
  const wishes = live.wishlistCount ?? wishCount

  return (
    <div className="container page">
      <div className="page-head">
        <div>
          <p className="crumb">Account</p>
          <h2 className="page-title">Your profile</h2>
          <p className="muted">
            <span className="live-dot pulse" />
            Live · last refresh {formatWhen(live.refreshedAt)}
          </p>
        </div>
        <button type="button" className="btn outline" onClick={() => refreshProfile().catch((e) => setError(e.message))}>
          Refresh now
        </button>
      </div>

      <div className="profile-layout">
        <aside className="panel profile-card">
          <div className="profile-avatar">{initials(user.name)}</div>
          <h3>{user.name}</h3>
          <p className="muted">{user.email}</p>
          <p className="status-pill ok" style={{ marginTop: 10 }}>{user.role || 'USER'}</p>
          <p className="muted" style={{ marginTop: 12 }}>Member since {formatWhen(user.createdAt || live.createdAt)}</p>
          <div className="profile-stats">
            <Link to="/orders" className="profile-stat">
              <b>{orders}</b>
              <span>Orders</span>
            </Link>
            <Link to="/cart" className="profile-stat">
              <b>{carts}</b>
              <span>Cart</span>
            </Link>
            <Link to="/wishlist" className="profile-stat">
              <b>{wishes}</b>
              <span>Wishlist</span>
            </Link>
          </div>
          {live.lastOrderNumber && (
            <p style={{ marginTop: 14 }}>
              Last order <Link className="link-btn" to={`/orders/${live.lastOrderNumber}`}>{live.lastOrderNumber}</Link>
            </p>
          )}
        </aside>

        <form className="panel wide" onSubmit={submit}>
          <h3>Edit details</h3>
          <p className="lead">Changes save to your account and show in the header right away.</p>
          <label>Full name</label>
          <input value={form.name} onChange={set('name')} required />
          <label>Email ID</label>
          <input type="email" value={form.email} onChange={set('email')} required />
          <label>Contact number</label>
          <input value={form.phone} onChange={set('phone')} inputMode="numeric" placeholder="10-digit mobile" />
          <label>Address</label>
          <textarea rows={3} value={form.address} onChange={set('address')} />
          <div className="split-3">
            <div>
              <label>City</label>
              <input value={form.city} onChange={set('city')} />
            </div>
            <div>
              <label>State</label>
              <input value={form.state} onChange={set('state')} />
            </div>
            <div>
              <label>Pincode</label>
              <input value={form.pincode} onChange={set('pincode')} inputMode="numeric" />
            </div>
          </div>
          {error && <p className="error">{error}</p>}
          {message && <p className="toast-ok" style={{ marginTop: 10 }}>{message}</p>}
          <button className="btn" style={{ marginTop: 16 }} disabled={busy}>{busy ? 'Saving...' : 'Save profile'}</button>
        </form>
      </div>
    </div>
  )
}
