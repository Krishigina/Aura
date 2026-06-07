import { useEffect, useState } from 'react'
import { ingredientKnowledgeApi } from '../api'

const FACT_STATUSES = [
  { value: 'auto_high_confidence', label: 'Авто, высокая уверенность' },
  { value: 'draft', label: 'Черновики' },
  { value: 'confirmed', label: 'Подтвержденные' },
  { value: 'rejected', label: 'Отклоненные' },
]

function countValue(item, keys) {
  for (const key of keys) {
    if (item[key] != null) return item[key]
  }
  return 0
}

function factPayload(fact, evidenceStatus) {
  return {
    evidence_status: evidenceStatus,
    effect_key: fact.effect_key,
    condition_type: fact.condition_type,
    condition_value: fact.condition_value,
    matching_effect: fact.matching_effect,
    matching_weight_delta: fact.matching_weight_delta,
    source_id: fact.source_id,
    evidence_quote: fact.evidence_quote,
    confidence: fact.confidence,
  }
}

function formatPercent(value) {
  const number = Number(value || 0)
  return `${Math.round(number * 100)}%`
}

export default function IngredientKnowledge() {
  const [ingredients, setIngredients] = useState([])
  const [facts, setFacts] = useState([])
  const [status, setStatus] = useState('auto_high_confidence')
  const [loadingIngredients, setLoadingIngredients] = useState(false)
  const [loadingFacts, setLoadingFacts] = useState(false)
  const [savingId, setSavingId] = useState(null)
  const [error, setError] = useState('')

  const loadIngredients = async () => {
    setLoadingIngredients(true)
    try {
      const data = await ingredientKnowledgeApi.listIngredients()
      setIngredients(data.items || [])
    } catch (err) {
      setError(err.message || 'Не удалось загрузить ингредиенты')
    } finally {
      setLoadingIngredients(false)
    }
  }

  const loadFacts = async () => {
    setLoadingFacts(true)
    try {
      const data = await ingredientKnowledgeApi.listFacts(status)
      setFacts(data.items || [])
    } catch (err) {
      setError(err.message || 'Не удалось загрузить факты')
    } finally {
      setLoadingFacts(false)
    }
  }

  useEffect(() => {
    setError('')
    loadIngredients()
  }, [])

  useEffect(() => {
    setError('')
    loadFacts()
  }, [status])

  const updateFactStatus = async (fact, nextStatus) => {
    try {
      setSavingId(fact.id)
      await ingredientKnowledgeApi.updateFact(fact.id, factPayload(fact, nextStatus))
      await Promise.all([loadFacts(), loadIngredients()])
    } catch (err) {
      setError(err.message || 'Не удалось обновить статус факта')
    } finally {
      setSavingId(null)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>База ингредиентов</h1>
          <p>Проверяйте ингредиенты и доказательные факты перед использованием в подборе продуктов.</p>
        </div>
      </div>

      {error && <div className="glass-card error-message">{error}</div>}

      <div className="table-card glass-card" style={{ marginBottom: 24 }}>
        <table className="table">
          <thead>
            <tr>
              <th>Название</th>
              <th>INCI</th>
              <th>Статус</th>
              <th>Алиасы</th>
              <th>Факты</th>
              <th>Продукты</th>
            </tr>
          </thead>
          <tbody>
            {ingredients.map((ingredient) => (
              <tr key={ingredient.id}>
                <td>{ingredient.canonical_name}</td>
                <td>{ingredient.inci_name || '-'}</td>
                <td>{ingredient.evidence_status || ingredient.verification_status || '-'}</td>
                <td>{countValue(ingredient, ['aliases_count', 'alias_count'])}</td>
                <td>{countValue(ingredient, ['facts_count', 'fact_count'])}</td>
                <td>{countValue(ingredient, ['products_count', 'product_count'])}</td>
              </tr>
            ))}
            {!ingredients.length && !loadingIngredients && (
              <tr><td colSpan="6">Ингредиенты не найдены</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {loadingIngredients && <div className="glass-card" style={{ marginBottom: 24 }}>Загрузка ингредиентов...</div>}

      <div className="page-header">
        <div>
          <h2>Очередь проверки фактов</h2>
          <p>Меняйте статус фактов без потери извлеченных полей.</p>
        </div>
        <select className="input" value={status} onChange={(event) => setStatus(event.target.value)}>
          {FACT_STATUSES.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
        </select>
      </div>

      {loadingFacts && <div className="glass-card">Загрузка фактов...</div>}

      <div style={{ display: 'grid', gap: 16 }}>
        {facts.map((fact) => (
          <div key={fact.id} className="glass-card" style={{ padding: 20 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'flex-start', flexWrap: 'wrap' }}>
              <div>
                <h3 style={{ margin: '0 0 8px' }}>{fact.canonical_name || `Ингредиент #${fact.ingredient_id}`}</h3>
                <p style={{ margin: 0 }}>
                  <strong>Эффект:</strong> {fact.effect_key || fact.matching_effect || '-'}
                </p>
                <p style={{ margin: '6px 0 0' }}>
                  <strong>Условие:</strong> {[fact.condition_type, fact.condition_value].filter(Boolean).join(': ') || '-'}
                </p>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div><strong>Уверенность:</strong> {formatPercent(fact.confidence)}</div>
                <div><strong>Статус:</strong> {fact.evidence_status}</div>
              </div>
            </div>

            <div style={{ marginTop: 12 }}>
              <strong>Источник:</strong> {fact.source_title || 'Без названия'} {fact.source_id ? `(ID ${fact.source_id})` : ''}
            </div>
            <blockquote style={{ margin: '12px 0 0', paddingLeft: 12, borderLeft: '3px solid var(--color-mint-green-dark)' }}>
              {fact.evidence_quote || 'Цитата не указана'}
            </blockquote>

            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 16 }}>
              <button className="btn btn-primary" disabled={savingId === fact.id} onClick={() => updateFactStatus(fact, 'confirmed')}>Подтвердить</button>
              <button className="btn btn-secondary" disabled={savingId === fact.id} onClick={() => updateFactStatus(fact, 'draft')}>В черновик</button>
              <button className="btn btn-ghost" disabled={savingId === fact.id} onClick={() => updateFactStatus(fact, 'rejected')}>Отклонить</button>
            </div>
          </div>
        ))}
      </div>

      {!facts.length && !loadingFacts && <div className="empty-state glass-card"><p>Фактов с этим статусом нет</p></div>}
    </div>
  )
}
