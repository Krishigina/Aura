from typing import Any, Dict, List, Optional
import logging
import httpx

from app.core.config import settings

logger = logging.getLogger(__name__)


class LLMClient:
    def __init__(self):
        self.provider = (settings.llm_provider or "openai").lower()
        self.base_url = settings.openai_base_url.rstrip("/")
        self.api_key = settings.openai_api_key
        self.model = settings.openai_model
        self.timeout_seconds = settings.llm_timeout_seconds

    def generate_with_context(
        self,
        query: str,
        context: str,
        user_context: Optional[Dict[str, Any]] = None,
    ) -> Optional[str]:
        if self.provider != "openai":
            logger.warning("Unsupported llm_provider '%s'. Fallback will be used.", self.provider)
            return None

        if not self.api_key:
            logger.info("OpenAI API key is not configured. Using local fallback generation.")
            return None

        system_prompt = (
            "Ты — эксперт по косметике и уходу за кожей. "
            "Отвечай кратко, практично и только на основе предоставленного контекста. "
            "Если контекст недостаточен — честно скажи об этом и предложи уточнить вопрос."
        )

        metadata = ""
        if user_context:
            try:
                pairs = [f"{k}: {v}" for k, v in user_context.items()]
                metadata = "\n".join(pairs)
            except Exception:
                metadata = ""

        user_prompt = (
            f"Вопрос пользователя:\n{query}\n\n"
            f"Контекст:\n{context}\n\n"
            f"Контекст пользователя:\n{metadata if metadata else 'нет данных'}\n\n"
            "Сформируй ответ на русском языке. "
            "В конце добавь блок 'Источники:' и перечисли кратко использованные фрагменты контекста."
        )

        payload: Dict[str, Any] = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            "temperature": 0.2,
        }

        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }

        try:
            with httpx.Client(timeout=float(self.timeout_seconds)) as client:
                response = client.post(
                    f"{self.base_url}/chat/completions",
                    headers=headers,
                    json=payload,
                )
                response.raise_for_status()
                body = response.json()

            choices = body.get("choices") if isinstance(body, dict) else None
            if not isinstance(choices, list) or not choices:
                logger.warning("LLM response has no choices")
                return None

            message = choices[0].get("message") if isinstance(choices[0], dict) else None
            content = message.get("content") if isinstance(message, dict) else None
            if isinstance(content, str) and content.strip():
                return content.strip()

            logger.warning("LLM response content is empty")
            return None
        except Exception as exc:
            logger.error("LLM request failed: %s", exc)
            return None


_llm_client: Optional[LLMClient] = None


def get_llm_client() -> LLMClient:
    global _llm_client
    if _llm_client is None:
        _llm_client = LLMClient()
    return _llm_client
