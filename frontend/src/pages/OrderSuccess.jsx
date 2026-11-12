import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { formatMoney } from '../money'
import { api } from '../api'

export default function OrderSuccess() {
  const { orderNumber } = useParams()
  const passed = useLocation().state
  const [order, setOrder] = useState(passed)
  const [error, setError] = useState('')
  const [emailing, setEmailing] = useState(false)
  const [emailMsg, setEmailMsg] = useState('')

  useEffect(() => {
    api(`/api/orders/${orderNumber}`)
      .then((data) => setOrder((prev) => ({
        ...prev,
        ...data,
        success: data.status === 'SUCCESS',
        emailStatus: prev?.emailStatus,
        message: prev?.message,
      })))
      .catch((e) => setError(e.message))
  }, [orderNumber])

  async function emailOrder() {
    setEmailMsg('')
    setError('')
    setEmailing(true)
    try {
      const result = await api(`/api/orders/${orderNumber}/email`, { method: 'POST' })
      setEmailMsg(result.message || 'Order details emailed')
    } catch (e) {
      setError(e.message)
    } finally {
      setEmailing(false)
    }
  }

  const payment = order?.payment
  const success = order?.success !== false && (order?.status ? order.status === 'SUCCESS' : true)
  const debited = order?.debitedAmount ?? payment?.debitedAmount
  const credited = order?.creditedAmount ?? payment?.creditedAmount

  return (
    <div className="container page">
      <div className="auth-box" style={{ maxWidth: 560, textAlign: 'center' }}>
        <div className="success-badge">{success ? '✓' : '!'}</div>
        <h2>{success ? 'Order confirmed' : 'Order failed'}</h2>
        <p className="lead">{order?.message || (success ? 'Your order is confirmed. A success notification was emailed to your account.' : 'Your order was cancelled. A failure notification was emailed to your account.')}</p>
        {(order?.emailStatus === 'SENT' || order?.emailStatus === 'QUEUED'
          || passed?.emailStatus === 'SENT' || passed?.emailStatus === 'QUEUED') && (
          <p className="toast-ok">Notification queued for email.</p>
        )}
        {(order?.emailStatus === 'FAILED' || passed?.emailStatus === 'FAILED') && (
          <p className="error">Notification email could not be delivered. Use the button below to retry.</p>
        )}
        {error && <p className="error">{error}</p>}
        <div className="summary" style={{ textAlign: 'left', marginTop: 8 }}>
          <div className="summary-row"><span>Order no.</span><b>{orderNumber}</b></div>
          {order?.id && <div className="summary-row"><span>Order DB id</span><b>{order.id}</b></div>}
          {(order?.paymentId || payment?.id) && <div className="summary-row"><span>Payment DB id</span><b>{order.paymentId || payment.id}</b></div>}
          {(order?.transactionRef || payment?.transactionRef) && <div className="summary-row"><span>Txn ref</span><b>{order.transactionRef || payment.transactionRef}</b></div>}
          {(order?.trackingNumber || order?.shipment?.trackingNumber) && (
            <div className="summary-row"><span>Tracking</span><b>{order.trackingNumber || order.shipment.trackingNumber}</b></div>
          )}
          {(order?.recipientName || order?.shipment?.recipientName) && (
            <div className="summary-row"><span>Deliver to</span><b>{order.recipientName || order.shipment.recipientName}</b></div>
          )}
          {(order?.address || order?.shipment?.fullAddress) && (
            <div className="summary-row"><span>Address</span><b>{order.address || order.shipment.fullAddress}</b></div>
          )}
          {(order?.contactNumber || order?.shipment?.contactNumber) && (
            <div className="summary-row"><span>Contact</span><b>{order.contactNumber || order.shipment.contactNumber}</b></div>
          )}
          {(order?.deliveryEmail || order?.shipment?.email) && (
            <div className="summary-row"><span>Email ID</span><b>{order.deliveryEmail || order.shipment.email}</b></div>
          )}
          {(order?.paymentMode || payment?.paymentMode) && <div className="summary-row"><span>Mode</span><b>{order.paymentMode || payment.paymentMode}</b></div>}
          {(order?.amount != null || payment?.amount != null) && <div className="summary-row"><span>Amount</span><b>{formatMoney(order.amount ?? payment.amount)}</b></div>}
          {debited != null && (
            <div className="summary-row"><span>Debited</span><b>{formatMoney(debited)}</b></div>
          )}
          {credited != null && (
            <div className="summary-row"><span>Credited to pharmacy</span><b>{formatMoney(credited)}</b></div>
          )}
          {(order?.refunded || payment?.refunded) && (
            <div className="summary-row"><span>Refund</span><b>{formatMoney(order?.refundedAmount ?? payment?.refundedAmount ?? debited ?? order?.amount ?? payment?.amount)}</b></div>
          )}
          <div className="summary-row"><span>Ledger</span><b>{(order?.ledgerPosted || payment?.ledgerPosted) ? 'Posted (debit + credit)' : 'Not posted'}</b></div>
        </div>
        <Link className="btn full" to="/shop" style={{ marginTop: 18 }}>Continue shopping</Link>
        {(order?.trackingNumber || order?.shipment?.trackingNumber) && (
          <Link className="btn outline full" to={`/shipments/${order.trackingNumber || order.shipment.trackingNumber}`} style={{ marginTop: 10 }}>
            Track shipment
          </Link>
        )}
        <button className="btn outline full" style={{ marginTop: 10 }} disabled={emailing} onClick={emailOrder}>
          {emailing ? 'Sending email...' : 'Email order details'}
        </button>
        {emailMsg && <p className="toast-ok" style={{ marginTop: 10 }}>{emailMsg}</p>}
        <Link className="link-btn" to="/orders" style={{ display: 'block', marginTop: 12 }}>View order history</Link>
      </div>
    </div>
  )
}
