import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useStore } from '../store'
import { api, asList, downloadFile } from '../api'
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
  const { user } = useStore()
  const [orders, setOrders] = useState([])
  const [filter, setFilter] = useState('ALL')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [exporting, setExporting] = useState('')

  useEffect(() => {
    if (!user) return
    setLoading(true)
    api('/api/orders')
      .then((data) => setOrders(asList(data)))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [user])

  const list = useMemo(() => {
    if (filter === 'ALL') return orders
    return orders.filter((o) => o.status === filter)
  }, [orders, filter])

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
          <p className="muted">{orders.length} order{orders.length === 1 ? '' : 's'} placed</p>
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
      {loading && <p className="muted">Loading orders...</p>}
      {!loading && list.length === 0 && (
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
              <Link className="btn outline" to={`/orders/${order.orderNumber}`}>View details</Link>
            </div>
          </article>
        ))}
      </div>
    </div>
  )
}
