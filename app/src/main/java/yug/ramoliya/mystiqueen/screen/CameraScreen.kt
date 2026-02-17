package yug.ramoliya.daymaker.screen

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onImageCaptured: (File) -> Unit,
    onVideoCaptured: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isRecording by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf<CaptureMode>(CaptureMode.PHOTO) }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    if (!permissionsState.allPermissionsGranted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Camera permission is required")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Grant Permission")
            }
        }
        return
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var recording: Recording? by remember { mutableStateOf(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    // Initialize camera when screen is shown
    LaunchedEffect(Unit) {
        try {
            val cameraProvider = cameraProviderFuture.get()
            // Camera will be bound when preview view is created
        } catch (e: Exception) {
            cameraError = "Failed to initialize camera: ${e.message}"
            e.printStackTrace()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                // Setup camera asynchronously
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        val imageCap = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        val recorder = Recorder.Builder()
                            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                            .build()

                        val videoCap = VideoCapture.withOutput(recorder)

                        imageCapture = imageCap
                        videoCapture = videoCap

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCap,
                            videoCap
                        )
                        cameraError = null
                    } catch (e: Exception) {
                        cameraError = "Camera error: ${e.message}"
                        e.printStackTrace()
                    }
                }

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Show error if camera fails
        cameraError?.let { error ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = error,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurface)
            }

            Row {
                IconButton(
                    onClick = { captureMode = CaptureMode.PHOTO },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (captureMode == CaptureMode.PHOTO)
                            MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, "Photo")
                }

                IconButton(
                    onClick = { captureMode = CaptureMode.VIDEO },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (captureMode == CaptureMode.VIDEO)
                            MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                ) {
                    Icon(Icons.Default.Videocam, "Video")
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (captureMode) {
                    CaptureMode.PHOTO -> {
                        IconButton(
                            onClick = {
                                capturePhoto(context, imageCapture) { file ->
                                    onImageCaptured(file)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                "Capture",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    CaptureMode.VIDEO -> {
                        IconButton(
                            onClick = {
                                if (!isRecording) {
                                    startRecording(context, videoCapture) { file ->
                                        onVideoCaptured(file)
                                    }
                                    isRecording = true
                                } else {
                                    stopRecording(recording) {
                                        isRecording = false
                                        onDismiss()
                                    }
                                }
                            },
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isRecording)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                                if (isRecording) "Stop" else "Record",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class CaptureMode {
    PHOTO, VIDEO
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    onPhotoCaptured: (File) -> Unit
) {
    val imageCap = imageCapture ?: return

    val photoFile = File(
        context.cacheDir,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
    )

    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCap.takePicture(
        outputFileOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onPhotoCaptured(photoFile)
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}

private fun startRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>?,
    onVideoCaptured: (File) -> Unit
) {
    val videoCap = videoCapture ?: return

    val videoFile = File(
        context.cacheDir,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".mp4"
    )

    val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
        context.contentResolver,
        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    )
        .setContentValues(android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, videoFile.name)
        })
        .build()

    val recording = videoCap.output
        .prepareRecording(context, mediaStoreOutputOptions)
        .start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    // Recording started
                }
                is VideoRecordEvent.Finalize -> {
                    if (!event.hasError()) {
                        onVideoCaptured(videoFile)
                    }
                }
            }
        }
}

private fun stopRecording(recording: Recording?, onStopped: () -> Unit = {}) {
    try {
        recording?.stop()
        onStopped()
    } catch (e: Exception) {
        e.printStackTrace()
        onStopped()
    }
}

