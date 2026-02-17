package yug.ramoliya.daymaker.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import yug.ramoliya.mystiqueen.constants.Constants
import yug.ramoliya.mystiqueen.constants.Constants.CURRENT_USER_ID
import yug.ramoliya.mystiqueen.constants.toChatTime
import yug.ramoliya.mystiqueen.data.MessageModel

@Composable
fun MessageBubble(
    message: MessageModel,
    modifier: Modifier = Modifier,
    onMediaClick: (String, String) -> Unit = { _, _ -> }
) {

    val isMe = message.senderId == CURRENT_USER_ID

    val bubbleColor = if (isMe)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)

    val textColor = if (isMe)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {

        Column(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(10.dp)
        ) {

            // ---------- CONTENT ----------
            when (message.type) {

                Constants.TYPE_TEXT -> {
                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 15.sp
                    )
                }

                Constants.TYPE_IMAGE -> {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "image",
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onMediaClick(message.mediaUrl, Constants.TYPE_IMAGE)
                            },
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                Constants.TYPE_VIDEO -> {
                    VideoMessageBubble(
                        videoUrl = message.mediaUrl,
                        textColor = textColor,
                        onVideoClick = { onMediaClick(message.mediaUrl, Constants.TYPE_VIDEO) }
                    )
                }

                Constants.TYPE_AUDIO -> {
                    AudioMessageBubble(
                        audioUrl = message.mediaUrl,
                        textColor = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ---------- TIME + STATUS ----------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = message.timestamp.toChatTime(),
                    fontSize = 9.sp,
                    color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray
                )

                if (isMe) {
                    Text(
                        text = statusIcon(message.status),
                        fontSize = 11.sp,
                        color = when (message.status) {
                            Constants.STATUS_SEEN -> Color(0xFF64B5F6)
                            Constants.STATUS_DELIVERED -> Color.White.copy(alpha = 0.9f)
                            else -> Color.White.copy(alpha = 0.7f)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------- VIDEO MESSAGE BUBBLE ----------
@Composable
fun VideoMessageBubble(
    videoUrl: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    onVideoClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable {
                onVideoClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play video",
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )

        Text(
            text = "Video",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp)
        )
    }
}

// ---------- AUDIO MESSAGE BUBBLE ----------
@Composable
fun AudioMessageBubble(
    audioUrl: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var audioPlayer by remember { mutableStateOf<androidx.media3.exoplayer.ExoPlayer?>(null) }

    LaunchedEffect(audioUrl) {
        audioPlayer = androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            val mediaItem = androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(audioUrl))
            setMediaItem(mediaItem)
            prepare()

            addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    isPlaying = isPlayingNow
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer?.release()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.15f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = {
                audioPlayer?.let { player ->
                    if (isPlaying) {
                        player.pause()
                        isPlaying = false
                    } else {
                        player.play()
                        isPlaying = true
                    }
                }
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = if (isPlaying) "Playing..." else "Audio",
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ---------- STATUS ICON ----------
@Composable
private fun statusIcon(status: String): String {
    return when (status) {
        Constants.STATUS_SENT -> "✓"
        Constants.STATUS_DELIVERED -> "✓✓"
        Constants.STATUS_SEEN -> "✓✓"
        else -> ""
    }
}
