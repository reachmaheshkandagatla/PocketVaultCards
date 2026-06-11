package com.mahesh.pocketvault

import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.app.KeyguardManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.text.format.DateFormat
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.mahesh.pocketvault.data.CardEntity
import com.mahesh.pocketvault.data.BankCardEntity
import com.mahesh.pocketvault.data.FolderEntity
import com.mahesh.pocketvault.data.GroceryItemEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val VaultInk = Color(0xFF14213D)
private val VaultBlue = Color(0xFF2457D6)
private val VaultTeal = Color(0xFF00A8A8)
private val VaultGold = Color(0xFFFFB703)
private val VaultCoral = Color(0xFFF77F6F)
private val VaultMist = Color(0xFFF6F8FC)
private val VaultSurface = Color(0xFFFFFFFF)
private val VaultLine = Color(0xFFE1E7F2)
private val BankCardColorKeys = listOf(
    BankCardEntity.COLOR_BLUE,
    BankCardEntity.COLOR_GREEN,
    BankCardEntity.COLOR_RED,
    BankCardEntity.COLOR_GOLD,
    BankCardEntity.COLOR_BLACK
)

private fun bankCardGradient(colorKey: String) = when (colorKey) {
    BankCardEntity.COLOR_GREEN -> listOf(Color(0xFF0D5B47), Color(0xFF18A36F))
    BankCardEntity.COLOR_RED -> listOf(Color(0xFF7D1D2D), Color(0xFFD5465A))
    BankCardEntity.COLOR_GOLD -> listOf(Color(0xFF7A5510), Color(0xFFE3A72F))
    BankCardEntity.COLOR_BLACK -> listOf(Color(0xFF171A22), Color(0xFF454B5E))
    else -> listOf(Color(0xFF162B5E), Color(0xFF0B6C76))
}

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

private fun billNameFor(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return "Bill ${formatter.format(Date(timestamp))}"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun MaxScreenBrightness() {
    val context = LocalContext.current

    DisposableEffect(context) {
        val activity = context.findActivity()
        val window = activity?.window
        if (window == null) {
            onDispose { }
        } else {
            val originalBrightness = window.attributes.screenBrightness
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }

            onDispose {
                window.attributes = window.attributes.apply {
                    screenBrightness = originalBrightness
                }
            }
        }
    }
}

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
                    },
                    onUnlock = { unlocked = true }
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

private fun authenticateForBankPin(activity: FragmentActivity, onSuccess: () -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        }
    )
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("View bank card PIN")
        .setSubtitle("Authenticate to reveal the saved PIN")
        .setNegativeButtonText("Cancel")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()

    biometricPrompt.authenticate(promptInfo)
}

@Composable
fun LockScreen(onRetry: () -> Unit, onUnlock: () -> Unit) {
    val context = LocalContext.current
    val keyguardManager = remember {
        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }
    val deviceCredentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onUnlock()
        }
    }

    fun unlockWithDeviceCredential() {
        val credentialIntent = keyguardManager.createConfirmDeviceCredentialIntent(
            "Unlock PocketVault",
            "Use your phone PIN, password, or pattern"
        )

        if (credentialIntent != null) {
            deviceCredentialLauncher.launch(credentialIntent)
        } else {
            onRetry()
        }
    }

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
                        OutlinedButton(
                            onClick = { unlockWithDeviceCredential() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Lock, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Use Phone PIN / Pattern")
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
    var selectedBill by remember { mutableStateOf<CardEntity?>(null) }
    var selectedBankCard by remember { mutableStateOf<BankCardEntity?>(null) }

    fun navigateBack() {
        screen = when (screen) {
            "category", "groceries", "bankCards", "bills" -> "home"
            "bankCardDetail" -> "bankCards"
            "billDetail" -> "bills"
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
                "home" -> HomeScreen(
                    vm,
                    onOpen = { folder ->
                        selectedFolder = folder
                        screen = when (folder.kind) {
                            FolderEntity.KIND_GROCERIES -> "groceries"
                            FolderEntity.KIND_BANK_CARDS -> "bankCards"
                            FolderEntity.KIND_BILLS -> "bills"
                            FolderEntity.KIND_COUPONS -> "category"
                            else -> "category"
                        }
                    }
                )
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
                "groceries" -> selectedFolder?.let { folder ->
                    GroceryListScreen(vm, folder, onBack = { screen = "home" })
                }
                "bills" -> selectedFolder?.let { folder ->
                    BillListScreen(
                        vm = vm,
                        folder = folder,
                        onBack = { screen = "home" },
                        onOpenBill = { bill ->
                            selectedBill = bill
                            screen = "billDetail"
                        }
                    )
                }
                "billDetail" -> selectedBill?.let { bill ->
                    BillDetailScreen(
                        bill = bill,
                        vm = vm,
                        onBack = { screen = "bills" }
                    )
                }
                "bankCards" -> selectedFolder?.let { folder ->
                    BankCardListScreen(
                        vm,
                        folder,
                        onBack = { screen = "home" },
                        onOpenBankCard = { bankCard ->
                            selectedBankCard = bankCard
                            screen = "bankCardDetail"
                        }
                    )
                }
                "bankCardDetail" -> selectedBankCard?.let { bankCard ->
                    BankCardDetailScreen(bankCard, vm, onBack = { screen = "bankCards" })
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
private fun ListKindIcon(folder: FolderEntity, modifier: Modifier = Modifier) {
    when (folder.kind) {
        FolderEntity.KIND_COUPONS -> Text("%", fontWeight = FontWeight.ExtraBold, fontSize = MaterialTheme.typography.titleLarge.fontSize)
        FolderEntity.KIND_GROCERIES -> Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = modifier)
        FolderEntity.KIND_BANK_CARDS -> Text("💳", fontSize = MaterialTheme.typography.titleLarge.fontSize)
        FolderEntity.KIND_BILLS -> Text("🧾", fontSize = MaterialTheme.typography.titleLarge.fontSize)
        else -> Icon(Icons.Default.AccountBox, contentDescription = null, modifier = modifier)
    }
}

@Composable
fun HomeScreen(vm: CardViewModel, onOpen: (FolderEntity) -> Unit) {
    val context = LocalContext.current
    val foldersFlow = remember { vm.folders() }
    val folders by foldersFlow.collectAsState(initial = emptyList())
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderPendingAction by remember { mutableStateOf<FolderEntity?>(null) }
    var folderPendingRename by remember { mutableStateOf<FolderEntity?>(null) }
    var folderPendingDelete by remember { mutableStateOf<FolderEntity?>(null) }

    LaunchedEffect(folders) {
        folders
            .filter { it.kind == FolderEntity.KIND_GROCERIES && GroceryReminderScheduler.isEnabled(context, it.id) }
            .forEach { GroceryReminderScheduler.schedule(context, it.id, it.name) }
    }

    VaultBackground {
        Box(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "PocketVault logo",
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
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

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Text("Folders", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(folders, key = { it.id }) { folder ->
                        val itemCountFlow = remember(folder.id, folder.kind) {
                            when (folder.kind) {
                                FolderEntity.KIND_COUPONS -> vm.count(folder.id)
                                FolderEntity.KIND_GROCERIES -> vm.groceryCount(folder.id)
                                FolderEntity.KIND_BANK_CARDS -> vm.bankCardCount(folder.id)
                                FolderEntity.KIND_BILLS -> vm.count(folder.id)
                                else -> vm.count(folder.id)
                            }
                        }
                        val itemCount by itemCountFlow.collectAsState(initial = 0)
                        FolderTile(
                            folder = folder,
                            subtitle = when (folder.kind) {
                                FolderEntity.KIND_COUPONS -> "$itemCount coupons"
                                FolderEntity.KIND_GROCERIES -> "$itemCount groceries"
                                FolderEntity.KIND_BANK_CARDS -> "$itemCount bank cards"
                                FolderEntity.KIND_BILLS -> "$itemCount bills"
                                else -> "$itemCount Cards"
                            },
                            onClick = { onOpen(folder) },
                            onLongPress = { folderPendingAction = folder }
                        )
                    }
                }

                Text(
                    "100% offline. No internet permission. Your cards stay on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 58.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 22.dp)
            ) {
                FilledIconButton(
                    onClick = { showCreateFolderDialog = true },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create list", modifier = Modifier.size(22.dp))
                }
            }
        }
    }
    
    if (showCreateFolderDialog) {
        NewFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { folderName, kind ->
                vm.addFolder(
                    FolderEntity(
                        name = folderName,
                        icon = when (kind) {
                            FolderEntity.KIND_COUPONS -> "COUPON"
                            FolderEntity.KIND_GROCERIES -> "🛒"
                            FolderEntity.KIND_BANK_CARDS -> "💳"
                            FolderEntity.KIND_BILLS -> "🧾"
                            else -> "ID"
                        },
                        kind = kind
                    )
                )
                showCreateFolderDialog = false
            }
        )
    }

    folderPendingAction?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderPendingAction = null },
            title = { Text(folder.name) },
            text = { Text("Choose an action for this folder.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        folderPendingAction = null
                        folderPendingRename = folder
                    }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rename")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            folderPendingAction = null
                            folderPendingDelete = folder
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete")
                    }
                    TextButton(onClick = { folderPendingAction = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    folderPendingRename?.let { folder ->
        RenameDialog(
            title = "Rename Folder",
            label = "Folder name",
            currentName = folder.name,
            onDismiss = { folderPendingRename = null },
            onConfirm = { newName ->
                vm.renameFolder(folder, newName)
                folderPendingRename = null
            }
        )
    }

    folderPendingDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderPendingDelete = null },
            title = { Text("Delete folder?") },
            text = {
                Text(
                    when (folder.kind) {
                        FolderEntity.KIND_COUPONS -> "This deletes ${folder.name} and every coupon card inside it."
                        FolderEntity.KIND_GROCERIES -> "This deletes ${folder.name} and every grocery item inside it."
                        FolderEntity.KIND_BANK_CARDS -> "This deletes ${folder.name} and every bank card inside it."
                        FolderEntity.KIND_BILLS -> "This deletes ${folder.name} and every scanned bill inside it."
                        else -> "This deletes ${folder.name} and every card inside it."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteFolder(folder)
                        folderPendingDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun FolderTile(folder: FolderEntity, subtitle: String, onClick: () -> Unit, onLongPress: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(VaultGold.copy(alpha = 0.75f), VaultCoral.copy(alpha = 0.85f)))),
                contentAlignment = Alignment.Center
            ) {
                ListKindIcon(folder, modifier = Modifier.size(28.dp))
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    folder.name,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
    val itemLabel = if (folder.kind == FolderEntity.KIND_COUPONS) "coupons" else "cards"
    val itemTitle = if (folder.kind == FolderEntity.KIND_COUPONS) "Coupons" else "Cards"
    var cardPendingRename by remember { mutableStateOf<CardEntity?>(null) }

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
                        ListKindIcon(folder, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(folder.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                        Text("${cards.size} active $itemLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                SectionTitle("Frequent $itemTitle", trailing = if (frequent.isEmpty()) "Pin favorites" else null)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(132.dp)) {
                    items(
                        items = frequent,
                        key = { it.id }
                    ) { card ->
                        SmallCard(card) { onOpenCard(card) }
                    }
                }

                SectionTitle("All $itemTitle")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(
                        items = cards,
                        key = { it.id }
                    ) { card ->
                        CardRow(card, vm, onOpenCard, onRename = { cardPendingRename = card })
                    }
                }
            }
        }
    }

    cardPendingRename?.let { card ->
        RenameDialog(
            title = "Rename ${if (folder.kind == FolderEntity.KIND_COUPONS) "Coupon" else "Card"}",
            label = "Name",
            currentName = card.name,
            onDismiss = { cardPendingRename = null },
            onConfirm = { newName ->
                vm.rename(card, newName)
                cardPendingRename = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroceryListScreen(vm: CardViewModel, folder: FolderEntity, onBack: () -> Unit) {
    val context = LocalContext.current
    val groceryItemsFlow = remember(folder.id) { vm.groceryItems(folder.id) }
    val groceryItems by groceryItemsFlow.collectAsState(initial = emptyList())
    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    val doneCount = groceryItems.count { it.isDone }
    val pendingCount = groceryItems.count { !it.isDone }
    var remindersEnabled by remember(folder.id) {
        mutableStateOf(GroceryReminderScheduler.isEnabled(context, folder.id))
    }
    var reminderTime by remember(folder.id) {
        mutableStateOf(GroceryReminderScheduler.getReminderTime(context, folder.id))
    }
    val reminderTimeLabel = GroceryReminderScheduler.formatReminderTime(reminderTime)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            GroceryReminderScheduler.setEnabled(context, folder.id, folder.name, true)
            remindersEnabled = true
        } else {
            remindersEnabled = false
        }
    }

    fun enableReminders() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GroceryReminderScheduler.setEnabled(context, folder.id, folder.name, true)
            remindersEnabled = true
        }
    }

    LaunchedEffect(remindersEnabled, pendingCount, folder.id, folder.name) {
        if (remindersEnabled) {
            if (pendingCount > 0) {
                GroceryReminderScheduler.schedule(context, folder.id, folder.name)
            } else {
                GroceryReminderScheduler.cancel(context, folder.id)
            }
        }
    }

    fun showReminderTimePicker() {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val selectedTime = GroceryReminderScheduler.ReminderTime(hourOfDay, minute)
                GroceryReminderScheduler.setReminderTime(context, folder.id, folder.name, hourOfDay, minute)
                reminderTime = selectedTime
                Toast.makeText(
                    context,
                    "Reminder set for ${GroceryReminderScheduler.formatReminderTime(selectedTime)}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            reminderTime.hour,
            reminderTime.minute,
            DateFormat.is24HourFormat(context)
        ).show()
    }

    fun addItem() {
        if (itemName.isNotBlank() && quantity.isNotBlank()) {
            vm.addGroceryItem(
                GroceryItemEntity(
                    folderId = folder.id,
                    name = itemName.trim(),
                    quantity = quantity.trim()
                )
            )
            itemName = ""
            quantity = ""
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
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(VaultTeal, VaultBlue))),
                    contentAlignment = Alignment.Center
                ) {
                    ListKindIcon(folder, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(folder.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "$doneCount of ${groceryItems.size} done",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { showReminderTimePicker() }
                ),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Grocery reminders", fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (remindersEnabled) {
                                if (pendingCount > 0) "On for $reminderTimeLabel purchase reminder" else "On at $reminderTimeLabel, no pending items"
                            } else {
                                "Off, set for $reminderTimeLabel"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                enableReminders()
                            } else {
                                GroceryReminderScheduler.setEnabled(context, folder.id, folder.name, false)
                                remindersEnabled = false
                            }
                        }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = { Text("Grocery") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Qty") },
                            modifier = Modifier.width(104.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    Button(
                        onClick = { addItem() },
                        enabled = itemName.isNotBlank() && quantity.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add Grocery")
                    }
                }
            }

            SectionTitle("Shopping List")
            if (groceryItems.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "No groceries yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(groceryItems, key = { it.id }) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isDone,
                                    onCheckedChange = { vm.toggleGroceryItem(item) }
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (item.isDone) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Text(
                                        item.quantity,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { vm.deleteGroceryItem(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete ${item.name}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BillListScreen(
    vm: CardViewModel,
    folder: FolderEntity,
    onBack: () -> Unit,
    onOpenBill: (CardEntity) -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val billsFlow = remember(folder.id) { vm.cards(folder.id) }
    val bills by billsFlow.collectAsState(initial = emptyList())
    val sortedBills = remember(bills) { bills.sortedByDescending { it.createdAt } }
    var isScanning by remember { mutableStateOf(false) }
    var billPendingRename by remember { mutableStateOf<CardEntity?>(null) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        isScanning = false
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val scannedUri = scanResult?.pages?.firstOrNull()?.imageUri

            if (scannedUri != null) {
                val capturedAt = System.currentTimeMillis()
                val imagePath = ImageStore.saveImage(context, scannedUri)
                vm.add(
                    CardEntity(
                        folderId = folder.id,
                        name = billNameFor(capturedAt),
                        frontImagePath = imagePath,
                        backImagePath = "",
                        createdAt = capturedAt
                    )
                )
            }
        }
    }

    fun launchBillScanner() {
        if (isScanning) return
        isScanning = true

        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                isScanning = false
                it.printStackTrace()
            }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { launchBillScanner() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = "Scan bill")
                }
            }
        }
    ) { pad ->
        VaultBackground {
            Column(
                Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        ListKindIcon(folder, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(folder.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                        Text("${sortedBills.size} scanned bills", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (sortedBills.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("No bills scanned yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                Button(onClick = { launchBillScanner() }, shape = RoundedCornerShape(16.dp)) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Scan Bill")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(
                            items = sortedBills,
                            key = { it.id }
                        ) { bill ->
                            BillRow(
                                bill = bill,
                                onOpen = { onOpenBill(bill) },
                                onRename = { billPendingRename = bill },
                                onShare = { ShareUtil.shareImage(context, bill.frontImagePath, bill.name) },
                                onDelete = { vm.softDelete(bill) }
                            )
                        }
                    }
                }
            }
        }
    }

    billPendingRename?.let { bill ->
        RenameDialog(
            title = "Rename Bill",
            label = "Bill name",
            currentName = bill.name,
            onDismiss = { billPendingRename = null },
            onConfirm = { newName ->
                vm.rename(bill, newName)
                billPendingRename = null
            }
        )
    }
}

@Composable
fun BillRow(bill: CardEntity, onOpen: () -> Unit, onRename: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
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
                modifier = Modifier.size(66.dp),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(File(bill.frontImagePath)),
                    contentDescription = "Bill scan",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(bill.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
                Text("Tap to view", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            FilledTonalIconButton(onClick = onRename, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(4.dp))
            FilledTonalIconButton(onClick = onShare, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(4.dp))
            FilledTonalIconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun BillDetailScreen(bill: CardEntity, vm: CardViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var currentBill by remember(bill.id) { mutableStateOf(bill) }
    var showRenameDialog by remember { mutableStateOf(false) }

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
                    Text(currentBill.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    Text("Scanned bill", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = { showRenameDialog = true }) {
                    Icon(Icons.Default.Edit, null)
                }
                Spacer(Modifier.width(6.dp))
                FilledTonalIconButton(onClick = { ShareUtil.shareImage(context, currentBill.frontImagePath, currentBill.name) }) {
                    Icon(Icons.Default.Share, null)
                }
            }

            Card(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(File(currentBill.frontImagePath)),
                    contentDescription = "Bill scan",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentScale = ContentScale.Fit
                )
            }

            OutlinedButton(
                onClick = { vm.softDelete(currentBill); onBack() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(Modifier.width(6.dp))
                Text("Delete")
            }
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            title = "Rename Bill",
            label = "Bill name",
            currentName = currentBill.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                val renamed = currentBill.copy(name = newName.trim())
                vm.rename(currentBill, newName)
                currentBill = renamed
                showRenameDialog = false
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BankCardListScreen(vm: CardViewModel, folder: FolderEntity, onBack: () -> Unit, onOpenBankCard: (BankCardEntity) -> Unit) {
    val bankCardsFlow = remember(folder.id) { vm.bankCards(folder.id) }
    val bankCards by bankCardsFlow.collectAsState(initial = emptyList())
    var name by remember { mutableStateOf("") }
    var cardType by remember { mutableStateOf(BankCardEntity.TYPE_DEBIT) }
    var cardColor by remember { mutableStateOf(BankCardEntity.COLOR_BLUE) }
    var showCardTypeMenu by remember { mutableStateOf(false) }
    var lastFourDigits by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showPinInput by remember { mutableStateOf(false) }
    var showAddBankCardDialog by remember { mutableStateOf(false) }

    fun addBankCard() {
        if (name.isNotBlank() && lastFourDigits.length == 4 && pin.isNotBlank()) {
            vm.addBankCard(
                BankCardEntity(
                    folderId = folder.id,
                    name = name.trim(),
                    cardType = cardType,
                    colorKey = cardColor,
                    lastFourDigits = lastFourDigits,
                    pin = pin
                )
            )
            name = ""
            cardType = BankCardEntity.TYPE_DEBIT
            cardColor = BankCardEntity.COLOR_BLUE
            lastFourDigits = ""
            pin = ""
            showPinInput = false
            showAddBankCardDialog = false
        }
    }

    VaultBackground {
        Box(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(VaultInk, VaultBlue))),
                    contentAlignment = Alignment.Center
                ) {
                    ListKindIcon(folder, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(folder.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${bankCards.size} masked bank cards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

                SectionTitle("Bank Cards")
                if (bankCards.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            "No bank cards yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bankCards, key = { it.id }) { bankCard ->
                            Card(
                                modifier = Modifier.clickable { onOpenBankCard(bankCard) },
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(Brush.linearGradient(bankCardGradient(bankCard.colorKey)))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            bankCard.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            color = Color.White.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                bankCard.cardType,
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        IconButton(onClick = { vm.deleteBankCard(bankCard) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete bank card", tint = Color.White)
                                        }
                                    }
                                    Text(
                                        "****  ****  ****  ${bankCard.lastFourDigits}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "PIN ****",
                                        color = Color.White.copy(alpha = 0.92f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            FilledIconButton(
                onClick = { showAddBankCardDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 18.dp)
                    .size(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add bank card")
            }
        }
    }

    if (showAddBankCardDialog) {
        AlertDialog(
            onDismissRequest = { showAddBankCardDialog = false },
            title = { Text("Add Bank Card") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Bank card name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    ExposedDropdownMenuBox(
                        expanded = showCardTypeMenu,
                        onExpandedChange = { showCardTypeMenu = !showCardTypeMenu }
                    ) {
                        OutlinedTextField(
                            value = cardType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Card type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCardTypeMenu) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = showCardTypeMenu,
                            onDismissRequest = { showCardTypeMenu = false }
                        ) {
                            listOf(BankCardEntity.TYPE_DEBIT, BankCardEntity.TYPE_CREDIT).forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        cardType = type
                                        showCardTypeMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Text("Bank color", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BankCardColorKeys.forEach { colorKey ->
                            val colors = bankCardGradient(colorKey)
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(colors))
                                    .border(
                                        width = if (cardColor == colorKey) 3.dp else 1.dp,
                                        color = if (cardColor == colorKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { cardColor = colorKey }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = lastFourDigits,
                        onValueChange = { input ->
                            lastFourDigits = input.filter(Char::isDigit).take(4)
                        },
                        label = { Text("Last 4 digits") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit) },
                        label = { Text("PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPinInput) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(onClick = { showPinInput = !showPinInput }) {
                                Text(if (showPinInput) "Hide" else "Show")
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { addBankCard() },
                    enabled = name.isNotBlank() && lastFourDigits.length == 4 && pin.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBankCardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BankCardDetailScreen(bankCard: BankCardEntity, vm: CardViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var showPin by remember { mutableStateOf(false) }

    VaultBackground {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                Column {
                    Text(bankCard.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Selected bank card",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(bankCardGradient(bankCard.colorKey)))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            bankCard.name,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                bankCard.cardType,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Text(
                        "****  ****  ****  ${bankCard.lastFourDigits}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (showPin) "PIN ${bankCard.pin}" else "PIN ****",
                        color = Color.White.copy(alpha = 0.94f),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            FilledTonalButton(
                onClick = {
                    if (showPin) {
                        showPin = false
                    } else {
                        authenticateForBankPin(activity) {
                            showPin = true
                            vm.markBankCardViewed(bankCard)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Lock, null)
                Spacer(Modifier.width(6.dp))
                Text(if (showPin) "Hide PIN" else "View PIN")
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
fun CardRow(card: CardEntity, vm: CardViewModel, onOpen: (CardEntity) -> Unit, onRename: () -> Unit) {
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
                if (card.frontImagePath.isBlank()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(File(card.frontImagePath)),
                        contentDescription = "Card front",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
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
                        text = { Text("Rename") },
                        onClick = { onRename(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
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

    val frontImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { frontUri = it }
    }

    val backImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { backUri = it }
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
                .verticalScroll(rememberScrollState())
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
                    Text("Scan or upload the front. Rear side is optional.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.weight(1f))

                FilledIconButton(
                    onClick = {
                        if (
                            !isSaving &&
                            name.isNotBlank() &&
                            frontUri != null
                        ) {
                            isSaving = true

                            val front = ImageStore.saveImage(context, frontUri!!)
                            val back = backUri?.let { ImageStore.saveImage(context, it) }.orEmpty()

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
                    enabled = name.isNotBlank() && frontUri != null && !isSaving
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
                        ListKindIcon(folder, modifier = Modifier.size(26.dp))
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
                onScan = { launchCardScanner(true) },
                onUpload = { frontImagePicker.launch("image/*") }
            )

            ScanImageBox(
                label = "Rear Image Optional",
                uri = backUri,
                onScan = { launchCardScanner(false) },
                onUpload = { backImagePicker.launch("image/*") }
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
fun ScanImageBox(label: String, uri: Uri?, onScan: () -> Unit, onUpload: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clickable { onScan() }
                    .background(Brush.linearGradient(listOf(Color.White, Color(0xFFEAF6FF)))),
                contentAlignment = Alignment.Center
            ) {
                if (uri == null) {
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
                        Text(label, fontWeight = FontWeight.SemiBold)
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onScan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Scan")
                }
                FilledTonalButton(
                    onClick = onUpload,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Upload")
                }
            }
        }
    }
}

@Composable
fun CardDetailScreen(card: CardEntity, vm: CardViewModel, onBack: () -> Unit) {
    MaxScreenBrightness()

    val context = LocalContext.current
    var currentCard by remember(card.id) { mutableStateOf(card) }
    var showBack by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    val imagePath = if (showBack) currentCard.backImagePath else currentCard.frontImagePath

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
                    Text(currentCard.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    Text(if (showBack) "Rear side" else "Front side", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = { showRenameDialog = true }) {
                    Icon(Icons.Default.Edit, null)
                }
                Spacer(Modifier.width(6.dp))
                FilledTonalIconButton(onClick = { vm.togglePin(currentCard); currentCard = currentCard.copy(isPinned = !currentCard.isPinned) }) {
                    Icon(Icons.Default.Star, null, tint = if (currentCard.isPinned) VaultGold else MaterialTheme.colorScheme.onSurfaceVariant)
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
                        if (imagePath.isBlank()) {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBox,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No ${if (showBack) "rear" else "front"} image saved",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Image(
                                rememberAsyncImagePainter(File(imagePath)),
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
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { showBack = false }, shape = RoundedCornerShape(16.dp)) { Text("Front") }
                Button(onClick = { showBack = true }, shape = RoundedCornerShape(16.dp)) { Text("Rear") }
                FilledTonalButton(
                    onClick = { ShareUtil.shareCard(context, currentCard, shareBack = showBack) },
                    enabled = imagePath.isNotBlank(),
                    shape = RoundedCornerShape(16.dp)
                ) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Share") }
            }
            currentCard.notes?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = { vm.softDelete(currentCard); onBack() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(6.dp)); Text("Delete") }
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            title = "Rename Card",
            label = "Card name",
            currentName = currentCard.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                val renamed = currentCard.copy(name = newName.trim())
                vm.rename(currentCard, newName)
                currentCard = renamed
                showRenameDialog = false
            }
        )
    }
}

@Composable
fun RenameDialog(
    title: String,
    label: String,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    val trimmedName = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(trimmedName) },
                enabled = trimmedName.isNotBlank() && trimmedName != currentName
            ) {
                Text("Save")
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
fun NewFolderDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var folderName by remember { mutableStateOf("") }
    var selectedKind by remember { mutableStateOf(FolderEntity.KIND_CARDS) }
    var showKindMenu by remember { mutableStateOf(false) }
    val folderKinds = listOf(
        FolderEntity.KIND_CARDS,
        FolderEntity.KIND_COUPONS,
        FolderEntity.KIND_BANK_CARDS,
        FolderEntity.KIND_GROCERIES,
        FolderEntity.KIND_BILLS
    )

    fun folderKindLabel(kind: String) = when (kind) {
        FolderEntity.KIND_COUPONS -> "Coupons"
        FolderEntity.KIND_BANK_CARDS -> "Bank Cards"
        FolderEntity.KIND_GROCERIES -> "Groceries"
        FolderEntity.KIND_BILLS -> "Bills"
        else -> "Cards"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Folder") },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box {
                    OutlinedButton(
                        onClick = { showKindMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(folderKindLabel(selectedKind), modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showKindMenu,
                        onDismissRequest = { showKindMenu = false }
                    ) {
                        folderKinds.forEach { kind ->
                            DropdownMenuItem(
                                text = { Text(folderKindLabel(kind)) },
                                onClick = {
                                    selectedKind = kind
                                    showKindMenu = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("List name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (folderName.isNotBlank()) onConfirm(folderName, selectedKind) },
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
