import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { rulesIngredientsApi } from '../api'
import MatchingRules from './MatchingRules'
import IngredientKnowledge from './IngredientKnowledge'

const TABS = [
  { key: 'overview', label: 'Обзор' },
  { key: 'active', label: 'Активные правила' },
  { key: 'review', label: 'На проверку' },
  { key: 'manual', label: 'Ручные правила' },
  { key: 'ingredients', label: 'Ингредиенты' },
]

function StatCard({ label, value }) {
  return (
    <div className="glass-card" style={{ padding: 16 }}>
      <div style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{label}</div>
      <div style={{ fontSize: 28, fontWeight: 700 }}>{value ?? 0}</div>
    </div>
  )
}

function ActiveRulesTable({ items, loading }) {
  return (
    <div className="table-card glass-card">
      <table className="table">
        <thead>
          <tr>
            <th>Тип</th>
            <th>Цель</th>
            <th>Условие</th>
            <th>Эффект</th>
            <th>Статус</th>
            <th>Источник</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={`${item.source_type}-${item.id}`}>
              <td>{item.source_type === 'ingredient_fact' ? 'Факт ингредиента' : 'Ручное правило'}</td>
              <td>{item.target_key || item.target_type || 'Не указано'}</td>
              <td>{[item.condition_type, item.condition_value].filter(Boolean).join(': ') || 'Без условия'}</td>
              <td>{item.effect} {item.weight_delta ?? ''}</td>
              <td>{item.evidence_status || item.status}</td>
              <td>{item.source_title || item.source_id || 'Нет источника'}</td>
            </tr>
          ))}
          {!items.length && !loading && <tr><td colSpan="6">Активных правил нет</td></tr>}
        </tbody>
      </table>
    </div>
  )
}

export default function RulesIngredients() {
  const [searchParams, setSearchParams] = useSearchParams()
  const initialTab = searchParams.get('tab') || 'overview'
  const [activeTab, setActiveTab] = useState(TABS.some((tab) => tab.key === initialTab) ? initialTab : 'overview')
  const [overview, setOverview] = useState({ summary: {}, active_rules: [] })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const loadOverview = async () => {
    setLoading(true)
    setError('')
    try {
      const data = await rulesIngredientsApi.getOverview()
      setOverview(data)
    } catch (err) {
      setError(err.message || 'Не удалось загрузить правила и ингредиенты')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadOverview()
  }, [])

  useEffect(() => {
    const tab = searchParams.get('tab') || 'overview'
    setActiveTab(TABS.some((item) => item.key === tab) ? tab : 'overview')
  }, [searchParams])

  const setTab = (tab) => {
    setActiveTab(tab)
    setSearchParams({ tab })
  }

  const summary = overview.summary || {}

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Правила и ингредиенты</h1>
          <p>Единый центр управления ручными правилами и фактами ингредиентов, которые влияют на подбор.</p>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 16 }}>
        {TABS.map((tab) => (
          <button
            key={tab.key}
            className={`btn ${activeTab === tab.key ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {error && <div className="glass-card error-message">{error}</div>}
      {loading && <div className="glass-card">Загрузка...</div>}

      {activeTab === 'overview' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 16 }}>
          <StatCard label="Ручные правила" value={summary.manual_rules_count} />
          <StatCard label="Активные факты" value={summary.active_ingredient_facts_count} />
          <StatCard label="Черновики фактов" value={summary.draft_ingredient_facts_count} />
          <StatCard label="Ингредиенты" value={summary.ingredients_count} />
          <StatCard label="Источники знаний" value={summary.knowledge_sources_count} />
        </div>
      )}

      {activeTab === 'active' && <ActiveRulesTable items={overview.active_rules || []} loading={loading} />}
      {activeTab === 'review' && <IngredientKnowledge />}
      {activeTab === 'manual' && <MatchingRules />}
      {activeTab === 'ingredients' && <IngredientKnowledge />}
    </div>
  )
}
