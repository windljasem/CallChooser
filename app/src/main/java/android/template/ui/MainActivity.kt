package android.template.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 1)
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CallChooserUI()
            }
        }
    }

    @Composable
    fun CallChooserUI() {
        var query by remember { mutableStateOf("") }
        var normalized by remember { mutableStateOf("") }
        var results by remember { mutableStateOf(listOf<Pair<String, String>>()) }
        val scope = rememberCoroutineScope()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2C5E86))
                .padding(16.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 150.dp)
            ) {

                Text(
                    "CallChooser",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        normalized = normalizeNumber(it)

                        if (it.length >= 2) {
                            scope.launch {
                                results = searchContactsAsync(it)
                            }
                        } else {
                            results = emptyList()
                        }
                    },
                    label = { Text("Імʼя або номер", color = Color.White) },
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                normalized = ""
                                results = emptyList()
                            }) {
                                Text("✕", fontSize = 18.sp, color = Color.White)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                if (results.isNotEmpty()) {
                    LazyColumn {
                        items(results) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        query = item.second
                                        normalized = normalizeNumber(item.second)
                                        results = emptyList()
                                    }
                                    .padding(12.dp)
                            ) {
                                Text(item.first, color = Color.White)
                                Text(item.second, style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }
                    }
                }
            }

            // ===== FLOATING BUTTON BAR =====
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .imePadding()
                    .navigationBarsPadding()
            ) {

                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f).padding(end = 6.dp)) {
                        IconButtonStyled("GSM", R.drawable.ic_gsm) { openGsm(normalized) }
                    }
                    Box(Modifier.weight(1f).padding(start = 6.dp)) {
                        IconButtonStyled("Telegram", R.drawable.ic_telegram) { openTelegram(normalized) }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f).padding(end = 6.dp)) {
                        IconButtonStyled("WhatsApp", R.drawable.ic_whatsapp) { openWhatsApp(normalized) }
                    }
                    Box(Modifier.weight(1f).padding(start = 6.dp)) {
                        IconButtonStyled("Viber", R.drawable.ic_viber) { openViber(normalized) }
                    }
                }
            }
        }
    }

    @Composable
    fun IconButtonStyled(text: String, icon: Int, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE8E6FF),
                contentColor = Color(0xFF4C5DFF)
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(text)
            }
        }
    }

    // ===== actions & utils залишились без змін =====
}
