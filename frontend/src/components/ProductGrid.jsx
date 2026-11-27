import ProductCard from './ProductCard'

export default function ProductGrid({ products, style }) {
  if (!products?.length) return null
  return (
    <div className="grid" style={style}>
      {products.map((product) => <ProductCard key={product.id} product={product} />)}
    </div>
  )
}
