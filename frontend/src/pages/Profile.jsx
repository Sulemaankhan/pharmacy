import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useStore } from '../store'
import ProfileTabs from '../components/ProfileTabs'
import { connectAccountSocket, fetchAccount, fetchPharmacyAccount } from '../account'
import { formatMoney } from '../money'

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
  const [account, setAccount] = useState(null)
  const [pharmacy, setPharmacy] = useState(null)

  useEffect(() => {
    if (!dirty) setForm(fromProfile(profile, user))
  }, [profile, user, dirty])

  useEffect(() => {
    if (!user?.userId) return undefined
    fetchAccount()
      .then(setAccount)
      .catch((e) => setError(e.message))
    fetchPharmacyAccount()
      .then(setPharmacy)
      .catch(() => {})
    return connectAccountSocket({
      userId: user.userId,
      onUpdate: (update) => {
        setAccount((prev) => ({
          ...prev,
          ...update,
          transactions: update.transactions?.length ? update.transactions : prev?.transactions,
        }))
        fetchPharmacyAccount().then(setPharmacy).catch(() => {})
      },
    })
  }, [user?.userId])

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
          <ProfileTabs />
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
            <div className="profile-stat">
              <b>{account?.availableBalance == null ? '—' : formatMoney(account.availableBalance)}</b>
              <span>Available balance</span>
            </div>
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
          <p style={{ marginTop: 14 }}>
            <Link className="link-btn" to="/lab-reports">Open lab reports</Link>
          </p>
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

      <div id="account" className="panel wide" style={{ marginTop: 18 }}>
        <h3>Your account</h3>
        <p className="muted">Live balance after each purchase. New accounts start with ₹10,000. Purchases debit this wallet and credit the pharmacy account.</p>
        <TxnTable rows={account?.transactions} />
      </div>

      <div id="pharmacy-account" className="panel wide" style={{ marginTop: 18 }}>
        <h3>Pharmacy account</h3>
        <p className="muted">
          Sales wallet {pharmacy?.email ? `· ${pharmacy.email}` : ''}.
          Balance {pharmacy?.availableBalance == null ? '—' : formatMoney(pharmacy.availableBalance)}.
        </p>
        <TxnTable rows={pharmacy?.transactions} empty="No pharmacy credits yet. Complete a purchase to credit this account." />
      </div>
    </div>
  )
}

function TxnTable({ rows, empty = 'No account transactions yet.' }) {
  if (!rows || rows.length === 0) {
    return <p className="muted" style={{ marginTop: 10 }}>{empty}</p>
  }
  return (
    <div className="summary" style={{ marginTop: 12 }}>
      {rows.map((txn) => (
        <div className="summary-row" key={txn.id || txn.transactionRef || txn.createdAt}>
          <span>
            {txn.type} {txn.orderNumber ? `· ${txn.orderNumber}` : ''}
            <div className="muted">{formatWhen(txn.createdAt)} {txn.description ? `· ${txn.description}` : ''}</div>
          </span>
          <b className={txn.type === 'DEBIT' ? 'error' : ''}>
            {txn.type === 'DEBIT' ? '-' : '+'}{formatMoney(txn.amount)}
            <div className="muted">Bal {formatMoney(txn.balanceAfter)}</div>
          </b>
        </div>
      ))}
    </div>
  )
}
