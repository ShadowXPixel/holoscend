package com.holoscend.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.holoscend.chat.ui.theme.ChatTheme

class MainActivity : ComponentActivity() {

    external fun stringFromJNI(): String

    init {
        System.loadLibrary("native-lib")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatScreen(getResponse = { stringFromJNI() })
        }
    }
}
@Composable
fun ChatApp() {
    ChatTheme {
        Surface {
            ChatScreen(getResponse = { stringFromJNI() })        }
    }
}

@Composable
fun ChatScreen(getResponse: () -> String) {
    var userInput by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("Model response will appear here") }
    
    LaunchedEffect(Unit) {
        val file = File(context.filesDir, "test.txt")
        file.writeText("hello bro")
    }
   
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top section: Response display
        Text(
            text = responseText,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
        )

        // Bottom section: Input and Send button
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
