package com.mahesh.pocketvault

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.mahesh.pocketvault.data.CardEntity
import com.mahesh.pocketvault.data.FolderEntity
import com.mahesh.pocketvault.ui.CardViewModel
import com.mahesh.pocketvault.util.ImageStore
import com.mahesh.pocketvault.util.ShareUtil
import java.io.File
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import android.app.Activity
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

private val VaultInk = Color(0xFF14213D)
private val VaultBlue = Color(0xFF2457D6)
private val VaultTeal = Color(0xFF00A8A8)
private val VaultGold = Color(0xFFFFB703)
private val VaultCoral = Color(0xFFF77F6F)
private val VaultMist = Color(0xFFF6F8FC)
private val VaultSurface = Color(0xFFFFFFFF)
private val VaultLine = Color(0xFFE1E7F2)

private val vaultColorScheme = lightColorScheme(
    primary = VaultBlue,
    onPrimary = Color.White,
    secondary = VaultTeal,
    tertiary = VaultGold,
    background = VaultMist,
    surface = VaultSurface,
    surfaceVariant = Color(0xFFEAF0FA),
    onSurface = VaultInk,
    onSurfaceVariant = Color(0xFF5E6A82),
    outline = VaultLine,
    error = Color(0xFFB3261E)
)

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var unlocked by mutableStateOf(false)

        authenticateUser(
            onSuccess = { unlocked = true },
            onFail = { unlocked = false }
        )

        setContent {
            if (unlocked) {
                PocketVaultApp()
            } else {
                LockScreen(
                    onRetry = {
                        authenticateUser(
                            onSuccess = { unlocked = true },
                            onFail = { unlocked = false }
                        )
                    }
                )
            }
        }
    }

    private fun authenticateUser(
        onSuccess: () -> Unit,
        onFail: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    onFail()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock PocketVault")
            .setSubtitle("Use fingerprint, face unlock, or device PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

}

@Composable
fun LockScreen(onRetry: () -> Unit) {
    MaterialTheme(colorScheme = vaultColorScheme) {
        VaultBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(VaultBlue, VaultTeal))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            "PocketVault",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Your cards stay private, offline, and ready when you need them.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Lock, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Unlock PocketVault")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PocketVaultApp(vm: CardViewModel = viewModel()) {
    var screen by remember { mutableStateOf("home") }
    var selectedFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var selectedCard by remember { mutableStateOf<CardEntity?>(null) }

    fun navigateBack() {
        screen = when (screen) {
            "category" -> "home"
            "add", "detail", "deleted" -> "category"
            else -> screen
        }
    }

    BackHandler(enabled = screen != "home") {
        navigateBack()
    }

    MaterialTheme(colorScheme = vaultColorScheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (screen) {
                "home" -> HomeScreen(vm, onOpen = { folder -> selectedFolder = folder; screen = "category" })
                "category" -> selectedFolder?.let { folder ->
                    CategoryScreen(
                        vm = vm,
                        folder = folder,
                        onBack = { screen = "home" },
                        onAdd = { screen = "add" },
                        onOpenCard = { card -> selectedCard = card; vm.markOpened(card); screen = "detail" },
                        onViewDeleted = { screen = "deleted" }
                    )
                }
                "add" -> selectedFolder?.let { folder ->
                    AddCardScreen(folder, onBack = { screen = "category" }, onSave = { vm.add(it); screen = "category" })
                }
                "detail" -> selectedCard?.let { card -> CardDetailScreen(card, vm, onBack = { screen = "category" }) }
                "deleted" -> selectedFolder?.let { folder ->
                    DeletedCardsScreen(
                        vm = vm,
                        folder = folder,
                        onBack = { screen = "category" }
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFEAF6FF),
                        Color(0xFFF8FAFD),
                        Color(0xFFFFF7E8)
                    )
                )
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            VaultBlue.copy(alpha = 0.18f),
                            VaultTeal.copy(alpha = 0.13f),
                            VaultGold.copy(alpha = 0.16f)
                        )
                    )
                )
        )
        content()
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        trailing?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun StatPill(text: String, icon: @Composable (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.78f),
        tonalElevation = 2.dp,
        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(50))
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon?.invoke()
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun HomeScreen(vm: CardViewModel, onOpen: (FolderEntity) -> Unit) {
    val foldersFlow = remember { vm.folders() }
    val folders by foldersFlow.collectAsState(initial = emptyList())
    var showNewFolderDialog by remember { mutableStateOf(false) }

    VaultBackground {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(VaultBlue, VaultTeal))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("PocketVault Cards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("Private card library", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Box(
                    Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF183B8F), Color(0xFF007C89), Color(0xFFFFB703))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Your pocket vault for the cards that matter.",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatPill("${folders.size} folders") {
                                Icon(Icons.Default.AccountBox, null, tint = VaultBlue, modifier = Modifier.size(16.dp))
                            }
                            StatPill("Offline vault") {
                                Icon(Icons.Default.Lock, null, tint = VaultTeal, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            SectionTitle("Folders", trailing = if (folders.isEmpty()) null else "${folders.size} total")

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(folders, key = { it.id }) { folder ->
                    val cardCountFlow = remember(folder.id) { vm.count(folder.id) }
                    val cardCount by cardCountFlow.collectAsState(initial = 0)
                    FolderTile(folder, "$cardCount Cards") { onOpen(folder) }
                }
            }

            Button(
                onClick = { showNewFolderDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Create New List")
            }

            Text(
                "100% offline. No internet permission. Your cards stay on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    if (showNewFolderDialog) {
        NewFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { folderName ->
                vm.addFolder(FolderEntity(name = folderName))
                showNewFolderDialog = false
            }
        )
    }
}

@Composable
fun FolderTile(folder: FolderEntity, subtitle: String, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(VaultGold.copy(alpha = 0.75f), VaultCoral.copy(alpha = 0.85f)))),
                contentAlignment = Alignment.Center
            ) {
                Text(folder.icon, fontSize = MaterialTheme.typography.headlineMedium.fontSize)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(folder.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Box(
                    Modifier
                        .width(92.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(VaultBlue, VaultTeal)))
                )
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun CategoryScreen(vm: CardViewModel, folder: FolderEntity, onBack: () -> Unit, onAdd: () -> Unit, onOpenCard: (CardEntity) -> Unit, onViewDeleted: () -> Unit) {
    val cardsFlow = remember(folder.id) {
        vm.cards(folder.id)
    }
    val deletedCountFlow = remember(folder.id) {
        vm.countDeleted(folder.id)
    }

    val cards by cardsFlow.collectAsState(initial = emptyList())
    val deletedCount by deletedCountFlow.collectAsState(initial = 0)
    val frequent = cards.filter { it.isPinned || it.usageCount > 0 }.take(8)

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            ) { Icon(Icons.Default.Add, null) }
        }
    ) { pad ->
        VaultBackground {
            Column(
                Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(VaultGold, VaultCoral))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(folder.icon, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(folder.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                        Text("${cards.size} active cards", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.weight(1f))
                    if (deletedCount > 0) {
                        FilledTonalButton(onClick = onViewDeleted, modifier = Modifier.height(40.dp), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("$deletedCount")
                        }
                    }
                }

                SectionTitle("Frequent Cards", trailing = if (frequent.isEmpty()) "Pin favorites" else null)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(132.dp)) {
                    items(
                        items = frequent,
                        key = { it.id }
                    ) { card ->
                        SmallCard(card) { onOpenCard(card) }
                    }
                }

                SectionTitle("All Cards")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(
                        items = cards,
                        key = { it.id }
                    ) { card ->
                        CardRow(card, vm, onOpenCard)
                    }
                }
            }
        }
    }
}

@Composable
fun SmallCard(card: CardEntity, onClick: () -> Unit) {
    Card(
        Modifier
            .width(148.dp)
            .fillMaxHeight()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF263D80), Color(0xFF0E938B))))
                .padding(14.dp)
        ) {
            Icon(
                if (card.isPinned) Icons.Default.Star else Icons.Default.AccountBox,
                null,
                tint = if (card.isPinned) VaultGold else Color.White.copy(alpha = 0.92f)
            )
            Spacer(Modifier.weight(1f))
            Text(card.name, fontWeight = FontWeight.ExtraBold, maxLines = 1, color = Color.White)
            Text("Opened ${card.usageCount}", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun CardRow(card: CardEntity, vm: CardViewModel, onOpen: (CardEntity) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen(card) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Card(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(File(card.frontImagePath)),
                    contentDescription = "Card front",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(card.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
                    if (card.isPinned) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Star, null, tint = VaultGold, modifier = Modifier.size(16.dp))
                    }
                }
                Text("Used ${card.usageCount} times", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (card.isPinned) "Unpin" else "Pin") },
                        onClick = { vm.togglePin(card); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Star, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { vm.softDelete(card); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }
            }
        }
    }
}

@Composable
fun AddCardScreen(folder: FolderEntity, onBack: () -> Unit, onSave: (CardEntity) -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity

    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var frontUri by remember { mutableStateOf<Uri?>(null) }
    var backUri by remember { mutableStateOf<Uri?>(null) }
    var scanningFront by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val scannedUri = scanResult
                ?.pages
                ?.firstOrNull()
                ?.imageUri

            if (scannedUri != null) {
                if (scanningFront) {
                    frontUri = scannedUri
                } else {
                    backUri = scannedUri
                }
            }
        }
    }

    fun launchCardScanner(isFront: Boolean) {
        scanningFront = isFront

        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
    }

    VaultBackground {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null)
                }

                Column {
                    Text(
                        "Add New Card",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text("Scan both sides for a polished vault entry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.weight(1f))

                FilledIconButton(
                    onClick = {
                        if (!isSaving && name.isNotBlank() && frontUri != null && backUri != null) {
                            isSaving = true

                            val front = ImageStore.saveImage(context, frontUri!!)
                            val back = ImageStore.saveImage(context, backUri!!)

                            onSave(
                                CardEntity(
                                    folderId = folder.id,
                                    name = name,
                                    frontImagePath = front,
                                    backImagePath = back,
                                    notes = notes.ifBlank { null }
                                )
                            )
                        }
                    },
                    enabled = name.isNotBlank() && frontUri != null && backUri != null && !isSaving
                ) {
                    Icon(Icons.Default.Check, null)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Card name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            )

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Brush.linearGradient(listOf(VaultGold, VaultCoral))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(folder.icon, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Folder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(folder.name, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            ScanImageBox(
                label = "Front Image",
                uri = frontUri,
                onScan = { launchCardScanner(true) }
            )

            ScanImageBox(
                label = "Back Image",
                uri = backUri,
                onScan = { launchCardScanner(false) }
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes optional") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
fun ScanImageBox(label: String, uri: Uri?, onScan: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onScan() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        if (uri == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color.White, Color(0xFFEAF6FF)))),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("Tap to scan $label", fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun CardDetailScreen(card: CardEntity, vm: CardViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var showBack by remember { mutableStateOf(false) }
    val file = File(if (showBack) card.backImagePath else card.frontImagePath)

    VaultBackground {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                Column(Modifier.weight(1f)) {
                    Text(card.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    Text(if (showBack) "Back side" else "Front side", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = { vm.togglePin(card) }) {
                    Icon(Icons.Default.Star, null, tint = if (card.isPinned) VaultGold else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(Modifier.fillMaxWidth().height(310.dp), contentAlignment = Alignment.Center) {
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                Card(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                offset += pan
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2f
                                }
                            })
                        },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Box(Modifier.background(Brush.linearGradient(listOf(Color.White, Color(0xFFEFF7FF))))) {
                        Image(
                            rememberAsyncImagePainter(file),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { showBack = false }, shape = RoundedCornerShape(16.dp)) { Text("Front") }
                Button(onClick = { showBack = true }, shape = RoundedCornerShape(16.dp)) { Text("Back") }
                FilledTonalButton(onClick = { ShareUtil.shareCard(context, card, shareBack = showBack) }, shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Share") }
            }
            card.notes?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = { vm.softDelete(card); onBack() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(6.dp)); Text("Delete") }
        }
    }
}

@Composable
fun NewFolderDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New List") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (folderName.isNotBlank()) onConfirm(folderName) },
                enabled = folderName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeletedCardsScreen(vm: CardViewModel, folder: FolderEntity, onBack: () -> Unit) {
    val deletedCardsFlow = remember(folder.id) {
        vm.deletedCards(folder.id)
    }

    val deletedCards by deletedCardsFlow.collectAsState(initial = emptyList())

    VaultBackground {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                Column {
                    Text("Deleted Cards", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    Text(folder.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (deletedCards.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("No deleted cards", Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(
                        items = deletedCards,
                        key = { it.id }
                    ) { card ->
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Row(
                                Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(
                                    modifier = Modifier.size(60.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(File(card.frontImagePath)),
                                        contentDescription = "Card front",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(card.name, fontWeight = FontWeight.ExtraBold)
                                    Text("Deleted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilledTonalIconButton(
                                        onClick = { vm.restore(card) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                                    }
                                    FilledTonalIconButton(
                                        onClick = { vm.delete(card) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
