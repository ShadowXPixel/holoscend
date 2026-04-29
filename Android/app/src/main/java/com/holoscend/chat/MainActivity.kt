package com.holoscend.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import java.io.File
import com.holoscend.chat.ui.theme.ChatTheme

class MainActivity : ComponentActivity() {

    external fun stringFromJNI(): String

    init {
        System.loadLibrary("native-lib")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatApp(getResponse = { stringFromJNI() })
        }
    }
}

@Composable
fun ChatApp(getResponse: () -> String) {
    ChatTheme {
        Surface {
            ChatScreen(getResponse = getResponse)
        }
    }
}

@Composable
fun ChatScreen(getResponse: () -> String) {

    val context = LocalContext.current

    var userInput by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("Model response will appear here") }

    // 🔥 test file creation
    LaunchedEffect(Unit) {
        val file = File(context.filesDir, "test.txt")
        file.writeText("hello bro")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Top: response
        Text(
            text = responseText,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
        )

        // Bottom: input + button
        Column(modifier = Modifier.fillMaxWidth()) {

            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Type your message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Button(
                onClick = {
                    responseText = getResponse()
                    userInput = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send")
            }
        }
    }
}
