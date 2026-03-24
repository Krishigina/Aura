import { useAuth } from '../context/AuthContext'
import { Users, Package, TrendingUp, Activity, Clock, CheckCircle, XCircle, AlertCircle } from 'lucide-react'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, AreaChart, Area } from 'recharts'
import './Dashboard.css'

// Mock data
const activityData = [
  { date: '01 мар', users: 45, recommendations: 120 },
  { date: '05 мар', users: 52, recommendations: 145 },
  { date: '10 мар', users: 48, recommendations: 132 },
  { date: '15 мар', users: 65, recommendations: 180 },
  { date: '20 мар', users: 58, recommendations: 165 },
  { date: '23 мар', users: 72, recommendations: 198 },
]

const recentActivity = [
  { id: 1, user: 'user1@example.com', action: 'Получил рекомендации', time: '23.03.2026 14:30', status: 'success' },
  { id: 2, user: 'user2@example.com', action: 'Заполнил анкету', time: '23.03.2026 14:25', status: 'success' },
  { id: 3, user: 'user3@example.com', action: 'Использовал продукт', time: '23.03.2026 14:20', status: 'success' },
  { id: 4, user: 'user4@example.com', action: 'Прошел сканирование', time: '23.03.2026 14:15', status: 'pending' },
  { id: 5, user: 'user5@example.com', action: 'Ошибка API', time: '23.03.2026 14:10', status: 'error' },
]

const stats = [
  { label: 'Всего пользователей', value: '1,234', change: '+12%', positive: true, icon: Users },
  { label: 'Всего продуктов', value: '567', change: '+5%', positive: true, icon: Package },
  { label: 'Рекомендаций', value: '8,901', change: '+23%', positive: true, icon: TrendingUp },
  { label: 'Активных пользователей', value: '456', change: '+8%', positive: true, icon: Activity },
]

export default function Dashboard() {
  const { user } = useAuth()

  const getStatusIcon = (status) => {
    switch (status) {
      case 'success': return <CheckCircle size={16} className="status-icon success" />
      case 'pending': return <AlertCircle size={16} className="status-icon pending" />
      case 'error': return <XCircle size={16} className="status-icon error" />
      default: return null
    }
  }

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h2>Добро пожаловать, {user?.name}!</h2>
        <p>Обзор системы Aura</p>
      </div>

      {/* Stats Grid */}
      <div className="stats-grid">
        {stats.map((stat, index) => (
          <div key={index} className="stat-card glass-card">
            <div className="stat-header">
              <stat.icon className="stat-icon" />
              <span className={`stat-change ${stat.positive ? 'positive' : 'negative'}`}>
                {stat.change}
              </span>
            </div>
            <div className="stat-value">{stat.value}</div>
            <div className="stat-label">{stat.label}</div>
          </div>
        ))}
      </div>

      {/* Charts */}
      <div className="charts-grid">
        <div className="chart-card glass-card">
          <div className="card-header">
            <h3 className="card-title">Активность пользователей</h3>
          </div>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart data={activityData}>
                <defs>
                  <linearGradient id="colorUsers" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#A7F3D0" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#A7F3D0" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis dataKey="date" stroke="#9CA3AF" fontSize={12} />
                <YAxis stroke="#9CA3AF" fontSize={12} />
                <Tooltip 
                  contentStyle={{ 
                    background: 'rgba(255,255,255,0.95)', 
                    border: 'none', 
                    borderRadius: '12px',
                    boxShadow: '0 4px 20px rgba(0,0,0,0.1)'
                  }}
                />
                <Area 
                  type="monotone" 
                  dataKey="users" 
                  stroke="#A7F3D0" 
                  strokeWidth={3}
                  fillOpacity={1} 
                  fill="url(#colorUsers)" 
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="chart-card glass-card">
          <div className="card-header">
            <h3 className="card-title">Рекомендации</h3>
          </div>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={activityData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis dataKey="date" stroke="#9CA3AF" fontSize={12} />
                <YAxis stroke="#9CA3AF" fontSize={12} />
                <Tooltip 
                  contentStyle={{ 
                    background: 'rgba(255,255,255,0.95)', 
                    border: 'none', 
                    borderRadius: '12px',
                    boxShadow: '0 4px 20px rgba(0,0,0,0.1)'
                  }}
                />
                <Line 
                  type="monotone" 
                  dataKey="recommendations" 
                  stroke="#FB6FE8" 
                  strokeWidth={3}
                  dot={{ fill: '#FB6FE8', strokeWidth: 2, r: 4 }}
                  activeDot={{ r: 6, fill: '#FB6FE8' }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Recent Activity */}
      <div className="activity-card glass-card">
        <div className="card-header">
          <h3 className="card-title">Последние действия</h3>
          <button className="btn btn-ghost">Показать все</button>
        </div>
        <div className="activity-list">
          {recentActivity.map(activity => (
            <div key={activity.id} className="activity-item">
              {getStatusIcon(activity.status)}
              <div className="activity-content">
                <div className="activity-user">{activity.user}</div>
                <div className="activity-action">{activity.action}</div>
              </div>
              <div className="activity-time">
                <Clock size={14} />
                {activity.time}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
