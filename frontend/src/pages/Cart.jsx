import { Link } from 'react-router-dom'
import EmptyState from '../components/EmptyState'
import LazyImage from '../components/LazyImage'
import RequireSignIn from '../components/RequireSignIn'
import { useStore } from '../store'
import { formatMoney } from '../money'

export default function Cart() {
  const { cart, cartTotal, updateCart, removeCart, user } = useStore()
  const shipping = cartTotal >= 499 || cart.length === 0 ? 0 : 49
  const grand = cartTotal + shipping

  return (
    <RequireSignIn user={user} title="Your cart is waiting" message="Please sign in to view and checkout your items.">
      <div className="container page">
        <p className="crumb">Home / Cart</p>
        <h2 className="page-title">Shopping Cart</h2>
        {cart.length === 0 ? (
          <EmptyState message="Your cart is empty." actionTo="/shop" actionLabel="Continue shopping" />
        ) : (
          <div className="cart-layout">
            <div className="panel wide">
              {cart.map((item) => (
                <div className="cart-row" key={item.id}>
                  <LazyImage src={item.product?.imageUrl} alt="" />
                  <div>
                    <b>{item.product?.name}</b>
                    <div className="muted">{formatMoney(item.product?.price)}</div>
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
    </RequireSignIn>
  )
}
