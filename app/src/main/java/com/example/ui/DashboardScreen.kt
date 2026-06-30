package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Cat
import com.example.data.Reminder
import com.example.data.TrackingLog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// --- WARM COZY MATERIAL 3 PALETTE ---
val CatPrimary = Color(0xFFD35E47) // Warm Terracotta
val CatSecondary = Color(0xFF7A9A60) // Sage Green
val CatTertiary = Color(0xFFE2A93E) // Warm Gold
val CatBackground = Color(0xFFFDFBF7) // Cozy Cream
val CatSurface = Color(0xFFFAF4EB) // Darker warm cream
val CatTextPrimary = Color(0xFF3E2A25) // Soft Dark Espresso
val CatTextSecondary = Color(0xFF7E6E6A) // Medium Cocoa Gray

val CatTrackerColorScheme = lightColorScheme(
    primary = CatPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFECE6),
    onPrimaryContainer = Color(0xFF3B0B00),
    secondary = CatSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDF5E8),
    onSecondaryContainer = Color(0xFF192F13),
    tertiary = CatTertiary,
    onTertiary = Color.White,
    background = CatBackground,
    onBackground = CatTextPrimary,
    surface = CatSurface,
    onSurface = CatTextPrimary,
    surfaceVariant = Color(0xFFEFE8DD),
    onSurfaceVariant = Color(0xFF53433F)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(viewModel: CatTrackerViewModel) {
    val cats by viewModel.allCats.collectAsStateWithLifecycle()
    val selectedCat by viewModel.selectedCat.collectAsStateWithLifecycle()
    val displayDate by viewModel.displayDate.collectAsStateWithLifecycle()
    val logs by viewModel.dailyLogs.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val adviceState by viewModel.aiAdviceState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showAddCatDialog by remember { mutableStateOf(false) }
    var showVetReminderDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Diet, 1: Activity, 2: Health, 3: Reminders & Vet

    MaterialTheme(
        colorScheme = CatTrackerColorScheme
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🐾", fontSize = 26.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Cat Tracker",
                                fontWeight = FontWeight.Bold,
                                color = CatTextPrimary,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = CatBackground
                    )
                )
            },
            containerColor = CatBackground
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                if (cats.isEmpty()) {
                    EmptyStateScreen(onAddCatClick = { showAddCatDialog = true })
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Profile Carousel
                        item {
                            CatProfileSelectorHeader(
                                cats = cats,
                                selectedCat = selectedCat,
                                onCatSelected = { viewModel.selectCat(it.id) },
                                onAddCatClick = { showAddCatDialog = true }
                            )
                        }

                        selectedCat?.let { activeCat ->
                            // 2. Active Cat Metadata
                            item {
                                ActiveCatDateHeader(
                                    cat = activeCat,
                                    displayDate = displayDate,
                                    onPrevDate = { viewModel.changeDate(-1) },
                                    onNextDate = { viewModel.changeDate(1) },
                                    onDeleteCat = { viewModel.deleteCat(activeCat) }
                                )
                            }

                            // 3. Status Overview Widgets
                            item {
                                QuickOverviewGrid(logs = logs, catWeight = activeCat.weightKg)
                            }

                            // 4. Mode Selection Navigation Tabs
                            item {
                                NavigationTabs(
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectedTab = it }
                                )
                            }

                            // 5. Active Logging Panels
                            item {
                                AnimatedContent(
                                    targetState = selectedTab,
                                    transitionSpec = {
                                        fadeIn() togetherWith fadeOut()
                                    },
                                    label = "tab_logging"
                                ) { tab ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = CatSurface),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE6DBC9))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            when (tab) {
                                                0 -> DietLoggingPanel(onLogMetric = viewModel::logMetric)
                                                1 -> ActivityLoggingPanel(onLogMetric = viewModel::logMetric)
                                                2 -> HealthLoggingPanel(
                                                    currentWeight = activeCat.weightKg,
                                                    onLogMetric = viewModel::logMetric
                                                )
                                                3 -> RemindersAndVetPanel(
                                                    reminders = reminders,
                                                    onAddReminderClick = { showVetReminderDialog = true },
                                                    onToggleCompleted = { viewModel.toggleReminderCompleted(it) },
                                                    onDeleteReminder = { viewModel.deleteReminder(it) },
                                                    onTestNotification = {
                                                        NotificationHelper.triggerNotification(
                                                            context = context,
                                                            title = "🐈 Cat Tracker Appointment Reminder",
                                                            message = "Friendly reminder: ${activeCat.name}'s veterinarian appointment is coming up soon! Please double check their hydration status today."
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Show logs header for diet, activity and health
                            if (selectedTab < 3) {
                                item {
                                    Text(
                                        "Today's Activity Logs",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = CatTextPrimary,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }

                                if (logs.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(CatSurface)
                                                .border(BorderStroke(1.dp, Color(0xFFEFE8DD)), RoundedCornerShape(16.dp))
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🐾", fontSize = 32.sp)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    "No logs for this day yet.",
                                                    color = CatTextSecondary,
                                                    fontSize = 14.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    "Fill out one of the panels above to start tracking!",
                                                    color = CatTextSecondary,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(logs, key = { it.id }) { log ->
                                        LogHistoryItem(
                                            log = log,
                                            onDelete = { viewModel.deleteLog(log) }
                                        )
                                    }
                                }
                            }

                            // 6. Gemini Insights Advisor Card
                            item {
                                SmartAiAdvisorCard(
                                    adviceState = adviceState,
                                    cat = activeCat,
                                    logs = logs,
                                    onGenerateAdvice = { viewModel.generateAiAdvice() }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }

                // Add Cat Dialog (Modal)
                if (showAddCatDialog) {
                    AddCatDialog(
                        onDismiss = { showAddCatDialog = false },
                        onAddCat = { name, breed, age, weight, avatar ->
                            viewModel.addCat(name, breed, age, weight, avatar)
                            showAddCatDialog = false
                        }
                    )
                }

                // Add Vet Appointment Dialog
                if (showVetReminderDialog) {
                    VetReminderDialog(
                        onDismiss = { showVetReminderDialog = false },
                        onAddReminder = { title, type, dueDate, notes ->
                            viewModel.addReminder(title, type, dueDate, notes)
                            
                            // Send immediate notification reminder to verify system works perfectly
                            NotificationHelper.triggerNotification(
                                context = context,
                                title = "🩺 Vet Appointment Scheduled",
                                message = "${selectedCat?.name ?: "Your cat"}'s $type \"$title\" is scheduled for $dueDate. We'll remind you!"
                            )
                            showVetReminderDialog = false
                        }
                    )
                }
            }
        }
    }
}

// --- SUB-COMPOSABLES ---

@Composable
fun EmptyStateScreen(onAddCatClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFECE6)),
                contentAlignment = Alignment.Center
            ) {
                Text("🐈", fontSize = 72.sp)
            }

            Text(
                text = "Welcome to Cat Tracker!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CatTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Keep your feline companions happy and healthy by monitoring their daily diet, active playtime, and veterinary logs. Start by adding your first cat profile!",
                style = MaterialTheme.typography.bodyMedium,
                color = CatTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Button(
                onClick = onAddCatClick,
                colors = ButtonDefaults.buttonColors(containerColor = CatPrimary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier
                    .testTag("add_first_cat_button")
                    .padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Your Cat", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun CatProfileSelectorHeader(
    cats: List<Cat>,
    selectedCat: Cat?,
    onCatSelected: (Cat) -> Unit,
    onAddCatClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Your Cats",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = CatTextSecondary
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(cats, key = { it.id }) { cat ->
                val isSelected = selectedCat?.id == cat.id
                val borderBrush = if (isSelected) {
                    Brush.sweepGradient(listOf(CatPrimary, CatTertiary, CatPrimary))
                } else {
                    null
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onCatSelected(cat) }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFFFFECE6) else CatSurface)
                            .then(
                                if (borderBrush != null) Modifier.border(
                                    3.dp,
                                    borderBrush,
                                    CircleShape
                                ) else Modifier.border(1.dp, Color(0xFFE6DBC9), CircleShape)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CatAvatarRenderer(cat.avatarEmoji, size = 60.dp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cat.name,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (isSelected) CatPrimary else CatTextPrimary,
                        modifier = Modifier.widthIn(max = 70.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Quick Add Cat Button
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onAddCatClick() }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .border(
                                BorderStroke(1.5.dp, CatPrimary),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Cat", tint = CatPrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "New Profile",
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = CatPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun CatAvatarRenderer(avatarString: String, size: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    if (avatarString.startsWith("ic_avatar_")) {
        val resId = context.resources.getIdentifier(avatarString, "drawable", context.packageName)
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = "Cat Avatar",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Fallback in case of ID missing
            Text("🐱", fontSize = (size.value * 0.5f).sp)
        }
    } else {
        // Render Emoji Text
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Text(avatarString, fontSize = (size.value * 0.5f).sp)
        }
    }
}

@Composable
fun ActiveCatDateHeader(
    cat: Cat,
    displayDate: String,
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit,
    onDeleteCat: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CatSurface)
            .border(BorderStroke(1.dp, Color(0xFFEFE8DD)), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CatAvatarRenderer(cat.avatarEmoji, size = 52.dp)
                Column {
                    Text(
                        text = cat.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = CatTextPrimary
                    )
                    Text(
                        text = "${cat.breed} • ${cat.ageMonths / 12} yrs ${cat.ageMonths % 12} mos",
                        fontSize = 14.sp,
                        color = CatTextSecondary
                    )
                }
            }
            
            IconButton(
                onClick = { showDeleteConfirm = true },
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFFC24C38))
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete Cat Profile")
            }
        }

        Divider(color = Color(0xFFEFE8DD), thickness = 1.dp)

        // Date selection row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevDate) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Day", tint = CatPrimary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = CatTextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = displayDate,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = CatTextPrimary
                )
            }

            IconButton(onClick = onNextDate) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Day", tint = CatPrimary)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Cat Profile") },
            text = { Text("Are you sure you want to delete ${cat.name}'s profile? This will remove all medical schedules, diet inputs, and activity summaries permanently.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCat()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC24C38))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun QuickOverviewGrid(logs: List<TrackingLog>, catWeight: Double) {
    val dryFoodTotal = logs.filter { it.metricName == "dry_food" }.sumOf { it.valueDouble }.roundToInt()
    val wetFoodTotal = logs.filter { it.metricName == "wet_food" }.sumOf { it.valueDouble }.roundToInt()
    val waterTotal = logs.filter { it.metricName == "water" }.sumOf { it.valueDouble }.roundToInt()
    val playTotal = logs.filter { it.metricName == "playtime" }.sumOf { it.valueDouble }.roundToInt()
    val sleepTotal = logs.filter { it.metricName == "sleep" }.sumOf { it.valueDouble }
    
    val latestWeightLog = logs.find { it.metricName == "weight" }?.valueDouble ?: catWeight
    val latestMoodLog = logs.find { it.metricName == "mood" }?.valueString ?: "Happy"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OverviewWidget(
            icon = "🍖",
            title = "Food Portions",
            value = "${dryFoodTotal + wetFoodTotal}g",
            subtext = "Dry: ${dryFoodTotal}g • Wet: ${wetFoodTotal}g",
            modifier = Modifier.weight(1f)
        )

        OverviewWidget(
            icon = "💧",
            title = "Water Intake",
            value = "${waterTotal}ml",
            subtext = if (waterTotal < 150) "Target: 180ml" else "Fully Hydrated!",
            modifier = Modifier.weight(1f)
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OverviewWidget(
            icon = "⚡",
            title = "Play & Sleep",
            value = "${playTotal}m / ${sleepTotal}h",
            subtext = "Naps: ${sleepTotal} hrs logged",
            modifier = Modifier.weight(1f)
        )

        OverviewWidget(
            icon = "🩺",
            title = "Health Vibe",
            value = "${latestWeightLog}kg",
            subtext = "Mood: $latestMoodLog",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun OverviewWidget(
    icon: String,
    title: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CatSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEFE8DD))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(icon, fontSize = 20.sp)
                Text(title, fontSize = 11.sp, color = CatTextSecondary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, color = CatTextPrimary, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtext, fontSize = 10.sp, color = CatTextSecondary, maxLines = 1)
        }
    }
}

@Composable
fun NavigationTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        contentColor = CatPrimary,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = CatPrimary,
                height = 3.dp
            )
        },
        divider = {
            Divider(color = Color(0xFFEFE8DD))
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            text = { Text("🍗 Diet", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            text = { Text("⚡ Active", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        )
        Tab(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            text = { Text("🩺 Health", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        )
        Tab(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            text = { Text("🩺 Reminders", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        )
    }
}

// --- LOGGING INPUT PANELS ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DietLoggingPanel(onLogMetric: (type: String, metricName: String, valueDouble: Double, valueString: String) -> Unit) {
    var foodType by remember { mutableStateOf("Salmon Kibble") }
    var foodAmount by remember { mutableStateOf(50) }
    var timeOfDay by remember { mutableStateOf("Morning 🌅") }
    var waterAmount by remember { mutableStateOf(80) }

    val foodTypesList = listOf("Salmon Kibble 🐟", "Chicken Pate 🍗", "Tuna Pouch 🥫", "Hypoallergenic 🩺", "Dry Mix 🌾")
    val timesList = listOf("Morning 🌅", "Afternoon ☀️", "Evening 🌌")

    Text("Log Food Portion", fontWeight = FontWeight.Bold, color = CatTextPrimary, fontSize = 15.sp)

    // Food type selection chips
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        foodTypesList.forEach { type ->
            val isSelected = foodType == type.split(" ")[0]
            FilterChip(
                selected = isSelected,
                onClick = { foodType = type.split(" ")[0] },
                label = { Text(type, fontSize = 11.sp) }
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Food Amount", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CatTextPrimary)
            Text("Dry or wet serving weight", fontSize = 11.sp, color = CatTextSecondary)
        }
        CounterControl(
            value = foodAmount,
            unit = "g",
            onIncrement = { foodAmount += 5 },
            onDecrement = { foodAmount = (foodAmount - 5).coerceAtLeast(5) }
        )
    }

    Text("Feeding Time of Day", fontWeight = FontWeight.Bold, color = CatTextPrimary, fontSize = 13.sp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        timesList.forEach { time ->
            val isSelected = timeOfDay == time
            ElevatedFilterChip(
                selected = isSelected,
                onClick = { timeOfDay = time },
                label = { Text(time, fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Divider(color = Color(0xFFEFE8DD), thickness = 1.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Water Intake", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CatTextPrimary)
            Text("Hydration bowl level", fontSize = 11.sp, color = CatTextSecondary)
        }
        CounterControl(
            value = waterAmount,
            unit = "ml",
            onIncrement = { waterAmount += 10 },
            onDecrement = { waterAmount = (waterAmount - 10).coerceAtLeast(10) }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                onLogMetric("DIET", "dry_food", foodAmount.toDouble(), "$foodType - $timeOfDay")
            },
            modifier = Modifier.weight(1f).testTag("log_food_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CatPrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Log Food", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = {
                onLogMetric("DIET", "water", waterAmount.toDouble(), timeOfDay)
            },
            modifier = Modifier.weight(1f).testTag("log_water_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CatSecondary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Log Water", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActivityLoggingPanel(onLogMetric: (type: String, metricName: String, valueDouble: Double, valueString: String) -> Unit) {
    var playtime by remember { mutableStateOf(15) }
    var sleepHours by remember { mutableStateOf(12.0) }
    var selectedEnergy by remember { mutableStateOf("Active") }

    Text("Log Play & Sleep", fontWeight = FontWeight.Bold, color = CatTextPrimary, fontSize = 15.sp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Chasing & Playtime", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CatTextPrimary)
            Text("Lasers, feathers, toys", fontSize = 11.sp, color = CatTextSecondary)
        }
        CounterControl(
            value = playtime,
            unit = "min",
            onIncrement = { playtime += 5 },
            onDecrement = { playtime = (playtime - 5).coerceAtLeast(0) }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Sleep Duration", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CatTextPrimary)
            Text("Cozy naps or night rest", fontSize = 11.sp, color = CatTextSecondary)
        }
        CounterControlDouble(
            value = sleepHours,
            unit = "hrs",
            onIncrement = { sleepHours = (sleepHours + 0.5).coerceAtMost(24.0) },
            onDecrement = { sleepHours = (sleepHours - 0.5).coerceAtLeast(0.0) }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Energy Level Rating", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CatTextSecondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val energies = listOf("Sleepy 😴", "Lazy 😐", "Active ⚡", "Hyper 🚀")
            energies.forEach { energy ->
                val simpleName = energy.split(" ")[0]
                val isSelected = selectedEnergy == simpleName
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedEnergy = simpleName },
                    label = { Text(energy, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Button(
        onClick = {
            if (playtime > 0) onLogMetric("ACTIVITY", "playtime", playtime.toDouble(), selectedEnergy)
            if (sleepHours > 0) onLogMetric("ACTIVITY", "sleep", sleepHours, "")
        },
        modifier = Modifier.fillMaxWidth().testTag("log_activity_button"),
        colors = ButtonDefaults.buttonColors(containerColor = CatPrimary),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Save Activity Log", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthLoggingPanel(
    currentWeight: Double,
    onLogMetric: (type: String, metricName: String, valueDouble: Double, valueString: String) -> Unit
) {
    var weightInput by remember { mutableStateOf(currentWeight.toString()) }
    var tempInput by remember { mutableStateOf("38.5") }
    var selectedMood by remember { mutableStateOf("Happy") }
    var notesInput by remember { mutableStateOf("") }

    Text("Log Daily Mood & Symptoms", fontWeight = FontWeight.Bold, color = CatTextPrimary, fontSize = 15.sp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it },
            label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CatPrimary,
                focusedLabelColor = CatPrimary
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
            value = tempInput,
            onValueChange = { tempInput = it },
            label = { Text("Body Temp (°C)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CatPrimary,
                focusedLabelColor = CatPrimary
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
        )
    }

    Text("Predefined Feline Mood", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CatTextSecondary)
    val moods = listOf("Happy 😊", "Anxious 😰", "Playful ⚡", "Lethargic 😴", "Cheeky 😼", "Unwell 🤒")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        moods.forEach { mood ->
            val simpleMood = mood.split(" ")[0]
            val isSelected = selectedMood == simpleMood
            FilterChip(
                selected = isSelected,
                onClick = { selectedMood = simpleMood },
                label = { Text(mood, fontSize = 11.sp) }
            )
        }
    }

    OutlinedTextField(
        value = notesInput,
        onValueChange = { notesInput = it },
        label = { Text("Custom wellness observation notes...") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CatPrimary,
            focusedLabelColor = CatPrimary
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        maxLines = 3
    )

    Button(
        onClick = {
            onLogMetric("HEALTH", "mood", 0.0, selectedMood)
            
            val weight = weightInput.toDoubleOrNull()
            if (weight != null) {
                onLogMetric("HEALTH", "weight", weight, "")
            }
            
            val temp = tempInput.toDoubleOrNull()
            if (temp != null) {
                onLogMetric("HEALTH", "temp", temp, "")
            }
            
            if (notesInput.isNotBlank()) {
                onLogMetric("HEALTH", "notes", 0.0, notesInput.trim())
            }
            
            notesInput = ""
        },
        modifier = Modifier.fillMaxWidth().testTag("log_health_button"),
        colors = ButtonDefaults.buttonColors(containerColor = CatPrimary),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Save Health Log", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun RemindersAndVetPanel(
    reminders: List<Reminder>,
    onAddReminderClick: () -> Unit,
    onToggleCompleted: (Reminder) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
    onTestNotification: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Vet Reminders & Schedules", fontWeight = FontWeight.Bold, color = CatTextPrimary, fontSize = 16.sp)
                Text("Local notifications of check-ups", fontSize = 11.sp, color = CatTextSecondary)
            }
            Button(
                onClick = onAddReminderClick,
                colors = ButtonDefaults.buttonColors(containerColor = CatPrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Schedule", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = onTestNotification,
            colors = ButtonDefaults.buttonColors(containerColor = CatSecondary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Test Reminder Notification 🔔", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Divider(color = Color(0xFFEFE8DD), thickness = 1.dp)

        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📅", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("No veterinary checkups scheduled yet.", fontSize = 12.sp, color = CatTextSecondary)
                    Text("Click 'Schedule' above to log appointments!", fontSize = 10.sp, color = CatTextSecondary)
                }
            }
        } else {
            reminders.forEach { reminder ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (reminder.isCompleted) Color(0xFFEFE8DD) else Color(0xFFFFECE6).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEFE8DD))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = reminder.isCompleted,
                                onCheckedChange = { onToggleCompleted(reminder) },
                                colors = CheckboxDefaults.colors(checkedColor = CatPrimary)
                            )
                            Column {
                                Text(
                                    text = reminder.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = CatTextPrimary,
                                    style = if (reminder.isCompleted) MaterialTheme.typography.bodySmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else LocalTextStyle.current
                                )
                                Text(
                                    text = "Date: ${reminder.dueDateString} • ${reminder.type}",
                                    fontSize = 11.sp,
                                    color = CatTextSecondary
                                )
                                if (reminder.notes.isNotEmpty()) {
                                    Text(
                                        text = "Notes: ${reminder.notes}",
                                        fontSize = 11.sp,
                                        color = CatTextSecondary,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { onDeleteReminder(reminder) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Delete appointment", tint = CatTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CounterControl(
    value: Int,
    unit: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onDecrement,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFE8DD)),
        ) {
            Text("–", fontWeight = FontWeight.Bold, color = CatTextPrimary)
        }

        Text(
            text = "$value $unit",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = CatTextPrimary,
            modifier = Modifier.widthIn(min = 55.dp),
            textAlign = TextAlign.Center
        )

        IconButton(
            onClick = onIncrement,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFE8DD)),
        ) {
            Text("+", fontWeight = FontWeight.Bold, color = CatTextPrimary)
        }
    }
}

@Composable
fun CounterControlDouble(
    value: Double,
    unit: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onDecrement,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFE8DD)),
        ) {
            Text("–", fontWeight = FontWeight.Bold, color = CatTextPrimary)
        }

        Text(
            text = "$value $unit",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = CatTextPrimary,
            modifier = Modifier.widthIn(min = 55.dp),
            textAlign = TextAlign.Center
        )

        IconButton(
            onClick = onIncrement,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFE8DD)),
        ) {
            Text("+", fontWeight = FontWeight.Bold, color = CatTextPrimary)
        }
    }
}

@Composable
fun LogHistoryItem(log: TrackingLog, onDelete: () -> Unit) {
    val icon = when (log.metricName) {
        "dry_food" -> "🍖"
        "wet_food" -> "🥫"
        "water" -> "💧"
        "playtime" -> "🧶"
        "sleep" -> "😴"
        "weight" -> "⚖️"
        "temp" -> "🌡️"
        "mood" -> "🐱"
        "notes" -> "📝"
        else -> "🐾"
    }

    val title = when (log.metricName) {
        "dry_food" -> "Food Portion Logged"
        "wet_food" -> "Wet Food Served"
        "water" -> "Water Consumed"
        "playtime" -> "Playtime Duration"
        "sleep" -> "Sleep Logged"
        "weight" -> "Weight Updated"
        "temp" -> "Body Temp Record"
        "mood" -> "Mood Status"
        "notes" -> "Health Observation Note"
        else -> log.metricName.replace("_", " ").capitalize()
    }

    val valueUnit = when (log.metricName) {
        "dry_food", "wet_food" -> "${log.valueDouble}g"
        "water" -> "${log.valueDouble}ml"
        "playtime" -> "${log.valueDouble} mins"
        "sleep" -> "${log.valueDouble} hours"
        "weight" -> "${log.valueDouble}kg"
        "temp" -> "${log.valueDouble}°C"
        else -> ""
    }

    val formatTime = try {
        val instant = java.time.Instant.ofEpochMilli(log.timestamp)
        val zdt = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
        zdt.format(DateTimeFormatter.ofPattern("h:mm a"))
    } catch (e: Exception) {
        ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CatSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFEFE8DD))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFE8DD)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 20.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CatTextPrimary)
                        if (formatTime.isNotEmpty()) {
                            Text("•", fontSize = 10.sp, color = CatTextSecondary)
                            Text(formatTime, fontSize = 10.sp, color = CatTextSecondary)
                        }
                    }

                    val details = buildString {
                        if (valueUnit.isNotEmpty()) append(valueUnit)
                        if (log.valueString.isNotEmpty()) {
                            if (isNotEmpty()) append(" ")
                            append("(${log.valueString})")
                        }
                    }
                    if (details.isNotEmpty()) {
                        Text(details, fontSize = 12.sp, color = CatTextSecondary)
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Delete log",
                    tint = CatTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// --- SMART AI ADVISOR CARD ---

@Composable
fun SmartAiAdvisorCard(
    adviceState: AiAdviceState,
    cat: Cat,
    logs: List<TrackingLog>,
    onGenerateAdvice: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFECE6),
                            Color(0xFFFAF4EB)
                        )
                    )
                )
                .border(BorderStroke(1.dp, CatPrimary.copy(alpha = 0.3f)), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔮", fontSize = 24.sp)
                    Column {
                        Text(
                            "Smart Feline Advisor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = CatTextPrimary
                        )
                        Text(
                            "AI-Powered Health Analysis",
                            fontSize = 11.sp,
                            color = CatTextSecondary
                        )
                    }
                }

                if (adviceState is AiAdviceState.Success) {
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(adviceState.advice)) },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = CatPrimary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Insights")
                    }
                }
            }

            Divider(color = CatPrimary.copy(alpha = 0.15f), thickness = 1.dp)

            when (adviceState) {
                is AiAdviceState.Idle -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Ready to compile your daily wellness briefing. I will check food portions, activity ratios, and behavioral logs to deliver vet recommendations.",
                            fontSize = 13.sp,
                            color = CatTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = onGenerateAdvice,
                            colors = ButtonDefaults.buttonColors(containerColor = CatPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze Cat's Day ✨", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is AiAdviceState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = CatPrimary, strokeWidth = 3.dp)
                        Text(
                            "Reading nutrition logs, behavioral indicators & weight charts...",
                            color = CatTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is AiAdviceState.Success -> {
                    Text(
                        text = adviceState.advice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CatTextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                is AiAdviceState.Error -> {
                    Text(
                        text = "Failed to load advice: ${adviceState.message}",
                        color = Color(0xFFC24C38),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is AiAdviceState.MissingKey -> {
                    Text(
                        text = "AI Advisor requires a valid Gemini API Key. Please configure it in your Secrets panel under the 'GEMINI_API_KEY' identifier.",
                        color = CatTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// --- DIALOGS (MODALS) ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCatDialog(
    onDismiss: () -> Unit,
    onAddCat: (name: String, breed: String, age: Int, weight: Double, avatar: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("Domestic Shorthair") }
    var ageYearsInput by remember { mutableStateOf("1") }
    var ageMonthsInput by remember { mutableStateOf("0") }
    var weightInput by remember { mutableStateOf("4.2") }
    var selectedAvatar by remember { mutableStateOf("ic_avatar_ginger") }

    val avatarsList = listOf(
        "ic_avatar_ginger" to "Ginger 🐈",
        "ic_avatar_siamese" to "Siamese 🐱",
        "ic_avatar_grey" to "Grey Shorthair 🐺",
        "🐱" to "Cat Emoji 🐱",
        "🐈" to "Orange Emoji 🐈",
        "🦁" to "Lion Emoji 🦁"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CatBackground),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            border = BorderStroke(1.dp, Color(0xFFE6DBC9))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Cat Profile 🐈",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CatTextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Feline Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CatPrimary,
                        focusedLabelColor = CatPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_cat_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Breed (e.g., Siamese, Persian)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CatPrimary,
                        focusedLabelColor = CatPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ageYearsInput,
                        onValueChange = { ageYearsInput = it },
                        label = { Text("Age (Years)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CatPrimary,
                            focusedLabelColor = CatPrimary
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = ageMonthsInput,
                        onValueChange = { ageMonthsInput = it },
                        label = { Text("Months") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CatPrimary,
                            focusedLabelColor = CatPrimary
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Initial Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CatPrimary,
                        focusedLabelColor = CatPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Pick Profile Picture / Avatar", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CatTextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    avatarsList.forEach { (avatarCode, label) ->
                        val isSelected = selectedAvatar == avatarCode
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAvatar = avatarCode },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = CatTextSecondary)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val years = ageYearsInput.toIntOrNull() ?: 1
                                val months = ageMonthsInput.toIntOrNull() ?: 0
                                val totalMonths = (years * 12) + months
                                val weight = weightInput.toDoubleOrNull() ?: 4.0
                                onAddCat(name.trim(), breed.trim(), totalMonths, weight, selectedAvatar)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CatPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("save_cat_button")
                    ) {
                        Text("Save Cat", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VetReminderDialog(
    onDismiss: () -> Unit,
    onAddReminder: (title: String, type: String, dueDate: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("Annual Check-up") }
    var type by remember { mutableStateOf("VACCINATION") }
    var checkupDaysInFuture by remember { mutableStateOf(14) } // slider/counter offset to make date calculation clean
    var notes by remember { mutableStateOf("") }

    val selectedDateString = LocalDate.now().plusDays(checkupDaysInFuture.toLong()).toString()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CatBackground),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFFE6DBC9))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Add Vet Schedule 🩺",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CatTextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What is this reminder for?") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CatPrimary,
                        focusedLabelColor = CatPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reminder_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Reminder Category", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CatTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val categories = listOf("VACCINATION" to "💉 Vaccine", "CHECK_UP" to "🩺 Vet Visit", "OTHER" to "💊 Other")
                    categories.forEach { (catType, label) ->
                        val isSelected = type == catType
                        FilterChip(
                            selected = isSelected,
                            onClick = { type = catType },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Schedule Date", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CatTextPrimary)
                        Text("In $checkupDaysInFuture days ($selectedDateString)", fontSize = 11.sp, color = CatTextSecondary)
                    }
                    CounterControl(
                        value = checkupDaysInFuture,
                        unit = "days",
                        onIncrement = { checkupDaysInFuture += 1 },
                        onDecrement = { checkupDaysInFuture = (checkupDaysInFuture - 1).coerceAtLeast(0) }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Specific instructions/notes (optional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CatPrimary,
                        focusedLabelColor = CatPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = CatTextSecondary)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onAddReminder(title.trim(), type, selectedDateString, notes.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CatPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("save_reminder_button")
                    ) {
                        Text("Add Schedule", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- HELPER FUNCTION FOR IMMUTABLE DELEGATE CREATION ---
@Composable
fun <T> varOf(initial: T): MutableState<T> = remember { mutableStateOf(initial) }
