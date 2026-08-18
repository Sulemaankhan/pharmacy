import { Link } from 'react-router-dom'
import { useStore } from '../store'
import { formatMoney } from '../money'

export default function Cart() {
  const { cart, cartTotal, updateCart, removeCart, user } = useStore()
  const shipping = cartTotal >= 499 || cart.length === 0 ? 0 : 49
  const grand = cartTotal + shipping

  if (!user) {
    return (
      <div className="container page">
        <div className="empty">
          <h2>Your cart is waiting</h2>
          <p className="muted">Please sign in to view and checkout your items.</p>
          <Link className="btn" to="/auth" style={{ marginTop: 16 }}>Sign in</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="container page">
      <p className="crumb">Home / Cart</p>
      <h2 className="page-title">Shopping Cart</h2>
      {cart.length === 0 ? (
        <div className="empty">
          <p>Your cart is empty.</p>
          <Link className="btn" to="/shop" style={{ marginTop: 16 }}>Continue shopping</Link>
        </div>
      ) : (
        <div className="cart-layout">
          <div className="panel wide">
            {cart.map((item) => (
              <div className="cart-row" key={item.id}>
                <img src={item.product.imageUrl} alt="" />
                <div>
                  <b>{item.product.name}</b>
                  <div className="muted">{formatMoney(item.product.price)}</div>
                </div>
                <div className="qty">
                  <button onClick={() => updateCart(item.id, item.quantity - 1)}>-</button>
                  <span>{item.quantity}</span>
                  <button onClick={() => updateCart(item.id, item.quantity + 1)}>+</button>
                </div>
                <button className="btn outline" onClick={() => removeCart(item.id)}>Remove</button>
              </div>
            ))}
          </div>
          <aside className="summary">
            <h3>Order summary</h3>
            <div className="summary-row"><span>Subtotal</span><span>{formatMoney(cartTotal)}</span></div>
            <div className="summary-row"><span>Shipping</span><span>{shipping === 0 ? 'Free' : formatMoney(shipping)}</span></div>
            <div className="summary-row total"><span>Total</span><span>{formatMoney(grand)}</span></div>
            <p className="muted" style={{ margin: '10px 0 16px', fontSize: 13 }}>
              {cartTotal >= 499 ? 'You unlocked free shipping.' : `Add ${formatMoney(499 - cartTotal)} more for free shipping.`}
            </p>
            <Link className="btn full" to="/checkout">Proceed to checkout</Link>
          </aside>
        </div>
      )}
    </div>
  )
}
