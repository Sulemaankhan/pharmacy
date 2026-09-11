const STEPS = [
  { code: 'CONFIRMED', label: 'Confirmed' },
  { code: 'PACKED', label: 'Packed' },
  { code: 'SHIPPED', label: 'Shipped' },
  { code: 'OUT_FOR_DELIVERY', label: 'Out for delivery' },
  { code: 'DELIVERED', label: 'Delivered' },
]

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

function statusClass(status) {
  if (status === 'DELIVERED' || status === 'CONFIRMED' || status === 'SUCCESS') return 'ok'
  if (status === 'CANCELLED' || status === 'FAILED') return 'bad'
  return 'wait'
}

function prettyStatus(status) {
  if (!status) return '—'
  return status.replaceAll('_', ' ')
}

export default function ShipmentTracker({ shipment }) {
  if (!shipment) {
    return <p className="muted">No shipment details yet.</p>
  }

  const current = shipment.status
  const cancelled = current === 'CANCELLED'
  const currentIndex = STEPS.findIndex((s) => s.code === current)

  return (
    <div className="ship-tracker">
      <div className="ship-tracker-head">
        <div>
          <div className="muted">Tracking ID</div>
          <b>{shipment.trackingNumber}</b>
        </div>
        <span className={`status-pill ${statusClass(current)}`}>
          <span className={`live-dot ${cancelled || current === 'DELIVERED' ? '' : 'pulse'}`} />
          {prettyStatus(current)}
        </span>
      </div>

      {!cancelled && (
        <ol className="ship-steps">
          {STEPS.map((step, idx) => {
            const done = currentIndex >= idx
            const active = currentIndex === idx
            return (
              <li key={step.code} className={`${done ? 'done' : ''} ${active ? 'active' : ''}`}>
                <span className="ship-dot" />
                <span>{step.label}</span>
              </li>
            )
          })}
        </ol>
      )}

      <div className="ship-contact">
        <h4>Deliver to</h4>
        <p><b>{shipment.recipientName}</b></p>
        <p>{shipment.fullAddress || shipment.address}</p>
        <p>Contact: {shipment.contactNumber}</p>
        <p>Email: {shipment.email}</p>
        {shipment.estimatedDelivery && current !== 'DELIVERED' && current !== 'CANCELLED' && (
          <p className="muted">ETA {formatDate(shipment.estimatedDelivery)}</p>
        )}
        {shipment.updatedAt && (
          <p className="muted">Last update {formatDate(shipment.updatedAt)}</p>
        )}
      </div>
    </div>
  )
}
