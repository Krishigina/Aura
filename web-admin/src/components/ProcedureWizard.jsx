import { useState, useEffect } from 'react'
import { ChevronLeft, ChevronRight, Check, X, Image as ImageIcon } from 'lucide-react'
import Select from '../components/Select'
import RichTextEditor from '../components/RichTextEditor'
import './ProcedureWizard.css'

const STEPS = [
  { id: 'basic', title: 'Базовая информация' },
  { id: 'description', title: 'Описание' },
  { id: 'indications', title: 'Показания' },
  { id: 'effect', title: 'Эффект' },
  { id: 'preparation', title: 'Подготовка' },
  { id: 'rehabilitation', title: 'Реабилитация' },
  { id: 'equipment', title: 'Оборудование и фото' },
]

const initialFormData = {
  name: '',
  direction: '',
  method_type: '',
  duration: '',
  equipment: '',
  zones: [],
  effects: [],
  problems: [],
  description: '',
  procedure_about: '',
  advantages: '',
  indications: '',
  principle: '',
  how_it_goes: '',
  for_whom: '',
  problems_solved: '',
  contraindications_full: '',
  preparation: '',
  recommended_course: '',
  rehabilitation: '',
  post_care: '',
  side_effects: '',
  photos: []
}

export default function ProcedureWizard({ initialData, dictionaries, onSave, onCancel }) {
  const [currentStep, setCurrentStep] = useState(0)
  const [formData, setFormData] = useState(initialFormData)
  const [uploadingPhoto, setUploadingPhoto] = useState(false)
  const [draggedPhotoIndex, setDraggedPhotoIndex] = useState(null)

  useEffect(() => {
    if (initialData) {
      const merged = { ...initialFormData }
      const arrayFields = ['zones', 'effects', 'problems']
      
      for (const key in initialData) {
        if (initialData[key] !== null && initialData[key] !== undefined) {
          if (arrayFields.includes(key)) {
            // Parse JSON string to array if needed
            if (typeof initialData[key] === 'string') {
              try {
                merged[key] = JSON.parse(initialData[key])
              } catch {
                merged[key] = []
              }
            } else if (Array.isArray(initialData[key])) {
              merged[key] = initialData[key]
            } else {
              merged[key] = []
            }
          } else {
            merged[key] = initialData[key]
          }
        }
      }
      setFormData(merged)
    }
  }, [initialData])

  const handleInputChange = (e) => {
    const { name, value } = e.target
    if (name === 'duration') {
      const numValue = parseInt(value)
      setFormData(prev => ({ ...prev, [name]: isNaN(numValue) ? value : numValue }))
    } else {
      setFormData(prev => ({ ...prev, [name]: value }))
    }
  }

  const handleSelectChange = (name, value) => {
    setFormData(prev => ({ ...prev, [name]: value }))
  }

  const handleRichTextChange = (name, html) => {
    setFormData(prev => ({ ...prev, [name]: html }))
  }

  const handlePhotoUpload = async (file) => {
    if (!file) return
    const validTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
    if (!validTypes.includes(file.type)) return
    if (file.size > 10 * 1024 * 1024) return

    setUploadingPhoto(true)
    try {
      const reader = new FileReader()
      reader.onload = () => {
        setFormData(prev => ({
          ...prev,
          photos: [...prev.photos, {
            id: Date.now().toString(),
            filename: file.name,
            content_type: file.type,
            data: reader.result.split(',')[1]
          }]
        }))
      }
      reader.readAsDataURL(file)
    } finally {
      setUploadingPhoto(false)
    }
  }

  const handlePhotoDelete = (photoId) => {
    setFormData(prev => ({
      ...prev,
      photos: prev.photos.filter(p => p.id !== photoId)
    }))
  }

  const handlePhotoDragStart = (index) => {
    setDraggedPhotoIndex(index)
  }

  const handlePhotoDragOver = (e) => {
    e.preventDefault()
  }

  const handlePhotoDrop = (dropIndex) => {
    if (draggedPhotoIndex === null || draggedPhotoIndex === dropIndex) {
      setDraggedPhotoIndex(null)
      return
    }
    const newPhotos = [...formData.photos]
    const [draggedPhoto] = newPhotos.splice(draggedPhotoIndex, 1)
    newPhotos.splice(dropIndex, 0, draggedPhoto)
    setDraggedPhotoIndex(null)
    setFormData(prev => ({ ...prev, photos: newPhotos }))
  }

  const goToNext = () => {
    if (currentStep < STEPS.length - 1) {
      setCurrentStep(prev => prev + 1)
    }
  }

  const goToPrev = () => {
    if (currentStep > 0) {
      setCurrentStep(prev => prev - 1)
    }
  }

  const handleSave = () => {
    if (onSave) {
      onSave(formData)
    }
  }

  const renderStepContent = () => {
    const stepId = STEPS[currentStep].id

    switch (stepId) {
      case 'basic':
        return (
          <div className="wizard-section">
            <div className="form-grid">
              <div className="form-group">
                <label>Название процедуры *</label>
                <input
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  className="input"
                  placeholder="Введите название процедуры"
                />
              </div>
              <Select
                label="Направление"
                name="direction"
                value={formData.direction}
                onChange={handleSelectChange}
                options={dictionaries.directions || []}
                placeholder="Выберите направление"
                searchable
              />
              <Select
                label="Тип метода"
                name="method_type"
                value={formData.method_type}
                onChange={handleSelectChange}
                options={dictionaries.method_types || []}
                placeholder="Выберите тип метода"
                searchable
              />
              <div className="form-group">
                <label>Длительность (минут)</label>
                <input
                  type="number"
                  name="duration"
                  value={formData.duration}
                  onChange={handleInputChange}
                  className="input"
                  placeholder="Например: 60"
                />
              </div>
            </div>
          </div>
        )

      case 'description':
        return (
          <div className="wizard-section">
            <div className="form-grid">
              <div className="form-group full-width">
                <label>Описание</label>
                <textarea
                  name="description"
                  value={formData.description}
                  onChange={handleInputChange}
                  className="input textarea"
                  rows="3"
                  placeholder="Общее описание процедуры..."
                />
              </div>
              <div className="form-group full-width">
                <label>О процедуре</label>
                <RichTextEditor
                  value={formData.procedure_about}
                  onChange={(html) => handleRichTextChange('procedure_about', html)}
                  placeholder="Подробная информация о процедуре..."
                  rows={5}
                />
              </div>
              <div className="form-group full-width">
                <label>Принцип действия</label>
                <RichTextEditor
                  value={formData.principle}
                  onChange={(html) => handleRichTextChange('principle', html)}
                  placeholder="Принцип действия метода..."
                  rows={4}
                />
              </div>
              <div className="form-group full-width">
                <label>Как проходит процедура</label>
                <RichTextEditor
                  value={formData.how_it_goes}
                  onChange={(html) => handleRichTextChange('how_it_goes', html)}
                  placeholder="Этапы и ход процедуры..."
                  rows={5}
                />
              </div>
            </div>
          </div>
        )

      case 'indications':
        return (
          <div className="wizard-section">
            <div className="form-grid">
              <div className="form-group full-width">
                <label>Показания</label>
                <textarea
                  name="indications"
                  value={formData.indications}
                  onChange={handleInputChange}
                  className="input textarea"
                  rows="3"
                  placeholder="Показания к процедуре..."
                />
              </div>
              <div className="form-group full-width">
                <label>Для кого подходит</label>
                <textarea
                  name="for_whom"
                  value={formData.for_whom}
                  onChange={handleInputChange}
                  className="input textarea"
                  rows="3"
                  placeholder="Целевая аудитория..."
                />
              </div>
              <div className="form-group full-width">
                <label>Решаемые проблемы</label>
                <Select
                  name="problems"
                  value={formData.problems}
                  onChange={handleSelectChange}
                  options={dictionaries.problems || []}
                  placeholder="Выберите проблемы"
                  multiple
                  searchable
                />
              </div>
            </div>
          </div>
        )

      case 'effect':
        return (
          <div className="wizard-section">
            <div className="form-grid">
              <div className="form-group full-width">
                <label>Эффекты</label>
                <Select
                  name="effects"
                  value={formData.effects}
                  onChange={handleSelectChange}
                  options={dictionaries.effects || []}
                  placeholder="Выберите эффекты"
                  multiple
                  searchable
                />
              </div>
              <div className="form-group full-width">
                <label>Преимущества</label>
                <textarea
                  name="advantages"
                  value={formData.advantages}
                  onChange={handleInputChange}
                  className="input textarea"
                  rows="4"
                  placeholder="Преимущества процедуры..."
                />
              </div>
            </div>
          </div>
        )

      case 'preparation':
        return (
          <div className="wizard-section">
            <div className="form-grid">
              <div className="form-group full-width">
                <label>Подготовка к процедуре</label>
                <textarea
                  name="preparation"
                  value={formData.preparation}
                  onChange={handleInputChange}
                  className="input textarea"
                  rows="4"
                  placeholder="Рекомендации по подготовке..."
                />
              </div>
              <div className="form-group full-width">
                <label>Рекомендуемый курс</label>
                <textarea
                  name="recommended_course"
                  value={formData.recommended_course}
                  onChange={handleInputChange}
                  className="input textarea"
                  rows="4"
                  placeholder="Количество сеансов, интервалы..."
                />
              </div>
            </div>
          </div>
        )

      case 'rehabilitation':
        return (
          <div className="wizard-section">
            <div className="form-grid">
              <div className="form-group full-width">
                <label>Реабилитация</label>
                <textarea
                  name="rehabilitation"
                  value={formData.rehabilitation}
                  onChange={handleInputChange}
                  className="input textarea"
                  rows="3"
                  placeholder="Восстановительный период..."
                />
              </div>
              <div className="form-group full-width">
                <label>Уход после процедуры</label>
                <textarea
                  name="post_care"
                  value={formData.post_care}
                  onChange={handleInputChange}
                  className="input textarea"
                  rows="3"
                  placeholder="Рекомендации по уходу..."
                />
              </div>
              <div className="form-group full-width">
                <label>Побочные эффекты</label>
                <textarea
                  name="side_effects"
                  value={formData.side_effects}
                  onChange={handleInputChange}
                  className="input textarea"
                  rows="3"
                  placeholder="Возможные побочные эффекты..."
                />
              </div>
              <div className="form-group full-width">
                <label>Полные противопоказания</label>
                <textarea
                  name="contraindications_full"
                  value={formData.contraindications_full}
                  onChange={handleInputChange}
                  className="input textarea"
                  rows="3"
                  placeholder="Противопоказания к процедуре..."
                />
              </div>
            </div>
          </div>
        )

      case 'equipment':
        return (
          <div className="wizard-section">
            <div className="form-grid">
              <Select
                label="Оборудование"
                name="equipment"
                value={formData.equipment}
                onChange={handleSelectChange}
                options={dictionaries.equipment || []}
                placeholder="Выберите оборудование"
                searchable
              />
              <div className="form-group full-width">
                <label>Зоны воздействия</label>
                <Select
                  name="zones"
                  value={formData.zones}
                  onChange={handleSelectChange}
                  options={dictionaries.zones || []}
                  placeholder="Выберите зоны"
                  multiple
                  searchable
                />
              </div>
              <div className="form-group full-width">
                <label>Фотографии</label>
                <div className="media-upload-section">
                  {formData.photos?.length > 0 && (
                    <div className="photo-grid">
                      {formData.photos.map((photo, idx) => (
                        <div
                          key={photo.id || idx}
                          className={`photo-card ${draggedPhotoIndex === idx ? 'dragging' : ''}`}
                          draggable
                          onDragStart={() => handlePhotoDragStart(idx)}
                          onDragOver={handlePhotoDragOver}
                          onDrop={() => handlePhotoDrop(idx)}
                        >
                          <div className="photo-preview">
                            {photo.data && photo.data.length > 0 ? (
                              <img
                                src={`data:${photo.content_type};base64,${photo.data}`}
                                alt=""
                              />
                            ) : (
                              <div className="photo-placeholder">
                                <ImageIcon size={32} />
                                <span className="photo-filename">{photo.filename?.split('.')[0] || 'Фото'}</span>
                                <span className="photo-type">{photo.content_type?.split('/')[1]?.toUpperCase() || ''}</span>
                              </div>
                            )}
                          </div>
                          <button
                            type="button"
                            className="photo-delete-btn"
                            onClick={(e) => { e.stopPropagation(); handlePhotoDelete(photo.id) }}
                          >
                            <X size={14} />
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                  <label className={`upload-zone ${uploadingPhoto ? 'uploading' : ''}`}>
                    {uploadingPhoto ? (
                      <div className="upload-content">
                        <span className="spinner" style={{ width: 24, height: 24 }}></span>
                        <span className="upload-text">Загрузка...</span>
                      </div>
                    ) : (
                      <>
                        <input
                          type="file"
                          accept="image/*"
                          onChange={e => e.target.files[0] && handlePhotoUpload(e.target.files[0])}
                          className="hidden-input"
                        />
                        <div className="upload-content">
                          <ImageIcon size={24} className="upload-icon" />
                          <span className="upload-text">Добавить фото</span>
                          <span className="upload-hint">PNG, JPG, WebP до 10MB</span>
                        </div>
                      </>
                    )}
                  </label>
                </div>
              </div>
            </div>
          </div>
        )

      default:
        return null
    }
  }

  return (
    <div className="procedure-wizard">
      <div className="wizard-header">
        <h3>{initialData ? 'Редактирование процедуры' : 'Создание процедуры'}</h3>
        <button className="btn btn-ghost btn-sm" onClick={onCancel}>
          <X size={20} />
        </button>
      </div>

      <div className="wizard-progress">
        {STEPS.map((step, idx) => (
          <div
            key={step.id}
            className={`progress-step ${idx === currentStep ? 'active' : ''} ${idx < currentStep ? 'completed' : ''}`}
            onClick={() => setCurrentStep(idx)}
          >
            <div className="step-number">
              {idx < currentStep ? <Check size={16} /> : idx + 1}
            </div>
            <span className="step-title">{step.title}</span>
          </div>
        ))}
      </div>

      <div className="wizard-content">
        <div className="step-indicator">
          Шаг {currentStep + 1} из {STEPS.length}: {STEPS[currentStep].title}
        </div>
        <div className="step-form">
          {renderStepContent()}
        </div>
      </div>

      <div className="wizard-actions">
        <button
          className="btn btn-ghost"
          onClick={goToPrev}
          disabled={currentStep === 0}
        >
          <ChevronLeft size={18} />
          Назад
        </button>
        <div className="wizard-actions-right">
          <button className="btn btn-ghost" onClick={onCancel}>
            Отмена
          </button>
          {currentStep === STEPS.length - 1 ? (
            <button className="btn btn-primary" onClick={handleSave}>
              <Check size={18} />
              Сохранить
            </button>
          ) : (
            <button className="btn btn-primary" onClick={goToNext}>
              Далее
              <ChevronRight size={18} />
            </button>
          )}
        </div>
      </div>
    </div>
  )
}