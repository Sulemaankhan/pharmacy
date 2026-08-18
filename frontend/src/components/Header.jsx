import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useStore } from '../store'
import { formatMoney } from '../money'
import { Icon, LogoMark, paths } from '../icons'

export default function Header() {
  const { user, cartCount, cartTotal, wishCount, query, setQuery, logout, categories } = useStore()
  const [open, setOpen] = useState(false)
  const [catsOpen, setCatsOpen] = useState(false)
  const navigate = useNavigate()

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
                  <span>Hi, {user.name}</span>
                  <Link to="/orders">Orders</Link>
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
                {categories.map((c) => (
                  <Link key={c.id} to={`/shop?category=${c.id}`} onClick={() => setCatsOpen(false)}>{c.name}</Link>
                ))}
              </div>
            )}
          </div>
          <div className="nav-links">
            <NavLink to="/" end>Home</NavLink>
            <NavLink to="/shop">Shop</NavLink>
            <NavLink to="/orders">Orders</NavLink>
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
          <Link to="/orders" onClick={() => setOpen(false)}>Orders</Link>
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
