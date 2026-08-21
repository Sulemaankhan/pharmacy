import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useStore } from '../store'
import { formatMoney } from '../money'
import { api } from '../api'
import { clientLog } from '../log'

const MODES = [
  { code: 'UPI', label: 'UPI', hint: 'GPay / PhonePe / Paytm' },
  { code: 'CARD', label: 'Card', hint: 'Visa, Mastercard, RuPay' },
  { code: 'NET_BANKING', label: 'Net banking', hint: 'SBI, HDFC, ICICI' },
  { code: 'WALLET', label: 'Wallet', hint: 'Paytm, PhonePe' },
  { code: 'COD', label: 'Cash on delivery', hint: 'Pay when delivered' }
]

export default function Checkout() {
  const { cart, cartTotal, user, refreshUserData } = useStore()
  const shipping = cartTotal >= 499 || cart.length === 0 ? 0 : 49
  const grand = cartTotal + shipping
  const navigate = useNavigate()
  const [mode, setMode] = useState('UPI')
  const [form, setForm] = useState({
    upiId: '',
    cardNumber: '',
    cardHolder: '',
    expiry: '',
    cvv: '',
    bankName: 'HDFC',
    walletProvider: 'PHONEPE',
    walletPhone: ''
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  const canPay = useMemo(() => Boolean(user && cart.length), [user, cart.length])

  async function pay(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      clientLog('info', 'checkout.start', { paymentMode: mode, cartItems: cart.length, total: grand })
      const result = await api('/api/payments/checkout', {
        method: 'POST',
        body: JSON.stringify({ paymentMode: mode, ...form })
      })
      await refreshUserData()
      if (result.success) {
        clientLog('info', 'checkout.success', {
          orderNumber: result.orderNumber,
          transactionRef: result.transactionRef,
          status: result.status,
        })
        navigate(`/order/${result.orderNumber}`, { state: result })
      } else {
        clientLog('warn', 'checkout.failed', { message: result.message, orderNumber: result.orderNumber })
        setError(result.message || 'Payment failed')
      }
    } catch (err) {
      clientLog('error', 'checkout.error', { error: err.message, paymentMode: mode })
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (!user) {
    return (
      <div className="container page">
        <div className="empty">
          <h2>Sign in to checkout</h2>
          <Link className="btn" to="/auth" style={{ marginTop: 16 }}>Sign in</Link>
        </div>
      </div>
    )
  }

  if (!cart.length) {
    return (
      <div className="container page">
        <div className="empty">
          <p>Your cart is empty.</p>
          <Link className="btn" to="/shop" style={{ marginTop: 16 }}>Continue shopping</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="container page">
      <p className="crumb">Home / Cart / Checkout</p>
      <h2 className="page-title">Checkout</h2>
      <form className="cart-layout" onSubmit={pay}>
        <div className="panel wide">
          <h3>Payment mode</h3>
          <div className="pay-modes">
            {MODES.map((m) => (
              <button type="button" key={m.code} className={`pay-mode ${mode === m.code ? 'active' : ''}`} onClick={() => setMode(m.code)}>
                <b>{m.label}</b>
                <small>{m.hint}</small>
              </button>
            ))}
          </div>

          {mode === 'UPI' && (
            <>
              <label>UPI ID</label>
              <input value={form.upiId} onChange={set('upiId')} placeholder="name@okaxis" required />
            </>
          )}
          {mode === 'CARD' && (
            <>
              <label>Card holder</label>
              <input value={form.cardHolder} onChange={set('cardHolder')} required />
              <label>Card number</label>
              <input value={form.cardNumber} onChange={set('cardNumber')} placeholder="4111111111111111" maxLength="16" required />
              <div className="split-2">
                <div>
                  <label>Expiry (MM/YY)</label>
                  <input value={form.expiry} onChange={set('expiry')} placeholder="12/28" required />
                </div>
                <div>
                  <label>CVV</label>
                  <input value={form.cvv} onChange={set('cvv')} type="password" maxLength="3" required />
                </div>
              </div>
            </>
          )}
          {mode === 'NET_BANKING' && (
            <>
              <label>Bank</label>
              <select value={form.bankName} onChange={set('bankName')}>
                {['SBI', 'HDFC', 'ICICI', 'AXIS', 'PNB', 'KOTAK'].map((b) => <option key={b}>{b}</option>)}
              </select>
            </>
          )}
          {mode === 'WALLET' && (
            <>
              <label>Wallet</label>
              <select value={form.walletProvider} onChange={set('walletProvider')}>
                <option value="PHONEPE">PhonePe</option>
                <option value="PAYTM">Paytm</option>
                <option value="AMAZONPAY">Amazon Pay</option>
                <option value="MOBIKWIK">MobiKwik</option>
              </select>
              <label>Mobile number</label>
              <input value={form.walletPhone} onChange={set('walletPhone')} placeholder="10-digit number" required />
            </>
          )}
          {mode === 'COD' && (
            <p className="demo-note">Pay in cash when your order arrives. Available for orders up to ₹5,000.</p>
          )}
          {error && <p className="error">{error}</p>}
        </div>

        <aside className="summary">
          <h3>Pay {formatMoney(grand)}</h3>
          {cart.map((item) => (
            <div className="summary-row" key={item.id}>
              <span>{item.product.name} × {item.quantity}</span>
              <span>{formatMoney(Number(item.product.price) * item.quantity)}</span>
            </div>
          ))}
          <div className="summary-row"><span>Shipping</span><span>{shipping === 0 ? 'Free' : formatMoney(shipping)}</span></div>
          <div className="summary-row total"><span>Total</span><span>{formatMoney(grand)}</span></div>
          <button className="btn full" disabled={busy || !canPay} style={{ marginTop: 16 }}>
            {busy ? 'Processing…' : `Pay with ${MODES.find((m) => m.code === mode).label}`}
          </button>
          <Link to="/cart" className="link-btn" style={{ display: 'block', marginTop: 12, textAlign: 'center' }}>Back to cart</Link>
        </aside>
      </form>
    </div>
  )
}
