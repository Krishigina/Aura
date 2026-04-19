package com.aura.feature.chat

import com.aura.feature.chat.presentation.model.PickedChatAttachment

object ChatAttachmentPickerBridge {
    var openPicker: (() -> Unit)? = null
    var onPicked: ((PickedChatAttachment) -> Unit)? = null

    fun openPicker() {
        openPicker?.invoke()
    }

    fun deliver(attachment: PickedChatAttachment) {
        onPicked?.invoke(attachment)
    }
}
