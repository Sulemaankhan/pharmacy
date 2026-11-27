export default function CatalogStatus({ loading, error, loadingLabel = 'Loading products...', onRetry }) {
  if (!loading && !error) return null
  return (
    <>
      {loading ? <p className="muted">{loadingLabel}</p> : null}
      {error ? (
        <p className="error">
          {error} {onRetry ? <button type="button" className="link-btn" onClick={() => onRetry()}>Retry</button> : null}
        </p>
      ) : null}
    </>
  )
}
