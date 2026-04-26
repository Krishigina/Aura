import { useEffect, useState } from 'react'
import { Download, FileText, CheckCircle, Users, Package } from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import { reportsApi, usersApi, productsApi } from '../api'
import './Reports.css'

function generateCSV(data, reportName) {
  const headers = Object.keys(data)
  const rows = Object.values(data).map(v => (typeof v === 'object' ? JSON.stringify(v) : v))
  return `${reportName}\n${headers.join(';')}\n${rows.join(';')}`
}

function generatePDFContent(data, reportName) {
  return `═══════════════════════════════════════════
${reportName.toUpperCase()}
Aura Cosmetics System
Дата: ${new Date().toLocaleDateString('ru-RU')}
═══════════════════════════════════════════

СТАТИСТИКА:
${Object.entries(data).map(([key, value]) => `• ${key}: ${typeof value === 'object' ? JSON.stringify(value) : value}`).join('\n')}

═══════════════════════════════════════════
Сгенерировано системой Aura
`
}

function downloadReport(report) {
  let content
  let filename
  let mimeType

  if (report.format === 'CSV') {
    content = generateCSV(report.data, report.name)
    filename = `${report.name.replace(/\s+/g, '_')}_${new Date().toISOString().split('T')[0]}.csv`
    mimeType = 'text/csv;charset=utf-8'
  } else if (report.format === 'PDF') {
    content = generatePDFContent(report.data, report.name)
    filename = `${report.name.replace(/\s+/g, '_')}_${new Date().toISOString().split('T')[0]}.txt`
    mimeType = 'text/plain;charset=utf-8'
  } else {
    content = generateCSV(report.data, report.name)
    filename = `${report.name.replace(/\s+/g, '_')}_${new Date().toISOString().split('T')[0]}.csv`
    mimeType = 'text/csv;charset=utf-8'
  }

  const blob = new Blob([content], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

export default function Reports() {
  const [monthlyData, setMonthlyData] = useState([])
  const [compatibilityData, setCompatibilityData] = useState([])
  const [reports, setReports] = useState([])
  const [downloaded, setDownloaded] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let isMounted = true

    const loadSummary = async () => {
      try {
        setLoading(true)
        setError('')
        const data = await reportsApi.getSummary()

        if (!isMounted) return

        setMonthlyData(Array.isArray(data?.monthlyData) ? data.monthlyData : [])
        setCompatibilityData(Array.isArray(data?.compatibilityData) ? data.compatibilityData : [])
        setReports(Array.isArray(data?.reports) ? data.reports : [])
      } catch (fetchError) {
        if (!isMounted) return
        setError(fetchError.message || 'Не удалось загрузить отчеты')
      } finally {
        if (isMounted) {
          setLoading(false)
        }
      }
    }

    loadSummary()

    return () => {
      isMounted = false
    }
  }, [])

  const handleDownload = (report) => {
    downloadReport(report)
    setDownloaded(prev => ({ ...prev, [report.id]: true }))
    setTimeout(() => {
      setDownloaded(prev => ({ ...prev, [report.id]: false }))
    }, 2000)
  }

  const handleExportUsers = async () => {
    try {
      const users = await usersApi.getAll()
      const report = {
        id: 'users-export',
        name: 'Пользователи',
        format: 'CSV',
        data: {
          totalUsers: Array.isArray(users) ? users.length : 0,
          emails: Array.isArray(users) ? users.map((item) => item.email).filter(Boolean).join(', ') : '',
        },
      }
      handleDownload(report)
    } catch (downloadError) {
      setError(downloadError.message || 'Не удалось выгрузить пользователей')
    }
  }

  const handleExportProducts = async () => {
    try {
      const products = await productsApi.getAll()
      const report = {
        id: 'products-export',
        name: 'Продукты',
        format: 'CSV',
        data: {
          totalProducts: Array.isArray(products) ? products.length : 0,
          names: Array.isArray(products) ? products.map((item) => item.name).filter(Boolean).join(', ') : '',
        },
      }
      handleDownload(report)
    } catch (downloadError) {
      setError(downloadError.message || 'Не удалось выгрузить продукты')
    }
  }

  return (
    <div className="reports-page">
      <div className="page-header">
        <div>
          <h2>Отчеты</h2>
          <p>Аналитика и экспорт данных</p>
        </div>
      </div>
      {error && <p>{error}</p>}

      <div className="charts-grid">
        <div className="chart-card glass-card">
          <h3>Рост пользователей</h3>
          <div className="chart-container">
            {loading ? (
              <p>Загрузка...</p>
            ) : monthlyData.length === 0 ? (
              <p>Данных пока нет</p>
            ) : (
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={monthlyData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                  <XAxis dataKey="month" stroke="#9CA3AF" fontSize={12} />
                  <YAxis stroke="#9CA3AF" fontSize={12} />
                  <Tooltip contentStyle={{ background: 'rgba(255,255,255,0.95)', border: 'none', borderRadius: '12px' }} />
                  <Bar dataKey="users" fill="#A7F3D0" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        <div className="chart-card glass-card">
          <h3>Совместимость продуктов</h3>
          <div className="chart-container">
            {loading ? (
              <p>Загрузка...</p>
            ) : compatibilityData.length === 0 ? (
              <p>Данных пока нет</p>
            ) : (
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie data={compatibilityData} cx="50%" cy="50%" innerRadius={60} outerRadius={100} dataKey="value" label={({ name, value }) => `${name}: ${value}`}>
                    {compatibilityData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>

      <div className="reports-section glass-card">
        <h3>Доступные отчеты</h3>
        <div className="reports-list">
          {!loading && reports.length === 0 ? (
            <p>Отчеты пока недоступны</p>
          ) : (
            reports.map(report => (
              <div key={report.id} className="report-item">
                <div className="report-icon"><FileText size={24} /></div>
                <div className="report-info">
                  <h4>{report.name}</h4>
                  <p>{report.description}</p>
                  <span className="report-meta">{report.format} • {report.date}</span>
                </div>
                <button
                  className={`btn ${downloaded[report.id] ? 'btn-primary' : 'btn-secondary'}`}
                  onClick={() => handleDownload(report)}
                >
                  {downloaded[report.id] ? (
                    <><CheckCircle size={16} />Скачано</>
                  ) : (
                    <><Download size={16} />Скачать</>
                  )}
                </button>
              </div>
            ))
          )}
        </div>
      </div>

      <div className="reports-section glass-card">
        <h3>Экспорт данных</h3>
        <div className="export-options">
          <div className="export-card">
            <div className="export-icon"><Users size={24} /></div>
            <div className="export-info">
              <h4>Все пользователи</h4>
              <p>Список всех пользователей с данными анализа</p>
            </div>
            <button className="btn btn-secondary" onClick={handleExportUsers}>
              <Download size={16} />CSV
            </button>
          </div>
          <div className="export-card">
            <div className="export-icon"><Package size={24} /></div>
            <div className="export-info">
              <h4>Все продукты</h4>
              <p>Каталог продуктов с совместимостью</p>
            </div>
            <button className="btn btn-secondary" onClick={handleExportProducts}>
              <Download size={16} />CSV
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
