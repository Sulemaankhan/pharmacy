import { NavLink } from 'react-router-dom'

export default function ProfileTabs() {
  return (
    <nav className="profile-tabs" aria-label="Profile">
      <NavLink to="/profile" end>Profile</NavLink>
      <NavLink to="/profile#account">Account</NavLink>
      <NavLink to="/profile#pharmacy-account">Pharmacy</NavLink>
      <NavLink to="/orders">Orders</NavLink>
      <NavLink to="/shipments">Shipments</NavLink>
    </nav>
  )
}
