export default function LazyImage({ src, alt = '', eager = false, ...rest }) {
  if (!src) return null
  return <img src={src} alt={alt} loading={eager ? 'eager' : 'lazy'} decoding="async" {...rest} />
}
