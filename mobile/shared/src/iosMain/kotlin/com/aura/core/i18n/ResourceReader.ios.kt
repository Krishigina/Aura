package com.aura.core.i18n

import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

internal actual fun readResourceText(path: String): String? {
    val parts = path.split('/')
    val name = parts.lastOrNull()?.substringBeforeLast('.') ?: return null
    val ext = parts.lastOrNull()?.substringAfterLast('.', "") ?: ""
    val resourcePath = NSBundle.mainBundle.pathForResource(name, ext.ifBlank { null }, parts.dropLast(1).joinToString("/"))
        ?: return null
    return NSString.stringWithContentsOfFile(resourcePath, NSUTF8StringEncoding, null) as String?
}
