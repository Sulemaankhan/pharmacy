import EmptyState from './EmptyState'

export default function RequireSignIn({ user, title, message, crumb, children }) {
  if (user) return children
  return (
    <div className="container page">
      {crumb ? <p className="crumb">{crumb}</p> : null}
      <EmptyState title={title} message={message} actionTo="/auth" actionLabel="Sign in" />
    </div>
  )
}
