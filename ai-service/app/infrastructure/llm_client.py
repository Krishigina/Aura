import json
import base64

import httpx

from app.core.config import settings


class LLMConfigurationError(RuntimeError):
    pass


class LLMRequestError(RuntimeError):
    pass


class OpenRouterClient:
    def __init__(
        self,
        api_key: str,
        model: str,
        site_url: str = "http://localhost:9001",
        app_name: str = "Aura",
    ):
        if not api_key:
            raise LLMConfigurationError("OpenRouter API key is required")

        self.api_key = api_key
        self.model = model
        self.site_url = site_url
        self.app_name = app_name

    async def generate_with_context(
        self, query: str, context: list[dict], system_prompt: str
    ) -> str:
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
            "HTTP-Referer": self.site_url,
            "X-Title": self.app_name,
        }
        payload = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": self._build_user_message(query, context)},
            ],
            "temperature": 0.2,
        }

        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                response = await client.post(
                    "https://openrouter.ai/api/v1/chat/completions",
                    headers=headers,
                    json=payload,
                )
                response.raise_for_status()
                data = response.json()
                return data["choices"][0]["message"]["content"].strip()
        except (httpx.HTTPError, KeyError, IndexError, TypeError, AttributeError) as exc:
            raise LLMRequestError("OpenRouter request failed") from exc

    async def summarize_image(self, image_bytes: bytes, content_type: str, prompt: str) -> str:
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
            "HTTP-Referer": self.site_url,
            "X-Title": self.app_name,
        }
        image_base64 = base64.b64encode(image_bytes).decode("ascii")
        payload = {
            "model": self.model,
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": prompt},
                        {
                            "type": "image_url",
                            "image_url": {"url": f"data:{content_type};base64,{image_base64}"},
                        },
                    ],
                }
            ],
            "temperature": 0.2,
        }

        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                response = await client.post(
                    "https://openrouter.ai/api/v1/chat/completions",
                    headers=headers,
                    json=payload,
                )
                response.raise_for_status()
                data = response.json()
                return data["choices"][0]["message"]["content"].strip()
        except (httpx.HTTPError, KeyError, IndexError, TypeError, AttributeError) as exc:
            raise LLMRequestError("OpenRouter vision request failed") from exc

    def _build_user_message(self, query: str, context: list[dict]) -> str:
        formatted_context = json.dumps(context, ensure_ascii=False, indent=2)
        return (
            "Отвечай только на основе предоставленного контекста. "
            "Если в контексте нет ответа, честно скажи, что информации недостаточно.\n\n"
            f"Контекст:\n{formatted_context}\n\n"
            f"Вопрос: {query}"
        )


_llm_client = None


def get_llm_client():
    global _llm_client

    if settings.llm_provider != "openrouter":
        raise LLMConfigurationError(f"Unsupported LLM provider: {settings.llm_provider}")

    if _llm_client is None:
        _llm_client = OpenRouterClient(
            api_key=settings.openrouter_api_key,
            model=settings.openrouter_model,
            site_url=settings.openrouter_site_url,
            app_name=settings.openrouter_app_name,
        )
    return _llm_client
