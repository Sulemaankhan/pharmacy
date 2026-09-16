import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useStore } from '../store'
import { Icon, paths } from '../icons'

export default function Footer() {
  const { user, logout } = useStore()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/auth')
  }

  return (
    <>
      <footer>
        <div className="container">
          <div className="newsletter">
            <div>
              <h3>Join our health newsletter</h3>
              <p>Offers, wellness tips and new arrivals — no spam.</p>
            </div>
            <form onSubmit={(e) => e.preventDefault()}>
              <input type="email" placeholder="Your email address" />
              <button type="submit">Subscribe</button>
            </form>
          </div>
          <div className="footer-grid">
            <div>
              <h3>Medicine Drugstore</h3>
              <p>Genuine medicines, medical devices and wellness products delivered to your door across India.</p>
            </div>
            <div>
              <h3>Shop</h3>
              <Link to="/shop">All products</Link>
              <Link to="/shop?deals=1">Deals of the day</Link>
              <Link to="/lab-reports">Lab Reports</Link>
              {user && <Link to="/wishlist">Wishlist</Link>}
            </div>
            <div>
              <h3>Help</h3>
              <Link to="/contact">Contact us</Link>
              <Link to="/about">About us</Link>
              <a href="tel:+917207290964">(+91) 720-729-0964</a>
            </div>
            <div>
              <h3>Account</h3>
              {user ? (
                <>
                  <Link to="/profile">Profile</Link>
                  <Link to="/lab-reports">Lab Reports</Link>
                  <Link to="/orders">Order history</Link>
                  <Link to="/shipments">Shipments</Link>
                  <Link to="/cart">Cart</Link>
                  <button type="button" className="link-btn footer-logout" onClick={handleLogout}>Logout</button>
                </>
              ) : (
                <Link to="/auth">Sign in</Link>
              )}
            </div>
          </div>
          <div className="copy">© {new Date().getFullYear()} Medicine Drugstore. All rights reserved.</div>
        </div>
      </footer>
      <nav className="bottom-nav">
        <NavLink to="/shop"><Icon d={paths.shop} size={18} />Shop</NavLink>
        <NavLink to="/lab-reports"><Icon d={paths.flask} size={18} />Labs</NavLink>
        <NavLink to="/" end className="home-pill"><Icon d={paths.home} size={18} />Home</NavLink>
        <NavLink to="/contact"><Icon d={paths.support} size={18} />Contact</NavLink>
        {user
          ? <NavLink to="/cart"><Icon d={paths.bag} size={18} />Checkout</NavLink>
          : <NavLink to="/auth"><Icon d={paths.bag} size={18} />Sign in</NavLink>}
      </nav>
    </>
  )
}
