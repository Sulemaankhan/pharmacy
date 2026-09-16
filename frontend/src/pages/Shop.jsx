import { useEffect, useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import ProductCard from '../components/ProductCard'
import { useStore } from '../store'
import { isLabProduct, isShopCategory } from '../labReports'

export default function Shop() {
  const { products, categories, loadingCatalog, catalogError, refreshCatalog, setQuery } = useStore()
  const [params, setParams] = useSearchParams()
  const q = params.get('q') || ''
  const categoryId = params.get('category')
  const dealsOnly = params.get('deals') === '1'
  const shopCategories = categories.filter(isShopCategory)
  const activeCategory = shopCategories.find((c) => String(c.id) === categoryId)

  useEffect(() => {
    if (products.length === 0) refreshCatalog().catch(() => {})
  }, [])

  const list = useMemo(() => {
    return products.filter((p) => {
      if (isLabProduct(p) && !q) return false
      if (dealsOnly && !p.dealOfTheDay) return false
      if (categoryId && String(p.category?.id) !== categoryId) return false
      if (q && !`${p.name || ''} ${p.brand || ''} ${p.labPanel || ''}`.toLowerCase().includes(q.toLowerCase())) return false
      return true
    })
  }, [products, q, categoryId, dealsOnly])

  function setCategory(id) {
    const next = new URLSearchParams(params)
    if (id) next.set('category', id)
    else next.delete('category')
    setParams(next)
  }

  function clearFilters() {
    setQuery('')
    setParams({})
  }

  const title = dealsOnly ? 'Hot Sale' : q ? `Search: ${q}` : activeCategory ? activeCategory.name : 'Shop'

  return (
    <div className="container page shop-layout">
      <aside className="side">
        <h3>Categories</h3>
        <button className={!categoryId ? 'active' : ''} onClick={() => setCategory(null)}>All products</button>
        {shopCategories.map((c) => (
          <button key={c.id} className={String(c.id) === categoryId ? 'active' : ''} onClick={() => setCategory(String(c.id))}>
            {c.name}
          </button>
        ))}
        <Link className="link-btn" to="/shop?deals=1" style={{ display: 'block', marginTop: 16 }}>Today's deals</Link>
      </aside>
      <div>
        <p className="crumb">Home / {title}</p>
        <h2 className="page-title">{title}</h2>
        <p className="muted">{list.length} products available</p>
        {catalogError && (
          <p className="error">
            {catalogError} <button className="link-btn" onClick={() => refreshCatalog()}>Retry</button>
          </p>
        )}
        {loadingCatalog && products.length === 0 && <p className="muted">Loading products...</p>}
        {!loadingCatalog && products.length > 0 && list.length === 0 && (
          <div className="empty" style={{ marginTop: 20 }}>
            <p>No products match this filter.</p>
            <button className="btn" style={{ marginTop: 12 }} onClick={clearFilters}>Clear filters</button>
          </div>
        )}
        <div className="grid" style={{ marginTop: 18 }}>
          {list.map((p) => <ProductCard key={p.id} product={p} />)}
        </div>
      </div>
    </div>
  )
}
