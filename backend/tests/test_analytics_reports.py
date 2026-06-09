from datetime import date, datetime
from pathlib import Path
import sys

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from backend.core.analytics_reports import build_dashboard_analytics_payload


@pytest.fixture
def anyio_backend():
    return "asyncio"


@pytest.mark.anyio
async def test_dashboard_analytics_uses_latest_activity_window():
    requests_anchor_day = date(2026, 6, 17)

    class FakeConn:
        async def fetchrow(self, query, *args):
            if "AS users" in query and "AS products" in query and "AS procedures" in query and "AS content" in query:
                return {
                    "users": 4,
                    "products": 64,
                    "procedures": 84,
                    "content": 3,
                }
            raise AssertionError(query)

        async def fetchval(self, query, *args):
            if "FROM chat_messages" in query:
                return requests_anchor_day
            raise AssertionError(query)

        async def fetch(self, query, *args):
            if "LEFT JOIN requests_daily" in query:
                assert args == (requests_anchor_day,)
                return [
                    {"day": date(2026, 6, 11), "requests": 10},
                    {"day": date(2026, 6, 12), "requests": 0},
                    {"day": date(2026, 6, 13), "requests": 12},
                    {"day": date(2026, 6, 14), "requests": 0},
                    {"day": date(2026, 6, 15), "requests": 4},
                    {"day": date(2026, 6, 16), "requests": 0},
                    {"day": date(2026, 6, 17), "requests": 42},
                ]
            if "FROM chat_messages" in query and "session_id IN (" in query:
                assert args == (requests_anchor_day,)
                return [
                    {
                        "session_id": 1,
                        "created_at": datetime(2026, 6, 11, 10, 0),
                        "role": "user",
                        "content": "which products fit me",
                    },
                    {
                        "session_id": 1,
                        "created_at": datetime(2026, 6, 11, 10, 0),
                        "role": "assistant",
                        "content": "Here are recommendations.",
                    },
                    {
                        "session_id": 2,
                        "created_at": datetime(2026, 6, 17, 21, 1),
                        "role": "user",
                        "content": "which cream suits me",
                    },
                    {
                        "session_id": 2,
                        "created_at": datetime(2026, 6, 17, 21, 2),
                        "role": "assistant",
                        "content": "Catalog match.",
                    },
                    {
                        "session_id": 2,
                        "created_at": datetime(2026, 6, 17, 21, 3),
                        "role": "user",
                        "content": "which one is better in the evening",
                    },
                    {
                        "session_id": 2,
                        "created_at": datetime(2026, 6, 17, 21, 4),
                        "role": "assistant",
                        "content": "This one fits.",
                    },
                ]
            if "ORDER BY event_time DESC" in query:
                return [
                    {
                        "actor": "La Roche-Posay Effaclar Duo+",
                        "action": "Added new product",
                        "event_time": datetime(2026, 6, 10, 14, 44),
                        "status": "success",
                    }
                ]
            raise AssertionError(query)

    payload = await build_dashboard_analytics_payload(FakeConn())

    assert payload["stats"] == {
        "users": 4,
        "products": 64,
        "procedures": 84,
        "content": 3,
    }
    assert payload["userActivityData"][0]["requests"] == 10
    assert payload["userActivityData"][-1]["requests"] == 42
    assert payload["recommendationActivityData"][0]["recommendations"] == 1
    assert payload["recommendationActivityData"][-1]["recommendations"] == 2
    assert payload["activityData"] == payload["recommendationActivityData"]
    assert payload["recentActivity"] == [
        {
            "id": 1,
            "user": "La Roche-Posay Effaclar Duo+",
            "action": "Added new product",
            "time": "10.06.2026 14:44",
            "status": "success",
        }
    ]
