import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useStore } from '../store'
import { downloadFile, api } from '../api'
import { formatMoney } from '../money'

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

function statusClass(status) {
  if (status === 'SUCCESS') return 'ok'
  if (status === 'FAILED') return 'bad'
  return 'wait'
}

export default function Orders() {
  const { user, orders } = useStore()
  const [filter, setFilter] = useState('ALL')
  const [error, setError] = useState('')
  const [exporting, setExporting] = useState('')
  const [emailing, setEmailing] = useState('')
  const [emailMsg, setEmailMsg] = useState('')

  const mine = useMemo(
    () => orders.filter((o) => o.userId == null || Number(o.userId) === Number(user?.userId)),
    [orders, user?.userId]
  )

  const list = useMemo(() => {
    if (filter === 'ALL') return mine
    return mine.filter((o) => o.status === filter)
  }, [mine, filter])

  async function emailOrder(orderNumber) {
    setError('')
    setEmailMsg('')
    setEmailing(orderNumber)
    try {
      const result = await api(`/api/orders/${orderNumber}/email`, { method: 'POST' })
      setEmailMsg(result.message || `Order details emailed to ${user.email}`)
    } catch (e) {
      setError(e.message)
    } finally {
      setEmailing('')
    }
  }

  async function exportHistory(format) {
    setError('')
    setExporting(format)
    try {
      const query = new URLSearchParams({ format, status: filter }).toString()
      await downloadFile(`/api/orders/export?${query}`, `order-history.${format === 'excel' ? 'xlsx' : 'pdf'}`)
    } catch (e) {
      setError(e.message)
    } finally {
      setExporting('')
    }
  }

  if (!user) {
    return (
      <div className="container page">
        <div className="empty">
          <h2>Sign in to see orders</h2>
          <p className="muted">Your purchase history is saved to your account.</p>
          <Link className="btn" to="/auth" style={{ marginTop: 16 }}>Sign in</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="container page">
      <p className="crumb">Home / Orders</p>
      <div className="page-head">
        <div>
          <h2 className="page-title">Order history</h2>
          <p className="muted">{mine.length} order{mine.length === 1 ? '' : 's'} placed</p>
        </div>
        <div className="export-actions">
          <button className="btn outline" disabled={!!exporting} onClick={() => exportHistory('pdf')}>
            {exporting === 'pdf' ? 'Preparing PDF...' : 'Download PDF'}
          </button>
          <button className="btn outline" disabled={!!exporting} onClick={() => exportHistory('excel')}>
            {exporting === 'excel' ? 'Preparing Excel...' : 'Download Excel'}
          </button>
        </div>
      </div>

      <div className="order-filters">
        {['ALL', 'SUCCESS', 'PENDING', 'FAILED'].map((key) => (
          <button key={key} className={filter === key ? 'active' : ''} onClick={() => setFilter(key)}>
            {key === 'ALL' ? 'All' : key.charAt(0) + key.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {error && <p className="error">{error}</p>}
      {emailMsg && <p className="toast-ok" style={{ marginBottom: 12 }}>{emailMsg}</p>}
      {list.length === 0 && (
        <div className="empty">
          <p>No orders in this view yet.</p>
          <Link className="btn" to="/shop" style={{ marginTop: 16 }}>Start shopping</Link>
        </div>
      )}

      <div className="order-list">
        {list.map((order) => (
          <article className="order-card" key={order.id || order.orderNumber}>
            <div className="order-card-head">
              <div>
                <b>{order.orderNumber}</b>
                <div className="muted">{formatDate(order.createdAt)}</div>
              </div>
              <span className={`status-pill ${statusClass(order.status)}`}>{order.status}</span>
            </div>
            <div className="order-thumbs">
              {(order.items || []).slice(0, 4).map((item) => (
                <img key={item.id || item.productName} src={item.imageUrl} alt={item.productName} />
              ))}
              <span className="muted">{(order.items || []).length} item{(order.items || []).length === 1 ? '' : 's'}</span>
            </div>
            <div className="order-card-foot">
              <div>
                <div className="muted">{order.payment?.paymentMode || '—'}</div>
                <strong>{formatMoney(order.total)}</strong>
              </div>
              <div className="export-actions">
                <button
                  className="btn outline"
                  disabled={!!emailing}
                  onClick={() => emailOrder(order.orderNumber)}
                >
                  {emailing === order.orderNumber ? 'Sending...' : 'Email'}
                </button>
                {order.shipment?.trackingNumber && (
                  <Link className="btn outline" to={`/shipments/${order.shipment.trackingNumber}`}>Track</Link>
                )}
                <Link className="btn outline" to={`/orders/${order.orderNumber}`}>View details</Link>
              </div>
            </div>
          </article>
        ))}
      </div>
    </div>
  )
}
