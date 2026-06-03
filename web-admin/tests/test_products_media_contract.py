from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def test_product_photo_preview_uses_stream_url_when_base64_is_not_inline():
    source = (ROOT / "src/pages/Products.jsx").read_text(encoding="utf-8")

    assert "getProductPhotoSrc" in source
    assert "photo.url" in source
    assert "productsApi.getPhotoUrl" in source
