import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useStore } from '../store'
import { formatMoney } from '../money'
import ProductCard from '../components/ProductCard'
import { isLabProduct, panelLabel } from '../labReports'

export default function ProductPage() {
  const { id } = useParams()
  const { products, addToCart, toggleWish, isWished, user } = useStore()
  const product = products.find((p) => String(p.id) === id)
  const [qty, setQty] = useState(1)
  const [msg, setMsg] = useState('')
  const related = products.filter((p) => {
    if (!product || p.id === product.id) return false
    if (isLabProduct(product)) return p.labPanel === product.labPanel
    return !isLabProduct(p) && p.category?.id === product.category?.id
  }).slice(0, 4)

  if (!product) return <div className="container page">Product not found.</div>
  const off = product.compareAtPrice ? Math.round((1 - Number(product.price) / Number(product.compareAtPrice)) * 100) : 0

  return (
    <div className="container page">
      <p className="crumb">
        Home / {isLabProduct(product)
          ? <Link to={`/lab-tests?panel=${product.labPanel}`}>Lab Reports / Lab Tests / {panelLabel(product.labPanel)}</Link>
          : <Link to="/shop">Shop</Link>} / {product.name}
      </p>
      <div className="detail">
        <div className="detail-img">
          {off > 0 && <span className="sale-tag">-{off}%</span>}
          <img src={product.imageUrl} alt={product.name} />
        </div>
        <div>
          <div className="muted">
            {product.brand} · {isLabProduct(product) ? `${panelLabel(product.labPanel)} lab test` : product.category?.name}
          </div>
          <h2 style={{ margin: '8px 0 10px' }}>{product.name}</h2>
          <div className="stars">★ {product.rating} ({product.reviewCount} reviews)</div>
          <div className="stock">In stock · {product.stock} units</div>
          <div className="prices" style={{ margin: '16px 0' }}>
            <strong>{formatMoney(product.price)}</strong>
            {product.compareAtPrice && <s>{formatMoney(product.compareAtPrice)}</s>}
          </div>
          <p className="lead">{product.description}</p>
          <div className="qty">
            <button onClick={() => setQty(Math.max(1, qty - 1))}>-</button>
            <b>{qty}</b>
            <button onClick={() => setQty(qty + 1)}>+</button>
          </div>
          <div className="card-actions">
            {user && (
              <button className="btn" onClick={() => addToCart(product.id, qty).then(() => setMsg('Added to cart')).catch((e) => setMsg(e.message))}>Add to cart</button>
            )}
            {user && (
              <button className="btn outline" onClick={() => toggleWish(product.id).catch((e) => setMsg(e.message))}>
                {isWished(product.id) ? 'Wishlisted' : 'Add to wishlist'}
              </button>
            )}
          </div>
          {!user && (
            <p className="muted" style={{ marginTop: 10 }}>
              <Link to="/auth">Sign in</Link> to add this item to your cart or wishlist.
            </p>
          )}
          {msg && <p className={msg === 'Added to cart' ? 'toast-ok' : 'muted'} style={{ marginTop: 10 }}>{msg}</p>}
        </div>
      </div>
      {related.length > 0 && (
        <section className="section" style={{ paddingTop: 40 }}>
          <div className="section-head">
            <h2>{isLabProduct(product) ? 'Related lab tests' : 'Related products'}</h2>
            <Link className="link-btn" to={isLabProduct(product) ? `/lab-tests?panel=${product.labPanel}` : '/shop'}>View all</Link>
          </div>
          <div className="grid">
            {related.map((p) => <ProductCard key={p.id} product={p} />)}
          </div>
        </section>
      )}
    </div>
  )
}
