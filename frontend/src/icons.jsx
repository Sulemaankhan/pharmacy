export function Icon({ d, size = 20 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d={d} />
    </svg>
  )
}

export function LogoMark() {
  return (
    <svg className="logo-svg" viewBox="0 0 48 48" aria-hidden="true">
      <circle cx="24" cy="24" r="23" fill="#d7eedf" />
      <path d="M16 22c0-6 4.5-10 10.5-10 5 0 8.5 3.2 9.5 7.5" fill="none" stroke="#0e6b36" strokeWidth="2.4" strokeLinecap="round" />
      <circle cx="36" cy="20" r="2.4" fill="#9b1d4a" />
      <rect x="10" y="20" width="20" height="11" rx="5.5" fill="#0e6b36" />
      <rect x="19" y="20" width="11" height="11" rx="5.5" fill="#9b1d4a" />
      <circle cx="16" cy="25.5" r="1.6" fill="#fff" />
    </svg>
  )
}

export const paths = {
  search: 'M11 19a8 8 0 1 1 0-16 8 8 0 0 1 0 16Zm10 2-5.2-5.2',
  heart: 'M19.5 12.6 12 20l-7.5-7.4a4.5 4.5 0 1 1 6.4-6.3L12 7.7l1.1-1.4a4.5 4.5 0 1 1 6.4 6.3Z',
  bag: 'M6 8h12l-.8 12.2a2 2 0 0 1-2 1.8H8.8a2 2 0 0 1-2-1.8L6 8Zm3 0V6a3 3 0 0 1 6 0v2',
  compare: 'M7 7h10M7 12h6M7 17h10M17 4v6M13 9v6',
  menu: 'M4 7h16M4 12h16M4 17h16',
  phone: 'M6.5 4.5h3l1.5 3.5-2 1.5a12 12 0 0 0 5.5 5.5l1.5-2 3.5 1.5v3A15 15 0 0 1 6.5 4.5Z',
  home: 'M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-5v-6H10v6H5a1 1 0 0 1-1-1v-9.5Z',
  shop: 'M4 7h16l-1.4 12.1A2 2 0 0 1 16.6 21H7.4a2 2 0 0 1-2-1.9L4 7Zm4 0V5a4 4 0 0 1 8 0v2',
  flame: 'M12 3s6 5 6 10a6 6 0 1 1-12 0c0-2 2-4.5 3-6 2 3 3 3 3 3s-1-4 0-7Z',
  check: 'M20 6.5 9.5 17 4 11.5',
  percent: 'M19 5 5 19M8 8.5A1.5 1.5 0 1 1 8 5.5 1.5 1.5 0 0 1 8 8.5ZM16 18.5A1.5 1.5 0 1 1 16 15.5 1.5 1.5 0 0 1 16 18.5Z',
  card: 'M3 8h18M3 10.5V17a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-6.5M3 8V7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v1',
  support: 'M12 21a9 9 0 1 0-9-9c0 3.2 1.6 5.2 3 6.5V21l3.5-2A9 9 0 0 0 12 21Z',
  truck: 'M3 7h11v9H3V7Zm11 3h4l3 3v3h-7v-6ZM6 19a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Zm10 0a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z',
  user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm7 9a7 7 0 1 0-14 0',
  flask: 'M9 3h6M10 3v5.2L6.2 16.5A4 4 0 0 0 9.8 22h4.4a4 4 0 0 0 3.6-5.5L14 8.2V3',
  report: 'M7 3h8l4 4v14H7V3Zm8 0v4h4M9 13h8M9 17h5M9 9h3'
}
