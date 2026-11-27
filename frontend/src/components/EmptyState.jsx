import { Link } from 'react-router-dom'

export default function EmptyState({ title, message, actionTo, actionLabel, onAction, style }) {
  return (
    <div className="empty" style={style}>
      {title ? <h2>{title}</h2> : null}
      {message ? <p className={title ? 'muted' : undefined}>{message}</p> : null}
      {actionTo ? (
        <Link className="btn" to={actionTo} style={{ marginTop: 16 }}>{actionLabel}</Link>
      ) : null}
      {onAction ? (
        <button type="button" className="btn" style={{ marginTop: 16 }} onClick={onAction}>{actionLabel}</button>
      ) : null}
    </div>
  )
}
