package com.aura.feature.catalog

import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeBase64ToImageBitmap(base64Data: String): ImageBitmap?
