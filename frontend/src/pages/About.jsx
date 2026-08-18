export default function About() {
  return (
    <div className="container page">
      <p className="crumb">Home / About Us</p>
      <div className="panel wide" style={{ padding: 32 }}>
        <h2>About Medicine Drugstore</h2>
        <p className="lead" style={{ maxWidth: 720 }}>
          We are an online pharmacy bringing genuine medical products, devices and wellness essentials to your home.
          We partner with licensed suppliers and support customers 24 hours a day.
        </p>
        <div className="features" style={{ padding: 0, marginTop: 12 }}>
          <div className="feature"><div><h3>Licensed supply</h3><p>Every product is sourced from verified distributors.</p></div></div>
          <div className="feature"><div><h3>Fast delivery</h3><p>Free shipping on orders ₹499 and above across India.</p></div></div>
          <div className="feature"><div><h3>Secure checkout</h3><p>Your payments and personal data stay protected.</p></div></div>
          <div className="feature"><div><h3>Pharmacist support</h3><p>Talk to us any time for product guidance.</p></div></div>
        </div>
      </div>
    </div>
  )
}
