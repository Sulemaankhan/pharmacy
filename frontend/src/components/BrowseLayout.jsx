export default function BrowseLayout({ sidebarTitle, sidebar, crumb, title, children }) {
  return (
    <div className="container page shop-layout">
      <aside className="side">
        <h3>{sidebarTitle}</h3>
        {sidebar}
      </aside>
      <div>
        <p className="crumb">{crumb}</p>
        <h2 className="page-title">{title}</h2>
        {children}
      </div>
    </div>
  )
}
