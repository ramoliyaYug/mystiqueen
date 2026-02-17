package yug.ramoliya.mystiqueen

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import yug.ramoliya.mystiqueen.screen.ChatScreen
import yug.ramoliya.mystiqueen.viewmodel.ChatViewModel
import yug.ramoliya.mystiqueen.ui.theme.MystiqueenTheme

class MainActivity : ComponentActivity() {

    private var viewModel: ChatViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable full screen mode
        enableFullScreen()

        enableEdgeToEdge()

        setContent {
            MystiqueenTheme {
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

    override fun onDestroy() {
        super.onDestroy()
        // Set offline status when activity is destroyed
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