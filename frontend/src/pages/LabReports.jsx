import { useEffect, useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import ProductCard from '../components/ProductCard'
import { useStore } from '../store'
import { LAB_PANELS, isLabProduct, panelLabel } from '../labReports'
import LabReportsTabs from '../components/LabReportsTabs'

export default function LabReports() {
  const { products, loadingCatalog, catalogError, refreshCatalog } = useStore()
  const [params, setParams] = useSearchParams()
  const panel = (params.get('panel') || '').toUpperCase()
  const active = LAB_PANELS.find((item) => item.id === panel)

  useEffect(() => {
    if (products.length === 0) refreshCatalog().catch(() => {})
  }, [])

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
    <div className="container page shop-layout">
      <aside className="side">
        <h3>Lab tests</h3>
        <button className={!panel ? 'active' : ''} onClick={() => setPanel(null)}>All tests</button>
        {LAB_PANELS.map((item) => (
          <button key={item.id} className={item.id === panel ? 'active' : ''} onClick={() => setPanel(item.id)}>
            {item.name}
          </button>
        ))}
      </aside>
      <div>
        <p className="crumb">Home / Lab Reports / Lab Tests{active ? ` / ${active.name}` : ''}</p>
        <h2 className="page-title">{title}</h2>
        <LabReportsTabs />
        <p className="muted">
          {active ? active.blurb : 'Book thyroid, kidney, liver, heart and diabetes tests. Add to cart like shop products.'}
        </p>
        <p className="muted">{list.length} tests available</p>
        {catalogError && (
          <p className="error">
            {catalogError} <button className="link-btn" onClick={() => refreshCatalog()}>Retry</button>
          </p>
        )}
        {loadingCatalog && products.length === 0 && <p className="muted">Loading lab tests...</p>}
        {!loadingCatalog && list.length === 0 && (
          <div className="empty" style={{ marginTop: 20 }}>
            <p>No lab tests in this group yet.</p>
            <Link className="btn" style={{ marginTop: 12 }} to="/lab-tests">View all tests</Link>
          </div>
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
        <div className="grid" style={{ marginTop: 18 }}>
          {list.map((p) => <ProductCard key={p.id} product={p} />)}
        </div>
        {list.length > 0 && panel && (
          <p className="muted" style={{ marginTop: 16 }}>
            Showing {panelLabel(panel)} tests. <Link className="link-btn" to="/lab-tests">See all lab tests</Link>
          </p>
        )}
      </div>
    </div>
  )
}
