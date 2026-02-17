package yug.ramoliya.mystiqueen.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import yug.ramoliya.mystiqueen.constants.Constants
import yug.ramoliya.mystiqueen.constants.generateMessageId
import yug.ramoliya.mystiqueen.data.FirebaseRepository
import yug.ramoliya.mystiqueen.data.GithubRepository
import yug.ramoliya.mystiqueen.service.NotificationService
import yug.ramoliya.mystiqueen.data.MessageModel
import java.io.File

private const val TAG = "ChatViewModel"

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseRepo = FirebaseRepository()
    private val githubRepo = GithubRepository()
    private val notificationService = NotificationService(application)

    // ---------- UI STATE ----------

    private val _messages = MutableStateFlow<List<MessageModel>>(emptyList())
    val messages: StateFlow<List<MessageModel>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _otherUserStatus = MutableStateFlow("offline")
    val otherUserStatus: StateFlow<String> = _otherUserStatus.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress.asStateFlow()

    private var typingTimeoutJob: Job? = null
    private var statusUpdateJob: Job? = null

    init {
        observeMessages()
        observeTyping()
        observeOnlineStatus()
        setOnline(true)
    }

    // ---------- INPUT ----------

    fun onTextChange(text: String) {
        _inputText.value = text

        // Cancel previous typing timeout job
        typingTimeoutJob?.cancel()

        if (text.isNotBlank()) {
            firebaseRepo.setTyping(true)

            // Schedule typing timeout
            typingTimeoutJob = viewModelScope.launch {
                delay(Constants.TYPING_TIMEOUT_MS)
                firebaseRepo.setTyping(false)
            }
        } else {
            firebaseRepo.setTyping(false)
        }
    }

    // ---------- SEND TEXT ----------

    fun sendTextMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) {
            Log.w(TAG, "Attempted to send empty message")
            return
        }

        val messageId = generateMessageId()
        val message = MessageModel(
            messageId = messageId,
            senderId = Constants.CURRENT_USER_ID,
            receiverId = Constants.OTHER_USER_ID,
            type = Constants.TYPE_TEXT,
            text = text,
            mediaUrl = "",
            timestamp = System.currentTimeMillis(),
            status = Constants.STATUS_SENT
        )

        Log.d(TAG, "Sending text message: $messageId from ${Constants.CURRENT_USER_ID}")
        firebaseRepo.sendMessage(message)
        _inputText.value = ""
        firebaseRepo.setTyping(false)

        // Update message status to delivered after a short delay
        scheduleStatusUpdate(messageId, Constants.STATUS_DELIVERED)
    }

    // ---------- SEND MEDIA ----------

    fun sendMediaMessage(file: File, type: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _uploadProgress.value = 0

            try {
                Log.d(TAG, "Starting media upload: file=${file.name}, type=$type, size=${file.length()} bytes")

                // Validate file
                if (!file.exists()) {
                    _error.value = "File no longer exists"
                    _loading.value = false
                    Log.e(TAG, "File does not exist: ${file.absolutePath}")
                    return@launch
                }

                val result = githubRepo.uploadFile(file, type)

                if (result.isSuccess) {
                    val url = result.getOrNull().orEmpty()
                    if (url.isBlank()) {
                        _error.value = "Failed to get file URL from GitHub"
                        _loading.value = false
                        Log.e(TAG, "GitHub returned empty URL")
                        return@launch
                    }

                    Log.d(TAG, "GitHub upload successful, URL: $url")

                    val messageId = generateMessageId()
                    val message = MessageModel(
                        messageId = messageId,
                        senderId = Constants.CURRENT_USER_ID,
                        receiverId = Constants.OTHER_USER_ID,
                        type = type,
                        text = "",
                        mediaUrl = url,
                        timestamp = System.currentTimeMillis(),
                        status = Constants.STATUS_SENT
                    )

                    Log.d(TAG, "Sending media message: $messageId")
                    firebaseRepo.sendMessage(message)
                    _uploadProgress.value = 100

                    // Update message status to delivered
                    scheduleStatusUpdate(messageId, Constants.STATUS_DELIVERED)

                    // Clean up temp file
                    file.delete()
                    Log.d(TAG, "Temp file deleted: ${file.absolutePath}")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to upload media"
                    _error.value = errorMsg
                    Log.e(TAG, "GitHub upload failed: $errorMsg", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _error.value = "Upload error: ${e.message}"
                Log.e(TAG, "Media send exception", e)
            } finally {
                _loading.value = false
            }
        }
    }

    // ---------- UPDATE STATUS ----------

    private fun scheduleStatusUpdate(messageId: String, newStatus: String) {
        statusUpdateJob?.cancel()
        statusUpdateJob = viewModelScope.launch {
            delay(500) // Small delay to simulate delivery
            firebaseRepo.updateMessageStatus(messageId, newStatus)
        }
    }

    // ---------- DELETE ----------

    fun deleteMessage(messageId: String) {
        firebaseRepo.deleteMessage(messageId)
    }

    // ---------- ERROR ----------

    fun clearError() {
        _error.value = null
    }

    // ---------- OBSERVERS ----------

    private fun observeMessages() {
        viewModelScope.launch {
            firebaseRepo.listenMessages().collect { list ->
                val previousMessages = _messages.value
                _messages.value = list

                // Show notification for new messages when app is in background
                if (previousMessages.isNotEmpty() && list.size > previousMessages.size) {
                    val newMessages = list.filter { newMsg ->
                        !previousMessages.any { it.messageId == newMsg.messageId } &&
                                newMsg.senderId != Constants.CURRENT_USER_ID
                    }

                    newMessages.forEach { message ->
                        val messageText = when (message.type) {
                            Constants.TYPE_TEXT -> message.text
                            Constants.TYPE_IMAGE -> "📷 Image"
                            Constants.TYPE_VIDEO -> "🎥 Video"
                            Constants.TYPE_AUDIO -> "🎤 Voice message"
                            else -> "New message"
                        }
                        notificationService.showNotification(
                            messageText = messageText,
                            senderName = Constants.OTHER_USER_ID
                        )
                    }
                }
            }
        }
    }

    private fun observeTyping() {
        viewModelScope.launch {
            firebaseRepo.listenTyping(Constants.OTHER_USER_ID)
                .collect { typing ->
                    _isTyping.value = typing
                }
        }
    }

    private fun observeOnlineStatus() {
        viewModelScope.launch {
            firebaseRepo.listenOnlineStatus(Constants.OTHER_USER_ID)
                .collect { status ->
                    _otherUserStatus.value = status
                }
        }
    }

    // ---------- ONLINE ----------

    fun setOnline(isOnline: Boolean) {
        firebaseRepo.setOnlineStatus(isOnline)
    }

    override fun onCleared() {
        super.onCleared()
        setOnline(false)
        typingTimeoutJob?.cancel()
        statusUpdateJob?.cancel()
    }
}
