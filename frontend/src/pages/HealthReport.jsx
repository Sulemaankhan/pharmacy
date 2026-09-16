import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, apiUpload, asList } from '../api'
import { useStore } from '../store'

const PANELS = [
  { id: 'THYROID', name: 'Thyroid' },
  { id: 'KIDNEY', name: 'Kidney' },
  { id: 'LIVER', name: 'Liver' },
  { id: 'HEART', name: 'Heart' },
  { id: 'DIABETES', name: 'Diabetes' }
]

function statusLabel(status) {
  if (status === 'NORMAL') return 'Typical'
  if (status === 'ATTENTION') return 'Watch'
  if (status === 'HIGH_RISK') return 'Needs review'
  if (status === 'HIGH') return 'High'
  if (status === 'LOW') return 'Low'
  if (status === 'NO_DATA') return 'No data'
  return status || '—'
}

function statusClass(status) {
  if (status === 'NORMAL') return 'ok'
  if (status === 'ATTENTION' || status === 'HIGH' || status === 'LOW') return 'warn'
  if (status === 'HIGH_RISK') return 'risk'
  return 'muted'
}

function formatWhen(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

function point(cx, cy, r, index, total, value = 1) {
  const angle = (-Math.PI / 2) + (index * 2 * Math.PI) / total
  return [cx + Math.cos(angle) * r * value, cy + Math.sin(angle) * r * value]
}

function RadarChart({ panels }) {
  const cx = 140
  const cy = 140
  const r = 92
  const rings = [0.25, 0.5, 0.75, 1]
  const grid = rings.map((scale) =>
    PANELS.map((_, i) => point(cx, cy, r, i, PANELS.length, scale).join(',')).join(' ')
  )
  const values = PANELS.map((panel) => {
    const found = panels?.find((item) => item.id === panel.id)
    return found?.score == null ? 0.12 : Math.max(0.12, found.score / 100)
  })
  const shape = values.map((value, i) => point(cx, cy, r, i, PANELS.length, value).join(',')).join(' ')
  return (
    <svg className="health-radar" viewBox="0 0 280 280" role="img" aria-label="Health panel scores">
      {grid.map((pts, i) => <polygon key={i} points={pts} className="radar-ring" />)}
      {PANELS.map((panel, i) => {
        const [x2, y2] = point(cx, cy, r, i, PANELS.length, 1)
        const [lx, ly] = point(cx, cy, r + 22, i, PANELS.length, 1)
        return (
          <g key={panel.id}>
            <line x1={cx} y1={cy} x2={x2} y2={y2} className="radar-axis" />
            <text x={lx} y={ly} textAnchor="middle" dominantBaseline="middle">{panel.name}</text>
          </g>
        )
      })}
      <polygon points={shape} className="radar-fill" />
    </svg>
  )
}

function ScoreRing({ score, status }) {
  const value = score == null ? 0 : score
  const r = 54
  const c = 2 * Math.PI * r
  const dash = (value / 100) * c
  return (
    <div className={`health-score ${statusClass(status)}`}>
      <svg viewBox="0 0 140 140" aria-hidden="true">
        <circle cx="70" cy="70" r={r} className="score-track" />
        <circle cx="70" cy="70" r={r} className="score-value" strokeDasharray={`${dash} ${c}`} transform="rotate(-90 70 70)" />
      </svg>
      <div>
        <b>{score == null ? '—' : score}</b>
        <small>{statusLabel(status)}</small>
      </div>
    </div>
  )
}

function MetricBar({ metric }) {
  const min = Number(metric.minNormal ?? 0)
  const max = Number(metric.maxNormal ?? 1)
  const value = Number(metric.value ?? 0)
  const span = Math.max(max - min, 0.001)
  const low = min - span * 0.35
  const high = max + span * 0.35
  const full = high - low
  const left = ((min - low) / full) * 100
  const width = ((max - min) / full) * 100
  const marker = Math.min(100, Math.max(0, ((value - low) / full) * 100))
  return (
    <article className={`metric-card ${statusClass(metric.status)}`}>
      <div className="metric-head">
        <div>
          <b>{metric.name}</b>
          <small>{metric.panel}</small>
        </div>
        <span className={`status-pill ${statusClass(metric.status)}`}>{statusLabel(metric.status)}</span>
      </div>
      <div className="metric-track">
        <span className="metric-range" style={{ left: `${left}%`, width: `${width}%` }} />
        <span className="metric-marker" style={{ left: `${marker}%` }} />
      </div>
      <div className="metric-foot">
        <strong>{metric.value} {metric.unit}</strong>
        <span>Typical {metric.minNormal}–{metric.maxNormal} {metric.unit}</span>
      </div>
    </article>
  )
}

function HistoryBars({ history }) {
  const rows = (history || []).slice(0, 6).reverse()
  if (rows.length === 0) return <p className="muted">Upload a report to start a trend.</p>
  return (
    <div className="history-bars">
      {rows.map((row) => (
        <div key={row.reportId} className="history-col">
          <div className="history-bar-wrap">
            <span style={{ height: `${Math.max(8, row.overallScore || 0)}%` }} className={statusClass(row.overallStatus)} />
          </div>
          <small>{row.overallScore ?? '—'}</small>
          <em>{new Date(row.uploadedAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' })}</em>
        </div>
      ))}
    </div>
  )
}

export default function HealthReport() {
  const { user } = useStore()
  const [status, setStatus] = useState(null)
  const [reports, setReports] = useState([])
  const [selected, setSelected] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)

  async function load() {
    const [statusRes, listRes] = await Promise.all([
      api('/api/health-reports/status'),
      api('/api/health-reports'),
    ])
    setStatus(statusRes)
    setReports(asList(listRes))
    if (statusRes?.latestReportId) {
      const detail = await api(`/api/health-reports/${statusRes.latestReportId}`)
      setSelected(detail)
    } else {
      setSelected(null)
    }
  }

  useEffect(() => {
    if (!user) {
      setLoading(false)
      return
    }
    setLoading(true)
    load().catch((err) => setError(err.message)).finally(() => setLoading(false))
  }, [user])

  async function onFile(file) {
    if (!file) return
    setError('')
    setBusy(true)
    try {
      const data = await apiUpload('/api/health-reports', file)
      setStatus(data.status)
      setSelected(data.report)
      setReports((prev) => [data.report, ...prev.filter((item) => item.id !== data.report.id)])
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function openReport(id) {
    setError('')
    try {
      const detail = await api(`/api/health-reports/${id}`)
      setSelected(detail)
    } catch (err) {
      setError(err.message)
    }
  }

  const panels = status?.panels || []
  const metrics = selected?.metrics?.length ? selected.metrics : status?.metrics || []
  const highlights = selected?.highlights || []
  const filled = useMemo(() => panels.filter((p) => p.score != null).length, [panels])

  if (!user) {
    return (
      <div className="container page">
        <p className="crumb">Home / Health Report</p>
        <div className="empty">
          <h2>Sign in to upload a health report</h2>
          <p className="muted">PDF lab reports are read with RAG, then your profile status is drawn as charts.</p>
          <Link className="btn" to="/auth" style={{ marginTop: 16 }}>Sign in</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="container page">
      <p className="crumb">Home / Health Report</p>
      <div className="page-head">
        <div>
          <h2 className="page-title">Health Report</h2>
          <p className="muted">Upload a PDF lab report. The app reads it with RAG, writes a summary, and graphs your health profile.</p>
        </div>
      </div>

      <label className={`health-upload${busy ? ' busy' : ''}`}>
        <input
          type="file"
          accept="application/pdf,.pdf"
          disabled={busy}
          onChange={(e) => {
            const file = e.target.files?.[0]
            e.target.value = ''
            onFile(file)
          }}
        />
        <b>{busy ? 'Reading PDF with RAG…' : 'Upload the report'}</b>
        <span>PDF up to 8 MB. Text-based lab reports work best (not scanned photos).</span>
      </label>
      {error && <p className="error">{error}</p>}
      {loading && <p className="muted">Loading health profile...</p>}

      <section className="health-layout">
        <div className="health-visuals">
          <div className="health-card">
            <h3>Profile status</h3>
            <ScoreRing score={status?.overallScore} status={status?.overallStatus} />
            <p className="muted" style={{ textAlign: 'center' }}>
              {filled} of 5 panels scored from the latest report
            </p>
          </div>
          <div className="health-card health-radar-wrap">
            <h3>Body systems</h3>
            <RadarChart panels={panels} />
          </div>
          <div className="health-card">
            <h3>Trend</h3>
            <HistoryBars history={status?.history} />
          </div>
        </div>

        <div className="health-copy">
          <div className="health-card">
            <h3>RAG summary</h3>
            <p className="lead">{selected?.summary || status?.summary || 'Upload a PDF to generate a summary from retrieved passages.'}</p>
            <div className="panel-pills">
              {panels.map((panel) => (
                <span key={panel.id} className={`status-pill ${statusClass(panel.status)}`}>
                  {panel.name}: {panel.score ?? '—'} · {statusLabel(panel.status)}
                </span>
              ))}
            </div>
          </div>

          {highlights.length > 0 && (
            <div className="health-card">
              <h3>Retrieved passages</h3>
              <div className="rag-hits">
                {highlights.map((hit, i) => (
                  <blockquote key={`${hit.panel}-${i}`}>
                    <b>{hit.panel}</b>
                    <p>{hit.snippet}</p>
                  </blockquote>
                ))}
              </div>
            </div>
          )}

          {metrics.length > 0 && (
            <div className="health-card">
              <h3>Extracted values</h3>
              <div className="metric-grid">
                {metrics.map((metric) => <MetricBar key={metric.id || metric.code} metric={metric} />)}
              </div>
            </div>
          )}

          <div className="health-card">
            <h3>Uploaded reports</h3>
            {reports.length === 0 && <p className="muted">No reports yet.</p>}
            <div className="report-list">
              {reports.map((row) => (
                <button
                  key={row.id}
                  type="button"
                  className={selected?.id === row.id ? 'active' : ''}
                  onClick={() => openReport(row.id)}
                >
                  <b>{row.fileName}</b>
                  <span>{formatWhen(row.uploadedAt)} · {row.overallScore ?? '—'} / 100 · {statusLabel(row.overallStatus)}</span>
                </button>
              ))}
            </div>
          </div>
          <p className="muted">Automated reading only. It is not a diagnosis — share unusual values with your doctor.</p>
        </div>
      </section>
    </div>
  )
}
