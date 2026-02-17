package yug.ramoliya.mystiqueen

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import yug.ramoliya.mystiqueen.R
import yug.ramoliya.mystiqueen.constants.Constants
import yug.ramoliya.mystiqueen.screen.ChatScreen
import yug.ramoliya.mystiqueen.ui.theme.MystiqueenTheme
import yug.ramoliya.mystiqueen.ui.theme.GradientPink
import yug.ramoliya.mystiqueen.ui.theme.GradientPurple
import yug.ramoliya.mystiqueen.ui.theme.GradientViolet
import yug.ramoliya.mystiqueen.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {

    private var viewModel: ChatViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable full screen mode
        enableFullScreen()

        enableEdgeToEdge()

        setContent {
            MystiqueenTheme (isFullScreen = true){
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1500)
                    showSplash = false
                }

                if (showSplash) {
                    SplashScreen()
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ChatScreenContent { vm ->
                            viewModel = vm
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Set online status when app comes to foreground
        viewModel?.setOnline(true)
    }

    override fun onPause() {
        super.onPause()
        // Set offline status when app goes to background
        viewModel?.setOnline(false)
    }

    override fun onStop() {
        super.onStop()
        // Ensure offline status is set when activity stops (more reliable than onDestroy)
        viewModel?.setOnline(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Final fallback to ensure offline status is set
        viewModel?.setOnline(false)
    }

    private fun enableFullScreen() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
}

@Composable
fun ChatScreenContent(onViewModelCreated: (ChatViewModel) -> Unit) {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
    val vm = viewModel<ChatViewModel>(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(application) as T
            }
        }
    )
    onViewModelCreated(vm)
    ChatScreen(vm = vm)
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GradientPurple,
                        GradientPink,
                        GradientViolet
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(GradientPink, GradientPurple)
                        ),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Transparent, shape = androidx.compose.foundation.shape.CircleShape)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = Constants.OTHER_USER_ID,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "cute little chat for two",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.9f)
                )
            )
        }
    }
}