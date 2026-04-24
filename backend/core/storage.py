from pathlib import Path


API_ROOT = Path(__file__).resolve().parents[1]


def upload_dir(name: str) -> Path:
    return API_ROOT / name


def ensure_upload_dir(name: str) -> Path:
    path = upload_dir(name)
    path.mkdir(parents=True, exist_ok=True)
    return path


def upload_path(directory: str, filename: str) -> Path:
    return upload_dir(directory) / filename
