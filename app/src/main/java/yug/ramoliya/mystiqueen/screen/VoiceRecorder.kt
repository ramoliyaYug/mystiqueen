package yug.ramoliya.mystiqueen.screen

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceRecorderDialog(
    onRecordingComplete: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0L) }
    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }

    val permissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (permissionState.status != com.google.accompanist.permissions.PermissionStatus.Granted) {
            permissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordingTime++
            }
        } else {
            recordingTime = 0
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Voice Recorder") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isRecording) {
                    Text(
                        text = formatTime(recordingTime),
                        style = MaterialTheme.typography.headlineMedium
                    )
                } else {
                    Text("Tap to start recording")
                }

                IconButton(
                    onClick = {
                        if (!isRecording) {
                            startRecording(context) { file ->
                                audioFile = file
                                mediaRecorder = createRecorder(file)
                                mediaRecorder?.start()
                                isRecording = true
                            }
                        } else {
                            stopRecording(mediaRecorder) {
                                audioFile?.let { onRecordingComplete(it) }
                                mediaRecorder = null
                                isRecording = false
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        ),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop" else "Record",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (isRecording) {
                Button(
                    onClick = {
                        stopRecording(mediaRecorder) {
                            audioFile?.let { onRecordingComplete(it) }
                            mediaRecorder = null
                            isRecording = false
                            onDismiss()
                        }
                    }
                ) {
                    Text("Stop & Send")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun startRecording(context: Context, onFileCreated: (File) -> Unit) {
    val audioFile = File(
        context.cacheDir,
        "voice_${System.currentTimeMillis()}.m4a"
    )
    onFileCreated(audioFile)
}

private fun createRecorder(outputFile: File): MediaRecorder {
    return MediaRecorder().apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setOutputFile(outputFile.absolutePath)
        prepare()
    }
}

private fun stopRecording(recorder: MediaRecorder?, onStopped: () -> Unit) {
    try {
        recorder?.apply {
            stop()
            release()
        }
        onStopped()
    } catch (e: Exception) {
        e.printStackTrace()
        onStopped()
    }
}

private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}

