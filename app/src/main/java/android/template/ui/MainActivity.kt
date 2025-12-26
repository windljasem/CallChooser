package android.template.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallChooserUI()
        }
    }
}

@Composable
fun CallChooserUI() {
    var phone by remember { mutableStateOf("") }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text("CallChooser", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone number") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                Text("GSM Call")
            }

            Button(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                Text("WhatsApp")
            }

            Button(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                Text("Telegram")
            }

            Button(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                Text("Viber")
            }

            Button(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                Text("Signal")
            }
        }
    }
}
