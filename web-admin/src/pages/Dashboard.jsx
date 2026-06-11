import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { Users, Package, TrendingUp, Activity, Clock, CheckCircle, XCircle, AlertCircle } from 'lucide-react'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, AreaChart, Area } from 'recharts'
import { analyticsApi } from '../api'
import './Dashboard.css'

export default function Dashboard() {
  const { user } = useAuth()
  const [stats, setStats] = useState({ users: 0, products: 0, procedures: 0, content: 0 })
  const [userActivityData, setUserActivityData] = useState([])
  const [recommendationActivityData, setRecommendationActivityData] = useState([])
  const [recentActivity, setRecentActivity] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let isMounted = true

    const loadDashboard = async () => {
      try {
        setLoading(true)
        setError('')
        const data = await analyticsApi.getDashboard()

        if (!isMounted) return

        setStats({
          users: Number(data?.stats?.users || 0),
          products: Number(data?.stats?.products || 0),
          procedures: Number(data?.stats?.procedures || 0),
          content: Number(data?.stats?.content || 0),
        })
        setUserActivityData(Array.isArray(data?.userActivityData) ? data.userActivityData : [])
        setRecommendationActivityData(
          Array.isArray(data?.recommendationActivityData)
            ? data.recommendationActivityData
            : Array.isArray(data?.activityData)
              ? data.activityData
              : []
        )
        setRecentActivity(Array.isArray(data?.recentActivity) ? data.recentActivity : [])
      } catch (fetchError) {
        if (!isMounted) return
        setError(fetchError.message || 'Не удалось загрузить данные дашборда')
      } finally {
        if (isMounted) {
          setLoading(false)
        }
      }
    }

    loadDashboard()

    return () => {
      isMounted = false
    }
  }, [])

  const statsView = [
    { label: 'Все пользователи', value: stats.users, icon: Users },
    { label: 'Продукты', value: stats.products, icon: Package },
    { label: 'Процедуры', value: stats.procedures, icon: TrendingUp },
    { label: 'Контент', value: stats.content, icon: Activity },
  ]

  const getStatusIcon = (status) => {
    switch (status) {
      case 'success': return <CheckCircle size={16} className="status-icon success" />
      case 'pending': return <AlertCircle size={16} className="status-icon pending" />
      case 'error': return <XCircle size={16} className="status-icon error" />
      default: return null
    }
  }

  const formatTooltipValue = (value, name) => [new Intl.NumberFormat('ru-RU').format(Number(value || 0)), name]

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h2>Добро пожаловать, {user?.name}!</h2>
        <p>Обзор системы Aura</p>
      </div>

      {/* Stats Grid */}
      <div className="stats-grid">
        {statsView.map((stat, index) => (
          <div key={index} className="stat-card glass-card">
            <div className="stat-header">
              <stat.icon className="stat-icon" />
            </div>
            <div className="stat-value">{new Intl.NumberFormat('ru-RU').format(stat.value)}</div>
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
            {loading ? (
              <p>Загрузка...</p>
            ) : userActivityData.length === 0 ? (
              <p>Данных пока нет</p>
            ) : (
              <ResponsiveContainer width="100%" height={280}>
                <AreaChart data={userActivityData}>
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
                    formatter={formatTooltipValue}
                    contentStyle={{
                      background: 'rgba(255,255,255,0.95)',
                      border: 'none',
                      borderRadius: '12px',
                      boxShadow: '0 4px 20px rgba(0,0,0,0.1)'
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="requests"
                    name="Сообщения пользователей"
                    stroke="#A7F3D0"
                    strokeWidth={3}
                    fillOpacity={1}
                    fill="url(#colorUsers)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        <div className="chart-card glass-card">
          <div className="card-header">
            <h3 className="card-title">Рекомендации</h3>
          </div>
          <div className="chart-container">
            {loading ? (
              <p>Загрузка...</p>
            ) : recommendationActivityData.length === 0 ? (
              <p>Данных пока нет</p>
            ) : (
              <ResponsiveContainer width="100%" height={280}>
                <LineChart data={recommendationActivityData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                  <XAxis dataKey="date" stroke="#9CA3AF" fontSize={12} />
                  <YAxis stroke="#9CA3AF" fontSize={12} />
                  <Tooltip
                    formatter={formatTooltipValue}
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
                    name="Запросы на рекомендации"
                    stroke="#FB6FE8"
                    strokeWidth={3}
                    dot={{ fill: '#FB6FE8', strokeWidth: 2, r: 4 }}
                    activeDot={{ r: 6, fill: '#FB6FE8' }}
                  />
                </LineChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>

      {/* Recent Activity */}
      <div className="activity-card glass-card">
        <div className="card-header">
          <h3 className="card-title">Последние действия</h3>
          <button className="btn btn-ghost">Показать все</button>
        </div>
        {error && <p>{error}</p>}
        <div className="activity-list">
          {!loading && recentActivity.length === 0 ? (
            <p>Действий пока нет</p>
          ) : (
            recentActivity.map((activity) => (
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
            ))
          )}
        </div>
      </div>
    </div>
  )
}
