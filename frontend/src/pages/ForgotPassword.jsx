import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { clientLog } from '../log'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')
    setMessage('')
    setBusy(true)
    try {
      clientLog('info', 'auth.forgot.start', { email })
      const result = await api('/api/auth/forgot-password', {
        method: 'POST',
        body: JSON.stringify({ email }),
      })
      setMessage(result.message || 'If that email is registered, a password reset link was sent.')
      clientLog('info', 'auth.forgot.sent', { email })
    } catch (err) {
      setError(err.message)
      clientLog('error', 'auth.forgot.failed', { email, error: err.message })
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="container page">
      <form className="auth-box" onSubmit={submit}>
        <h2>Forgot password</h2>
        <p className="lead">Enter the email on your account. If it is registered, we will email a reset link that expires in 30 minutes.</p>
        <label>Email</label>
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
        {error && <p className="error">{error}</p>}
        {message && <p className="toast-ok" style={{ marginTop: 10 }}>{message}</p>}
        <button className="btn full" style={{ marginTop: 16 }} disabled={busy}>
          {busy ? 'Sending...' : 'Send reset link'}
        </button>
        <Link className="link-btn" to="/auth" style={{ display: 'block', marginTop: 14 }}>Back to sign in</Link>
      </form>
    </div>
  )
}
