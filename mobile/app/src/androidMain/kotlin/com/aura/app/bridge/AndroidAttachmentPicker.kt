package com.aura.app.bridge

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResult
import com.aura.feature.chat.ChatAttachmentPickerBridge
import com.aura.feature.chat.presentation.model.PickedChatAttachment

internal fun Context.buildAttachmentChooserIntent(): Intent {
    val mimeTypes = arrayOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "image/jpeg",
        "image/png",
    )

    val openDocumentIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
        putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
    }

    val mediaIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
        type = "image/*"
    }

    return Intent.createChooser(openDocumentIntent, "Выберите источник").apply {
        putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(mediaIntent))
    }
}

internal fun Context.deliverPickedAttachment(result: ActivityResult) {
    if (result.resultCode != Activity.RESULT_OK) return

    result.data?.data?.let { selectedUri ->
        readPickedAttachment(selectedUri)?.let(ChatAttachmentPickerBridge::deliver)
        return
    }

    result.data?.clipData?.getItemAt(0)?.uri?.let { selectedUri ->
        readPickedAttachment(selectedUri)?.let(ChatAttachmentPickerBridge::deliver)
    }
}

private fun Context.readPickedAttachment(uri: Uri): PickedChatAttachment? {
    val contentType = contentResolver.getType(uri) ?: return null
    val filename = queryDisplayName(uri) ?: "attachment"
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return PickedChatAttachment(filename = filename, contentType = contentType, bytes = bytes)
}

private fun Context.queryDisplayName(uri: Uri): String? {
    return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
    }
}
