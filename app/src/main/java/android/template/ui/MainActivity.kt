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
import androidx.compose.foundation.isSystemInDarkTheme
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
            val dark = isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (dark) darkColorScheme() else lightColorScheme()
            ) {
                CallChooserUI()
            }
        }
    }

    // ================= UI =================

    @Composable
    fun CallChooserUI() {
        var query by remember { mutableStateOf("") }
        var normalized by remember { mutableStateOf("") }
        var results by remember { mutableStateOf(listOf<Pair<String, String>>()) }
        val scope = rememberCoroutineScope()

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                Text("CallChooser", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))

 // ===== BOTTOM SHEET CALL UI =====

var showSheet by remember { mutableStateOf(false) }

Button(
    onClick = { showSheet = true },
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
    shape = MaterialTheme.shapes.large
) {
    Text("Call", style = MaterialTheme.typography.titleMedium)
}

if (showSheet) {
    ModalBottomSheet(
        onDismissRequest = { showSheet = false }
    ) {
        Column(Modifier.padding(16.dp)) {

            Text("Оберіть спосіб", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            BottomItem("📞  GSM") {
                showSheet = false
                openGsm(normalized)
            }

            BottomItem("✈️  Telegram") {
                showSheet = false
                openTelegram(normalized)
            }

            BottomItem("🟢  WhatsApp") {
                showSheet = false
                openWhatsApp(normalized)
            }

            BottomItem("🟣  Viber") {
                showSheet = false
                openViber(normalized)
            }
        }
    }
}

                Spacer(Modifier.height(8.dp))

                if (results.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
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
                                    Text(item.first)
                                    Text(item.second, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Spacer(Modifier.height(8.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { openGsm(normalized) }, modifier = Modifier.weight(1f)) {
                                Text("GSM")
                            }
                            Button(onClick = { openWhatsApp(normalized) }, modifier = Modifier.weight(1f)) {
                                Text("WhatsApp")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { openTelegram(normalized) }, modifier = Modifier.weight(1f)) {
                                Text("Telegram")
                            }
                            Button(onClick = { openViber(normalized) }, modifier = Modifier.weight(1f)) {
                                Text("Viber")
                            }
                        }
                    }
                }
            }
        }
    }
// ===== BOTTOM SHEET ITEM =====

@Composable
fun BottomItem(title: String, action: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { action() }
            .padding(14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

    // ================= ACTIONS =================

    private fun openGsm(num: String) {
        if (num.isBlank()) return
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num")))
    }

    private fun openWhatsApp(num: String) {
        openAppOrFallback(Uri.parse("https://wa.me/$num"), "com.whatsapp", num)
    }

    private fun openTelegram(num: String) {
        openAppOrFallback(Uri.parse("tg://resolve?phone=$num"), "org.telegram.messenger", num)
    }

    private fun openViber(num: String) {
        openAppOrFallback(Uri.parse("viber://chat?number=$num"), "com.viber.voip", num)
    }

    private fun openAppOrFallback(uri: Uri, pkg: String, num: String) {
        try {
            val i = Intent(Intent.ACTION_VIEW, uri)
            i.setPackage(pkg)
            startActivity(i)
        } catch (e: Exception) {
            openGsm(num)
        }
    }

    // ================= UTILS =================

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
