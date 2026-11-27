import { useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import BrowseLayout from '../components/BrowseLayout'
import CatalogStatus from '../components/CatalogStatus'
import EmptyState from '../components/EmptyState'
import ProductGrid from '../components/ProductGrid'
import { useStore } from '../store'
import { LAB_PANELS, isLabProduct, panelLabel } from '../labReports'
import LabReportsTabs from '../components/LabReportsTabs'

export default function LabReports() {
  const { products, loadingCatalog, catalogError, refreshCatalog } = useStore()
  const [params, setParams] = useSearchParams()
  const panel = (params.get('panel') || '').toUpperCase()
  const active = LAB_PANELS.find((item) => item.id === panel)

  const list = useMemo(() => {
    return products.filter((p) => isLabProduct(p) && (!panel || p.labPanel === panel))
  }, [products, panel])

  function setPanel(id) {
    const next = new URLSearchParams(params)
    if (id) next.set('panel', id)
    else next.delete('panel')
    setParams(next)
  }

  const title = active ? `${active.name} tests` : 'Lab Tests'

  return (
    <BrowseLayout
      sidebarTitle="Lab tests"
      crumb={`Home / Lab Reports / Lab Tests${active ? ` / ${active.name}` : ''}`}
      title={title}
      sidebar={(
        <>
          <button className={!panel ? 'active' : ''} onClick={() => setPanel(null)}>All tests</button>
          {LAB_PANELS.map((item) => (
            <button key={item.id} className={item.id === panel ? 'active' : ''} onClick={() => setPanel(item.id)}>
              {item.name}
            </button>
          ))}
        </>
      )}
    >
      <LabReportsTabs />
      <p className="muted">
        {active ? active.blurb : 'Book thyroid, kidney, liver, heart and diabetes tests. Add to cart like shop products.'}
      </p>
      <p className="muted">{list.length} tests available</p>
      <CatalogStatus
        loading={loadingCatalog && products.length === 0}
        error={catalogError}
        loadingLabel="Loading lab tests..."
        onRetry={() => refreshCatalog({ force: true })}
      />
      {!loadingCatalog && list.length === 0 && (
        <EmptyState
          message="No lab tests in this group yet."
          actionTo="/lab-tests"
          actionLabel="View all tests"
          style={{ marginTop: 20 }}
        />
      )}
      <div className="lab-chips">
        {LAB_PANELS.map((item) => (
          <Link
            key={item.id}
            className={`lab-chip${item.id === panel ? ' on' : ''}`}
            to={`/lab-tests?panel=${item.id}`}
          >
            <b>{item.name}</b>
            <span>{item.blurb}</span>
          </Link>
        ))}
      </div>
      <ProductGrid products={list} style={{ marginTop: 18 }} />
      {list.length > 0 && panel && (
        <p className="muted" style={{ marginTop: 16 }}>
          Showing {panelLabel(panel)} tests. <Link className="link-btn" to="/lab-tests">See all lab tests</Link>
        </p>
      )}
    </BrowseLayout>
  )
}
