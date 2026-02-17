package yug.ramoliya.mystiqueen.constants

object Constants {

    // ---------- USER ----------
    // Change this in second app
    const val CURRENT_USER_ID = "daymaker"
    const val OTHER_USER_ID = "mystiqueen"

    // ---------- CHAT ----------
    const val CHAT_ID = "mystiqueen_daymaker"
    const val MESSAGES_NODE = "messages"
    const val CHATS_NODE = "chats"
    const val STATUS_NODE = "status"
    const val TYPING_NODE = "typing"

    // ---------- MESSAGE TYPES ----------
    const val TYPE_TEXT = "text"
    const val TYPE_IMAGE = "image"
    const val TYPE_VIDEO = "video"
    const val TYPE_AUDIO = "audio"

    // ---------- MESSAGE STATUS ----------
    const val STATUS_SENT = "sent"
    const val STATUS_DELIVERED = "delivered"
    const val STATUS_SEEN = "seen"

    // ---------- GITHUB ----------
    // NEVER expose real token in production
    const val GITHUB_BASE_URL = "https://api.github.com/"
    const val GITHUB_OWNER = "ramoliyaYug"
    const val GITHUB_REPO = "upload"
    const val GITHUB_BRANCH = "main"
    const val GITHUB_TOKEN = ""

    // ---------- MEDIA FOLDERS ----------
    const val IMAGE_FOLDER = "images/"
    const val VIDEO_FOLDER = "videos/"
    const val AUDIO_FOLDER = "audios/"

    // ---------- LIMITS ----------
    const val MAX_IMAGE_SIZE_MB = 5
    const val MAX_VIDEO_SIZE_MB = 25
    const val MAX_AUDIO_SIZE_MB = 10

    // ---------- TIMING ----------
    const val TYPING_TIMEOUT_MS = 2000L
}