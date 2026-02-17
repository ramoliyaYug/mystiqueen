package yug.ramoliya.mystiqueen.constants

import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.text.endsWith
import kotlin.text.isLowerCase
import kotlin.text.replaceFirstChar
import kotlin.text.substringAfterLast
import kotlin.text.titlecase

// ---------- TIME FORMAT ----------

// Convert Long timestamp to "hh:mm a"  (10:45 PM)
fun Long.toChatTime(): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(this))
}

// Convert Long timestamp to "dd MMM yyyy"
fun Long.toChatDate(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

// Check if two timestamps are same day
fun Long.isSameDay(other: Long): Boolean {
    val d1 = Date(this)
    val d2 = Date(other)
    return DateFormat.format("yyyyMMdd", d1) ==
            DateFormat.format("yyyyMMdd", d2)
}

// ---------- STRING ----------

// Capitalize first letter
fun String.capitalizeFirst(): String {
    return replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault())
        else it.toString()
    }
}

// ---------- ID / RANDOM ----------

// Generate random message id
fun generateMessageId(): String {
    return UUID.randomUUID().toString()
}

// ---------- FILE / MEDIA ----------

// Get file extension from url
fun String.getFileExtension(): String {
    return substringAfterLast('.', "")
}

// Simple check for image url
fun String.isImageUrl(): Boolean {
    return endsWith(".jpg") ||
            endsWith(".jpeg") ||
            endsWith(".png") ||
            endsWith(".webp")
}

// Simple check for video url
fun String.isVideoUrl(): Boolean {
    return endsWith(".mp4") ||
            endsWith(".mkv") ||
            endsWith(".webm")
}

// Simple check for audio url
fun String.isAudioUrl(): Boolean {
    return endsWith(".mp3") ||
            endsWith(".wav") ||
            endsWith(".aac")
}