import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { formatMoney } from '../money'
import { api } from '../api'

export default function OrderSuccess() {
  const { orderNumber } = useParams()
  const passed = useLocation().state
  const [order, setOrder] = useState(passed)
  const [error, setError] = useState('')

  useEffect(() => {
    api(`/api/orders/${orderNumber}`)
      .then((data) => setOrder((prev) => ({ ...prev, ...data, success: data.status === 'SUCCESS' })))
      .catch((e) => setError(e.message))
  }, [orderNumber])

  const payment = order?.payment
  const success = order?.success !== false && (order?.status ? order.status === 'SUCCESS' : true)

  return (
    <div className="container page">
      <div className="auth-box" style={{ maxWidth: 560, textAlign: 'center' }}>
        <div className="success-badge">{success ? '✓' : '!'}</div>
        <h2>{success ? 'Payment saved in database' : 'Payment not completed'}</h2>
        <p className="lead">{order?.message || (success ? 'Order, payment and ledger entries were committed in one MySQL transaction.' : 'The failed attempt was still recorded.')}</p>
        {error && <p className="error">{error}</p>}
        <div className="summary" style={{ textAlign: 'left', marginTop: 8 }}>
          <div className="summary-row"><span>Order no.</span><b>{orderNumber}</b></div>
          {order?.id && <div className="summary-row"><span>Order DB id</span><b>{order.id}</b></div>}
          {(order?.paymentId || payment?.id) && <div className="summary-row"><span>Payment DB id</span><b>{order.paymentId || payment.id}</b></div>}
          {(order?.transactionRef || payment?.transactionRef) && <div className="summary-row"><span>Txn ref</span><b>{order.transactionRef || payment.transactionRef}</b></div>}
          {(order?.paymentMode || payment?.paymentMode) && <div className="summary-row"><span>Mode</span><b>{order.paymentMode || payment.paymentMode}</b></div>}
          {(order?.amount != null || payment?.amount != null) && <div className="summary-row"><span>Amount</span><b>{formatMoney(order.amount ?? payment.amount)}</b></div>}
          <div className="summary-row"><span>Ledger</span><b>{(order?.ledgerPosted || payment?.ledgerPosted) ? 'Posted (debit + credit)' : 'Not posted'}</b></div>
        </div>
        <Link className="btn full" to="/shop" style={{ marginTop: 18 }}>Continue shopping</Link>
        <Link className="link-btn" to="/orders" style={{ display: 'block', marginTop: 12 }}>View order history</Link>
      </div>
    </div>
  )
}
