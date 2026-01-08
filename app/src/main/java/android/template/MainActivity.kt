package com.callchooser.app

import android.Manifest
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
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
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    // Поточна локалізація (оновлюється з UI)
    private var currentLanguage: Language = Language.UK
    private var currentStrings: Strings = getStrings(Language.UK)

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val VOICE_SEARCH_REQUEST_CODE = 101
        
        // Package names месенджерів
        const val WHATSAPP_PACKAGE = "com.whatsapp"
        const val TELEGRAM_PACKAGE = "org.telegram.messenger"
        const val VIBER_PACKAGE = "com.viber.voip"
        
        // Версія програми
        const val APP_VERSION = "1.0"
        const val RELEASE_DATE = "08.01.2026"
    }

    // ================= LOCALIZATION =================
    
    enum class Language {
        UK, EN
    }
    
    data class Strings(
        val appName: String,
        val searchHint: String,
        val listening: String,
        val found: String,
        val recentCalls: String,
        val loadingCalls: String,
        val noRecentCalls: String,
        val refresh: String,
        val available: String,
        val notDefined: String,
        // Toast messages
        val recordAudioPermissionNeeded: String,
        val voiceRecognitionError: String,
        val voiceRecognitionUnavailable: String,
        val messengerUnavailable: String,
        val numberCopied: String,
        // Version dialog
        val aboutApp: String,
        val version: String,
        val releaseDate: String,
        val close: String
    )
    
    private fun getStrings(language: Language): Strings {
        return when (language) {
            Language.UK -> Strings(
                appName = "Call Chooser",
                searchHint = "Ім'я або номер",
                listening = "Слухаю...",
                found = "Знайдено",
                recentCalls = "Останні дзвінки",
                loadingCalls = "Завантаження дзвінків...",
                noRecentCalls = "Немає останніх дзвінків",
                refresh = "🔄 Оновити",
                available = "доступний",
                notDefined = "не визначено",
                recordAudioPermissionNeeded = "Потрібен дозвіл на мікрофон",
                voiceRecognitionError = "Помилка розпізнавання голосу",
                voiceRecognitionUnavailable = "Голосовий пошук недоступний на цьому пристрої",
                messengerUnavailable = "Месенджер недоступний, відкриваю GSM",
                numberCopied = "Номер скопійовано",
                aboutApp = "Про програму",
                version = "Версія",
                releaseDate = "Дата релізу",
                close = "Закрити"
            )
            Language.EN -> Strings(
                appName = "Call Chooser",
                searchHint = "Name or number",
                listening = "Listening...",
                found = "Found",
                recentCalls = "Recent calls",
                loadingCalls = "Loading calls...",
                noRecentCalls = "No recent calls",
                refresh = "🔄 Refresh",
                available = "available",
                notDefined = "not defined",
                recordAudioPermissionNeeded = "Microphone permission needed",
                voiceRecognitionError = "Voice recognition error",
                voiceRecognitionUnavailable = "Voice search unavailable on this device",
                messengerUnavailable = "Messenger unavailable, opening GSM",
                numberCopied = "Number copied",
                aboutApp = "About",
                version = "Version",
                releaseDate = "Release date",
                close = "Close"
            )
        }
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
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            android.util.Log.d("CallChooser", "onRequestPermissionsResult called")
            
            permissions.forEachIndexed { index, permission ->
                val granted = grantResults[index] == PackageManager.PERMISSION_GRANTED
                android.util.Log.d("CallChooser", "$permission: ${if (granted) "GRANTED" else "DENIED"}")
            }
            
            // Перезапустити UI після надання дозволів
            android.util.Log.d("CallChooser", "Restarting UI after permission result")
            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    CallChooserUI()
                }
            }
        }
    }

    // ================= RECENT CALLS =================
    @Composable
    fun CallChooserUI() {
        var query by remember { mutableStateOf("") }
        var normalized by remember { mutableStateOf("") }
        var searchResults by remember { mutableStateOf(listOf<ContactItem>()) }
        var recentCalls by remember { mutableStateOf(listOf<RecentCall>()) }
        var selectedContactId by remember { mutableStateOf<Long?>(null) }
        var selectedContactName by remember { mutableStateOf<String?>(null) }
        var messengerStates by remember { mutableStateOf(MessengerAvailability()) }
        var isLoadingCalls by remember { mutableStateOf(false) }
        var isListening by remember { mutableStateOf(false) }
        var currentLanguage by remember { mutableStateOf(Language.UK) }
        var showVersionDialog by remember { mutableStateOf(false) }
        
        val strings = getStrings(currentLanguage)
        
        // Оновлюємо currentStrings при зміні мови (для Toast повідомлень)
        LaunchedEffect(currentLanguage) {
            this@MainActivity.currentLanguage = currentLanguage
            currentStrings = strings
        }
        
        val scope = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current
        val lifecycleOwner = LocalLifecycleOwner.current

        // Функція для завантаження останніх дзвінків
        fun loadRecentCalls() {
            scope.launch {
                android.util.Log.d("CallChooser", "Loading recent calls...")
                android.util.Log.d("CallChooser", "Has READ_CALL_LOG permission: ${hasCallLogPermission()}")
                
                if (hasCallLogPermission()) {
                    android.util.Log.d("CallChooser", "Starting to load recent calls...")
                    isLoadingCalls = true
                    recentCalls = loadRecentCallsAsync()
                    isLoadingCalls = false
                    android.util.Log.d("CallChooser", "Loaded ${recentCalls.size} recent calls")
                } else {
                    android.util.Log.w("CallChooser", "READ_CALL_LOG permission not granted yet")
                }
            }
        }

        // Оновлюємо список дзвінків при кожному поверненні до екрану (ON_RESUME)
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    android.util.Log.d("CallChooser", "ON_RESUME: Reloading recent calls")
                    loadRecentCalls()
                }
            }
            
            lifecycleOwner.lifecycle.addObserver(observer)
            
            onDispose {
                android.util.Log.d("CallChooser", "DisposableEffect: Removing lifecycle observer")
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // Функція для ручного оновлення списку
        fun refreshRecentCalls() {
            android.util.Log.d("CallChooser", "Manual refresh triggered")
            loadRecentCalls()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2C5E86))
                .padding(16.dp)
                .statusBarsPadding()
        ) {

            // Заголовок з кнопками мови
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Динамічний заголовок: ім'я контакта або назва програми
                Text(
                    text = selectedContactName ?: strings.appName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.95f),
                    letterSpacing = 1.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { 
                            // Показуємо діалог тільки якщо це назва програми, не ім'я контакта
                            if (selectedContactName == null) {
                                showVersionDialog = true
                            }
                        }
                )
                
                // Кнопки перемикання мови
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Кнопка UK
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (currentLanguage == Language.UK) 
                                    Color.White.copy(alpha = 0.3f) 
                                else 
                                    Color.Transparent
                            )
                            .clickable { currentLanguage = Language.UK },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "UK",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (currentLanguage == Language.UK) 
                                FontWeight.Bold 
                            else 
                                FontWeight.Normal
                        )
                    }
                    
                    // Кнопка EN
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (currentLanguage == Language.EN) 
                                    Color.White.copy(alpha = 0.3f) 
                                else 
                                    Color.Transparent
                            )
                            .clickable { currentLanguage = Language.EN },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "EN",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (currentLanguage == Language.EN) 
                                FontWeight.Bold 
                            else 
                                FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Поле пошуку
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    normalized = normalizeNumber(it)
                    selectedContactId = null
                    selectedContactName = null
                    messengerStates = MessengerAvailability()

                    if (it.length >= 2) {
                        scope.launch {
                            searchResults = searchContactsAsync(it)
                        }
                    } else {
                        searchResults = emptyList()
                    }
                },
                label = { Text(strings.searchHint) },
                textStyle = LocalTextStyle.current.copy(
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal
                ),
                trailingIcon = {
                    Row {
                        // Кнопка голосового пошуку
                        if (query.isEmpty()) {
                            IconButton(
                                onClick = { startVoiceSearch { result -> 
                                    android.util.Log.d("CallChooser", "Voice callback: result='$result', length=${result.length}")
                                    
                                    if (result.isNotBlank() && result.length >= 2) {
                                        query = result
                                        normalized = normalizeNumber(result)
                                        selectedContactId = null
                                        selectedContactName = null
                                        messengerStates = MessengerAvailability()
                                        isListening = false
                                        
                                        // Прибираємо фокус з поля
                                        focusManager.clearFocus()
                                        
                                        // Запускаємо пошук автоматично
                                        android.util.Log.d("CallChooser", "Voice callback: launching search for '$result'")
                                        scope.launch {
                                            searchResults = searchContactsAsync(result)
                                            android.util.Log.d("CallChooser", "Voice callback: search completed, found ${searchResults.size}")
                                        }
                                    } else {
                                        android.util.Log.d("CallChooser", "Voice callback: query too short or blank, result='$result'")
                                        isListening = false
                                    }
                                }}
                            ) {
                                Text(
                                    text = "🎤",
                                    fontSize = 20.sp,
                                    color = if (isListening) Color.Red else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        // Кнопка очищення
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                normalized = ""
                                searchResults = emptyList()
                                selectedContactId = null
                                selectedContactName = null
                                messengerStates = MessengerAvailability()
                            }) {
                                Text("✕", fontSize = 18.sp, color = Color.White)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Пульсуючий індикатор під час голосового запису
            androidx.compose.animation.AnimatedVisibility(
                visible = isListening,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size((10 * scale).dp)
                            .background(Color.Red, shape = androidx.compose.foundation.shape.CircleShape)
                    )
                    
                    Spacer(Modifier.width(8.dp))
                    
                    Text(
                        strings.listening,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Результати пошуку або останні дзвінки
            when {
                searchResults.isNotEmpty() -> {
                    // Показуємо результати пошуку
                    Text(
                        "${strings.found}: ${searchResults.size}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(searchResults) { contact ->
                            ContactCard(
                                contact = contact,
                                onClick = {
                                    selectedContactName = contact.name
                                    query = contact.number
                                    normalized = normalizeNumber(contact.number)
                                    selectedContactId = contact.id
                                    searchResults = emptyList()
                                    focusManager.clearFocus()
                                    
                                    // Перевірка месенджерів
                                    if (contact.id != 0L) {
                                        scope.launch {
                                            val phoneNum = normalizeNumber(contact.number)
                                            messengerStates = checkAllMessengers(contact.id, phoneNum)
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
                        strings.recentCalls,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(recentCalls) { call ->
                            RecentCallCard(
                                call = call,
                                onClick = {
                                    selectedContactName = call.name
                                    query = call.number
                                    normalized = call.normalizedNumber
                                    selectedContactId = call.contactId
                                    focusManager.clearFocus()
                                    
                                    // Перевірка месенджерів
                                    if (call.contactId != null && call.contactId != 0L) {
                                        scope.launch {
                                            messengerStates = checkAllMessengers(call.contactId, call.normalizedNumber)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                
                else -> {
                    // Порожній простір якщо немає ні пошуку ні дзвінків
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when {
                            isLoadingCalls -> {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    strings.loadingCalls,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            }
                            
                            hasCallLogPermission() && query.isEmpty() -> {
                                Text(
                                    strings.noRecentCalls,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { refreshRecentCalls() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Text(strings.refresh, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Кнопки месенджерів (завжди внизу після списку)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                        MessengerButton(
                            name = "Telegram",
                            bg = Color(0xFFEAF6FD),
                            fg = Color(0xFF229ED9),
                            isAvailable = messengerStates.telegram,
                            hasNumber = normalized.isNotEmpty(),
                            strings = strings,
                            onClick = { openTelegram(normalized) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f).padding(end = 6.dp)) {
                        MessengerButton(
                            name = "WhatsApp",
                            bg = Color(0xFFE9F9EF),
                            fg = Color(0xFF25D366),
                            isAvailable = messengerStates.whatsApp,
                            hasNumber = normalized.isNotEmpty(),
                            strings = strings,
                            onClick = { openWhatsApp(normalized) }
                        )
                    }
                    Box(Modifier.weight(1f).padding(start = 6.dp)) {
                        MessengerButton(
                            name = "Viber",
                            bg = Color(0xFFF0EDFF),
                            fg = Color(0xFF7360F2),
                            isAvailable = messengerStates.viber,
                            hasNumber = normalized.isNotEmpty(),
                            strings = strings,
                            onClick = { openViber(normalized) }
                        )
                    }
                }
            }
            
            // Діалог версії програми
            if (showVersionDialog) {
                AlertDialog(
                    onDismissRequest = { showVersionDialog = false },
                    title = {
                        Text(
                            text = strings.aboutApp,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Text(
                                text = "${strings.version}: $APP_VERSION",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "${strings.releaseDate}: $RELEASE_DATE",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showVersionDialog = false }) {
                            Text(strings.close)
                        }
                    }
                )
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
        val isMissed = call.type == CallLog.Calls.MISSED_TYPE
        
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
                when (call.type) {
                    CallLog.Calls.INCOMING_TYPE -> {
                        // Зелена стрілка вниз (жирна)
                        Text(
                            "↓",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50),  // Зелений
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                    CallLog.Calls.OUTGOING_TYPE -> {
                        // Синя стрілка вверх (жирна)
                        Text(
                            "↑",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3),  // Синій
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                    CallLog.Calls.MISSED_TYPE -> {
                        // Червоне коло
                        Text(
                            "●",
                            fontSize = 20.sp,
                            color = Color(0xFFF44336),  // Червоний
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    // Ім'я або номер (червоний для пропущених)
                    Text(
                        call.name ?: call.number,
                        color = if (isMissed) Color(0xFFF44336) else Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    
                    if (call.name != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            call.number,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isMissed) 
                                Color(0xFFF44336).copy(alpha = 0.7f) 
                            else 
                                Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                    
                    Spacer(Modifier.height(2.dp))
                    Text(
                        call.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMissed) 
                            Color(0xFFF44336).copy(alpha = 0.6f) 
                        else 
                            Color.White.copy(alpha = 0.5f),
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
    fun MessengerButton(
        name: String,
        bg: Color,
        fg: Color,
        isAvailable: Boolean,
        hasNumber: Boolean,
        strings: Strings,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            enabled = hasNumber,  // Кнопка активна якщо є номер
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = bg,
                contentColor = fg,
                disabledContainerColor = bg.copy(alpha = 0.3f),
                disabledContentColor = fg.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Індикатор (кільце)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFF44336),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    
                    Spacer(Modifier.width(6.dp))
                    
                    // Назва месенджера
                    Text(
                        name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
                
                // Статус
                Text(
                    if (isAvailable) strings.available else strings.notDefined,
                    fontSize = 10.sp,
                    color = if (isAvailable) 
                        Color(0xFF2E7D32)  // Темно-зелений для available
                    else 
                        Color(0xFFD32F2F),  // Темно-червоний для not defined
                    fontWeight = FontWeight.Medium
                )
            }
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
                .clip(RoundedCornerShape(12.dp))
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

    // Перевірка чи можна відкрити месенджер з цим номером (Intent Resolver)
    private fun canOpenInMessenger(phoneNumber: String, messengerPackage: String): Boolean {
        return try {
            // Спробувати відкрити через tel: intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("tel:$phoneNumber")
                setPackage(messengerPackage)
            }
            
            val canOpen = intent.resolveActivity(packageManager) != null
            android.util.Log.d("CallChooser", "Intent resolver for $messengerPackage with $phoneNumber: $canOpen")
            canOpen
        } catch (e: Exception) {
            android.util.Log.e("CallChooser", "Error checking intent for $messengerPackage", e)
            false
        }
    }

    // Перевірка чи встановлений месенджер (PackageManager)
    private fun isMessengerInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            android.util.Log.d("CallChooser", "Package $packageName: INSTALLED")
            true
        } catch (e: PackageManager.NameNotFoundException) {
            android.util.Log.d("CallChooser", "Package $packageName: NOT FOUND")
            false
        }
    }

    // Перевірка всіх месенджерів (ContactsContract + Intent Resolver)
    private suspend fun checkAllMessengers(contactId: Long, phoneNumber: String = ""): MessengerAvailability {
        return withContext(Dispatchers.IO) {
            var whatsApp = false
            var telegram = false
            var viber = false

            // Крок 1: Спробувати знайти в ContactsContract (100% точність)
            try {
                android.util.Log.d("CallChooser", "Checking ContactsContract for contact $contactId")
                
                val cursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data.MIMETYPE),
                    "${ContactsContract.Data.CONTACT_ID}=?",
                    arrayOf(contactId.toString()),
                    null
                )

                cursor?.use {
                    while (it.moveToNext()) {
                        val mimetype = it.getString(0)
                        android.util.Log.d("CallChooser", "MIMETYPE: $mimetype")
                        
                        when {
                            mimetype.contains("whatsapp", ignoreCase = true) -> whatsApp = true
                            mimetype.contains("telegram", ignoreCase = true) -> telegram = true
                            mimetype.contains("viber", ignoreCase = true) -> viber = true
                        }
                    }
                }
                
                android.util.Log.d("CallChooser", "ContactsContract result - WA:$whatsApp TG:$telegram VB:$viber")
            } catch (e: Exception) {
                android.util.Log.e("CallChooser", "Error checking ContactsContract", e)
            }

            // Крок 2: Intent Resolver для тих що не знайшлись (точніше ніж PackageManager)
            // Перевіряємо чи можна відкрити месенджер з цим номером
            if (!whatsApp && phoneNumber.isNotEmpty()) {
                whatsApp = canOpenInMessenger(phoneNumber, WHATSAPP_PACKAGE)
                if (whatsApp) {
                    android.util.Log.d("CallChooser", "WhatsApp available via Intent Resolver")
                }
            }
            
            if (!telegram && phoneNumber.isNotEmpty()) {
                telegram = canOpenInMessenger(phoneNumber, TELEGRAM_PACKAGE)
                if (telegram) {
                    android.util.Log.d("CallChooser", "Telegram available via Intent Resolver")
                }
            }
            
            if (!viber && phoneNumber.isNotEmpty()) {
                viber = canOpenInMessenger(phoneNumber, VIBER_PACKAGE)
                if (viber) {
                    android.util.Log.d("CallChooser", "Viber available via Intent Resolver")
                }
            }
            
            android.util.Log.d("CallChooser", "Final result - WA:$whatsApp TG:$telegram VB:$viber")

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
                android.util.Log.d("CallChooser", "Loading recent calls...")
                
                val cursor = contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.CACHED_NAME,
                        CallLog.Calls.DATE,
                        CallLog.Calls.TYPE
                    ),
                    "${CallLog.Calls.TYPE} IN (?,?,?)",
                    arrayOf(
                        CallLog.Calls.INCOMING_TYPE.toString(),
                        CallLog.Calls.OUTGOING_TYPE.toString(),
                        CallLog.Calls.MISSED_TYPE.toString()
                    ),
                    "${CallLog.Calls.DATE} DESC"
                )

                cursor?.use {
                    android.util.Log.d("CallChooser", "Cursor count: ${it.count}")
                    while (it.moveToNext() && list.size < 10) {
                        val number = it.getString(0) ?: continue
                        
                        // Пропускаємо приховані номери
                        if (number in listOf("-1", "-2", "-3", "")) continue
                        
                        val normalized = normalizeNumber(number)
                        
                        // Пропускаємо дублікати
                        if (normalized in seenNumbers) continue
                        seenNumbers.add(normalized)

                        val cachedName = it.getString(1)  // Стара назва з CallLog
                        val timestamp = it.getLong(2)
                        val type = it.getInt(3)
                        
                        // Отримуємо contactId та актуальне ім'я з ContactsContract
                        val contactId = getContactIdByNumber(normalized)
                        val actualName = getContactNameByNumber(normalized)
                        
                        // Використовуємо актуальне ім'я, якщо знайдено, інакше CACHED_NAME
                        val name = actualName ?: cachedName
                        
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
                
                android.util.Log.d("CallChooser", "Loaded ${list.size} recent calls")
            } catch (e: Exception) {
                android.util.Log.e("CallChooser", "Error loading recent calls", e)
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
    
    private fun getContactNameByNumber(normalizedNumber: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode("+$normalizedNumber")
            )
            
            val cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(0)
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

    // ================= ACTIONS =================

    private fun copyNumber(num: String) {
        if (num.isBlank()) return

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("phone", "+$num"))

        Toast.makeText(this, currentStrings.numberCopied, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, currentStrings.messengerUnavailable, Toast.LENGTH_SHORT).show()
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

    // ================= VOICE SEARCH =================

    private fun hasRecordAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun startVoiceSearch(onResult: (String) -> Unit) {
        if (!hasRecordAudioPermission()) {
            Toast.makeText(this, currentStrings.recordAudioPermissionNeeded, Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
            return
        }

        // Спочатку пробуємо SpeechRecognizer (Google Services)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            startSpeechRecognizer(onResult)
        } else {
            // Fallback: Intent API для Huawei та інших без Google Services
            startVoiceSearchIntent()
        }
    }

    private var speechRecognizer: SpeechRecognizer? = null

    private fun startSpeechRecognizer(onResult: (String) -> Unit) {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uk-UA")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    android.util.Log.d("CallChooser", "Voice: Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    android.util.Log.d("CallChooser", "Voice: Speech started")
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    android.util.Log.d("CallChooser", "Voice: Speech ended")
                }

                override fun onError(error: Int) {
                    android.util.Log.e("CallChooser", "Voice: Error $error")
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            currentStrings.voiceRecognitionError,
                            Toast.LENGTH_SHORT
                        ).show()
                        onResult("")
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val recognizedText = matches[0]
                        android.util.Log.d("CallChooser", "Voice: Recognized '$recognizedText'")
                        runOnUiThread {
                            onResult(recognizedText)
                        }
                    } else {
                        android.util.Log.w("CallChooser", "Voice: No results")
                        runOnUiThread {
                            onResult("")
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            android.util.Log.e("CallChooser", "Voice: Exception", e)
            Toast.makeText(this, currentStrings.voiceRecognitionError, Toast.LENGTH_SHORT).show()
            onResult("")
        }
    }

    private fun startVoiceSearchIntent() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uk-UA")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажіть ім'я або номер")
            }
            startActivityForResult(intent, VOICE_SEARCH_REQUEST_CODE)
        } catch (e: Exception) {
            android.util.Log.e("CallChooser", "Voice Intent: Exception", e)
            Toast.makeText(
                this,
                currentStrings.voiceRecognitionUnavailable,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VOICE_SEARCH_REQUEST_CODE && resultCode == RESULT_OK) {
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                android.util.Log.d("CallChooser", "Voice Intent: Recognized '$recognizedText'")
                
                // Перезапустити UI з результатом
                setContent {
                    MaterialTheme(colorScheme = darkColorScheme()) {
                        CallChooserUI()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

    // ================= TRANSLITERATION =================

    private fun transliterate(text: String): String {
        val lowerText = text.lowercase()
        val result = StringBuilder()
        
        val ukrToLat = mapOf(
            'а' to "a", 'б' to "b", 'в' to "v", 'г' to "h", 'ґ' to "g",
            'д' to "d", 'е' to "e", 'є' to "ye", 'ж' to "zh", 'з' to "z",
            'и' to "y", 'і' to "i", 'ї' to "yi", 'й' to "y", 'к' to "k",
            'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p",
            'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f",
            'х' to "kh", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "shch",
            'ь' to "", 'ю' to "yu", 'я' to "ya"
        )

        val latToUkr = mapOf(
            "a" to 'а', "b" to 'б', "v" to 'в', "h" to 'г', "g" to 'ґ',
            "d" to 'д', "e" to 'е', "ye" to 'є', "zh" to 'ж', "z" to 'з',
            "y" to 'и', "i" to 'і', "yi" to 'ї', "k" to 'к',
            "l" to 'л', "m" to 'м', "n" to 'н', "o" to 'о', "p" to 'п',
            "r" to 'р', "s" to 'с', "t" to 'т', "u" to 'у', "f" to 'ф',
            "kh" to 'х', "ts" to 'ц', "ch" to 'ч', "sh" to 'ш', "shch" to 'щ',
            "yu" to 'ю', "ya" to 'я'
        )

        // Визначаємо напрямок транслітерації
        val isCyrillic = lowerText.any { it in 'а'..'я' || it == 'ґ' || it == 'є' || it == 'і' || it == 'ї' }

        if (isCyrillic) {
            // Кирилиця → Латинка
            for (char in lowerText) {
                result.append(ukrToLat[char] ?: char)
            }
        } else {
            // Латинка → Кирилиця (складніше, бо багатосимвольні комбінації)
            var i = 0
            while (i < lowerText.length) {
                var found = false
                
                // Спробувати 4-символьні комбінації
                if (i + 4 <= lowerText.length) {
                    val fourChars = lowerText.substring(i, i + 4)
                    latToUkr[fourChars]?.let {
                        result.append(it)
                        i += 4
                        found = true
                    }
                }
                
                // Спробувати 2-символьні комбінації
                if (!found && i + 2 <= lowerText.length) {
                    val twoChars = lowerText.substring(i, i + 2)
                    latToUkr[twoChars]?.let {
                        result.append(it)
                        i += 2
                        found = true
                    }
                }
                
                // Спробувати односимвольні
                if (!found) {
                    val char = lowerText[i].toString()
                    result.append(latToUkr[char] ?: lowerText[i])
                    i++
                }
            }
        }

        return result.toString()
    }

    // ================= SEARCH WITH TRANSLITERATION =================

    private fun generateSearchVariants(q: String): List<String> {
        val variants = mutableListOf<String>()
        val lowerQuery = q.lowercase()
        
        // 1. Оригінал (lowercase)
        variants.add(lowerQuery)
        
        // 2. Стандартна транслітерація
        val translit = transliterate(lowerQuery)
        if (translit != lowerQuery) {
            variants.add(translit)
        }
        
        // 3. Альтернативні варіанти транслітерації
        // в → w (замість v)
        if (translit.contains('v')) {
            variants.add(translit.replace('v', 'w'))
        }
        
        // ч → c (замість ch)
        if (translit.contains("ch")) {
            variants.add(translit.replace("ch", "c"))
        }
        
        // х → h або x (замість kh)
        if (translit.contains("kh")) {
            variants.add(translit.replace("kh", "h"))
            variants.add(translit.replace("kh", "x"))
        }
        
        // Комбінація: в→w і ч→c
        if (translit.contains('v') && translit.contains("ch")) {
            variants.add(translit.replace('v', 'w').replace("ch", "c"))
        }
        
        android.util.Log.d("CallChooser", "Search variants for '$q': $variants")
        
        return variants.distinct()
    }

    private suspend fun searchContactsAsync(q: String): List<ContactItem> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<ContactItem>()
            val variants = generateSearchVariants(q)

            try {
                android.util.Log.d("CallChooser", "Search: query='$q', variants=$variants")
                
                // Перевіряємо чи є цифри в запиті
                val hasDigits = q.any { it.isDigit() }
                
                // Створюємо WHERE clause з усіма варіантами
                val whereClause = variants.joinToString(" OR ") { 
                    "LOWER(${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}) LIKE ?" 
                } + if (hasDigits) {
                    " OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
                } else {
                    ""
                }
                
                // Створюємо параметри (всі варіанти + номер тільки якщо є цифри)
                val whereArgs = variants.map { "%$it%" }.toMutableList()
                if (hasDigits) {
                    whereArgs.add("%${q.filter { it.isDigit() }}%")
                }
                
                android.util.Log.d("CallChooser", "Search: hasDigits=$hasDigits")
                android.util.Log.d("CallChooser", "Search: WHERE=$whereClause")
                android.util.Log.d("CallChooser", "Search: ARGS=${whereArgs.joinToString()}")
                
                val cursor: Cursor? = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    whereClause,
                    whereArgs.toTypedArray(),
                    null
                )

                cursor?.use {
                    while (it.moveToNext()) {
                        val contactId = it.getLong(0)
                        val name = it.getString(1)
                        val number = it.getString(2)
                        list.add(ContactItem(contactId, name, number))
                        android.util.Log.d("CallChooser", "Search: found '$name'")
                    }
                }
                
                android.util.Log.d("CallChooser", "Search: total found ${list.size} results")
            } catch (e: Exception) {
                android.util.Log.e("CallChooser", "Search: Exception", e)
            }

            list
        }
    }
}
