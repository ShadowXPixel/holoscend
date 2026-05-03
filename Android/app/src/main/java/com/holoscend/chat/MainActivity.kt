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
import android.util.Log

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    external fun stringFromJNI(): String
    external fun initLlama(modelPath: String): Boolean
    external fun completion(prompt: String): String

    private val isModelLoaded = mutableStateOf(false)
    private val statusMessage = mutableStateOf("Initializing...")
    private var libraryLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate started")
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "Loading native-lib")
            System.loadLibrary("native-lib")
            libraryLoaded = true
            Log.d(TAG, "native-lib loaded successfully")
            Log.d(TAG, "Testing JNI: ${stringFromJNI()}")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native-lib", e)
            statusMessage.value = "Error: Native library not found!"
        }

        Log.d(TAG, "Checking permissions")
        checkPermissions()

        Log.d(TAG, "Setting content")
        setContent {
// ...
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
    Log.d(TAG, "checkPermissions called")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            Log.d(TAG, "Requesting MANAGE_EXTERNAL_STORAGE")
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start manage storage activity with package", e)
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to start manage storage activity", e2)
                }
            }

        }
    } else {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting READ_EXTERNAL_STORAGE")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
        }
    }
}

private fun loadModel() {
    Log.d(TAG, "loadModel called, libraryLoaded: $libraryLoaded")
    if (!libraryLoaded) return
    lifecycleScope.launch {
        val modelPath = "/sdcard/Download/tiny-llama-chat.gguf"
        Log.d(TAG, "Attempting to load model from $modelPath")
        val success = withContext(Dispatchers.IO) {
            if (File(modelPath).exists()) {
                Log.d(TAG, "Model file exists, initializing llama")
                initLlama(modelPath)
            } else {
                Log.w(TAG, "Model file not found at $modelPath")
                null // File not found
            }
        }
        Log.d(TAG, "initLlama result: $success")
        when (success) {
// ...
                true -> {
                    isModelLoaded.value = true
                    statusMessage.value = "Model Loaded!"
                }
                false -> {
                    statusMessage.value = "Error: Failed to load model from $modelPath (invalid file or memory issues)"
                }
                null -> {
                    statusMessage.value = "Error: Model not found at $modelPath. Please download tiny-llama-chat.gguf and place it in your Download folder."
                }
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
