from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def test_product_detail_hydrates_streaming_photo_urls_in_network_client():
    source = (ROOT / "shared/src/commonMain/kotlin/com/aura/core/data/api/client/ProductNetworkClient.kt").read_text(encoding="utf-8")

    assert "hydrateProductDetailPhotos" in source
    assert "return hydrateProductDetailPhotos(" in source
    assert "product.photos.orEmpty().map { photo -> hydratePhotoDataFromUrl(photo) }" in source


def test_product_chat_header_shows_product_back_arrow():
    chat_screen_source = (ROOT / "shared/src/commonMain/kotlin/com/aura/feature/chat/ChatScreen.kt").read_text(encoding="utf-8")
    chat_header_source = (ROOT / "shared/src/commonMain/kotlin/com/aura/feature/chat/components/ChatHeader.kt").read_text(encoding="utf-8")
    navigation_source = (ROOT / "shared/src/commonMain/kotlin/com/aura/core/navigation/MainNavigationGraph.kt").read_text(encoding="utf-8")

    assert "onBack: () -> Unit = {}" in chat_screen_source
    assert "showBackButton = uiState.productContextActive" in chat_screen_source
    assert "onBack = onBack" in chat_screen_source
    assert "Icons.Rounded.ArrowBack" in chat_header_source
    assert "contentDescription = \"Назад\"" in chat_header_source
    assert "ChatScreen(" in navigation_source
    assert "onBack = { navController.popBackStack() }" in navigation_source
