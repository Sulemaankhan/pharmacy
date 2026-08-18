import { useEffect, useState } from 'react'
import ProductCard from '../components/ProductCard'
import { useStore } from '../store'
import { Link } from 'react-router-dom'
import { Icon, paths } from '../icons'

function Virus({ style }) {
  return (
    <svg className="virus" style={style} viewBox="0 0 48 48" fill="currentColor">
      <circle cx="24" cy="24" r="11" />
      <circle cx="24" cy="6" r="3" />
      <circle cx="24" cy="42" r="3" />
      <circle cx="6" cy="24" r="3" />
      <circle cx="42" cy="24" r="3" />
      <circle cx="11" cy="11" r="2.5" />
      <circle cx="37" cy="11" r="2.5" />
      <circle cx="11" cy="37" r="2.5" />
      <circle cx="37" cy="37" r="2.5" />
    </svg>
  )
}

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
  const { deals, featured, products, categories, loadingCatalog, catalogError, refreshCatalog } = useStore()
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
            <span className="badge-green">KILLS 99.9% OF BACTERIA</span>
            <h2>Hand Sanitizer</h2>
            <p>Non-irritating, moisturizing formula for hospitals, clinics and everyday protection. Trusted genuine products, delivered fast.</p>
            <div className="price-from">Start From <b>₹89</b></div>
            <Link className="btn" to="/shop">Shop Now</Link>
            <div className="hero-dots"><span className="on" /><span /><span /></div>
          </div>
          <div className="hero-art">
            <span className="orb" style={{ width: 240, height: 240, right: 36, top: 28 }} />
            <span className="orb" style={{ width: 96, height: 96, left: 24, bottom: 46, opacity: 0.32 }} />
            <Virus style={{ right: 28, top: 36 }} />
            <Virus style={{ left: 48, top: 92, width: 28, height: 28 }} />
            <Virus style={{ right: 90, bottom: 64, width: 32, height: 32 }} />
            <img src="https://images.unsplash.com/photo-1584483766114-2cea6facdf57?w=700" alt="Hand sanitizer" />
          </div>
        </div>
      </section>

      <section className="container promo-row">
        <Link className="promo-card promo-a" to="/shop?deals=1">
          <span>Limited time</span>
          <b>Deals of the day</b>
        </Link>
        <Link className="promo-card promo-b" to="/shop?category=5">
          <span>Immunity</span>
          <b>Vitamins & wellness</b>
        </Link>
        <Link className="promo-card promo-c" to="/shop">
          <span>Trusted supply</span>
          <b>Genuine medicines</b>
        </Link>
      </section>

      <section className="container features">
        <div className="feature"><div className="feature-icon"><Icon d={paths.check} /></div><div><h3>100% Genuine</h3><p>We are committed to selling 100% genuine products.</p></div></div>
        <div className="feature"><div className="feature-icon"><Icon d={paths.percent} /></div><div><h3>Member Discount</h3><p>We refund one-for-one for non-standard products.</p></div></div>
        <div className="feature"><div className="feature-icon"><Icon d={paths.card} /></div><div><h3>Online Payment</h3><p>We guarantee absolute safety and security.</p></div></div>
        <div className="feature"><div className="feature-icon"><Icon d={paths.support} /></div><div><h3>24h Support</h3><p>Always ready to answer customer support.</p></div></div>
      </section>

      {categories.length > 0 && (
        <section className="container section">
          <div className="section-head"><h2>Shop By Category</h2><Link className="link-btn" to="/shop">View all</Link></div>
          <div className="cats">
            {categories.map((c) => (
              <Link className="cat-chip" key={c.id} to={`/shop?category=${c.id}`}>
                <span style={{ fontSize: 28 }}>{catEmoji[c.name] || '📦'}</span>
                <b>{c.name}</b>
              </Link>
            ))}
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
