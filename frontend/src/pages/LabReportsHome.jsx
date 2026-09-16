import { Link } from 'react-router-dom'
import LabReportsTabs from '../components/LabReportsTabs'

export default function LabReportsHome() {
  return (
    <div className="container page">
      <p className="crumb">Home / Lab Reports</p>
      <div className="page-head">
        <div>
          <h2 className="page-title">Lab Reports</h2>
          <LabReportsTabs />
          <p className="muted">Upload a report to see your status, or book thyroid, kidney, liver, heart and diabetes tests.</p>
        </div>
      </div>
      <div className="lab-hub">
        <Link className="health-card lab-hub-card" to="/report-status">
          <b>Report Status</b>
          <span>Upload a PDF lab report. RAG reads it and graphs your profile status.</span>
        </Link>
        <Link className="health-card lab-hub-card" to="/lab-tests">
          <b>Lab Tests</b>
          <span>Book Thyroid, Kidney, Liver, Heart and Diabetes tests and add them to cart.</span>
        </Link>
      </div>
    </div>
  )
}
