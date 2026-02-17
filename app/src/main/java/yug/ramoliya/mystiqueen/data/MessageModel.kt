package yug.ramoliya.mystiqueen.data

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class MessageModel(

    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",

    // text / image / video / audio
    val type: String = "",

    // used only for text messages
    val text: String = "",

    // used for image / video / audio
    val mediaUrl: String = "",

    // epoch millis
    val timestamp: Long = 0L,

    // sent / delivered / seen
    val status: String = "",

    // optional reply feature
    val replyToMessageId: String? = null,
    val replyPreviewText: String? = null

)
