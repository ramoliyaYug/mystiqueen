package yug.ramoliya.mystiqueen.data

import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import yug.ramoliya.mystiqueen.constants.Constants
import yug.ramoliya.mystiqueen.constants.generateMessageId
import kotlin.sequences.ifEmpty

private const val TAG = "FirebaseRepository"

class FirebaseRepository {

    private val db: FirebaseDatabase = FirebaseDatabase.getInstance(
        "https://twinchatapp-be11d-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    private val messagesRef: DatabaseReference =
        db.getReference(Constants.CHATS_NODE)
            .child(Constants.CHAT_ID)
            .child(Constants.MESSAGES_NODE)

    private val typingRef: DatabaseReference =
        db.getReference(Constants.TYPING_NODE)

    private val statusRef: DatabaseReference =
        db.getReference(Constants.STATUS_NODE)

    // ---------- SEND MESSAGE ----------
    fun sendMessage(message: MessageModel) {
        val key = message.messageId.ifEmpty { generateMessageId() }
        val messageToSend = message.copy(messageId = key)

//        Log.d(TAG, "Attempting to send message to path: ${messagesRef.child(key).path}")
        Log.d(TAG, "Message data: $messageToSend")

        messagesRef.child(key).setValue(messageToSend)
            .addOnSuccessListener {
                Log.d(TAG, "Message sent successfully: $key")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send message: ${e.message}", e)
//                Log.e(TAG, "Error code: ${e.code}, Details: ${e.details}")
            }
    }

    // ---------- LISTEN RECENT MESSAGES (PAGINATED) ----------
    /**
     * Listen to the most recent [limit] messages and keep them live-updated.
     * This does NOT load the entire history, only a window from the bottom.
     */
    fun listenRecentMessages(limit: Int = 10): Flow<List<MessageModel>> = callbackFlow {

        val query = messagesRef
            .orderByChild("timestamp")
            .limitToLast(limit)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<MessageModel>()

                for (child in snapshot.children) {
                    val msg = child.getValue(MessageModel::class.java)
                    if (msg != null) {
                        list.add(msg)
                    }
                }

                trySend(list.sortedBy { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)

        awaitClose {
            messagesRef.removeEventListener(listener)
        }
    }

    /**
     * Load older messages once, before the given [beforeTimestamp].
     * Returns at most [limit] older messages (sorted oldest -> newest).
     */
    suspend fun loadOlderMessages(beforeTimestamp: Long, limit: Int = 10): List<MessageModel> {
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            val query = messagesRef
                .orderByChild("timestamp")
                .endAt((beforeTimestamp - 1).toDouble())
                .limitToLast(limit)

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<MessageModel>()
                    for (child in snapshot.children) {
                        val msg = child.getValue(MessageModel::class.java)
                        if (msg != null) {
                            list.add(msg)
                        }
                    }
                    if (cont.isActive) {
                        cont.resume(list.sortedBy { it.timestamp }, null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (cont.isActive) {
                        cont.resume(emptyList(), null)
                    }
                }
            })
        }
    }

    // ---------- UPDATE MESSAGE STATUS ----------
    fun updateMessageStatus(messageId: String, status: String) {
        messagesRef.child(messageId)
            .child("status")
            .setValue(status)
    }

    // ---------- DELETE MESSAGE ----------
    fun deleteMessage(messageId: String) {
        messagesRef.child(messageId).removeValue()
    }

    // ---------- TYPING INDICATOR ----------
    fun setTyping(isTyping: Boolean) {
        typingRef.child(Constants.CURRENT_USER_ID).setValue(isTyping)
            .addOnSuccessListener {
                Log.d(TAG, "Typing status updated: $isTyping")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update typing status: ${e.message}", e)
            }
    }

    fun listenTyping(otherUserId: String): Flow<Boolean> = callbackFlow {

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(Boolean::class.java) ?: false
                trySend(value)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        typingRef.child(otherUserId).addValueEventListener(listener)

        awaitClose {
            typingRef.child(otherUserId).removeEventListener(listener)
        }
    }

    // ---------- ONLINE STATUS ----------
    fun setOnlineStatus(isOnline: Boolean) {
        val status = if (isOnline) "online" else "offline"
        Log.d(TAG, "Setting online status: $status for user: ${Constants.CURRENT_USER_ID}")

        statusRef.child(Constants.CURRENT_USER_ID).setValue(status)
            .addOnSuccessListener {
                Log.d(TAG, "Status updated to: $status")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update status: ${e.message}", e)
            }
    }

    fun listenOnlineStatus(otherUserId: String): Flow<String> = callbackFlow {

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(String::class.java) ?: "offline"
                trySend(value)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        statusRef.child(otherUserId).addValueEventListener(listener)

        awaitClose {
            statusRef.child(otherUserId).removeEventListener(listener)
        }
    }
}
