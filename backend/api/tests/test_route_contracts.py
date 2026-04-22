from backend.api.main import app


def _route_pairs():
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


def test_core_entity_routes_exist():
    pairs = _route_pairs()

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

    missing = expected - pairs
    assert not missing, f"Missing route contracts: {sorted(missing)}"


def test_single_health_route_in_active_backend():
    pairs = _route_pairs()
    health_count = sum(1 for pair in pairs if pair == ("GET", "/api/health"))
    assert health_count == 1, f"Expected 1 /api/health route, got {health_count}"
