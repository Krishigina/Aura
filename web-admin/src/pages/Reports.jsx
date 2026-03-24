import { useState } from 'react'
import { Download, FileText, CheckCircle } from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import './Reports.css'

const monthlyData = [
  { month: 'Янв', users: 120, products: 45 },
  { month: 'Фев', users: 145, products: 52 },
  { month: 'Мар', users: 168, products: 68 },
  { month: 'Апр', users: 190, products: 75 },
  { month: 'Май', users: 210, products: 82 },
  { month: 'Июн', users: 245, products: 95 },
]

const compatibilityData = [
  { name: 'Высокая (>90%)', value: 450, color: '#A7F3D0' },
  { name: 'Средняя (70-90%)', value: 280, color: '#FB6FE8' },
  { name: 'Низкая (<70%)', value: 120, color: '#E0C3FC' },
]

const reports = [
  { id: 1, name: 'Отчет по пользователям', description: 'Статистика регистраций и активности', format: 'PDF', date: '23.03.2026', data: { totalUsers: 1245, activeUsers: 892, newThisMonth: 156 } },
  { id: 2, name: 'Отчет по продуктам', description: 'Популярность и совместимость', format: 'Excel', date: '23.03.2026', data: { totalProducts: 89, highCompatibility: 67, avgCompatibility: 82 } },
  { id: 3, name: 'Отчет по рекомендациям', description: 'AI рекомендации и их эффективность', format: 'PDF', date: '22.03.2026', data: { totalRecommendations: 4521, acceptedRecommendations: 3890, satisfactionRate: 86 } },
]

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
  const [downloaded, setDownloaded] = useState({})

  const handleDownload = (report) => {
    downloadReport(report)
    setDownloaded(prev => ({ ...prev, [report.id]: true }))
    setTimeout(() => {
      setDownloaded(prev => ({ ...prev, [report.id]: false }))
    }, 2000)
  }

  return (
    <div className="reports-page">
      <div className="page-header">
        <div>
          <h2>Отчеты</h2>
          <p>Аналитика и экспорт данных</p>
        </div>
      </div>

      <div className="charts-grid">
        <div className="chart-card glass-card">
          <h3>Рост пользователей</h3>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={monthlyData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis dataKey="month" stroke="#9CA3AF" fontSize={12} />
                <YAxis stroke="#9CA3AF" fontSize={12} />
                <Tooltip contentStyle={{ background: 'rgba(255,255,255,0.95)', border: 'none', borderRadius: '12px' }} />
                <Bar dataKey="users" fill="#A7F3D0" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="chart-card glass-card">
          <h3>Совместимость продуктов</h3>
          <div className="chart-container">
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
          </div>
        </div>
      </div>

      <div className="reports-section glass-card">
        <h3>Доступные отчеты</h3>
        <div className="reports-list">
          {reports.map(report => (
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
          ))}
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
            <button className="btn btn-secondary" onClick={() => handleDownload({ id: 'users', name: 'Пользователи', format: 'CSV', data: { users: 'Полный список' } })}>
              <Download size={16} />CSV
            </button>
          </div>
          <div className="export-card">
            <div className="export-icon"><Package size={24} /></div>
            <div className="export-info">
              <h4>Все продукты</h4>
              <p>Каталог продуктов с совместимостью</p>
            </div>
            <button className="btn btn-secondary" onClick={() => handleDownload({ id: 'products', name: 'Продукты', format: 'CSV', data: { products: 'Каталог' } })}>
              <Download size={16} />CSV
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

function Users({ size }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width={size || 24} height={size || 24} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path>
      <circle cx="9" cy="7" r="4"></circle>
      <path d="M22 21v-2a4 4 0 0 0-3-3.87"></path>
      <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
    </svg>
  )
}

function Package({ size }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width={size || 24} height={size || 24} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="m7.5 4.27 9 5.15"></path>
      <path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"></path>
      <path d="m3.3 7 8.7 5 8.7-5"></path>
      <path d="M12 22V12"></path>
    </svg>
  )
}
