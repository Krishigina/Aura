from functools import lru_cache

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "Aura AI Service"
    debug: bool = False

    host: str = "0.0.0.0"
    port: int = 9001

    vector_db_type: str = "weaviate"
    weaviate_url: str = "http://localhost:8080"
    embedding_service_url: str = "http://localhost:8080"
    embedding_model: str = "sentence-transformers/all-MiniLM-L6-v2"
    embedding_dimension: int = 384

    llm_provider: str = "openrouter"
    openai_api_key: str = ""
    openai_model: str = "gpt-3.5-turbo"
    openai_base_url: str = "https://api.openai.com/v1"
    llm_timeout_seconds: int = 30
    openrouter_api_key: str = ""
    openrouter_model: str = "openai/gpt-4o-mini"
    openrouter_site_url: str = "http://localhost:9001"
    openrouter_app_name: str = "Aura"

    collection_products: str = "products"
    collection_ingredients: str = "ingredients"
    collection_knowledge: str = "AuraKnowledge"
    collection_knowledge_user_prefix: str = "knowledge_user_"

    class Config:
        env_file = ".env"
        case_sensitive = False


@lru_cache()
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
