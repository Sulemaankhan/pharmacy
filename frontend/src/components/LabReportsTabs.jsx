import { NavLink } from 'react-router-dom'

export default function LabReportsTabs() {
  return (
    <nav className="profile-tabs" aria-label="Lab Reports">
      <NavLink to="/lab-reports" end>Lab Reports</NavLink>
      <NavLink to="/report-status">Report Status</NavLink>
      <NavLink to="/lab-tests">Lab Tests</NavLink>
    </nav>
  )
}
