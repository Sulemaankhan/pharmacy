import { useEffect, useState } from 'react'
import ProductCard from '../components/ProductCard'
import { useStore } from '../store'
import { Link } from 'react-router-dom'
import { Icon, paths } from '../icons'
import { isShopCategory } from '../labReports'

function useCountdown(iso) {
  const [left, setLeft] = useState({ d: 0, h: 0, m: 0, s: 0 })
  useEffect(() => {
    const tick = () => {
      const ms = Math.max(0, new Date(iso).getTime() - Date.now())
      setLeft({
        d: Math.floor(ms / 86400000),
        h: Math.floor((ms % 86400000) / 3600000),
        m: Math.floor((ms % 3600000) / 60000),
        s: Math.floor((ms % 60000) / 1000)
      })
    }
    tick()
    const id = setInterval(tick, 1000)
    return () => clearInterval(id)
  }, [iso])
  return left
}

const catEmoji = {
  'Medical Devices': '🩺',
  'Masks & Protection': '😷',
  'Personal Care': '🧴',
  'First Aid': '🩹',
  'Vitamins': '💊',
  'Wellness': '❤️'
}

export default function Home() {
  const { user, deals, featured, products, categories, loadingCatalog, catalogError, refreshCatalog } = useStore()
  const shopCategories = categories.filter(isShopCategory)
  const ends = deals[0]?.dealEndsAt || new Date(Date.now() + 86400000 * 2).toISOString()
  const t = useCountdown(ends)

  useEffect(() => {
    if (products.length === 0) refreshCatalog().catch(() => {})
  }, [])

  return (
    <>
      <section className="hero">
        <div className="container hero-inner">
          <div>
            <span className="badge-green"><span className="live-dot pulse" /> Open now · same-day dispatch</span>
            <h2>Your pharmacy,<br />delivered today</h2>
            <p>Genuine medicines, medical devices and lab tests from a licensed drugstore. Track orders live and read your reports in one place.</p>
            <div className="hero-actions">
              <Link className="btn" to="/shop">Shop medicines</Link>
              <Link className="btn outline" to="/lab-tests">Book lab tests</Link>
            </div>
            <div className="hero-stats">
              <span><b>2–4 hr</b> city dispatch</span>
              <span><b>100%</b> genuine stock</span>
              <span><b>24×7</b> support</span>
            </div>
          </div>
          <div className="hero-art">
            <img src="https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=900" alt="Pharmacy medicines and care" />
            <div className="hero-float">
              <small>Live cart ready</small>
              <b>From ₹89</b>
            </div>
          </div>
        </div>
      </section>

      <section className="container quick-actions">
        <Link to="/shop" className="quick-card">
          <span className="quick-ico"><Icon d={paths.shop} /></span>
          <b>Order medicines</b>
          <small>Devices, care and daily essentials</small>
        </Link>
        <Link to="/lab-tests" className="quick-card">
          <span className="quick-ico"><Icon d={paths.flask} /></span>
          <b>Book lab tests</b>
          <small>Thyroid, kidney, liver, heart, diabetes</small>
        </Link>
        <Link to="/report-status" className="quick-card">
          <span className="quick-ico"><Icon d={paths.report} /></span>
          <b>Upload report</b>
          <small>See your health status as charts</small>
        </Link>
        <Link to={user ? '/shipments' : '/auth'} className="quick-card">
          <span className="quick-ico"><Icon d={paths.truck} /></span>
          <b>Track order</b>
          <small>Live shipment updates</small>
        </Link>
      </section>

      <section className="container promo-row">
        <Link className="promo-card promo-a" to="/shop?deals=1">
          <span>Limited time</span>
          <b>Deals of the day</b>
          <em>Save on devices and wellness</em>
        </Link>
        <Link className="promo-card promo-b" to="/lab-reports">
          <span>Diagnostics</span>
          <b>Lab reports & tests</b>
          <em>Book packs or upload a PDF</em>
        </Link>
        <Link className="promo-card promo-c" to="/shop">
          <span>Trusted supply</span>
          <b>Genuine medicines</b>
          <em>Licensed drugstore stock</em>
        </Link>
      </section>

      <section className="container features">
        <div className="feature"><div className="feature-icon"><Icon d={paths.check} /></div><div><h3>100% Genuine</h3><p>We are committed to selling 100% genuine products.</p></div></div>
        <div className="feature"><div className="feature-icon"><Icon d={paths.percent} /></div><div><h3>Member Discount</h3><p>We refund one-for-one for non-standard products.</p></div></div>
        <div className="feature"><div className="feature-icon"><Icon d={paths.card} /></div><div><h3>Online Payment</h3><p>We guarantee absolute safety and security.</p></div></div>
        <div className="feature"><div className="feature-icon"><Icon d={paths.support} /></div><div><h3>24h Support</h3><p>Always ready to answer customer support.</p></div></div>
      </section>

      {shopCategories.length > 0 && (
        <section className="container section">
          <div className="section-head"><h2>Shop By Category</h2><Link className="link-btn" to="/shop">View all</Link></div>
          <div className="cats">
            {shopCategories.map((c) => (
              <Link className="cat-chip" key={c.id} to={`/shop?category=${c.id}`}>
                <span className="cat-ico">{catEmoji[c.name] || '📦'}</span>
                <b>{c.name}</b>
              </Link>
            ))}
            <Link className="cat-chip" to="/lab-reports">
              <span className="cat-ico">🧪</span>
              <b>Lab Reports</b>
            </Link>
          </div>
        </section>
      )}

      {(catalogError || (loadingCatalog && products.length === 0)) && (
        <section className="container section">
          {loadingCatalog && <p className="muted">Loading products...</p>}
          {catalogError && (
            <p className="error">
              {catalogError} <button className="link-btn" onClick={() => refreshCatalog()}>Retry</button>
            </p>
          )}
        </section>
      )}

      <section className="container section">
        <div className="section-head">
          <h2>Deals Of The Day</h2>
          <div className="hurry">
            Hurry Up! Offer ends in
            <div className="timer">
              {[['Days', t.d], ['Hrs', t.h], ['Min', t.m], ['Sec', t.s]].map(([label, val]) => (
                <span key={label}><b>{String(val).padStart(2, '0')}</b><small>{label}</small></span>
              ))}
            </div>
          </div>
        </div>
        <div className="grid">
          {deals.map((p) => <ProductCard key={p.id} product={p} />)}
        </div>
      </section>

      <section className="container section">
        <div className="section-head"><h2>Featured Products</h2><Link className="link-btn" to="/shop">Shop all</Link></div>
        <div className="grid">
          {featured.map((p) => <ProductCard key={p.id} product={p} />)}
        </div>
      </section>
    </>
  )
}
