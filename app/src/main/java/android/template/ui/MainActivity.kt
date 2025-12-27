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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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
            CallChooserUI()
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
                    .padding(bottom = 160.dp)
            ) {

                BasicText(
                    "CallChooser",
                    style = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                )

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3A6F97), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            normalized = normalizeNumber(it)
                            if (it.length >= 2) {
                                scope.launch { results = searchContactsAsync(it) }
                            } else results = emptyList()
                        },
                        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (query.isNotEmpty()) {
                        BasicText(
                            "✕",
                            style = TextStyle(color = Color.White, fontSize = 20.sp),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clickable {
                                    query = ""
                                    normalized = ""
                                    results = emptyList()
                                }
                        )
                    }
                }

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
                                BasicText(item.first, style = TextStyle(color = Color.White))
                                BasicText(item.second, style = TextStyle(color = Color.White, fontSize = 12.sp))
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {

                Row(Modifier.fillMaxWidth()) {
                    ActionButton("GSM", Color.Black, Color(0xFFF0F0F0)) { openGsm(normalized) }
                    ActionButton("Telegram", Color(0xFF229ED9), Color(0xFFEAF6FD)) { openTelegram(normalized) }
                }

                Row(Modifier.fillMaxWidth()) {
                    ActionButton("WhatsApp", Color(0xFF25D366), Color(0xFFE9F9EF)) { openWhatsApp(normalized) }
                    ActionButton("Viber", Color(0xFF7360F2), Color(0xFFF0EDFF)) { openViber(normalized) }
                }
            }
        }
    }

    @Composable
    fun ActionButton(text: String, textColor: Color, bg: Color, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(6.dp)
                .height(56.dp)
                .background(bg, RoundedCornerShape(28.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            BasicText(text, style = TextStyle(color = textColor, fontWeight = FontWeight.SemiBold))
        }
    }

    // ===== LOGIC =====

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

    private fun normalizeNumber(input: String): String {
        var digits = input.filter { it.isDigit() }
        if (digits.startsWith("0") && digits.length == 10) digits = "38$digits"
        if (digits.startsWith("380") && digits.length > 12) digits = digits.take(12)
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
                while (it.moveToNext()) list.add(it.getString(0) to it.getString(1))
            }
            list
        }
    }
}
