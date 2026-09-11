import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useStore } from '../store'

export default function Auth() {
  const { login, register, user, logout } = useStore()
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const navigate = useNavigate()

  async function submit(e) {
    e.preventDefault()
    setError('')
    try {
      if (mode === 'login') await login(form)
      else await register(form)
      navigate('/')
    } catch (err) {
      setError(err.message)
    }
  }

  if (user) {
    return (
      <div className="container page">
        <div className="auth-box">
          <h2>Your account</h2>
          <p className="lead">Signed in as <b>{user.name}</b><br />{user.email}</p>
          <Link className="btn full" to="/orders">View order history</Link>
          <Link className="btn outline full" to="/shipments" style={{ marginTop: 10 }}>Track shipments</Link>
          <button type="button" className="btn outline full" style={{ marginTop: 10 }} onClick={() => { logout(); navigate('/auth') }}>Logout</button>
        </div>
      </div>
    )
  }

  return (
    <div className="container page">
      <form className="auth-box" onSubmit={submit}>
        <h2>{mode === 'login' ? 'Welcome back' : 'Create account'}</h2>
        <p className="lead">{mode === 'login' ? 'Sign in to manage cart, wishlist and orders.' : 'Join Medicine Drugstore for faster checkout.'}</p>
        {mode === 'register' && (
          <>
            <label>Name</label>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </>
        )}
        <label>Email</label>
        <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
        <label>Password</label>
        <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
        {error && <p className="error">{error}</p>}
        <button className="btn full" style={{ marginTop: 16 }}>{mode === 'login' ? 'Sign In' : 'Create account'}</button>
        <p className="demo-note">Demo login: demo@pharmacy.com / demo123</p>
        <button type="button" className="link-btn" style={{ marginTop: 14 }}
          onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
          {mode === 'login' ? 'Need an account? Register' : 'Have an account? Sign in'}
        </button>
      </form>
    </div>
  )
}
