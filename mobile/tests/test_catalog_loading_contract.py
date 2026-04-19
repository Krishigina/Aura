from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def test_catalog_does_not_hydrate_full_product_photo_payloads():
    source = (ROOT / "shared/src/commonMain/kotlin/com/aura/feature/catalog/data/repository/CatalogRepositoryImpl.kt").read_text(encoding="utf-8")

    assert "hydratePhotos = true" not in source
    assert "hydratePhotos = shouldHydrateCatalogProductPhotos()" in source


def test_catalog_does_not_block_first_render_on_photo_hydration():
    logic_source = (ROOT / "shared/src/commonMain/kotlin/com/aura/feature/catalog/CatalogLogic.kt").read_text(encoding="utf-8")
    api_source = (ROOT / "shared/src/commonMain/kotlin/com/aura/core/data/api/AuraApiClient.kt").read_text(encoding="utf-8")

    assert "shouldHydrateCatalogProductPhotos(): Boolean = false" in logic_source
    assert "getProductPhotos(product.id)" in api_source


def test_product_list_api_defaults_to_not_hydrating_photos():
    source = (ROOT / "shared/src/commonMain/kotlin/com/aura/core/data/api/AuraApiClient.kt").read_text(encoding="utf-8")

    assert "suspend fun getProducts(token: String? = null, hydratePhotos: Boolean = false)" in source


def test_product_matches_request_asks_for_catalog_sized_limit():
    source = (ROOT / "shared/src/commonMain/kotlin/com/aura/core/data/api/AuraApiClient.kt").read_text(encoding="utf-8")

    assert 'put("limit", 500)' in source


def test_authenticated_catalog_keeps_all_products_even_when_matching_is_empty():
    source = (ROOT / "shared/src/commonMain/kotlin/com/aura/core/data/api/AuraApiClient.kt").read_text(encoding="utf-8")

    assert "baseProducts.filter { compatibilityByProductId.containsKey(it.id) }" not in source
    assert "baseProducts.map { product ->" in source


def test_product_matches_request_applies_minimum_compatibility_threshold():
    source = (ROOT / "shared/src/commonMain/kotlin/com/aura/core/data/api/AuraApiClient.kt").read_text(encoding="utf-8")

    assert "minCompatibilityPercent: Int = 50" in source
    assert 'put("min_compatibility_percent", minCompatibilityPercent)' in source


def test_catalog_product_matches_request_keeps_all_indexes():
    source = (ROOT / "shared/src/commonMain/kotlin/com/aura/core/data/api/AuraApiClient.kt").read_text(encoding="utf-8")

    assert "getProductMatches(token, minCompatibilityPercent = 0)" in source
    assert "private suspend fun getProductMatches(token: String, minCompatibilityPercent: Int = 50)" in source
    assert 'put("min_compatibility_percent", minCompatibilityPercent)' in source


def test_mobile_product_photos_support_streaming_urls():
    model_source = (ROOT / "shared/src/commonMain/kotlin/com/aura/core/domain/model/Models.kt").read_text(encoding="utf-8")
    api_source = (ROOT / "shared/src/commonMain/kotlin/com/aura/core/data/api/AuraApiClient.kt").read_text(encoding="utf-8")

    assert "val url: String? = null" in model_source
    assert "hydratePhotoDataFromUrl" in api_source
    assert "photo.url" in api_source


def test_catalog_products_support_lightweight_thumbnail_urls():
    model_source = (ROOT / "shared/src/commonMain/kotlin/com/aura/core/domain/model/Models.kt").read_text(encoding="utf-8")
    api_source = (ROOT / "shared/src/commonMain/kotlin/com/aura/core/data/api/AuraApiClient.kt").read_text(encoding="utf-8")
    catalog_source = (ROOT / "shared/src/commonMain/kotlin/com/aura/feature/catalog/presentation/CatalogViewModel.kt").read_text(encoding="utf-8")

    assert '@SerialName("thumbnail_url") val thumbnailUrl: String? = null' in model_source
    assert 'thumbnailUrl = item["thumbnail_url"]?.jsonPrimitive?.contentOrNull' in api_source
    assert "product.thumbnailUrl" in catalog_source
    assert "loadImageDataFromUrl" in api_source
    assert "thumbnailDataByProductId" in catalog_source


def test_product_detail_hydrates_streaming_photo_urls():
    api_source = (ROOT / "shared/src/commonMain/kotlin/com/aura/core/data/api/AuraApiClient.kt").read_text(encoding="utf-8")

    assert "hydrateProductDetailPhotos" in api_source
    assert "product.photos.orEmpty().map { photo -> hydratePhotoDataFromUrl(photo) }" in api_source


def test_product_detail_passes_assistant_context_to_rag_chat():
    detail_source = (ROOT / "shared/src/commonMain/kotlin/com/aura/feature/product/ProductDetailScreen.kt").read_text(encoding="utf-8")
    repository_source = (ROOT / "shared/src/commonMain/kotlin/com/aura/feature/chat/data/repository/ChatRepositoryImpl.kt").read_text(encoding="utf-8")
    view_model_source = (ROOT / "shared/src/commonMain/kotlin/com/aura/feature/chat/presentation/ChatViewModel.kt").read_text(encoding="utf-8")

    assert "chatNavigationState.startProductChat(detail.assistantContext)" in detail_source
    assert "chatApi.queryRagChat(token, message, sessionId, productContext)" in repository_source
    assert "productContext = null" not in view_model_source
