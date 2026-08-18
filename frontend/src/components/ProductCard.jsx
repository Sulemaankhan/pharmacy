import { Link } from 'react-router-dom'
import { useStore } from '../store'
import { useState } from 'react'
import { formatMoney } from '../money'
import { Icon, paths } from '../icons'

export default function ProductCard({ product }) {
  const { addToCart, toggleWish, isWished } = useStore()
  const [msg, setMsg] = useState('')
  if (!product) return null
  const off = product.compareAtPrice ? Math.round((1 - Number(product.price) / Number(product.compareAtPrice)) * 100) : 0
  const wished = isWished(product.id)

  async function add() {
    try {
      await addToCart(product.id, 1)
      setMsg('Added to cart')
    } catch (e) {
      setMsg(e.message)
    }
  }

  return (
    <article className="card">
      {off > 0 && <span className="sale-tag">-{off}%</span>}
      <Link to={`/product/${product.id}`} className="img-wrap">
        <img src={product.imageUrl} alt={product.name} />
      </Link>
      <div className="brand">{product.brand}</div>
      <h3><Link to={`/product/${product.id}`}>{product.name}</Link></h3>
      <div className="stars">{'★'.repeat(Math.round(product.rating || 0))} <span className="muted">({product.reviewCount})</span></div>
      <div className="prices">
        <strong>{formatMoney(product.price)}</strong>
        {product.compareAtPrice && <s>{formatMoney(product.compareAtPrice)}</s>}
      </div>
      <div className="card-actions">
        <button className="btn" onClick={add}>Add to cart</button>
        <button className={`wish ${wished ? 'on' : ''}`} onClick={() => toggleWish(product.id).catch((e) => setMsg(e.message))} aria-label="Wishlist">
          <Icon d={paths.heart} size={18} />
        </button>
      </div>
      {msg && <small className={msg === 'Added to cart' ? 'toast-ok' : 'muted'}>{msg}</small>}
    </article>
  )
}
