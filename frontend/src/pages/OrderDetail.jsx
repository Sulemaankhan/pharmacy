import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useStore } from '../store'
import { api, downloadFile } from '../api'
import { formatMoney } from '../money'
import ShipmentTracker from '../components/ShipmentTracker'
import { fetchOrderShipment } from '../shipments'

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

export default function OrderDetail() {
  const { orderNumber } = useParams()
  const { user, addToCart } = useStore()
  const [order, setOrder] = useState(null)
  const [shipment, setShipment] = useState(null)
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')
  const [exporting, setExporting] = useState('')
  const [emailing, setEmailing] = useState(false)

  useEffect(() => {
    setOrder(null)
    setError('')
    if (!user?.userId) return
    api(`/api/orders/${orderNumber}`)
      .then((data) => {
        if (data?.userId != null && Number(data.userId) !== Number(user.userId)) {
          setError('Order not found')
          return
        }
        setOrder(data)
        setShipment(data.shipment || null)
      })
      .catch((e) => setError(e.message))
  }, [user?.userId, orderNumber])

  useEffect(() => {
    if (!user?.userId || !orderNumber) return undefined
    let stopped = false
    const loadShip = () => {
      fetchOrderShipment(orderNumber)
        .then((data) => {
          if (!data) return
          setShipment(data)
          if (data.status === 'DELIVERED' || data.status === 'CANCELLED') stopped = true
        })
        .catch(() => { stopped = true })
    }
    loadShip()
    const timer = setInterval(() => { if (!stopped) loadShip() }, 4000)
    return () => clearInterval(timer)
  }, [user?.userId, orderNumber])

  async function exportOrder(format) {
    setMsg('')
    setExporting(format)
    try {
      await downloadFile(
        `/api/orders/${orderNumber}/export?format=${format}`,
        `${orderNumber}.${format === 'excel' ? 'xlsx' : 'pdf'}`,
      )
    } catch (e) {
      setMsg(e.message)
    } finally {
      setExporting('')
    }
  }

  async function emailOrder() {
    setMsg('')
    setEmailing(true)
    try {
      const result = await api(`/api/orders/${orderNumber}/email`, { method: 'POST' })
      setMsg(result.message || `Order details emailed to ${user.email}`)
    } catch (e) {
      setMsg(e.message)
    } finally {
      setEmailing(false)
    }
  }

  async function buyAgain() {
    setMsg('')
    try {
      for (const item of order.items || []) {
        if (item.productId) await addToCart(item.productId, item.quantity)
      }
      setMsg('Items added to cart')
    } catch (e) {
      setMsg(e.message)
    }
  }

  if (!user) {
    return (
      <div className="container page">
        <div className="empty">
          <p>Please sign in to view this order.</p>
          <Link className="btn" to="/auth" style={{ marginTop: 16 }}>Sign in</Link>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="container page">
        <p className="crumb"><Link to="/orders">Orders</Link> / {orderNumber}</p>
        <p className="error">{error}</p>
      </div>
    )
  }

  if (!order) return <div className="container page"><p className="muted">Loading order...</p></div>

  const payment = order.payment || {}

  return (
    <div className="container page">
      <p className="crumb"><Link to="/orders">Orders</Link> / {order.orderNumber}</p>
      <div className="order-detail-head">
        <div>
          <h2 className="page-title">{order.orderNumber}</h2>
          <p className="muted">Placed {formatDate(order.createdAt)}</p>
        </div>
        <span className={`status-pill ${order.status === 'SUCCESS' ? 'ok' : order.status === 'FAILED' ? 'bad' : 'wait'}`}>{order.status}</span>
      </div>

      <div className="cart-layout">
        <div className="panel wide">
          {(order.items || []).map((item) => (
            <div className="cart-row" key={item.id || item.productName}>
              <img src={item.imageUrl} alt="" />
              <div>
                {item.productId ? <Link to={`/product/${item.productId}`}><b>{item.productName}</b></Link> : <b>{item.productName}</b>}
                <div className="muted">Qty {item.quantity}</div>
              </div>
              <div />
              <b>{formatMoney(Number(item.unitPrice) * item.quantity)}</b>
            </div>
          ))}
          {shipment && (
            <div style={{ marginTop: 22 }}>
              <h3 style={{ marginBottom: 12 }}>Shipment</h3>
              <ShipmentTracker shipment={shipment} />
              {shipment.trackingNumber && (
                <Link className="link-btn" to={`/shipments/${shipment.trackingNumber}`} style={{ display: 'inline-block', marginTop: 12 }}>
                  Open live tracking
                </Link>
              )}
            </div>
          )}
        </div>
        <aside className="summary">
          <h3>Payment</h3>
          <div className="summary-row"><span>Mode</span><span>{payment.paymentMode || '—'}</span></div>
          <div className="summary-row"><span>Txn ref</span><span>{payment.transactionRef || '—'}</span></div>
          <div className="summary-row"><span>Ledger</span><span>{payment.ledgerPosted ? 'Posted' : 'Not posted'}</span></div>
          <div className="summary-row"><span>Subtotal</span><span>{formatMoney(order.subtotal)}</span></div>
          <div className="summary-row"><span>Shipping</span><span>{Number(order.shipping) === 0 ? 'Free' : formatMoney(order.shipping)}</span></div>
          <div className="summary-row total"><span>Total</span><span>{formatMoney(order.total)}</span></div>
          <div className="export-actions stacked" style={{ marginTop: 16 }}>
            <button className="btn outline full" disabled={!!exporting} onClick={() => exportOrder('pdf')}>
              {exporting === 'pdf' ? 'Preparing PDF...' : 'Download PDF'}
            </button>
            <button className="btn outline full" disabled={!!exporting} onClick={() => exportOrder('excel')}>
              {exporting === 'excel' ? 'Preparing Excel...' : 'Download Excel'}
            </button>
            <button className="btn full" disabled={emailing} onClick={emailOrder}>
              {emailing ? 'Sending email...' : `Email details to ${user.email}`}
            </button>
          </div>
          {order.status === 'SUCCESS' && (
            <button className="btn outline full" style={{ marginTop: 12 }} onClick={buyAgain}>Buy again</button>
          )}
          {msg && <p className={msg.toLowerCase().includes('email') || msg.includes('cart') ? 'toast-ok' : 'error'} style={{ marginTop: 10 }}>{msg}</p>}
          <Link className="link-btn" to="/orders" style={{ display: 'block', marginTop: 12, textAlign: 'center' }}>Back to orders</Link>
        </aside>
      </div>
    </div>
  )
}
