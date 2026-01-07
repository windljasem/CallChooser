package com.callchooser.app

import android.Manifest
import androidx.compose.foundation.combinedClickable
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Запит обох дозволів
        requestPermissionsIfNeeded()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CallChooserUI()
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.READ_CONTACTS)
        }
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.READ_CALL_LOG)
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    @Composable
    fun CallChooserUI() {
        var query by remember { mutableStateOf("") }
        var normalized by remember { mutableStateOf("") }
        var searchResults by remember { mutableStateOf(listOf<ContactItem>()) }
        var recentCalls by remember { mutableStateOf(listOf<RecentCall>()) }
        var selectedContactId by remember { mutableStateOf<Long?>(null) }
        var messengerStates by remember { mutableStateOf(MessengerAvailability()) }
        
        val scope = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current

        // Завантаження останніх дзвінків при запуску
        LaunchedEffect(Unit) {
            if (hasCallLogPermission()) {
                recentCalls = loadRecentCallsAsync()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2C5E86))
                .padding(16.dp)
                .statusBarsPadding()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 150.dp)
            ) {

                // Заголовок
                Text(
                    "Call Chooser",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Light,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Поле пошуку
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        normalized = normalizeNumber(it)
                        selectedContactId = null
                        messengerStates = MessengerAvailability()

                        if (it.length >= 2) {
                            scope.launch {
                                searchResults = searchContactsAsync(it)
                            }
                        } else {
                            searchResults = emptyList()
                        }
                    },
                    label = { Text("Ім'я або номер") },
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                normalized = ""
                                searchResults = emptyList()
                                selectedContactId = null
                                messengerStates = MessengerAvailability()
                            }) {
                                Text("✕", fontSize = 18.sp, color = Color.White)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Результати пошуку або останні дзвінки
                when {
                    searchResults.isNotEmpty() -> {
                        // Показуємо результати пошуку
                        Text(
                            "Знайдено: ${searchResults.size}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(searchResults) { contact ->
                                ContactCard(
                                    contact = contact,
                                    onClick = {
                                        query = contact.number
                                        normalized = normalizeNumber(contact.number)
                                        selectedContactId = contact.id
                                        searchResults = emptyList()
                                        focusManager.clearFocus()
                                        
                                        // Перевірка месенджерів
                                        if (contact.id != 0L) {
                                            scope.launch {
                                                messengerStates = checkAllMessengers(contact.id)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        focusManager.clearFocus()
                                    }
                                )
                            }
                        }
                    }
                    
                    query.isEmpty() && recentCalls.isNotEmpty() -> {
                        // Показуємо останні дзвінки
                        Text(
                            "Останні дзвінки",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(recentCalls) { call ->
                                RecentCallCard(
                                    call = call,
                                    onClick = {
                                        query = call.number
                                        normalized = call.normalizedNumber
                                        selectedContactId = call.contactId
                                        focusManager.clearFocus()
                                        
                                        // Перевірка месенджерів
                                        if (call.contactId != null && call.contactId != 0L) {
                                            scope.launch {
                                                messengerStates = checkAllMessengers(call.contactId)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Кнопки месенджерів
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
                        StyledButtonWithLongPress(
                            text = "GSM",
                            bg = Color(0xFFF0F0F0),
                            fg = Color.Black,
                            enabled = normalized.isNotEmpty(),
                            onClick = { openGsm(normalized) },
                            onLongPress = { copyNumber(normalized) }
                        )
                    }
                    Box(Modifier.weight(1f).padding(start = 6.dp)) {
                        StyledButton(
                            "Telegram",
                            bg = Color(0xFFEAF6FD),
                            fg = Color(0xFF229ED9),
                            enabled = messengerStates.telegram && normalized.isNotEmpty()
                        ) { openTelegram(normalized) }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f).padding(end = 6.dp)) {
                        StyledButton(
                            "WhatsApp",
                            bg = Color(0xFFE9F9EF),
                            fg = Color(0xFF25D366),
                            enabled = messengerStates.whatsApp && normalized.isNotEmpty()
                        ) { openWhatsApp(normalized) }
                    }
                    Box(Modifier.weight(1f).padding(start = 6.dp)) {
                        StyledButton(
                            "Viber",
                            bg = Color(0xFFF0EDFF),
                            fg = Color(0xFF7360F2),
                            enabled = messengerStates.viber && normalized.isNotEmpty()
                        ) { openViber(normalized) }
                    }
                }
            }
        }
    }

    @Composable
    fun ContactCard(
        contact: ContactItem,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            color = Color.White.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    contact.name,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    contact.number,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }

    @Composable
    fun RecentCallCard(
        call: RecentCall,
        onClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick),
            color = Color.White.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Іконка типу дзвінка
                Text(
                    if (call.type == CallLog.Calls.OUTGOING_TYPE) "📤" else "📥",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        call.name ?: call.number,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    
                    if (call.name != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            call.number,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                    
                    Spacer(Modifier.height(2.dp))
                    Text(
                        call.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    @Composable
    fun StyledButton(
        text: String,
        bg: Color,
        fg: Color,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = bg.copy(alpha = if (enabled) 1f else 0.3f),
                contentColor = fg.copy(alpha = if (enabled) 1f else 0.4f),
                disabledContainerColor = bg.copy(alpha = 0.3f),
                disabledContentColor = fg.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(50)
        ) {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }

    @Composable
    fun StyledButtonWithLongPress(
        text: String,
        bg: Color,
        fg: Color,
        enabled: Boolean = true,
        onClick: () -> Unit,
        onLongPress: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(50))
                .background(bg.copy(alpha = if (enabled) 1f else 0.3f))
                .combinedClickable(
                    enabled = enabled,
                    onClick = onClick,
                    onLongClick = onLongPress
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                color = fg.copy(alpha = if (enabled) 1f else 0.4f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    // ================= DATA CLASSES =================
    
    data class ContactItem(
        val id: Long,
        val name: String,
        val number: String
    )

    data class RecentCall(
        val number: String,
        val normalizedNumber: String,
        val name: String?,
        val timestamp: Long,
        val type: Int,
        val contactId: Long?,
        val formattedDate: String
    )

    data class MessengerAvailability(
        val whatsApp: Boolean = false,
        val telegram: Boolean = false,
        val viber: Boolean = false
    )

    // ================= MESSENGER CHECK =================

    private suspend fun checkAllMessengers(contactId: Long): MessengerAvailability {
        return withContext(Dispatchers.IO) {
            var whatsApp = false
            var telegram = false
            var viber = false

            try {
                val cursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data.MIMETYPE),
                    "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE} IN (?,?,?)",
                    arrayOf(
                        contactId.toString(),
                        "vnd.android.cursor.item/vnd.com.whatsapp.profile",
                        "vnd.android.cursor.item/vnd.org.telegram.messenger.android.profile",
                        "vnd.android.cursor.item/vnd.com.viber.voip.call"
                    ),
                    null
                )

                cursor?.use {
                    while (it.moveToNext()) {
                        when (val mimetype = it.getString(0)) {
                            "vnd.android.cursor.item/vnd.com.whatsapp.profile" -> whatsApp = true
                            "vnd.android.cursor.item/vnd.org.telegram.messenger.android.profile" -> telegram = true
                            "vnd.android.cursor.item/vnd.com.viber.voip.call" -> viber = true
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            MessengerAvailability(whatsApp, telegram, viber)
        }
    }

    // ================= RECENT CALLS =================

    private fun hasCallLogPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == 
               PackageManager.PERMISSION_GRANTED
    }

    private suspend fun loadRecentCallsAsync(): List<RecentCall> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<RecentCall>()
            val seenNumbers = mutableSetOf<String>()

            try {
                val cursor = contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.CACHED_NAME,
                        CallLog.Calls.DATE,
                        CallLog.Calls.TYPE
                    ),
                    "${CallLog.Calls.TYPE} IN (?,?)",
                    arrayOf(
                        CallLog.Calls.INCOMING_TYPE.toString(),
                        CallLog.Calls.OUTGOING_TYPE.toString()
                    ),
                    "${CallLog.Calls.DATE} DESC"
                )

                cursor?.use {
                    while (it.moveToNext() && list.size < 10) {
                        val number = it.getString(0) ?: continue
                        
                        // Пропускаємо приховані номери
                        if (number in listOf("-1", "-2", "-3", "")) continue
                        
                        val normalized = normalizeNumber(number)
                        
                        // Пропускаємо дублікати
                        if (normalized in seenNumbers) continue
                        seenNumbers.add(normalized)

                        val name = it.getString(1)
                        val timestamp = it.getLong(2)
                        val type = it.getInt(3)
                        
                        // Отримуємо contactId якщо контакт існує
                        val contactId = getContactIdByNumber(normalized)
                        
                        list.add(
                            RecentCall(
                                number = number,
                                normalizedNumber = normalized,
                                name = name,
                                timestamp = timestamp,
                                type = type,
                                contactId = contactId,
                                formattedDate = formatCallDate(timestamp)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            list
        }
    }

    private fun getContactIdByNumber(normalizedNumber: String): Long? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode("+$normalizedNumber")
            )
            
            val cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.CONTACT_ID),
                null,
                null,
                null
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getLong(0)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatCallDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        val diff = now - timestamp

        calendar.timeInMillis = timestamp

        return when {
            diff < 60_000 -> "Щойно"
            diff < 3_600_000 -> "${diff / 60_000} хв тому"
            diff < 86_400_000 -> {
                val hours = diff / 3_600_000
                if (hours == 1L) "Годину тому" else "$hours год тому"
            }
            else -> {
                val dateFormat = SimpleDateFormat("d MMM, HH:mm", Locale("uk"))
                dateFormat.format(Date(timestamp))
            }
        }
    }

    // ================= SEARCH =================

    private suspend fun searchContactsAsync(q: String): List<ContactItem> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<ContactItem>()

            try {
                val cursor: Cursor? = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?",
                    arrayOf("%$q%", "%$q%"),
                    null
                )

                cursor?.use {
                    while (it.moveToNext()) {
                        val contactId = it.getLong(0)
                        val name = it.getString(1)
                        val number = it.getString(2)
                        list.add(ContactItem(contactId, name, number))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            list
        }
    }

    // ================= ACTIONS =================

    private fun copyNumber(num: String) {
        if (num.isBlank()) return

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("phone", "+$num"))

        Toast.makeText(this, "+$num скопійовано", Toast.LENGTH_SHORT).show()
    }

    private fun openGsm(num: String) {
        if (num.isBlank()) return
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+$num")))
        } catch (e: Exception) {
            Toast.makeText(this, "Помилка відкриття дзвінка", Toast.LENGTH_SHORT).show()
        }
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
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage(pkg)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Месенджер недоступний, відкриваю GSM", Toast.LENGTH_SHORT).show()
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
}
