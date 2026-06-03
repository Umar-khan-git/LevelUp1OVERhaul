@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MoneyAccountEntity
import com.example.data.TransactionEntity
import com.example.data.CategoryEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// Theme helper colors
val BlueIncome = Color(0xFF29B6F6)
val RedExpense = Color(0xFFEF5350)
val DarkGreyBg = Color(0xFF111111)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceTabScreen(viewModel: DashboardViewModel) {
    var activeSubTab by rememberSaveable { mutableStateOf("trans") } // "trans", "stats", "accounts", "total"
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val accounts by viewModel.moneyAccounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showAddTxSheet by rememberSaveable { mutableStateOf(false) }
    val currentMonthKey = remember {
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }
    var selectedMonthKey by rememberSaveable { mutableStateOf(currentMonthKey) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CanvasBg,
        bottomBar = {
            FinanceSubNavBar(
                selectedTab = activeSubTab,
                onTabSelected = { activeSubTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeSubTab) {
                "trans" -> TransactionsSubScreen(
                    transactions = transactions,
                    onAddTxClick = { showAddTxSheet = true },
                    viewModel = viewModel,
                    selectedMonthKey = selectedMonthKey,
                    onMonthKeyChange = { selectedMonthKey = it }
                )
                "stats" -> StatsSubScreen(
                    transactions = transactions,
                    selectedMonthKey = selectedMonthKey,
                    onMonthKeyChange = { selectedMonthKey = it }
                )
                "accounts" -> AccountsSubScreen(
                    accounts = accounts,
                    viewModel = viewModel
                )
                "total" -> TotalSubScreen(
                    transactions = transactions,
                    accounts = accounts,
                    onAddTxClick = { showAddTxSheet = true },
                    selectedMonthKey = selectedMonthKey,
                    onMonthKeyChange = { selectedMonthKey = it }
                )
            }

            if (showAddTxSheet) {
                AddTransactionDialog(
                    accounts = accounts,
                    categories = categories,
                    viewModel = viewModel,
                    onDismiss = { showAddTxSheet = false },
                    onSave = { type, amount, category, account, toAccount, note, dateStr ->
                        val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(Date())
                        viewModel.addTransaction(
                            type = type,
                            amount = amount,
                            category = category,
                            account = account,
                            toAccount = toAccount,
                            dateString = dateStr,
                            timeString = timeStr,
                            note = note
                        )
                        if (dateStr.length >= 7) {
                            selectedMonthKey = dateStr.substring(0, 7)
                        }
                        showAddTxSheet = false
                    }
                )
            }
        }
    }
}

// ============================================
// FINANCE INTERNAL SUB NAVBAR
// ============================================
@Composable
fun FinanceSubNavBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(60.dp),
        color = Color(0xFF141414),
        border = BorderStroke(1.dp, BorderHighlight)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FinanceSubTabItem(
                id = "trans",
                label = "Trans.",
                isSelected = selectedTab == "trans",
                onClick = { onTabSelected("trans") },
                iconPainter = { color ->
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val h = size.height
                        val w = size.width
                        drawRect(color, size = androidx.compose.ui.geometry.Size(w, h * 0.15f), topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.15f))
                        drawRect(color, size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.15f), topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.45f))
                        drawRect(color, size = androidx.compose.ui.geometry.Size(w, h * 0.15f), topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.75f))
                    }
                }
            )

            FinanceSubTabItem(
                id = "stats",
                label = "Stats",
                isSelected = selectedTab == "stats",
                onClick = { onTabSelected("stats") },
                iconPainter = { color ->
                    Canvas(modifier = Modifier.size(18.dp)) {
                        drawArc(
                            color = color,
                            startAngle = -45f,
                            sweepAngle = 270f,
                            useCenter = true
                        )
                    }
                }
            )

            FinanceSubTabItem(
                id = "accounts",
                label = "Accounts",
                isSelected = selectedTab == "accounts",
                onClick = { onTabSelected("accounts") },
                iconPainter = { color ->
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val h = size.height
                        val w = size.width
                        drawRoundRect(
                            color = color,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.15f),
                            size = androidx.compose.ui.geometry.Size(w, h * 0.7f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawRect(
                            color = color,
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.35f),
                            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.3f)
                        )
                    }
                }
            )

            FinanceSubTabItem(
                id = "total",
                label = "Total",
                isSelected = selectedTab == "total",
                onClick = { onTabSelected("total") },
                iconPainter = { color ->
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val h = size.height
                        val w = size.width
                        drawCircle(color, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.25f))
                        drawCircle(color, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.5f))
                        drawCircle(color, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.75f))
                    }
                }
            )
        }
    }
}

@Composable
fun FinanceSubTabItem(
    id: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    iconPainter: @Composable (Color) -> Unit
) {
    val activeColor = remember { Color(0xFFFD5A4E) } // Peach-Red accent matching Screenshot colors
    val tint = if (isSelected) activeColor else MutedText

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 12.dp)
            .testTag("finance_sub_${id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        iconPainter(tint)
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

// ============================================
// TRANSACTIONS SUB SCREEN
// ============================================
@Composable
fun TransactionsSubScreen(
    transactions: List<TransactionEntity>,
    onAddTxClick: () -> Unit,
    viewModel: DashboardViewModel,
    selectedMonthKey: String,
    onMonthKeyChange: (String) -> Unit
) {
    var transCategoryTab by remember { mutableStateOf("daily") } // "daily", "calendar", "monthly"
    val calendar = Calendar.getInstance()

    Column(modifier = Modifier.fillMaxSize()) {
        // Inner Header tab selections Daily | Calendar | Monthly
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141414))
                .border(BorderStroke(1.dp, BorderHighlight))
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("daily" to "Daily", "calendar" to "Calendar", "monthly" to "Monthly").forEach { (tabId, label) ->
                val activeColors = remember { Color(0xFFFD5A4E) }
                val isSel = transCategoryTab == tabId
                Column(
                    modifier = Modifier
                        .clickable { transCategoryTab = tabId }
                        .padding(vertical = 10.dp)
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        color = if (isSel) Color.White else MutedText,
                        fontSize = 13.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isSel) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(2.dp)
                                .background(activeColors)
                        )
                    }
                }
            }
        }

        // Month Switcher Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141414))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onMonthKeyChange(getPreviousMonthKey(selectedMonthKey)) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Text("◀", color = Color.White, fontSize = 16.sp)
                }
                Text(
                    text = formatYearMonth(selectedMonthKey), 
                    color = Color.White, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onMonthKeyChange(getNextMonthKey(selectedMonthKey)) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Text("▶", color = Color.White, fontSize = 16.sp)
                }
            }
            Text("Monthly Filter", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Statistics bar at top of Transactions: Income, Expenses, Total
        val totalIncome = remember(transactions, selectedMonthKey) {
            transactions.filter { it.type == "INCOME" && it.dateString.startsWith(selectedMonthKey) }.sumOf { it.amount }
        }
        val totalExpense = remember(transactions, selectedMonthKey) {
            transactions.filter { it.type == "EXPENSE" && it.dateString.startsWith(selectedMonthKey) }.sumOf { it.amount }
        }
        val currentBalance = totalIncome - totalExpense

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .border(BorderStroke(1.dp, BorderHighlight))
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Income", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(String.format("DH %,.2f", totalIncome), color = BlueIncome, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Expenses", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(String.format("DH %,.2f", totalExpense), color = RedExpense, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(String.format("DH %,.2f", currentBalance), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (transCategoryTab) {
                "daily" -> {
                    val groupedTx = remember(transactions, selectedMonthKey) {
                        transactions
                            .filter { it.dateString.startsWith(selectedMonthKey) }
                            .groupBy { it.dateString }
                            .toSortedMap(reverseOrder())
                    }

                    if (groupedTx.isEmpty()) {
                        EmptyStatePlaceholder()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            groupedTx.forEach { (dateStr, itemsList) ->
                                val dateIncome = itemsList.filter { it.type == "INCOME" }.sumOf { it.amount }
                                val dateExpense = itemsList.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                                val parsedDate = try {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
                                } catch (e: Exception) {
                                    null
                                }
                                val dayNum = parsedDate?.let { SimpleDateFormat("dd", Locale.US).format(it) } ?: ""
                                val weekday = parsedDate?.let { SimpleDateFormat("EEE", Locale.US).format(it) } ?: ""
                                val monthYearStr = parsedDate?.let { SimpleDateFormat("MM.yyyy", Locale.US).format(it) } ?: ""

                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0F0F0F))
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = dayNum,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                            Column {
                                                Text(
                                                    text = weekday.uppercase(),
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = monthYearStr,
                                                    color = MutedText,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            if (dateIncome > 0) {
                                                Text(
                                                    text = String.format("DH %,.0f", dateIncome),
                                                    color = BlueIncome,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            if (dateExpense > 0) {
                                                Text(
                                                    text = String.format("DH %,.0f", dateExpense),
                                                    color = RedExpense,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = BorderHighlight)
                                }

                                items(itemsList) { tx ->
                                    TransactionRowItem(
                                        tx = tx,
                                        onDelete = { viewModel.deleteTransaction(tx) },
                                        onEdit = { updated -> viewModel.editTransaction(tx, updated) }
                                    )
                                    HorizontalDivider(color = BorderHighlight)
                                }
                            }
                        }
                    }
                }
                "calendar" -> {
                    CalendarViewScreen(transactions = transactions, selectedMonthKey = selectedMonthKey)
                }
                "monthly" -> {
                    MonthlySummaryView(transactions = transactions, selectedMonthKey = selectedMonthKey)
                }
            }

            // High Fidelity Floating Action Button matching Screenshot Red/Orange theme color
            FloatingActionButton(
                onClick = onAddTxClick,
                containerColor = Color(0xFFFD5A4E),
                contentColor = Color.White,
                shape = RoundedCornerShape(100),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(56.dp)
                    .testTag("finance_add_tx_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    tx: TransactionEntity,
    onDelete: () -> Unit,
    onEdit: (TransactionEntity) -> Unit
) {
    var showDeleteAlert by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { showEditDialog = true },
                onLongClick = { showDeleteAlert = true }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circle category symbol
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF222222), shape = RoundedCornerShape(100)),
                contentAlignment = Alignment.Center
            ) {
                val emojiSymbol = when (tx.category.trim().lowercase()) {
                    "food" -> "🍱"
                    "transport" -> "🚌"
                    "wifi" -> "🌐"
                    "salary" -> "💵"
                    "freelance" -> "💻"
                    "baber", "baqer" -> "🥖"
                    else -> "🛒"
                }
                Text(emojiSymbol, fontSize = 16.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = tx.category,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tx.account,
                        color = MutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (tx.note.isNotEmpty()) {
                    Text(
                        text = tx.note,
                        color = MutedText,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Text(
            text = if (tx.type == "EXPENSE") String.format("-DH %,.2f", tx.amount) else String.format("DH %,.2f", tx.amount),
            color = if (tx.type == "EXPENSE") RedExpense else BlueIncome,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }

    if (showEditDialog) {
        EditTransactionDialog(
            tx = tx,
            onDismiss = { showEditDialog = false },
            onSave = { updated -> onEdit(updated); showEditDialog = false }
        )
    }

    if (showDeleteAlert) {
        AlertDialog(
            onDismissRequest = { showDeleteAlert = false },
            title = { Text("Delete Transaction?", color = Color.White) },
            text = { Text("Are you sure you want to delete this ${tx.category} record?", color = Color.Gray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteAlert = false
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAlert = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}

// ============================================
// EDIT TRANSACTION DIALOG
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    tx: TransactionEntity,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    var amount by remember { mutableStateOf(tx.amount.toBigDecimal().stripTrailingZeros().toPlainString()) }
    var category by remember { mutableStateOf(tx.category) }
    var account by remember { mutableStateOf(tx.account) }
    var note by remember { mutableStateOf(tx.note) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A1A),
            border = BorderStroke(1.dp, BorderHighlight),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Edit Transaction",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = tx.type,
                    color = if (tx.type == "EXPENSE") RedExpense else BlueIncome,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                TextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.Gray,
                        focusedContainerColor = Color(0xFF222222),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.Gray,
                        focusedContainerColor = Color(0xFF222222),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("Account") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.Gray,
                        focusedContainerColor = Color(0xFF222222),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.Gray,
                        focusedContainerColor = Color(0xFF222222),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newAmount = amount.toDoubleOrNull() ?: tx.amount
                            if (category.isNotBlank() && account.isNotBlank()) {
                                onSave(
                                    tx.copy(
                                        amount = newAmount,
                                        category = category.trim(),
                                        account = account.trim(),
                                        note = note.trim()
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFD5A4E))
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    }
}

// ============================================
// CALENDAR VIEW COMPONENT
// ============================================
@Composable
fun CalendarViewScreen(
    transactions: List<TransactionEntity>,
    selectedMonthKey: String
) {
    val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val daysInMonth = remember(selectedMonthKey) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = sdf.parse(selectedMonthKey) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        } catch (e: Exception) {
            31
        }
    }

    val startDayOffset = remember(selectedMonthKey) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = sdf.parse(selectedMonthKey) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.get(Calendar.DAY_OF_WEEK) - 1 // 0: Sun, 1: Mon, etc.
        } catch (e: Exception) {
            0
        }
    }

    val daysGrid = remember(daysInMonth, startDayOffset) {
        val list = mutableListOf<String>()
        // Pad offset days
        for (i in 0 until startDayOffset) {
            list.add("")
        }
        for (i in 1..daysInMonth) {
            list.add(i.toString())
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Week Names row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            weekDays.forEach { d ->
                Text(
                    text = d.uppercase(),
                    color = MutedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid contents
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true,
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            items(daysGrid.size) { index ->
                val day = daysGrid[index]
                if (day.isEmpty()) {
                    Box(modifier = Modifier.aspectRatio(1f))
                } else {
                    val dateFormattedStr = String.format(Locale.US, "%s-%02d", selectedMonthKey, day.toInt())
                    val dayTx = transactions.filter { it.dateString == dateFormattedStr }
                    val dayIncome = dayTx.filter { it.type == "INCOME" }.sumOf { it.amount }
                    val dayExpense = dayTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                    val todaySdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
                    val todayDateStr = remember { todaySdf.format(Date()) }
                    val currentDayStr = remember { todayDateStr.substring(8, 10).toInt().toString() }
                    val currentMonthKeyStr = remember { todayDateStr.substring(0, 7) }
                    val isTodayInContext = day == currentDayStr && selectedMonthKey == currentMonthKeyStr

                    Box(
                        modifier = Modifier
                            .aspectRatio(0.8f)
                            .border(BorderStroke(0.5.dp, Color(0x33FFFFFF)))
                            .background(if (isTodayInContext) Color(0xFF222222) else Color.Transparent)
                            .padding(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = day,
                                color = if (isTodayInContext) Color.White else MutedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(2.dp)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                if (dayIncome > 0) {
                                    Text(
                                        text = String.format("%.0f", dayIncome),
                                        color = BlueIncome,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (dayExpense > 0) {
                                    Text(
                                        text = String.format("%.0f", dayExpense),
                                        color = RedExpense,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// MONTHLY VIEW SUMMARY COMPONENT
// ============================================
@Composable
fun MonthlySummaryView(
    transactions: List<TransactionEntity>,
    selectedMonthKey: String
) {
    val monthPart = selectedMonthKey.drop(5) // e.g. "05"
    val yearPart = selectedMonthKey.take(4)

    val daysInMonth = try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = sdf.parse(selectedMonthKey) ?: Date()
        val cal = Calendar.getInstance()
        cal.time = date
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    } catch (e: Exception) {
        31
    }

    val filterMayIncome = remember(transactions, selectedMonthKey) {
        transactions.filter { it.type == "INCOME" && it.dateString.startsWith(selectedMonthKey) }.sumOf { it.amount }
    }
    val filterMayExpense = remember(transactions, selectedMonthKey) {
        transactions.filter { it.type == "EXPENSE" && it.dateString.startsWith(selectedMonthKey) }.sumOf { it.amount }
    }

    val dynamicWeeks = remember(transactions, selectedMonthKey, daysInMonth) {
        val monthTx = transactions.filter { it.dateString.startsWith(selectedMonthKey) }
        val ranges = listOf(
            1..7,
            8..14,
            15..21,
            22..28,
            29..daysInMonth
        )
        ranges.mapNotNull { range ->
            if (range.first > daysInMonth) null else {
                val label = String.format("%02d.%s ~ %02d.%s", range.first, monthPart, range.last.coerceAtMost(daysInMonth), monthPart)
                val weekTx = monthTx.filter {
                    val day = it.dateString.substringAfterLast("-").toIntOrNull() ?: 0
                    day in range
                }
                val inc = weekTx.filter { it.type == "INCOME" }.sumOf { it.amount }
                val exp = weekTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                Triple(label, inc, exp)
            }
        }.reversed()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Month group summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                border = BorderStroke(1.dp, BorderHighlight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${formatYearMonth(selectedMonthKey)} Summary", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Income", color = MutedText, fontSize = 12.sp)
                        Text(String.format("DH %,.2f", filterMayIncome), color = BlueIncome, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Expenses", color = MutedText, fontSize = 12.sp)
                        Text(String.format("DH %,.2f", filterMayExpense), color = RedExpense, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = BorderHighlight, modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Savings", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(String.format("DH %,.2f", filterMayIncome - filterMayExpense), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Weekly Breakdowns", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
        }

        items(dynamicWeeks) { (weekRange, inc, exp) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LayerCard, shape = RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, BorderHighlight), shape = RoundedCornerShape(8.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(weekRange, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(String.format("In: DH %,.0f", inc), color = BlueIncome, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("Out: DH %,.0f", exp), color = RedExpense, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================
// STATS / ANALYTICS CHART SCREEN
// ============================================
@Composable
fun StatsSubScreen(
    transactions: List<TransactionEntity>,
    selectedMonthKey: String,
    onMonthKeyChange: (String) -> Unit
) {
    var statsType by remember { mutableStateOf("EXPENSE") } // "EXPENSE", "INCOME"

    val filteredTx = remember(transactions, statsType, selectedMonthKey) {
        transactions.filter { it.type == statsType && it.dateString.startsWith(selectedMonthKey) }
    }

    val totalAmount = remember(filteredTx) {
        filteredTx.sumOf { it.amount }
    }

    // Group transactions by category
    val categoryTotals = remember(filteredTx) {
        filteredTx.groupBy { it.category }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
    }

    // Visual Palette supporting vibrant pieces for the donut arcs
    val colorPalette = listOf(
        Color(0xFFFD5A4E), // Coral Red
        Color(0xFF29B6F6), // Sky Blue
        Color(0xFFFFC312), // Yellow
        Color(0xFF90FF90), // Soft Green
        Color(0xFFFF85FF), // Pastel Pink
        Color(0xFF12CBC4), // Cyan Blue
        Color(0xFFD980FA), // Lavender
        Color(0xFFA3CB38), // Lime
        Color(0xFFF79F1F), // Orange
        Color(0xFF9980FA)  // Violet
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Browser Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Backward switcher
                IconButton(
                    onClick = { onMonthKeyChange(getPreviousMonthKey(selectedMonthKey)) }
                ) {
                    Text(
                        text = "◀",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                // Header Month Name
                Text(
                    text = formatYearMonth(selectedMonthKey),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Forward switcher
                IconButton(
                    onClick = { onMonthKeyChange(getNextMonthKey(selectedMonthKey)) }
                ) {
                    Text(
                        text = "▶",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Segmented Tab bar: EXPENSES vs INCOME Breakdown
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161616), shape = RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("EXPENSE" to "Expenses", "INCOME" to "Income").forEach { (typeKey, label) ->
                    val isSel = statsType == typeKey
                    val activeBg = if (isSel) {
                        if (typeKey == "EXPENSE") Color(0xFFEF5350) else Color(0xFF29B6F6)
                    } else {
                        Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(activeBg, shape = RoundedCornerShape(6.dp))
                            .clickable { statsType = typeKey }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label.uppercase(Locale.getDefault()),
                            color = if (isSel) Color.White else MutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        if (totalAmount == 0.0) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmptyStatePlaceholder()
                }
            }
        } else {
            // Pie Chart with labels and lines
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LayerCard),
                    border = BorderStroke(1.dp, BorderHighlight),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (statsType == "EXPENSE") "EXPENSES BREAKDOWN" else "INCOME BREAKDOWN",
                            color = MutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = String.format("DH %,.2f", totalAmount),
                            color = if (statsType == "EXPENSE") RedExpense else BlueIncome,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val density = androidx.compose.ui.platform.LocalDensity.current
                        val textPaint = remember {
                            android.graphics.Paint().apply {
                                isAntiAlias = true
                                textAlign = android.graphics.Paint.Align.LEFT
                            }
                        }

                        val mainSlices = categoryTotals

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                        ) {
                            val canvasW = size.width
                            val canvasH = size.height
                            val pieRadius = minOf(canvasW, canvasH) * 0.24f
                            val centerX = canvasW / 2f
                            val centerY = canvasH / 2f
                            val labelFontSize = with(density) { 8.5.sp.toPx() }
                            val minLabelGap = labelFontSize * 1.55f

                            val n = mainSlices.size
                            val startAngles = FloatArray(n)
                            val sweepAngles = FloatArray(n)
                            val midRads = DoubleArray(n)
                            val pcts = DoubleArray(n)

                            // Pass 1: compute geometry for every slice
                            var currentAngle = -90f
                            mainSlices.forEachIndexed { idx, (_, amnt) ->
                                val sweep = if (totalAmount > 0.0) ((amnt / totalAmount) * 360f).toFloat() else 0f
                                startAngles[idx] = currentAngle
                                sweepAngles[idx] = sweep
                                midRads[idx] = Math.toRadians((currentAngle + sweep / 2f).toDouble())
                                pcts[idx] = if (totalAmount > 0.0) (amnt / totalAmount) * 100.0 else 0.0
                                currentAngle += sweep
                            }

                            // Pass 2: draw all arcs
                            mainSlices.forEachIndexed { idx, _ ->
                                val col = colorPalette.getOrElse(idx) { Color.Gray }
                                drawArc(
                                    color = col,
                                    startAngle = startAngles[idx],
                                    sweepAngle = sweepAngles[idx],
                                    useCenter = true,
                                    topLeft = androidx.compose.ui.geometry.Offset(centerX - pieRadius, centerY - pieRadius),
                                    size = androidx.compose.ui.geometry.Size(pieRadius * 2, pieRadius * 2)
                                )
                                drawArc(
                                    color = Color(0xFF111111),
                                    startAngle = startAngles[idx],
                                    sweepAngle = sweepAngles[idx],
                                    useCenter = true,
                                    topLeft = androidx.compose.ui.geometry.Offset(centerX - pieRadius, centerY - pieRadius),
                                    size = androidx.compose.ui.geometry.Size(pieRadius * 2, pieRadius * 2),
                                    style = Stroke(width = 1.5f)
                                )
                            }

                            // Split indices by side, sorted by natural Y so stacking is top-to-bottom
                            val rightIdx = (0 until n).filter { cos(midRads[it]) >= 0 }
                                .sortedBy { sin(midRads[it]) }
                            val leftIdx = (0 until n).filter { cos(midRads[it]) < 0 }
                                .sortedBy { sin(midRads[it]) }

                            // Push labels apart until no two are closer than minLabelGap
                            fun resolveY(indices: List<Int>): MutableList<Float> {
                                val ys = indices.map {
                                    (centerY + (pieRadius + 18f) * sin(midRads[it])).toFloat()
                                }.toMutableList()
                                repeat(50) {
                                    for (i in 1 until ys.size) {
                                        if (ys[i] - ys[i - 1] < minLabelGap) {
                                            val mid = (ys[i] + ys[i - 1]) / 2f
                                            ys[i - 1] = mid - minLabelGap / 2f
                                            ys[i] = mid + minLabelGap / 2f
                                        }
                                    }
                                    for (i in ys.size - 2 downTo 0) {
                                        if (ys[i + 1] - ys[i] < minLabelGap) {
                                            val mid = (ys[i + 1] + ys[i]) / 2f
                                            ys[i] = mid - minLabelGap / 2f
                                            ys[i + 1] = mid + minLabelGap / 2f
                                        }
                                    }
                                }
                                return ys
                            }

                            val rightYs = resolveY(rightIdx)
                            val leftYs  = resolveY(leftIdx)

                            // Pass 3: draw leader lines + labels
                            val rightAnchorX = centerX + pieRadius + 28f
                            rightIdx.forEachIndexed { i, idx ->
                                val col = colorPalette.getOrElse(idx) { Color.Gray }
                                val labelY = rightYs[i]
                                val edgeX = centerX + pieRadius * cos(midRads[idx]).toFloat()
                                val edgeY = centerY + pieRadius * sin(midRads[idx]).toFloat()
                                val kinkX = centerX + (pieRadius + 14f) * cos(midRads[idx]).toFloat()
                                val kinkY = centerY + (pieRadius + 14f) * sin(midRads[idx]).toFloat()

                                drawLine(col.copy(alpha = 0.7f),
                                    start = androidx.compose.ui.geometry.Offset(edgeX, edgeY),
                                    end = androidx.compose.ui.geometry.Offset(kinkX, kinkY), strokeWidth = 1f)
                                drawLine(col.copy(alpha = 0.7f),
                                    start = androidx.compose.ui.geometry.Offset(kinkX, kinkY),
                                    end = androidx.compose.ui.geometry.Offset(rightAnchorX, labelY), strokeWidth = 1f)
                                drawLine(col.copy(alpha = 0.7f),
                                    start = androidx.compose.ui.geometry.Offset(rightAnchorX, labelY),
                                    end = androidx.compose.ui.geometry.Offset(rightAnchorX + 10f, labelY), strokeWidth = 1f)

                                val text = "${mainSlices[idx].first.take(11)} ${String.format("%.1f", pcts[idx])}%"
                                textPaint.textSize = labelFontSize
                                textPaint.textAlign = android.graphics.Paint.Align.LEFT
                                textPaint.color = android.graphics.Color.argb(255,
                                    (col.red * 255).toInt(), (col.green * 255).toInt(), (col.blue * 255).toInt())
                                drawIntoCanvas { it.nativeCanvas.drawText(text, rightAnchorX + 12f, labelY + labelFontSize / 3f, textPaint) }
                            }

                            val leftAnchorX = centerX - pieRadius - 28f
                            leftIdx.forEachIndexed { i, idx ->
                                val col = colorPalette.getOrElse(idx) { Color.Gray }
                                val labelY = leftYs[i]
                                val edgeX = centerX + pieRadius * cos(midRads[idx]).toFloat()
                                val edgeY = centerY + pieRadius * sin(midRads[idx]).toFloat()
                                val kinkX = centerX + (pieRadius + 14f) * cos(midRads[idx]).toFloat()
                                val kinkY = centerY + (pieRadius + 14f) * sin(midRads[idx]).toFloat()

                                drawLine(col.copy(alpha = 0.7f),
                                    start = androidx.compose.ui.geometry.Offset(edgeX, edgeY),
                                    end = androidx.compose.ui.geometry.Offset(kinkX, kinkY), strokeWidth = 1f)
                                drawLine(col.copy(alpha = 0.7f),
                                    start = androidx.compose.ui.geometry.Offset(kinkX, kinkY),
                                    end = androidx.compose.ui.geometry.Offset(leftAnchorX, labelY), strokeWidth = 1f)
                                drawLine(col.copy(alpha = 0.7f),
                                    start = androidx.compose.ui.geometry.Offset(leftAnchorX, labelY),
                                    end = androidx.compose.ui.geometry.Offset(leftAnchorX - 10f, labelY), strokeWidth = 1f)

                                val text = "${mainSlices[idx].first.take(11)} ${String.format("%.1f", pcts[idx])}%"
                                textPaint.textSize = labelFontSize
                                textPaint.textAlign = android.graphics.Paint.Align.RIGHT
                                textPaint.color = android.graphics.Color.argb(255,
                                    (col.red * 255).toInt(), (col.green * 255).toInt(), (col.blue * 255).toInt())
                                drawIntoCanvas { it.nativeCanvas.drawText(text, leftAnchorX - 12f, labelY + labelFontSize / 3f, textPaint) }
                                textPaint.textAlign = android.graphics.Paint.Align.LEFT
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (statsType == "EXPENSE") "Expense Category Slices" else "Income Category Slices",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Beautiful slice list mimicking Screenshot 3 layout with circular colored progress pills
            items(categoryTotals.size) { index ->
                val (cat, amnt) = categoryTotals[index]
                val pct = if (totalAmount > 0.0) (amnt / totalAmount) * 100.0 else 0.0
                val activeColor = colorPalette.getOrElse(index) { Color.Gray }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LayerCard, shape = RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, BorderHighlight), shape = RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Colored progress square pills with percent
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(activeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                                .border(BorderStroke(1.dp, activeColor.copy(alpha = 0.3f)), shape = RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format("%.0f%%", pct),
                                color = activeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = cat,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = String.format("DH %,.2f", amnt),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Utility to format YearMonth string YYYY-MM into readable label
fun formatYearMonth(yearMonth: String): String {
    return try {
        val sdfIn = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = sdfIn.parse(yearMonth) ?: return yearMonth
        val sdfOut = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdfOut.format(date)
    } catch (e: Exception) {
        yearMonth
    }
}

fun getPreviousMonthKey(yearMonth: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = sdf.parse(yearMonth) ?: return sdf.format(Date())
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.MONTH, -1)
        sdf.format(cal.time)
    } catch (e: Exception) {
        yearMonth
    }
}

fun getNextMonthKey(yearMonth: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = sdf.parse(yearMonth) ?: return sdf.format(Date())
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.MONTH, 1)
        sdf.format(cal.time)
    } catch (e: Exception) {
        yearMonth
    }
}

// ============================================
// ACCOUNTS SUB SCREEN
// ============================================
@Composable
fun AccountsSubScreen(
    accounts: List<MoneyAccountEntity>,
    viewModel: DashboardViewModel
) {
    var showAddAccountDialog by remember { mutableStateOf(false) }

    val totalAssets = remember(accounts) {
        accounts.filter { it.type != "CARD" }.sumOf { it.balance }
    }
    val totalLiabilities = remember(accounts) {
        accounts.filter { it.type == "CARD" }.sumOf { it.balance }
    }
    val netWorth = totalAssets - totalLiabilities

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Upper aggregate summary table (Screenshot 2 style)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                border = BorderStroke(1.dp, BorderHighlight),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Assets", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(String.format("%,.2f", totalAssets), color = BlueIncome, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Liabilities", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(String.format("%,.2f", totalLiabilities), color = RedExpense, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(String.format("%,.2f", netWorth), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Subheaders CASH accounts
        val cashAccounts = accounts.filter { it.type == "CASH" }
        if (cashAccounts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cash", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("DH %,.2f", cashAccounts.sumOf { it.balance }), color = BlueIncome, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            items(cashAccounts) { acc ->
                AccountItemRow(
                    acc = acc,
                    onDelete = { viewModel.deleteMoneyAccount(acc.id) },
                    onEdit = { updated -> viewModel.editMoneyAccount(updated) }
                )
                HorizontalDivider(color = BorderHighlight)
            }
        }

        // Subheaders BANK accounts
        val bankAccounts = accounts.filter { it.type == "BANK" }
        if (bankAccounts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Accounts", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("DH %,.2f", bankAccounts.sumOf { it.balance }), color = BlueIncome, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            items(bankAccounts) { acc ->
                AccountItemRow(
                    acc = acc,
                    onDelete = { viewModel.deleteMoneyAccount(acc.id) },
                    onEdit = { updated -> viewModel.editMoneyAccount(updated) }
                )
                HorizontalDivider(color = BorderHighlight)
            }
        }

        // Subheaders CARD accounts
        val cardAccounts = accounts.filter { it.type == "CARD" }
        if (cardAccounts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Card", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("DH %,.2f", cardAccounts.sumOf { it.balance }), color = RedExpense, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            items(cardAccounts) { acc ->
                AccountItemRow(
                    acc = acc,
                    onDelete = { viewModel.deleteMoneyAccount(acc.id) },
                    onEdit = { updated -> viewModel.editMoneyAccount(updated) }
                )
                HorizontalDivider(color = BorderHighlight)
            }
        }

        item {
            Button(
                onClick = { showAddAccountDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add New Account", color = Color.White, fontSize = 13.sp)
            }
        }
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onAdd = { name, type, initBal ->
                viewModel.addMoneyAccount(name, type, initBal)
                showAddAccountDialog = false
            }
        )
    }
}

@Composable
fun AccountItemRow(
    acc: MoneyAccountEntity,
    onDelete: () -> Unit,
    onEdit: (MoneyAccountEntity) -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { showEdit = true },
                onLongClick = { showDelete = true }
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(acc.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(acc.type, color = MutedText, fontSize = 10.sp)
        }
        Text(
            text = String.format("DH %,.2f", acc.balance),
            color = if (acc.type == "CARD") RedExpense else BlueIncome,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }

    if (showEdit) {
        EditAccountDialog(
            acc = acc,
            onDismiss = { showEdit = false },
            onSave = { updated -> onEdit(updated); showEdit = false }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete Account?", color = Color.White) },
            text = { Text("Remove \"${acc.name}\" from your accounts list?", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDelete = false }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
}

// ============================================
// EDIT ACCOUNT DIALOG COMPONENT
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountDialog(
    acc: MoneyAccountEntity,
    onDismiss: () -> Unit,
    onSave: (MoneyAccountEntity) -> Unit
) {
    var name by remember { mutableStateOf(acc.name) }
    var selectedType by remember { mutableStateOf(acc.type) }
    var balanceStr by remember { mutableStateOf(acc.balance.toBigDecimal().stripTrailingZeros().toPlainString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkGreyBg,
            border = BorderStroke(1.dp, BorderHighlight),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Edit Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.Gray,
                        focusedContainerColor = Color(0xFF222222),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CASH" to "Cash", "BANK" to "Bank", "CARD" to "Card").forEach { (typeVal, label) ->
                        val isSel = selectedType == typeVal
                        Button(
                            onClick = { selectedType = typeVal },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) Color(0xFFFD5A4E) else Color(0xFF222222)
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                TextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Balance") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.Gray,
                        focusedContainerColor = Color(0xFF222222),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val bal = balanceStr.toDoubleOrNull() ?: acc.balance
                            if (name.isNotBlank()) {
                                onSave(acc.copy(name = name.trim(), type = selectedType, balance = bal))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFD5A4E))
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    }
}

// ============================================
// ADD ACCOUNT DIALOG COMPONENT
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("CASH") } // "CASH", "BANK", "CARD"
    var balanceStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkGreyBg,
            border = BorderStroke(1.dp, BorderHighlight),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add New Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.Gray,
                        focusedContainerColor = Color(0xFF222222),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CASH" to "Cash", "BANK" to "Bank", "CARD" to "Card").forEach { (typeVal, label) ->
                        val isSel = selectedType == typeVal
                        Button(
                            onClick = { selectedType = typeVal },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) Color(0xFFFD5A4E) else Color(0xFF222222)
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                TextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Initial Balance") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF222222),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val bal = balanceStr.toDoubleOrNull() ?: 0.0
                            if (name.isNotEmpty()) {
                                onAdd(name, selectedType, bal)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFD5A4E))
                    ) {
                        Text("Add", color = Color.White)
                    }
                }
            }
        }
    }
}

// ============================================
// TOTALS & EXTRA BUDGET SUB SCREEN
// ============================================
@Composable
fun TotalSubScreen(
    transactions: List<TransactionEntity>,
    accounts: List<MoneyAccountEntity>,
    onAddTxClick: () -> Unit,
    selectedMonthKey: String,
    onMonthKeyChange: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE) }
    var monthlyBudget by remember(selectedMonthKey) { 
        mutableStateOf(prefs.getFloat("monthly_budget_$selectedMonthKey", 10000f)) 
    }
    var showBudgetDialog by remember { mutableStateOf(false) }

    val totalIncome = remember(transactions, selectedMonthKey) {
        transactions.filter { it.type == "INCOME" && it.dateString.startsWith(selectedMonthKey) }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions, selectedMonthKey) {
        transactions.filter { it.type == "EXPENSE" && it.dateString.startsWith(selectedMonthKey) }.sumOf { it.amount }
    }
    val totalValue = totalIncome - totalExpense

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Header mimicking screenshot 4
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onMonthKeyChange(getPreviousMonthKey(selectedMonthKey)) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Text("◀", color = Color.White, fontSize = 16.sp)
                    }
                    Text(
                        text = formatYearMonth(selectedMonthKey), 
                        color = Color.White, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { onMonthKeyChange(getNextMonthKey(selectedMonthKey)) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Text("▶", color = Color.White, fontSize = 16.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Star, contentDescription = "Star", tint = MutedText, modifier = Modifier.size(20.dp))
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MutedText, modifier = Modifier.size(20.dp))
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MutedText, modifier = Modifier.size(20.dp))
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                border = BorderStroke(1.dp, BorderHighlight),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Income", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(String.format("%,.2f", totalIncome), color = BlueIncome, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Expenses", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(String.format("%,.2f", totalExpense), color = RedExpense, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(String.format("%,.2f", totalValue), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Budget settings selector Row
        item {
            val percentSpent = if (monthlyBudget > 0) (totalExpense / monthlyBudget) * 100 else 0.0
            val budgetStatus = if (totalExpense > monthlyBudget) "Over budget!" else String.format("%.1f%% spent", percentSpent)

            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                border = BorderStroke(1.dp, BorderHighlight),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBudgetDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Check, contentDescription = "Budget", tint = if (totalExpense > monthlyBudget) Color(0xFFEF5350) else Color(0xFFFD5A4E), modifier = Modifier.size(18.dp))
                        Column {
                            Text("Budget", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format("Spent: DH %,.2f / DH %,.2f (%s)", totalExpense, monthlyBudget.toDouble(), budgetStatus),
                                color = if (totalExpense > monthlyBudget) Color(0xFFEF5350) else MutedText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text("Budget Setting >", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Accounts list parameters screenshot 4 box
        item {
            val yearPart = selectedMonthKey.take(4)
            val monthPart = selectedMonthKey.drop(5)
            val daysInMonth = try {
                val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
                val date = sdf.parse(selectedMonthKey) ?: Date()
                val cal = Calendar.getInstance()
                cal.time = date
                cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            } catch (e: Exception) {
                31
            }

            // Dynamically resolve Cash / Bank vs Card expenses
            val cashBankAccounts = accounts.filter { it.type.trim().uppercase() in listOf("CASH", "BANK") }.map { it.name.trim().lowercase() }
            val cardAccounts = accounts.filter { it.type.trim().uppercase() == "CARD" }.map { it.name.trim().lowercase() }

            val cashExpenses = transactions.filter {
                it.type == "EXPENSE" &&
                it.dateString.startsWith(selectedMonthKey) &&
                (cashBankAccounts.contains(it.account.trim().lowercase()) || cardAccounts.isEmpty() || !cardAccounts.contains(it.account.trim().lowercase()))
            }.sumOf { it.amount }

            val cardExpenses2 = transactions.filter {
                it.type == "EXPENSE" &&
                it.dateString.startsWith(selectedMonthKey) &&
                cardAccounts.contains(it.account.trim().lowercase())
            }.sumOf { it.amount }

            val transferTotal = transactions.filter {
                it.type == "TRANSFER" &&
                it.dateString.startsWith(selectedMonthKey)
            }.sumOf { it.amount }

            val previousMonthKey = getPreviousMonthKey(selectedMonthKey)
            val prevExpense = transactions.filter { it.type == "EXPENSE" && it.dateString.startsWith(previousMonthKey) }.sumOf { it.amount }
            val currentExpense = transactions.filter { it.type == "EXPENSE" && it.dateString.startsWith(selectedMonthKey) }.sumOf { it.amount }
            
            val comparedPercentStr = if (prevExpense > 0) {
                String.format("%.0f%%", (currentExpense / prevExpense) * 100.0)
            } else if (currentExpense > 0) {
                "100%"
            } else {
                "0%"
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                border = BorderStroke(1.dp, BorderHighlight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Menu, contentDescription = "Accounts Overview", tint = BlueIncome, modifier = Modifier.size(18.dp))
                            Text("Accounts", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = String.format("01.%s.%s ~ %02d.%s", monthPart, yearPart, daysInMonth, monthPart),
                            color = MutedText,
                            fontSize = 11.sp
                        )
                    }
                    HorizontalDivider(color = BorderHighlight)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Compared Expenses (Last month)", color = MutedText, fontSize = 12.sp)
                        Text(comparedPercentStr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Expenses (Cash, Accounts)", color = MutedText, fontSize = 12.sp)
                        Text(String.format("DH %,.2f", cashExpenses), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Expenses (Card)", color = MutedText, fontSize = 12.sp)
                        Text(String.format("DH %,.2f", cardExpenses2), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Transfer (Cash, Accounts → ... )", color = MutedText, fontSize = 12.sp)
                        Text(String.format("DH %,.2f", transferTotal), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Export data button
        item {
            val context = LocalContext.current
            Button(
                onClick = {
                    ToastHelper.showToast(context, "Exporting finance data to Excel...")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Excel icon", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export data to Excel", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showBudgetDialog) {
        var budgetText by remember { mutableStateOf(if (monthlyBudget > 0) monthlyBudget.toInt().toString() else "") }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Monthly Budget Goal", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your budget limit for ${formatYearMonth(selectedMonthKey)}:", color = MutedText, fontSize = 12.sp)
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it },
                        placeholder = { Text("e.g. 10000") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFD5A4E),
                            unfocusedBorderColor = BorderHighlight
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val limit = budgetText.toFloatOrNull() ?: 10000f
                        prefs.edit().putFloat("monthly_budget_$selectedMonthKey", limit).apply()
                        monthlyBudget = limit
                        ToastHelper.showToast(context, "Budget limit saved successfully: DH $limit")
                        showBudgetDialog = false
                    }
                ) {
                    Text("Save", color = Color(0xFFFD5A4E))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel", color = MutedText)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}

// Simple Toast Helper to bypass framework errors
object ToastHelper {
    fun showToast(context: android.content.Context, msg: String) {
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
}

// ============================================
// HIGH FIDELITY TRANSACTIONS WIZARD DIALOG
// ============================================
@Composable
fun AddTransactionDialog(
    accounts: List<MoneyAccountEntity>,
    categories: List<CategoryEntity>,
    viewModel: DashboardViewModel,
    onDismiss: () -> Unit,
    onSave: (type: String, amount: Double, category: String, account: String, toAccount: String?, note: String, dateStr: String) -> Unit
) {
    var type by rememberSaveable { mutableStateOf("EXPENSE") } // "INCOME", "EXPENSE", "TRANSFER"
    var activeAmount by rememberSaveable { mutableStateOf("0") }
    var selectedCategory by rememberSaveable { mutableStateOf("") }
    var selectedAccount by rememberSaveable { mutableStateOf("") }
    var selectedToAccount by rememberSaveable { mutableStateOf("") } // for Transfer
    var note by rememberSaveable { mutableStateOf("") }
    var activeCurrency by rememberSaveable { mutableStateOf("DH") } // "DH", "₹"

    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var newCategoryText by rememberSaveable { mutableStateOf("") }

    var showAddAccountDialog by rememberSaveable { mutableStateOf(false) }
    var newAccountText by rememberSaveable { mutableStateOf("") }
    var newAccountType by rememberSaveable { mutableStateOf("CASH") }
    var newAccountBal by rememberSaveable { mutableStateOf("0.0") }

    val filteredDbCategories = remember(categories, type) {
        categories.filter { it.type == type }.map { it.name }
    }
    val fallbackExpenseCats = listOf("Food", "Baqer", "Transport", "Eva VR", "emi TV", "LuLu", "wifi", "Tabby", "Tamara", "Appi", "Others")
    val fallbackIncomeCats = listOf("Salary", "Freelance", "Other")

    val activeCats = if (filteredDbCategories.isNotEmpty()) filteredDbCategories else {
        if (type == "EXPENSE") fallbackExpenseCats else fallbackIncomeCats
    }

    val dateFormatted = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.format(Date())
    }
    val dateLabel = remember {
        val sdf = SimpleDateFormat("dd/MM/yyyy (EEE) h:mm a", Locale.getDefault())
        sdf.format(Date())
    }

    LaunchedEffect(accounts) {
        if (selectedAccount.isEmpty() && accounts.isNotEmpty()) {
            selectedAccount = accounts.first().name
        }
        if (selectedToAccount.isEmpty() && accounts.size > 1) {
            selectedToAccount = accounts[1].name
        }
    }

    LaunchedEffect(categories, type) {
        if (selectedCategory.isEmpty() && activeCats.isNotEmpty()) {
            selectedCategory = activeCats.first()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60D0D0D)) // immersive overlay
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                // Consume all touch/click/tap gestures so they do not propagate to components behind the dialog
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Row: Back button, Title Expense, Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = if (type == "EXPENSE") "Expense" else if (type == "INCOME") "Income" else "Transfer",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Default.Star, contentDescription = "Fav", tint = MutedText, modifier = Modifier.size(20.dp))
            }

            // Top Tab Pills Income | Expense | Transfer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("INCOME" to "Income", "EXPENSE" to "Expense", "TRANSFER" to "Transfer").forEach { (typeVal, label) ->
                    val isSel = type == typeVal
                    val activeColor = if (typeVal == "EXPENSE") Color(0xFFEF5350) else BlueIncome
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSel) activeColor.copy(alpha = 0.15f) else Color(0xFF1E1E1E),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                width = if (isSel) 1.dp else 0.dp,
                                color = if (isSel) activeColor else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                type = typeVal
                                val listCats = if (typeVal == "EXPENSE") {
                                    val dbCats = categories.filter { it.type == "EXPENSE" }.map { it.name }
                                    if (dbCats.isNotEmpty()) dbCats else fallbackExpenseCats
                                } else {
                                    val dbCats = categories.filter { it.type == "INCOME" }.map { it.name }
                                    if (dbCats.isNotEmpty()) dbCats else fallbackIncomeCats
                                }
                                selectedCategory = listCats.firstOrNull() ?: ""
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) activeColor else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Entry fields (mimicking Screenshot 1 exactly)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // DATE ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Date", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                    Text(dateLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Refresh, contentDescription = "Repeat icon", tint = MutedText, modifier = Modifier.size(16.dp))
                }

                HorizontalDivider(color = BorderHighlight)

                // AMOUNT ROW WITH ACTIVE CURRENCY DRAWN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Amount", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "$activeCurrency ",
                            color = if (type == "EXPENSE") Color(0xFFEF5350) else BlueIncome,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = activeAmount,
                            color = if (type == "EXPENSE") Color(0xFFEF5350) else BlueIncome,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.border(BorderStroke(0.5.dp, Color(0x33FFFFFF))).padding(horizontal = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = BorderHighlight)

                // CATEGORY ROW
                if (type != "TRANSFER") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Category", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            activeCats.forEach { cat ->
                                val isSel = selectedCategory == cat
                                Text(
                                    text = cat,
                                    color = if (isSel) Color.White else MutedText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            color = if (isSel) Color(0xFFFD5A4E) else Color(0xFF161616),
                                            shape = RoundedCornerShape(100)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFF222222),
                                        shape = RoundedCornerShape(100)
                                    )
                                    .clickable { showAddCategoryDialog = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Category", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = BorderHighlight)
                }

                // ACCOUNT SELECTION ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (type == "TRANSFER") "From" else "Account", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        accounts.forEach { acc ->
                            val isSel = selectedAccount == acc.name
                            Text(
                                text = acc.name,
                                color = if (isSel) Color.White else MutedText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        color = if (isSel) Color(0xFF2196F3) else Color(0xFF161616),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedAccount = acc.name }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF222222),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { showAddAccountDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Account", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderHighlight)

                // TO ACCOUNT FOR TRANSFERS
                if (type == "TRANSFER") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("To Account", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            accounts.forEach { acc ->
                                val isSel = selectedToAccount == acc.name
                                Text(
                                    text = acc.name,
                                    color = if (isSel) Color.White else MutedText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            color = if (isSel) Color(0xFF4CAF50) else Color(0xFF161616),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { selectedToAccount = acc.name }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = BorderHighlight)
                }

                // NOTE INPUT ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Note", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                    TextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("Notes info...", color = MutedText, fontSize = 14.sp) },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = BorderHighlight)
            }

            // High Fidelity NUMPAD Custom Keyboard strictly matching Screenshot 1
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF191F26)) // Dark bluish grey panel
            ) {
                // Currency Tab Panel: DH vs ₹
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0xFF1E2732))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (activeCurrency == "DH") Color(0xFF293542) else Color.Transparent)
                            .clickable { activeCurrency = "DH" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("DH", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (activeCurrency == "₹") Color(0xFF293542) else Color.Transparent)
                            .clickable { activeCurrency = "₹" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("₹", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Custom Numpad Grid layout
                val numpadKeys = listOf(
                    listOf("1", "2", "3", "BACK"),
                    listOf("4", "5", "6", "-"),
                    listOf("7", "8", "9", "MINIMIZE"),
                    listOf("0", ".", "DONE")
                )

                numpadKeys.forEach { rowKeys ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowKeys.forEach { key ->
                            val isAction = key == "BACK" || key == "-" || key == "MINIMIZE" || key == "DONE"
                            val weight = if (key == "DONE") 2f else 1f
                            val containerCol = if (key == "DONE") Color(0xFFFD5A4E) else if (isAction) Color(0xFF212A34) else Color(0xFF191F26)

                            Box(
                                modifier = Modifier
                                    .weight(weight)
                                    .height(56.dp)
                                    .border(1.dp, Color(0x1AFFFFFF))
                                    .background(containerCol)
                                    .clickable {
                                        when (key) {
                                            "BACK" -> {
                                                if (activeAmount.length > 1) {
                                                    activeAmount = activeAmount.dropLast(1)
                                                } else {
                                                    activeAmount = "0"
                                                }
                                            }
                                            "-" -> {
                                                // Negative amount or math or ignored
                                            }
                                            "MINIMIZE" -> {
                                                // Dismiss/closes soft custom panel
                                            }
                                            "DONE" -> {
                                                val amtParsed = activeAmount.toDoubleOrNull() ?: 0.0
                                                if (amtParsed > 0.0) {
                                                    onSave(
                                                        type,
                                                        amtParsed,
                                                        if (type == "TRANSFER") "Transfer" else selectedCategory,
                                                        selectedAccount,
                                                        if (type == "TRANSFER") selectedToAccount else null,
                                                        note,
                                                        dateFormatted
                                                    )
                                                }
                                            }
                                            else -> {
                                                if (activeAmount == "0") {
                                                    activeAmount = key
                                                } else {
                                                    activeAmount += key
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "BACK") {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Backspace",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else if (key == "MINIMIZE") {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Minimize keypad",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showAddCategoryDialog) {
                AlertDialog(
                    onDismissRequest = { showAddCategoryDialog = false },
                    title = { Text("Add Custom Category", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Category Name for $type", color = MutedText, fontSize = 12.sp)
                            TextField(
                                value = newCategoryText,
                                onValueChange = { newCategoryText = it },
                                placeholder = { Text("e.g. Shopping", color = MutedText, fontSize = 14.sp) },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF262626),
                                    unfocusedContainerColor = Color(0xFF1E1E1E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color(0xFFFD5A4E),
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val trimmed = newCategoryText.trim()
                                if (trimmed.isNotEmpty()) {
                                    viewModel.addCategory(trimmed, type)
                                    selectedCategory = trimmed
                                    newCategoryText = ""
                                    showAddCategoryDialog = false
                                }
                            }
                        ) {
                            Text("Add", color = Color(0xFFFD5A4E), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddCategoryDialog = false; newCategoryText = "" }) {
                            Text("Cancel", color = Color.White)
                        }
                    },
                    containerColor = Color(0xFF161616),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (showAddAccountDialog) {
                AlertDialog(
                    onDismissRequest = { showAddAccountDialog = false },
                    title = { Text("Add Custom Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Account Name", color = MutedText, fontSize = 12.sp)
                            TextField(
                                value = newAccountText,
                                onValueChange = { newAccountText = it },
                                placeholder = { Text("e.g. Pocket Cash", color = MutedText, fontSize = 14.sp) },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF262626),
                                    unfocusedContainerColor = Color(0xFF1E1E1E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color(0xFF2196F3),
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Account Type", color = MutedText, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("CASH" to "Cash", "BANK" to "Bank", "CARD" to "Card").forEach { (valType, label) ->
                                    val isSelected = newAccountType == valType
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (isSelected) Color(0xFF2196F3).copy(alpha = 0.2f) else Color(0xFF1E1E1E),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                width = if (isSelected) 1.dp else 0.dp,
                                                color = if (isSelected) Color(0xFF2196F3) else Color.Transparent,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { newAccountType = valType }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, color = if (isSelected) Color(0xFF2196F3) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text("Initial Balance", color = MutedText, fontSize = 12.sp)
                            TextField(
                                value = newAccountBal,
                                onValueChange = { newAccountBal = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF262626),
                                    unfocusedContainerColor = Color(0xFF1E1E1E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color(0xFF2196F3),
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val trimmed = newAccountText.trim()
                                val balDouble = newAccountBal.toDoubleOrNull() ?: 0.0
                                if (trimmed.isNotEmpty()) {
                                    viewModel.addMoneyAccount(trimmed, newAccountType, balDouble)
                                    selectedAccount = trimmed
                                    newAccountText = ""
                                    newAccountBal = "0.0"
                                    showAddAccountDialog = false
                                }
                            }
                        ) {
                            Text("Add", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddAccountDialog = false; newAccountText = ""; newAccountBal = "0.0" }) {
                            Text("Cancel", color = Color.White)
                        }
                    },
                    containerColor = Color(0xFF161616),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "No data",
            tint = MutedText,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No financial entries logged yet",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tap the '+' button to log your first income or expense transaction.",
            color = MutedText,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
