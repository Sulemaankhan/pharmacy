import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useStore } from '../store'
import { formatMoney } from '../money'
import { Icon, LogoMark, paths } from '../icons'
import { LAB_PANELS, isShopCategory } from '../labReports'

export default function Header() {
  const { user, cartCount, cartTotal, wishCount, query, setQuery, logout, categories } = useStore()
  const [open, setOpen] = useState(false)
  const [catsOpen, setCatsOpen] = useState(false)
  const [labsOpen, setLabsOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const shopCategories = categories.filter(isShopCategory)
  const navigate = useNavigate()
  const location = useLocation()
  const labsSection = location.pathname.startsWith('/lab-reports')
    || location.pathname.startsWith('/lab-tests')
    || location.pathname.startsWith('/report-status')
    || location.pathname.startsWith('/health-report')
  const profileSection = location.pathname.startsWith('/profile')
    || location.pathname.startsWith('/orders')
    || location.pathname.startsWith('/shipments')

  function search(e) {
    e.preventDefault()
    const term = query.trim()
    navigate(term ? `/shop?q=${encodeURIComponent(term)}` : '/shop')
  }

  function handleLogout() {
    logout()
    setOpen(false)
    navigate('/auth')
  }

  return (
    <div className="site-header">
      <div className="topbar">
        <div className="container topbar-inner">
          <div className="topbar-left">
            <div className="socials">
              <span>f</span><span>t</span><span>p</span><span>in</span><span>yt</span>
            </div>
            <span className="phone"><Icon d={paths.phone} size={14} /> (+91) 720-729-0964</span>
          </div>
          <div className="topbar-right">
            <span>INR</span>
            <span>English</span>
              {user ? (
                <>
                  <Link to="/profile">Hi, {user.name}</Link>
                  <button type="button" className="link-btn" onClick={handleLogout}>Logout</button>
                </>
              ) : (
              <Link to="/auth">Sign In / Register</Link>
            )}
          </div>
        </div>
      </div>

      <header className="header">
        <div className="container header-inner">
          <button className="menu-toggle" onClick={() => setOpen(!open)} aria-label="Menu">
            <Icon d={paths.menu} size={24} />
          </button>
          <Link to="/" className="logo">
            <LogoMark />
            <div>
              <h1>MEDICINE<br />DRUGSTORE</h1>
            </div>
          </Link>
          <form className="search" onSubmit={search}>
            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Enter your keyword..." />
            <button type="submit">Search</button>
          </form>
          <div className="header-actions">
            <Link className="icon-btn hide-sm" to="/shop">
              <div className="icon-glyph"><Icon d={paths.compare} /></div>
              Compare
              <span className="badge">0</span>
            </Link>
            {user && (
              <>
                <Link className="icon-btn" to="/wishlist">
                  <div className="icon-glyph"><Icon d={paths.heart} /></div>
                  Wishlist
                  <span className="badge">{wishCount}</span>
                </Link>
                <Link className="icon-btn" to="/cart">
                  <div className="icon-glyph"><Icon d={paths.bag} /></div>
                  <div className="cart-total">{formatMoney(cartTotal)}</div>
                  <span className="badge">{cartCount}</span>
                </Link>
              </>
            )}
            {user ? (
              <button type="button" className="btn logout-btn" onClick={handleLogout}>Logout</button>
            ) : (
              <Link className="btn logout-btn" to="/auth">Sign In</Link>
            )}
          </div>
        </div>
      </header>

      <nav className="navbar">
        <div className="container nav-inner">
          <div className="cat-wrap" onMouseLeave={() => setCatsOpen(false)}>
            <button className="cat-btn" type="button" onClick={() => setCatsOpen((v) => !v)} onMouseEnter={() => setCatsOpen(true)}>
              <Icon d={paths.menu} size={16} /> Shop By Categories
            </button>
            {catsOpen && (
              <div className="cat-drop">
                <Link to="/shop" onClick={() => setCatsOpen(false)}>All products</Link>
                {shopCategories.map((c) => (
                  <Link key={c.id} to={`/shop?category=${c.id}`} onClick={() => setCatsOpen(false)}>{c.name}</Link>
                ))}
              </div>
            )}
          </div>
          <div className="nav-links">
            <NavLink to="/" end>Home</NavLink>
            <NavLink to="/shop">Shop</NavLink>
            <div className="nav-drop-wrap" onMouseLeave={() => setLabsOpen(false)}>
              <NavLink
                to="/lab-reports"
                onMouseEnter={() => setLabsOpen(true)}
                className={() => (labsSection ? 'active' : undefined)}
              >
                Lab Reports
              </NavLink>
              {labsOpen && (
                <div className="cat-drop nav-subdrop">
                  <Link to="/report-status" onClick={() => setLabsOpen(false)}>Report Status</Link>
                  <Link to="/lab-tests" onClick={() => setLabsOpen(false)}>Lab Tests</Link>
                  {LAB_PANELS.map((panel) => (
                    <Link
                      key={panel.id}
                      className="drop-sub"
                      to={`/lab-tests?panel=${panel.id}`}
                      onClick={() => setLabsOpen(false)}
                    >
                      {panel.name}
                    </Link>
                  ))}
                </div>
              )}
            </div>
            {user && (
              <div className="nav-drop-wrap" onMouseLeave={() => setProfileOpen(false)}>
                <NavLink
                  to="/profile"
                  onMouseEnter={() => setProfileOpen(true)}
                  className={() => (profileSection ? 'active' : undefined)}
                >
                  Profile
                </NavLink>
                {profileOpen && (
                  <div className="cat-drop nav-subdrop">
                    <Link to="/profile" onClick={() => setProfileOpen(false)}>My profile</Link>
                    <Link to="/orders" onClick={() => setProfileOpen(false)}>Orders</Link>
                    <Link to="/shipments" onClick={() => setProfileOpen(false)}>Shipments</Link>
                  </div>
                )}
              </div>
            )}
            <NavLink to="/about">About Us</NavLink>
            <NavLink to="/contact">Contact Us</NavLink>
          </div>
          <div className="free-ship"><Icon d={paths.truck} size={16} /> Free Shipping on Orders ₹499+</div>
        </div>
      </nav>

      {open && (
        <div className="container mobile-menu">
          <Link to="/" onClick={() => setOpen(false)}>Home</Link>
          <Link to="/shop" onClick={() => setOpen(false)}>Shop</Link>
          <Link to="/lab-reports" onClick={() => setOpen(false)}>Lab Reports</Link>
          <Link className="mobile-sub" to="/report-status" onClick={() => setOpen(false)}>Report Status</Link>
          <Link className="mobile-sub" to="/lab-tests" onClick={() => setOpen(false)}>Lab Tests</Link>
          {LAB_PANELS.map((panel) => (
            <Link key={panel.id} className="mobile-sub" to={`/lab-tests?panel=${panel.id}`} onClick={() => setOpen(false)}>
              {panel.name}
            </Link>
          ))}
          {user && (
            <>
              <Link to="/profile" onClick={() => setOpen(false)}>Profile</Link>
              <Link className="mobile-sub" to="/orders" onClick={() => setOpen(false)}>Orders</Link>
              <Link className="mobile-sub" to="/shipments" onClick={() => setOpen(false)}>Shipments</Link>
            </>
          )}
          <Link to="/about" onClick={() => setOpen(false)}>About</Link>
          <Link to="/contact" onClick={() => setOpen(false)}>Contact</Link>
          {user ? (
            <button type="button" className="link-btn" onClick={handleLogout}>Logout</button>
          ) : (
            <Link to="/auth" onClick={() => setOpen(false)}>Sign In</Link>
          )}
        </div>
      )}
    </div>
  )
}
