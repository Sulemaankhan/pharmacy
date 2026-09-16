import { BrowserRouter, Route, Routes } from 'react-router-dom'
import Header from './components/Header'
import Footer from './components/Footer'
import Home from './pages/Home'
import Shop from './pages/Shop'
import LabReports from './pages/LabReports'
import HealthReport from './pages/HealthReport'
import ProductPage from './pages/Product'
import Cart from './pages/Cart'
import Checkout from './pages/Checkout'
import OrderSuccess from './pages/OrderSuccess'
import Orders from './pages/Orders'
import OrderDetail from './pages/OrderDetail'
import Auth from './pages/Auth'
import Profile from './pages/Profile'
import ForgotPassword from './pages/ForgotPassword'
import ResetPassword from './pages/ResetPassword'
import Wishlist from './pages/Wishlist'
import About from './pages/About'
import Contact from './pages/Contact'
import Shipments from './pages/Shipments'
import ShipmentDetail from './pages/ShipmentDetail'
import { StoreProvider } from './store'

export default function App() {
  return (
    <StoreProvider>
      <BrowserRouter>
        <Header />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/shop" element={<Shop />} />
          <Route path="/health-report" element={<HealthReport />} />
          <Route path="/lab-reports" element={<LabReports />} />
          <Route path="/product/:id" element={<ProductPage />} />
          <Route path="/cart" element={<Cart />} />
          <Route path="/checkout" element={<Checkout />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/orders/:orderNumber" element={<OrderDetail />} />
          <Route path="/shipments" element={<Shipments />} />
          <Route path="/shipments/:trackingNumber" element={<ShipmentDetail />} />
          <Route path="/order/:orderNumber" element={<OrderSuccess />} />
          <Route path="/wishlist" element={<Wishlist />} />
          <Route path="/auth" element={<Auth />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/about" element={<About />} />
          <Route path="/contact" element={<Contact />} />
        </Routes>
        <Footer />
      </BrowserRouter>
    </StoreProvider>
  )
}
