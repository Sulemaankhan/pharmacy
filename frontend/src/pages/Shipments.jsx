import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useStore } from '../store'
import { fetchShipments } from '../shipments'
import ProfileTabs from '../components/ProfileTabs'

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

function statusClass(status) {
  if (status === 'DELIVERED') return 'ok'
  if (status === 'CANCELLED') return 'bad'
  return 'wait'
}

function prettyStatus(status) {
  if (!status) return '—'
  return status.replaceAll('_', ' ')
}

export default function Shipments() {
  const { user } = useStore()
  const [list, setList] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user?.userId) return undefined
    let timer
    const load = () => {
      fetchShipments()
        .then(setList)
        .catch((e) => setError(e.message))
    }
    load()
    timer = setInterval(load, 5000)
    return () => clearInterval(timer)
  }, [user?.userId])

  if (!user) {
    return (
      <div className="container page">
        <div className="empty">
          <h2>Sign in to track shipments</h2>
          <p className="muted">Address, contact number and email ID are saved with every order.</p>
          <Link className="btn" to="/auth" style={{ marginTop: 16 }}>Sign in</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="container page">
      <p className="crumb">Home / Profile / Shipments</p>
      <div className="page-head">
        <div>
          <h2 className="page-title">Shipments</h2>
          <ProfileTabs />
          <p className="muted">Live tracking for {list.length} shipment{list.length === 1 ? '' : 's'}</p>
        </div>
      </div>
      {error && <p className="error">{error}</p>}
      {list.length === 0 && !error && (
        <div className="empty">
          <p>No shipments yet.</p>
          <Link className="btn" to="/shop" style={{ marginTop: 16 }}>Start shopping</Link>
        </div>
      )}
      <div className="order-list">
        {list.map((ship) => (
          <article className="order-card" key={ship.id || ship.trackingNumber}>
            <div className="order-card-head">
              <div>
                <b>{ship.trackingNumber}</b>
                <div className="muted">{ship.orderNumber} · {formatDate(ship.createdAt)}</div>
              </div>
              <span className={`status-pill ${statusClass(ship.status)}`}>
                <span className={`live-dot ${ship.status === 'DELIVERED' || ship.status === 'CANCELLED' ? '' : 'pulse'}`} />
                {prettyStatus(ship.status)}
              </span>
            </div>
            <p style={{ marginTop: 8 }}><b>{ship.recipientName}</b></p>
            <p className="muted">{ship.fullAddress}</p>
            <p className="muted">Contact {ship.contactNumber} · {ship.email}</p>
            <div className="order-card-foot">
              <div className="muted">{ship.carrier || 'Medicine Express'}</div>
              <Link className="btn outline" to={`/shipments/${ship.trackingNumber}`}>Track live</Link>
            </div>
          </article>
        ))}
      </div>
    </div>
  )
}
