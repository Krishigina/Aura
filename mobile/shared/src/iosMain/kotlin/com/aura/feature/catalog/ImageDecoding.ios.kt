package com.aura.feature.catalog

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.ktor.util.decodeBase64Bytes
import org.jetbrains.skia.Image

actual fun decodeBase64ToImageBitmap(base64Data: String): ImageBitmap? {
    return runCatching {
        val bytes = base64Data.decodeBase64Bytes()
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }.getOrNull()
}
