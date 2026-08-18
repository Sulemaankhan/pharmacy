import { Link } from 'react-router-dom'
import ProductCard from '../components/ProductCard'
import { useStore } from '../store'

export default function Wishlist() {
  const { wishlist, user } = useStore()
  if (!user) {
    return (
      <div className="container page">
        <div className="empty">
          <h2>Save your favourites</h2>
          <p className="muted">Sign in to view your wishlist.</p>
          <Link className="btn" to="/auth" style={{ marginTop: 16 }}>Sign in</Link>
        </div>
      </div>
    )
  }
  return (
    <div className="container page">
      <p className="crumb">Home / Wishlist</p>
      <h2 className="page-title">Wishlist</h2>
      {wishlist.length === 0 ? (
        <div className="empty">
          <p>No saved products yet.</p>
          <Link className="btn" to="/shop" style={{ marginTop: 16 }}>Browse products</Link>
        </div>
      ) : (
        <div className="grid" style={{ marginTop: 16 }}>
          {wishlist.map((w) => <ProductCard key={w.id} product={w.product} />)}
        </div>
      )}
    </div>
  )
}
