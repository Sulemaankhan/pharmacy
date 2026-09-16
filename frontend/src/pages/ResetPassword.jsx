import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api'
import { clientLog } from '../log'

export default function ResetPassword() {
  const [params] = useSearchParams()
  const token = (params.get('token') || '').trim()
  const navigate = useNavigate()
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')
    setMessage('')
    if (password.length < 6) {
      setError('Password must be at least 6 characters')
      return
    }
    if (password !== confirm) {
      setError('Passwords do not match')
      return
    }
    if (!token) {
      setError('This reset link is missing a token. Request a new one.')
      return
    }
    setBusy(true)
    try {
      clientLog('info', 'auth.reset.start', {})
      const result = await api('/api/auth/reset-password', {
        method: 'POST',
        body: JSON.stringify({ token, password }),
      })
      setMessage(result.message || 'Password updated.')
      clientLog('info', 'auth.reset.success', {})
      setTimeout(() => navigate('/auth'), 1200)
    } catch (err) {
      setError(err.message)
      clientLog('error', 'auth.reset.failed', { error: err.message })
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="container page">
      <form className="auth-box" onSubmit={submit}>
        <h2>Set a new password</h2>
        <p className="lead">Choose a new password for your Medicine Drugstore account.</p>
        {!token && <p className="error">This reset link is invalid. Request a new one from the sign-in page.</p>}
        <label>New password</label>
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={6} autoComplete="new-password" />
        <label>Confirm password</label>
        <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required minLength={6} autoComplete="new-password" />
        {error && <p className="error">{error}</p>}
        {message && <p className="toast-ok" style={{ marginTop: 10 }}>{message}</p>}
        <button className="btn full" style={{ marginTop: 16 }} disabled={busy || !token}>
          {busy ? 'Updating...' : 'Update password'}
        </button>
        <Link className="link-btn" to="/forgot-password" style={{ display: 'block', marginTop: 14 }}>Request a new reset link</Link>
      </form>
    </div>
  )
}
