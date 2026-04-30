import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.holoscend.chat.ui.theme.ChatTheme

class MainActivity : ComponentActivity() {

    external fun stringFromJNI(): String
    external fun initLlama(modelPath: String): Boolean
    external fun completion(prompt: String): String

    init {
        System.loadLibrary("native-lib")
    }

    private val isModelLoaded = mutableStateOf(false)
    private val statusMessage = mutableStateOf("Initializing...")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissions()

        setContent {
            ChatApp(
                isLoaded = isModelLoaded.value,
                status = statusMessage.value,
                getResponse = { prompt -> 
                    completion(prompt)
                }
            )
        }
        
        loadModel()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
            }
        }
    }

    private fun loadModel() {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                val modelPath = "/sdcard/Download/tiny-llama-chat.gguf"
                if (File(modelPath).exists()) {
                    initLlama(modelPath)
                } else {
                    false
                }
            }
            if (success) {
                isModelLoaded.value = true
                statusMessage.value = "Model Loaded!"
            } else {
                statusMessage.value = "Error: Model not found at /sdcard/Download/tiny-llama-chat.gguf"
            }
        }
    }
}

@Composable
fun ChatApp(isLoaded: Boolean, status: String, getResponse: (String) -> String) {
    ChatTheme {
        Surface {
            ChatScreen(isLoaded = isLoaded, status = status, getResponse = getResponse)
        }
    }
}

@Composable
fun ChatScreen(isLoaded: Boolean, status: String, getResponse: (String) -> String) {
    val scope = rememberCoroutineScope()

    var userInput by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf(status) }
    var isThinking by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        if (!isThinking) responseText = status
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = if (isThinking) "Thinking..." else responseText,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
        )

        Column(modifier = Modifier.fillMaxWidth()) {

            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Type your message") },
                enabled = isLoaded && !isThinking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Button(
                onClick = {
                    val prompt = userInput
                    userInput = ""
                    isThinking = true
                    scope.launch(Dispatchers.IO) {
                        val resp = getResponse(prompt)
                        withContext(Dispatchers.Main) {
                            responseText = resp
                            isThinking = false
                        }
                    }
                },
                enabled = isLoaded && !isThinking && userInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send")
            }
        }
    }
}
