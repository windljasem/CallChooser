package android.template.ui

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
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

            Text(
                text = "Нормалізований номер: $normalized",
                style = MaterialTheme.typography.bodySmall
            )

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
        }
    }

    private fun normalizeNumber(input: String): String {
        var digits = input.filter { it.isDigit() }

        // Україна
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
