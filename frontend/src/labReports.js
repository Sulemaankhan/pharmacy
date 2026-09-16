export const LAB_PANELS = [
  { id: 'THYROID', name: 'Thyroid', blurb: 'T3, T4, TSH and thyroid packages' },
  { id: 'KIDNEY', name: 'Kidney', blurb: 'Kidney function and creatinine tests' },
  { id: 'LIVER', name: 'Liver', blurb: 'Liver function and enzyme tests' },
  { id: 'HEART', name: 'Heart', blurb: 'Lipid profile and cardiac risk markers' },
  { id: 'DIABETES', name: 'Diabetes', blurb: 'HbA1c, blood sugar and screening packs' }
]

export function isLabProduct(product) {
  return Boolean(product?.labPanel)
}

export function isShopCategory(category) {
  const name = category?.name
  return name !== 'Lab Reports' && name !== 'Lab Tests'
}

export function panelLabel(id) {
  return LAB_PANELS.find((panel) => panel.id === id)?.name || id
}
