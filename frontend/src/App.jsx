import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import Header from './components/Header'
import Footer from './components/Footer'
import PageFallback from './components/PageFallback'
import Home from './pages/Home'
import { StoreProvider } from './store'

const Shop = lazy(() => import('./pages/Shop'))
const LabReports = lazy(() => import('./pages/LabReports'))
const LabReportsHome = lazy(() => import('./pages/LabReportsHome'))
const HealthReport = lazy(() => import('./pages/HealthReport'))
const ProductPage = lazy(() => import('./pages/Product'))
const Cart = lazy(() => import('./pages/Cart'))
const Checkout = lazy(() => import('./pages/Checkout'))
const OrderSuccess = lazy(() => import('./pages/OrderSuccess'))
const Orders = lazy(() => import('./pages/Orders'))
const OrderDetail = lazy(() => import('./pages/OrderDetail'))
const Auth = lazy(() => import('./pages/Auth'))
const Profile = lazy(() => import('./pages/Profile'))
const ForgotPassword = lazy(() => import('./pages/ForgotPassword'))
const ResetPassword = lazy(() => import('./pages/ResetPassword'))
const Wishlist = lazy(() => import('./pages/Wishlist'))
const About = lazy(() => import('./pages/About'))
const Contact = lazy(() => import('./pages/Contact'))
const Shipments = lazy(() => import('./pages/Shipments'))
const ShipmentDetail = lazy(() => import('./pages/ShipmentDetail'))

export default function App() {
  return (
    <StoreProvider>
      <BrowserRouter>
        <Header />
        <Suspense fallback={<PageFallback />}>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/shop" element={<Shop />} />
            <Route path="/lab-reports" element={<LabReportsHome />} />
            <Route path="/report-status" element={<HealthReport />} />
            <Route path="/health-report" element={<Navigate to="/report-status" replace />} />
            <Route path="/lab-tests" element={<LabReports />} />
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
        </Suspense>
        <Footer />
      </BrowserRouter>
    </StoreProvider>
  )
}
