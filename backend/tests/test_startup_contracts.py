from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_start_bat_preserves_existing_postgres_container():
    script = (ROOT / "start.bat").read_text(encoding="utf-8")

    assert "docker start aura_postgres" in script
    assert "docker rm -f aura_postgres 2>nul" not in script


def test_start_bat_uses_persistent_postgres_volume():
    script = (ROOT / "start.bat").read_text(encoding="utf-8")

    postgres_start = script[script.index("docker run -d --name aura_postgres"):]

    assert "-v aura_postgres_data:/var/lib/postgresql/data" in postgres_start
