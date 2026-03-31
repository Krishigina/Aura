from pydantic import BaseModel
from typing import Optional, List, Union, Any
from enum import Enum

class ProductTypeEnum(str, Enum):
    Крем = "Крем"
    Сыворотка = "Сыворотка"
    Лосьон = "Лосьон"
    Тоник = "Тоник"
    Эмульсия = "Эмульсия"
    Масло = "Масло"
    Гель = "Гель"
    Пилинг = "Пилинг"
    Маска = "Маска"
    Бальзам = "Бальзам"
    Спрей = "Спрей"
    Мист = "Мист"

class ForWhomEnum(str, Enum):
    Универсальный = "Универсальный"
    Мужчинам = "Мужчинам"
    Женщинам = "Женщинам"

class PurposeEnum(str, Enum):
    Увлажнение = "Увлажнение"
    Очищение = "Очищение"
    Питание = "Питание"
    Антивозрастной = "Антивозрастной"
    Отбеливание = "Отбеливание"
    Защита_от_солнца = "Защита от солнца"
    Проблемная_кожа = "Проблемная кожа"
    Восстановление = "Восстановление"
    Матирование = "Матирование"
    Тонирование = "Тонирование"

class SkinTypeEnum(str, Enum):
    Сухая = "Сухая"
    Жирная = "Жирная"
    Комбинированная = "Комбинированная"
    Нормальная = "Нормальная"
    Чувствительная = "Чувствительная"
    Проблемная = "Проблемная"

class ApplicationTimeEnum(str, Enum):
    Утро = "Утро"
    Вечер = "Вечер"
    Утро_Вечер = "Утро/Вечер"

class AreaEnum(str, Enum):
    Лицо = "Лицо"
    Тело = "Тело"
    Волосы = "Волосы"
    Губы = "Губы"
    Руки = "Руки"
    Веки = "Веки"
    Зона_вокруг_глаз = "Зона вокруг глаз"

class SegmentEnum(str, Enum):
    Бюджетная = "Бюджетная"
    Люкс = "Люкс"
    Профессиональная = "Профессиональная"
    Космецевтика = "Космецевтика"

class PhotoItem(BaseModel):
    id: str
    filename: str
    data: str
    content_type: str

class ProductBase(BaseModel):
    name: Optional[str] = None
    what_is_it: Optional[str] = None
    brand: Optional[str] = None
    product_type: Optional[str] = None
    for_whom: Optional[str] = None
    purpose: Optional[Union[str, List[str]]] = None
    skin_type: Optional[str] = None
    application_time: Optional[str] = None
    area: Optional[str] = None
    active_ingredient: Optional[str] = None
    volume: Optional[str] = None
    segment: Optional[str] = None
    composition: Optional[str] = None
    application_info: Optional[str] = None
    country: Optional[str] = None
    country_origin: Optional[str] = None
    manufacturer: Optional[str] = None
    description: Optional[str] = None
    photos: Optional[Any] = None
    has_video: Optional[bool] = False

class ProductCreate(ProductBase):
    pass

class Product(ProductBase):
    id: int
    created_at: Optional[str] = None

    class Config:
        from_attributes = True
