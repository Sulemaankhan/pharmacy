import ProductCard from '../components/ProductCard'
import EmptyState from '../components/EmptyState'
import RequireSignIn from '../components/RequireSignIn'
import { useStore } from '../store'

export default function Wishlist() {
  const { wishlist, user } = useStore()
  return (
    <RequireSignIn user={user} title="Save your favourites" message="Sign in to view your wishlist.">
      <div className="container page">
        <p className="crumb">Home / Wishlist</p>
        <h2 className="page-title">Wishlist</h2>
        {wishlist.length === 0 ? (
          <EmptyState message="No saved products yet." actionTo="/shop" actionLabel="Browse products" />
        ) : (
          <div className="grid" style={{ marginTop: 16 }}>
            {wishlist.map((w) => <ProductCard key={w.id} product={w.product} />)}
          </div>
        )}
      </div>
    </RequireSignIn>
  )
}
