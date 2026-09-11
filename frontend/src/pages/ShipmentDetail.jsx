import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useStore } from '../store'
import ShipmentTracker from '../components/ShipmentTracker'
import { fetchShipmentByTracking } from '../shipments'

export default function ShipmentDetail() {
  const { trackingNumber } = useParams()
  const { user } = useStore()
  const [shipment, setShipment] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user?.userId || !trackingNumber) return undefined
    let stopped = false
    const load = () => {
      fetchShipmentByTracking(trackingNumber)
        .then((data) => {
          if (!data) {
            setError('Shipment not found')
            stopped = true
            return
          }
          setError('')
          setShipment(data)
          if (data.status === 'DELIVERED' || data.status === 'CANCELLED') stopped = true
        })
        .catch((e) => {
          setError(e.message)
          stopped = true
        })
    }
    load()
    const timer = setInterval(() => { if (!stopped) load() }, 4000)
    return () => clearInterval(timer)
  }, [user?.userId, trackingNumber])

  if (!user) {
    return (
      <div className="container page">
        <div className="empty">
          <p>Please sign in to track this shipment.</p>
          <Link className="btn" to="/auth" style={{ marginTop: 16 }}>Sign in</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="container page">
      <p className="crumb"><Link to="/shipments">Shipments</Link> / {trackingNumber}</p>
      {error && <p className="error">{error}</p>}
      {!shipment && !error && <p className="muted">Loading live tracking...</p>}
      {shipment && (
        <div className="panel wide" style={{ maxWidth: 720, margin: 0 }}>
          <h2 className="page-title">Live shipment</h2>
          <p className="muted" style={{ marginBottom: 16 }}>
            Order <Link to={`/orders/${shipment.orderNumber}`}>{shipment.orderNumber}</Link>
          </p>
          <ShipmentTracker shipment={shipment} />
        </div>
      )}
    </div>
  )
}
