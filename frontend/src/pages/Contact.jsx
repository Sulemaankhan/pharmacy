export default function Contact() {
  return (
    <div className="container page">
      <p className="crumb">Home / Contact Us</p>
      <div className="form-box">
        <h2>Contact Us</h2>
        <p className="lead">Phone: (+91) 720-729-0964<br />We usually reply within a few hours.</p>
        <label>Name</label><input />
        <label>Email</label><input type="email" />
        <label>Message</label><textarea rows="4" />
        <button className="btn full" style={{ marginTop: 16 }}>Send message</button>
      </div>
    </div>
  )
}
