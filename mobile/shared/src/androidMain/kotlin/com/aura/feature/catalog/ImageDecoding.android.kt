package com.aura.feature.catalog

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodeBase64ToImageBitmap(base64Data: String): ImageBitmap? {
    return runCatching {
        val bytes = Base64.decode(base64Data, Base64.DEFAULT)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        val target = 1280
        var sample = 1
        while (maxDim / sample > target) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        }

        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    }.getOrNull()
}
