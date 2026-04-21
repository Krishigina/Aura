package com.aura.feature.product

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aura.core.data.api.AuraApiClient
import com.aura.core.domain.model.Product
import com.aura.core.domain.model.ProductPhoto
import com.aura.core.ui.components.ProductNetworkImage
import com.aura.core.ui.theme.AppState
import com.aura.core.ui.theme.AuraPalette
import com.aura.core.ui.theme.auraThemeColors
import kotlin.math.absoluteValue

@Composable
fun ProductDetailScreen(
    productId: String,
    apiClient: AuraApiClient,
    onBack: () -> Unit
) {
    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val colors = auraThemeColors(dark)

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var product by remember { mutableStateOf<Product?>(null) }
    var productPhotos by remember { mutableStateOf<List<ProductPhoto>>(emptyList()) }
    var photosLoading by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        loading = true
        error = null
        photosLoading = true
        val resolved = runCatching {
            apiClient.getProducts().firstOrNull { it.id.toString() == productId }
        }
        resolved
            .onSuccess { found ->
                product = found
                if (found == null) error = "Продукт не найден"
                productPhotos = found?.let { apiClient.getProductPhotos(it.id) } ?: emptyList()
                photosLoading = false
            }
            .onFailure { throwable ->
                error = throwable.message ?: "Не удалось загрузить продукт"
                productPhotos = emptyList()
                photosLoading = false
            }
        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        ProductBackgroundBlobs()

        when {
            loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AuraPalette.BrandPrimaryBlue
                )
            }
            error != null || product == null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = error ?: "Продукт не найден",
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Вернитесь назад и выберите другой продукт",
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                val current = product ?: return@Box
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 80.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    ProductVisualCard(current, productPhotos, apiClient, photosLoading)
                    ProductMainInfoCard(current)
                    ProductTechnicalCard(current, productPhotos)
                }
            }
        }

        ProductTopBar(onBack = onBack)
    }
}

@Composable
private fun ProductBackgroundBlobs() {
    Canvas(modifier = Modifier.fillMaxSize().blur(90.dp)) {
        val width = size.width
        val height = size.height

        drawCircle(
            color = AuraPalette.BrandMint.copy(alpha = 0.22f),
            radius = width * 0.42f,
            center = Offset(width * 1.08f, height * 0.2f)
        )

        drawCircle(
            color = AuraPalette.BlobBlue.copy(alpha = 0.24f),
            radius = width * 0.5f,
            center = Offset(-width * 0.05f, height * 0.75f)
        )
    }
}

@Composable
private fun ProductTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад", tint = AuraPalette.TextPrimaryLight)
        }
        Text(
            text = "AURA",
            fontSize = 20.sp,
            fontWeight = FontWeight.Light,
            color = AuraPalette.TextPrimaryLight,
            letterSpacing = 4.sp
        )
        IconButton(onClick = {}) {
            Icon(Icons.Rounded.Info, contentDescription = "Инфо", tint = AuraPalette.TextPrimaryLight)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ProductVisualCard(
    product: Product,
    photos: List<ProductPhoto>,
    apiClient: AuraApiClient,
    photosLoading: Boolean
) {
    var fullscreenVisible by remember { mutableStateOf(false) }
    var fullscreenStartIndex by remember { mutableStateOf(0) }

    val photoUrls = remember(product.id, photos) {
        photos.mapNotNull { photo ->
            val id = photo.id.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            apiClient.getProductPhotoUrl(product.id, id)
        }
    }
    val pagerState = rememberPagerState(pageCount = { photoUrls.size.coerceAtLeast(1) })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
    ) {
        if (photoUrls.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                val alpha = 0.65f + (1f - pageOffset.coerceIn(0f, 1f)) * 0.35f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.alpha = alpha }
                        .clickable {
                            fullscreenStartIndex = page
                            fullscreenVisible = true
                        }
                ) {
                    ProductNetworkImage(
                        url = photoUrls[page],
                        contentDescription = product.name ?: "Фото продукта",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        } else if (photosLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AuraPalette.BrandPrimaryBlue)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                AuraPalette.BrandMint.copy(alpha = 0.25f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = AuraPalette.TextSecondaryLight.copy(alpha = 0.25f),
                    modifier = Modifier.size(120.dp)
                )
            }
        }

        val compatibility = remember(product) { calculateCompatibility(product) }
        if (photoUrls.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${photoUrls.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AuraPalette.BrandPink)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$compatibility% совместимость",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val photoCount = photoUrls.size.takeIf { it > 0 } ?: (product.images?.size ?: 0)
            val indicators = if (photoCount <= 1) 1 else minOf(photoCount, 4)
            repeat(indicators) { index ->
                val active = index == pagerState.currentPage.coerceAtMost(indicators - 1)
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(if (active) 30.dp else 8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (active) 0.95f else 0.45f))
                )
            }
        }

        if (photoUrls.isNotEmpty()) {
            Text(
                text = "Нажмите на фото для полноэкранного просмотра",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }

    if (fullscreenVisible && photoUrls.isNotEmpty()) {
        ProductPhotoViewerDialog(
            photoUrls = photoUrls,
            initialPage = fullscreenStartIndex,
            productName = product.name ?: "Фото продукта",
            onDismiss = { fullscreenVisible = false }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ProductPhotoViewerDialog(
    photoUrls: List<String>,
    initialPage: Int,
    productName: String,
    onDismiss: () -> Unit
) {
    val start = initialPage.coerceIn(0, photoUrls.lastIndex)
    val pagerState = rememberPagerState(initialPage = start, pageCount = { photoUrls.size })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ProductNetworkImage(
                    url = photoUrls[page],
                    contentDescription = productName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White
                    )
                }

                Text(
                    text = "${pagerState.currentPage + 1}/${photoUrls.size}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "Свайп влево/вправо",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ProductMainInfoCard(product: Product) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.36f))
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = (product.brand ?: "AURA").uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = AuraPalette.TextSecondaryLight,
            letterSpacing = 1.3.sp
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name ?: "Без названия",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraPalette.TextPrimaryLight,
                    lineHeight = 28.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = product.description?.takeIf { it.isNotBlank() }
                        ?: product.desc?.takeIf { it.isNotBlank() }
                        ?: product.what_is_it?.takeIf { it.isNotBlank() }
                        ?: "Описание отсутствует",
                    fontSize = 14.sp,
                    color = AuraPalette.TextBodyLight,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = product.volume ?: "—",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    color = AuraPalette.TextPrimaryLight
                )
                Text(
                    text = "ОБЪЕМ",
                    fontSize = 10.sp,
                    color = AuraPalette.TextSecondaryLight,
                    letterSpacing = 0.8.sp
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = buildList {
                product.product_type?.takeIf { it.isNotBlank() }?.let(::add)
                product.area?.takeIf { it.isNotBlank() }?.let(::add)
                product.for_whom?.takeIf { it.isNotBlank() }?.let(::add)
                product.category?.takeIf { it.isNotBlank() }?.let(::add)
                product.application_time?.takeIf { it.isNotBlank() }?.let(::add)
                product.skin_type?.filter { it.isNotBlank() }?.firstOrNull()?.let(::add)
            }

            if (chips.isEmpty()) {
                ProductChip(text = "Данные уточняются", isPrimary = false)
            } else {
                chips.forEachIndexed { index, chip ->
                    ProductChip(text = chip, isPrimary = index == 0)
                }
            }
        }

        ProductBentoCard {
            SectionTitle("Активные компоненты")
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = product.active_ingredient?.takeIf { it.isNotBlank() } ?: "Не указаны",
                color = AuraPalette.TextBodyLight,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProductBentoCard(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Sell, contentDescription = null, tint = AuraPalette.TextSecondaryLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    SectionTitle("Применение")
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Время: ${product.application_time ?: "—"}", color = AuraPalette.TextBodyLight, fontSize = 13.sp)
                Text("Зона: ${product.area ?: "—"}", color = AuraPalette.TextBodyLight, fontSize = 13.sp)
                Text("Инструкция: ${product.application_info ?: "—"}", color = AuraPalette.TextBodyLight, fontSize = 13.sp)
            }

            ProductBentoCard(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Public, contentDescription = null, tint = AuraPalette.TextSecondaryLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    SectionTitle("Происхождение")
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Страна: ${product.country ?: "—"}", color = AuraPalette.TextBodyLight, fontSize = 13.sp)
                Text("Country origin: ${product.country_origin ?: "—"}", color = AuraPalette.TextBodyLight, fontSize = 13.sp)
                Text("Производитель: ${product.manufacturer ?: "—"}", color = AuraPalette.TextBodyLight, fontSize = 13.sp)
            }
        }

        var expanded by remember { mutableStateOf(false) }
        val rotation = if (expanded) 180f else 0f
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(top = 4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ПОЛНЫЙ СОСТАВ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraPalette.TextSecondaryLight,
                    letterSpacing = 0.9.sp
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = AuraPalette.TextSecondaryLight,
                    modifier = Modifier.rotate(rotation)
                )
            }

            if (expanded) {
                Text(
                    text = product.composition?.takeIf { it.isNotBlank() } ?: "Состав не указан",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = AuraPalette.TextBodyLight,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ProductTechnicalCard(product: Product, photos: List<ProductPhoto>) {
    val purposeItems = remember(product) { splitToItems(product.purpose?.joinToString(", ")) }
    val skinTypeItems = remember(product) { splitToItems(product.skin_type?.joinToString(", ")) }
    val ingredientItems = remember(product) { splitToItems(product.active_ingredient) }
    val usageSteps = remember(product) { buildUsageSteps(product.application_info) }
    val imagePaths = remember(product) { product.images?.takeIf { it.isNotEmpty() } ?: emptyList() }
    val hasVideo = product.has_video == true || !product.video.isNullOrBlank()
    val productPrice = remember(product) {
        val price = product.price?.takeIf { it.isNotBlank() }
        val currency = product.currency?.takeIf { it.isNotBlank() }
        when {
            price != null && currency != null -> "$price $currency"
            price != null -> price
            else -> "Цена не указана"
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionTitle("О продукте в деталях")

        ProductBentoCard {
            Text(
                text = "Кому и для чего",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AuraPalette.TextPrimaryLight
            )
            Text(
                text = "Помогает быстро оценить, подойдет ли средство под ваш сценарий ухода.",
                fontSize = 12.sp,
                color = AuraPalette.TextSecondaryLight,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProductChip(text = product.product_type ?: "Тип не указан", isPrimary = true)
                ProductChip(text = product.for_whom ?: "Для всех")
                ProductChip(text = product.category ?: "Без категории")
                ProductChip(text = product.segment ?: "Segment не указан")
                purposeItems.take(3).forEach { ProductChip(text = it) }
                skinTypeItems.take(3).forEach { ProductChip(text = it) }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ProductBentoCard(modifier = Modifier.weight(1f)) {
                Text("Применение", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AuraPalette.TextPrimaryLight)
                Spacer(modifier = Modifier.height(10.dp))
                Text("${product.application_time ?: "Время не указано"}", color = AuraPalette.TextBodyLight, fontSize = 13.sp)
                Text(
                    text = "${product.area ?: "Зона не указана"}",
                    color = AuraPalette.TextSecondaryLight,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            ProductBentoCard(modifier = Modifier.weight(1f)) {
                Text("Формат", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AuraPalette.TextPrimaryLight)
                Spacer(modifier = Modifier.height(10.dp))
                Text(product.volume ?: "Объем не указан", color = AuraPalette.TextBodyLight, fontSize = 13.sp)
                Text(productPrice, color = AuraPalette.TextSecondaryLight, fontSize = 12.sp)
            }
        }

        ProductBentoCard {
            Text("Активные компоненты", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AuraPalette.TextPrimaryLight)
            Text(
                text = "Главные ингредиенты и ожидаемый эффект.",
                fontSize = 12.sp,
                color = AuraPalette.TextSecondaryLight,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (ingredientItems.isEmpty()) {
                Text("Ингредиенты не указаны", color = AuraPalette.TextSecondaryLight, fontSize = 12.sp)
            } else {
                ingredientItems.take(6).forEach { item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(6.dp)
                                .background(AuraPalette.BrandMint, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item,
                            fontSize = 13.sp,
                            color = AuraPalette.TextBodyLight,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        ProductBentoCard {
            Text("Как использовать", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AuraPalette.TextPrimaryLight)
            Text(
                text = "Короткая пошаговая инструкция по применению.",
                fontSize = 12.sp,
                color = AuraPalette.TextSecondaryLight,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (usageSteps.isEmpty()) {
                Text("Инструкция не указана", color = AuraPalette.TextSecondaryLight, fontSize = 12.sp)
            } else {
                usageSteps.forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(AuraPalette.BrandMint, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString().padStart(2, '0'),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuraPalette.TextPrimaryLight
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = step,
                            fontSize = 13.sp,
                            color = AuraPalette.TextBodyLight,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        ProductBentoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (hasVideo) Icons.Rounded.VideoLibrary else Icons.Rounded.Public,
                    contentDescription = null,
                    tint = AuraPalette.TextSecondaryLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Медиа и происхождение", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AuraPalette.TextPrimaryLight)
            }
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Фото: ${photos.size} • Изображения: ${imagePaths.size} • Видео: ${if (hasVideo) "есть" else "нет"}",
                fontSize = 13.sp,
                color = AuraPalette.TextBodyLight
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (photos.isNotEmpty()) "Фото продукта доступны и используются в карточке." else "Фото пока не добавлены, показывается нейтральная заглушка.",
                fontSize = 12.sp,
                color = AuraPalette.TextSecondaryLight,
                lineHeight = 18.sp
            )

            if (hasVideo) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "У продукта есть видео-материалы.",
                    fontSize = 12.sp,
                    color = AuraPalette.TextSecondaryLight
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Страна: ${product.country ?: "—"} • Происхождение: ${product.country_origin ?: "—"}",
                fontSize = 12.sp,
                color = AuraPalette.TextSecondaryLight
            )
            Text(
                text = "Производитель: ${product.manufacturer ?: "—"}",
                fontSize = 12.sp,
                color = AuraPalette.TextSecondaryLight
            )
        }

        ProductBentoCard {
            Text("Паспорт продукта", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AuraPalette.TextPrimaryLight)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ID ${product.id} • ${product.created_at ?: "дата не указана"}",
                fontSize = 12.sp,
                color = AuraPalette.TextSecondaryLight
            )
            if (!product.what_is_it.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = product.what_is_it,
                    fontSize = 13.sp,
                    color = AuraPalette.TextBodyLight,
                    lineHeight = 19.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!product.desc.isNullOrBlank() && product.desc != product.description) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = product.desc,
                    fontSize = 12.sp,
                    color = AuraPalette.TextSecondaryLight,
                    lineHeight = 18.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProductChip(text: String, isPrimary: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (isPrimary) {
                    Modifier.background(AuraPalette.BrandMint)
                } else {
                    Modifier
                        .background(Color.White.copy(alpha = 0.42f))
                        .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(50))
                }
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
            color = AuraPalette.TextBodyLight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProductBentoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.32f))
            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = AuraPalette.TextSecondaryLight,
        letterSpacing = 0.9.sp
    )
}

private fun calculateCompatibility(product: Product): Int {
    val points = listOfNotNull(
        product.skin_type?.takeIf { it.isNotEmpty() }?.let { 18 },
        product.purpose?.takeIf { it.isNotEmpty() }?.let { 14 },
        product.for_whom?.takeIf { it.isNotBlank() }?.let { 10 },
        product.application_time?.takeIf { it.isNotBlank() }?.let { 10 },
        product.area?.takeIf { it.isNotBlank() }?.let { 10 },
        product.active_ingredient?.takeIf { it.isNotBlank() }?.let { 12 },
        product.composition?.takeIf { it.isNotBlank() }?.let { 10 },
        product.description?.takeIf { it.isNotBlank() }?.let { 8 },
        product.brand?.takeIf { it.isNotBlank() }?.let { 4 },
        product.category?.takeIf { it.isNotBlank() }?.let { 4 }
    ).sum()

    return points.coerceIn(45, 99)
}

private fun splitToItems(raw: String?): List<String> {
    return raw
        ?.split(',', ';', '\n', '|')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?: emptyList()
}

private fun buildUsageSteps(applicationInfo: String?): List<String> {
    val raw = applicationInfo?.trim().orEmpty()
    if (raw.isBlank()) return emptyList()

    val steps = raw
        .split('.', '•', '\n')
        .map { it.trim() }
        .filter { it.isNotBlank() }

    return if (steps.isEmpty()) listOf(raw) else steps.take(4)
}
