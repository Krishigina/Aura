import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from backend.api.main import app


def collect_pairs():
    pairs = set()
    for route in app.routes:
        methods = getattr(route, "methods", set())
        path = getattr(route, "path", None)
        if not path:
            continue
        for method in methods:
            if method in {"HEAD", "OPTIONS"}:
                continue
            pairs.add((method, path))
    return pairs


def main():
    pairs = collect_pairs()
    expected = {
        ("GET", "/api/health"),
        ("POST", "/api/auth/login"),
        ("POST", "/api/auth/register"),
        ("GET", "/api/auth/me"),
        ("GET", "/api/profile/skin-passport"),
        ("PUT", "/api/profile/skin-passport"),
        ("GET", "/api/home/feed"),
        ("GET", "/api/home/status"),
        ("GET", "/api/diagnostics/summary"),
        ("GET", "/api/survey/schema"),
        ("GET", "/api/chat/bootstrap"),
        ("POST", "/api/chat/messages"),
        ("GET", "/api/analytics/dashboard"),
        ("GET", "/api/reports/summary"),
        ("GET", "/api/products"),
        ("POST", "/api/products"),
        ("GET", "/api/dictionaries/{key}"),
        ("POST", "/api/dictionaries/{key}"),
        ("PUT", "/api/dictionaries/{key}"),
        ("DELETE", "/api/dictionaries/{key}/{value}"),
        ("PUT", "/api/dictionaries/brands"),
        ("GET", "/api/procedures"),
        ("POST", "/api/procedures"),
        ("GET", "/api/content"),
        ("POST", "/api/content"),
    }

    missing = sorted(expected - pairs)
    if missing:
        raise SystemExit(f"Missing route contracts: {missing}")

    health_count = sum(1 for p in pairs if p == ("GET", "/api/health"))
    if health_count != 1:
        raise SystemExit(f"Expected exactly one /api/health route, got {health_count}")

    print("Route contracts OK")


if __name__ == "__main__":
    main()
