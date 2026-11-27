import { useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import BrowseLayout from '../components/BrowseLayout'
import CatalogStatus from '../components/CatalogStatus'
import EmptyState from '../components/EmptyState'
import ProductGrid from '../components/ProductGrid'
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
    <BrowseLayout
      sidebarTitle="Categories"
      crumb={`Home / ${title}`}
      title={title}
      sidebar={(
        <>
          <button className={!categoryId ? 'active' : ''} onClick={() => setCategory(null)}>All products</button>
          {shopCategories.map((c) => (
            <button key={c.id} className={String(c.id) === categoryId ? 'active' : ''} onClick={() => setCategory(String(c.id))}>
              {c.name}
            </button>
          ))}
          <Link className="link-btn" to="/shop?deals=1" style={{ display: 'block', marginTop: 16 }}>Today's deals</Link>
        </>
      )}
    >
      <p className="muted">{list.length} products available</p>
      <CatalogStatus
        loading={loadingCatalog && products.length === 0}
        error={catalogError}
        onRetry={refreshCatalog}
      />
      {!loadingCatalog && products.length > 0 && list.length === 0 && (
        <EmptyState
          message="No products match this filter."
          actionLabel="Clear filters"
          onAction={clearFilters}
          style={{ marginTop: 20 }}
        />
      )}
      <ProductGrid products={list} style={{ marginTop: 18 }} />
    </BrowseLayout>
  )
}
