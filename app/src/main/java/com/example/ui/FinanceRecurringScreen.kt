package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CategoryEntity
import com.example.data.MoneyAccountEntity
import com.example.data.RecurringEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val RecAccent = Color(0xFFFD5A4E)

private fun fmtMoney(v: Double): String = String.format("DH %,.2f", v)

private fun frequencyLabel(frequency: String, interval: Int): String {
    val n = if (interval < 1) 1 else interval
    val unit = when (frequency) {
        "DAILY" -> "day"
        "WEEKLY" -> "week"
        "MONTHLY" -> "month"
        "YEARLY" -> "year"
        else -> "month"
    }
    return if (n == 1) "Every $unit" else "Every $n ${unit}s"
}

private fun todayString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun isValidDate(s: String): Boolean {
    if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(s)) return false
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US); sdf.isLenient = false; sdf.parse(s); true
    } catch (e: Exception) { false }
}

// ============================================
// RECURRING SUB SCREEN
// ============================================
@Composable
fun RecurringSubScreen(
    recurring: List<RecurringEntity>,
    accounts: List<MoneyAccountEntity>,
    categories: List<CategoryEntity>,
    viewModel: DashboardViewModel
) {
    var editing by remember { mutableStateOf<RecurringEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(CanvasBg)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF141414))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Recurring", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Auto-posted when due", color = MutedText, fontSize = 11.sp)
            }
            Surface(
                modifier = Modifier.clickable { showAdd = true },
                color = RecAccent,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("+ Add", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }

        if (recurring.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No recurring transactions yet.\nTap + Add to set up rent, salary, subscriptions, etc.",
                    color = MutedText, fontSize = 13.sp,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recurring) { rule ->
                    RecurringRow(
                        rule = rule,
                        onClick = { editing = rule },
                        onToggleActive = { viewModel.updateRecurring(rule.copy(active = !rule.active)) }
                    )
                }
            }
        }
    }

    if (showAdd) {
        RecurringEditDialog(
            existing = null,
            accounts = accounts,
            categories = categories,
            onDismiss = { showAdd = false },
            onSave = { rule -> viewModel.addRecurring(rule); showAdd = false },
            onDelete = null
        )
    }

    editing?.let { rule ->
        RecurringEditDialog(
            existing = rule,
            accounts = accounts,
            categories = categories,
            onDismiss = { editing = null },
            onSave = { updated -> viewModel.updateRecurring(updated); editing = null },
            onDelete = { viewModel.deleteRecurring(rule); editing = null }
        )
    }
}

@Composable
private fun RecurringRow(rule: RecurringEntity, onClick: () -> Unit, onToggleActive: () -> Unit) {
    val amountColor = if (rule.type == "INCOME") BlueIncome else if (rule.type == "EXPENSE") RedExpense else Color.White
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = LayerCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderHighlight)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (rule.type == "TRANSFER") "Transfer" else rule.category,
                    color = if (rule.active) Color.White else MutedText,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${frequencyLabel(rule.frequency, rule.intervalCount)} • next ${rule.nextDate}",
                    color = MutedText, fontSize = 11.sp
                )
                Text(
                    if (rule.type == "TRANSFER") "${rule.account} → ${rule.toAccount ?: "?"}" else rule.account,
                    color = MutedText, fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(fmtMoney(rule.amount), color = if (rule.active) amountColor else MutedText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Switch(
                    checked = rule.active,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RecAccent,
                        uncheckedThumbColor = MutedText,
                        uncheckedTrackColor = Color(0xFF2A2A2A)
                    )
                )
            }
        }
    }
}

@Composable
private fun RecurringEditDialog(
    existing: RecurringEntity?,
    accounts: List<MoneyAccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (RecurringEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var type by remember { mutableStateOf(existing?.type ?: "EXPENSE") }
    var amountStr by remember {
        mutableStateOf(existing?.amount?.takeIf { it > 0 }?.let { it.toBigDecimal().stripTrailingZeros().toPlainString() } ?: "")
    }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var account by remember { mutableStateOf(existing?.account ?: accounts.firstOrNull()?.name ?: "") }
    var toAccount by remember { mutableStateOf(existing?.toAccount ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var frequency by remember { mutableStateOf(existing?.frequency ?: "MONTHLY") }
    var intervalStr by remember { mutableStateOf((existing?.intervalCount ?: 1).toString()) }
    var startDate by remember { mutableStateOf(existing?.startDate ?: todayString()) }

    val accountNames = accounts.map { it.name }
    val typeCats = categories.filter { it.type == type }.map { it.name }

    Dialog(onDismissRequest = onDismiss) {
        Surface(color = LayerCard, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderHighlight)) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                Text(if (existing == null) "New recurring" else "Edit recurring", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))

                // Type segmented
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("EXPENSE" to "Expense", "INCOME" to "Income", "TRANSFER" to "Transfer").forEach { (t, lbl) ->
                        val sel = type == t
                        Surface(
                            modifier = Modifier.weight(1f).clickable {
                                type = t
                                if (t != "TRANSFER" && category !in categories.filter { it.type == t }.map { c -> c.name }) category = ""
                            },
                            color = if (sel) RecAccent else Color(0xFF222222),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(lbl, color = if (sel) Color.White else MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                FieldTextInput(value = amountStr, label = "Amount (DH)", keyboard = KeyboardType.Decimal,
                    onChange = { amountStr = it.filter { ch -> ch.isDigit() || ch == '.' } })

                if (type != "TRANSFER") {
                    Spacer(Modifier.height(10.dp))
                    DropdownField(label = "Category", value = category.ifBlank { "Select" }, options = typeCats, onSelect = { category = it })
                }

                Spacer(Modifier.height(10.dp))
                DropdownField(label = if (type == "TRANSFER") "From account" else "Account", value = account.ifBlank { "Select" }, options = accountNames, onSelect = { account = it })

                if (type == "TRANSFER") {
                    Spacer(Modifier.height(10.dp))
                    DropdownField(label = "To account", value = toAccount.ifBlank { "Select" }, options = accountNames, onSelect = { toAccount = it })
                }

                Spacer(Modifier.height(14.dp))
                Text("Frequency", color = MutedText, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("DAILY" to "Day", "WEEKLY" to "Week", "MONTHLY" to "Month", "YEARLY" to "Year").forEach { (f, lbl) ->
                        val sel = frequency == f
                        Surface(
                            modifier = Modifier.weight(1f).clickable { frequency = f },
                            color = if (sel) RecAccent else Color(0xFF222222),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(lbl, color = if (sel) Color.White else MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                FieldTextInput(value = intervalStr, label = "Repeat every N (interval)", keyboard = KeyboardType.Number,
                    onChange = { intervalStr = it.filter { ch -> ch.isDigit() }.take(3) })

                Spacer(Modifier.height(10.dp))
                FieldTextInput(value = startDate, label = "Start date (yyyy-MM-dd)", keyboard = KeyboardType.Number,
                    onChange = { startDate = it.filter { ch -> ch.isDigit() || ch == '-' }.take(10) })

                Spacer(Modifier.height(10.dp))
                FieldTextInput(value = note, label = "Note (optional)", keyboard = KeyboardType.Text, onChange = { note = it })

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) { Text("Delete", color = RedExpense) }
                        Spacer(Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    TextButton(onClick = onDismiss) { Text("Cancel", color = MutedText) }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            val interval = intervalStr.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            val cat = if (type == "TRANSFER") "Transfer" else category
                            val validTransfer = type != "TRANSFER" || (toAccount.isNotBlank() && toAccount != account)
                            if (amt > 0 && account.isNotBlank() && isValidDate(startDate) &&
                                (type == "TRANSFER" || category.isNotBlank()) && validTransfer
                            ) {
                                val rule = (existing ?: RecurringEntity(
                                    type = type, amount = amt, category = cat, account = account,
                                    frequency = frequency, startDate = startDate, nextDate = startDate
                                )).copy(
                                    type = type, amount = amt, category = cat, account = account,
                                    toAccount = if (type == "TRANSFER") toAccount else null,
                                    note = note, frequency = frequency, intervalCount = interval,
                                    startDate = startDate,
                                    // editing keeps its place in the schedule unless the start moved forward
                                    nextDate = if (existing == null) startDate else maxOf(existing.nextDate, startDate)
                                )
                                onSave(rule)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RecAccent)
                    ) { Text("Save", color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun FieldTextInput(value: String, label: String, keyboard: KeyboardType, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RecAccent,
            unfocusedBorderColor = BorderHighlight,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = RecAccent,
            focusedLabelColor = RecAccent,
            unfocusedLabelColor = MutedText
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DropdownField(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, color = MutedText, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Box {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                color = Color(0xFF222222),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderHighlight)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(value, color = if (value == "Select") MutedText else Color.White, fontSize = 14.sp)
                    Text("▾", color = MutedText, fontSize = 12.sp)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (options.isEmpty()) {
                    DropdownMenuItem(text = { Text("No options") }, onClick = { expanded = false })
                } else {
                    options.forEach { opt ->
                        DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
                    }
                }
            }
        }
    }
}
