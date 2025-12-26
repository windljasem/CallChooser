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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            CallChooserScreen()
        }
    }

    @Composable
    fun CallChooserScreen() {
        var query by remember { mutableStateOf("") }
        var normalized by remember { mutableStateOf("") }
        var results by remember { mutableStateOf(listOf<Pair<String, String>>()) }
        val scope = rememberCoroutineScope()

        Column(Modifier.padding(16.dp)) {

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
                label = { Text("Імʼя або номер") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text("Нормалізований: $normalized", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))

            LazyColumn {
                items(results) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                query = item.second
                                normalized = normalizeNumber(item.second)
                            }
                            .padding(12.dp)
                    ) {
                        Text(item.first)
                        Text(item.second, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { openGsm(normalized) }) { Text("GSM") }
                Button(onClick = { openWhatsApp(normalized) }) { Text("WhatsApp") }
                Button(onClick = { openTelegram(normalized) }) { Text("Telegram") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { openViber(normalized) }) { Text("Viber") }
                Button(onClick = { openSignal(normalized) }) { Text("Signal") }
            }
        }
    }

    // --- Actions ---

    private fun openGsm(num: String) {
    if (num.isBlank()) return
    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num")))
}

private fun openWhatsApp(num: String) {
    openAppOrFallback(
        Uri.parse("https://wa.me/$num"),
        "com.whatsapp",
        num
    )
}

private fun openTelegram(num: String) {
    openAppOrFallback(
        Uri.parse("tg://resolve?phone=$num"),
        "org.telegram.messenger",
        num
    )
}

private fun openViber(num: String) {
    openAppOrFallback(
        Uri.parse("viber://chat?number=$num"),
        "com.viber.voip",
        num
    )
}

private fun openSignal(num: String) {
    openAppOrFallback(
        Uri.parse("sgnl://send?phone=$num"),
        "org.thoughtcrime.securesms",
        num
    )
}

private fun openAppOrFallback(uri: Uri, packageName: String, num: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(packageName)
        startActivity(intent)
    } catch (e: Exception) {
        // якщо месенджера немає — GSM
        openGsm(num)
    }
}

    // --- Utils ---

    private fun normalizeNumber(input: String): String {
        var digits = input.filter { it.isDigit() }

        if (digits.startsWith("0") && digits.length == 10) {
            digits = "38$digits"
        }

        if (digits.startsWith("380") && digits.length > 12) {
            digits = digits.take(12)
        }

        return digits
    }

    private suspend fun searchContactsAsync(q: String): List<Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<Pair<String, String>>()

            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?",
                arrayOf("%$q%", "%$q%"),
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(0)
                    val number = it.getString(1)
                    list.add(name to number)
                }
            }

            list
        }
    }
}
