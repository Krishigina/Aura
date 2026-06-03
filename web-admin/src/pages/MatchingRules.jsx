import { useEffect, useState } from 'react'
import { matchingApi } from '../api'

const ALL_STATUSES_VALUE = 'all'
const STATUSES = [
  { value: ALL_STATUSES_VALUE, label: 'Все' },
  { value: 'draft', label: 'Черновики' },
  { value: 'confirmed', label: 'Подтвержденные' },
  { value: 'rejected', label: 'Отклоненные' },
  { value: 'archived', label: 'Архив' },
]

const getStatusLabel = (value) => STATUSES.find((item) => item.value === value)?.label || value

export default function MatchingRules() {
  const [status, setStatus] = useState(ALL_STATUSES_VALUE)
  const [items, setItems] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const loadRules = async () => {
    setLoading(true)
    setError('')
    try {
      const data = await matchingApi.listRules(status === ALL_STATUSES_VALUE ? null : status)
      setItems(data.items || [])
    } catch (err) {
      setError(err.message || 'Не удалось загрузить правила')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadRules()
  }, [status])

  const updateStatus = async (id, nextStatus) => {
    await matchingApi.updateRule(id, { status: nextStatus })
    await loadRules()
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Правила подбора</h1>
          <p>Проверяйте черновые правила перед тем, как они начнут влиять на рекомендации.</p>
        </div>
        <select className="input" value={status} onChange={(event) => setStatus(event.target.value)}>
          {STATUSES.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
        </select>
      </div>

      {error && <div className="glass-card error-message">{error}</div>}
      {loading && <div className="glass-card">Загрузка...</div>}

      <div className="table-card glass-card">
        <table className="table">
          <thead>
            <tr>
              <th>Правило</th>
              <th>Условие</th>
              <th>Эффект</th>
              <th>Источник</th>
              <th>Статус</th>
              <th>Действия</th>
            </tr>
          </thead>
          <tbody>
            {items.map((rule) => (
              <tr key={rule.id}>
                <td>{rule.target_key || rule.target_id || rule.target_type}</td>
                <td>{rule.condition_type}: {rule.condition_value}</td>
                <td>{rule.effect} {rule.weight_delta}</td>
                <td>{rule.source_title || rule.source_id || 'Нет источника'}</td>
                <td>{getStatusLabel(rule.status)}</td>
                <td>
                  <button className="btn btn-secondary" disabled={rule.status === 'confirmed'} onClick={() => updateStatus(rule.id, 'confirmed')}>Подтвердить</button>
                  <button className="btn btn-ghost" disabled={rule.status === 'rejected'} onClick={() => updateStatus(rule.id, 'rejected')}>Отклонить</button>
                </td>
              </tr>
            ))}
            {!items.length && !loading && (
              <tr>
                <td colSpan="6">
                  <div className="empty-state">
                    <p><strong>Правила подбора пока не созданы</strong></p>
                    <p>Загрузите доказательные источники знаний, затем создайте правила с цитатами и привязкой к источникам.</p>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
