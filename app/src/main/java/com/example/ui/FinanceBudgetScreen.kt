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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.BudgetEntity
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.ui.theme.*

private val BudgetAccent = Color(0xFFFD5A4E)
private val BudgetGood = Color(0xFF26C281)
private val BudgetWarn = Color(0xFFFFB300)

private fun money(v: Double): String = String.format("DH %,.2f", v)

/** Effective budget amount for a category in a given month: month-specific override wins, else the repeating default. */
private fun effectiveBudget(budgets: List<BudgetEntity>, category: String, monthKey: String): BudgetEntity? =
    budgets.firstOrNull { it.category == category && it.yearMonth == monthKey }
        ?: budgets.firstOrNull { it.category == category && it.yearMonth == null }

// ============================================
// BUDGET SUB SCREEN
// ============================================
@Composable
fun BudgetSubScreen(
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    selectedMonthKey: String,
    onMonthKeyChange: (String) -> Unit,
    viewModel: DashboardViewModel
) {
    // category being edited; "" = none, BudgetEntity.TOTAL = overall budget
    var editingCategory by remember { mutableStateOf<String?>(null) }

    val monthExpenses = remember(transactions, selectedMonthKey) {
        transactions.filter { it.type == "EXPENSE" && it.dateString.startsWith(selectedMonthKey) }
    }
    val spentByCat = remember(monthExpenses) {
        monthExpenses.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }
    }
    val totalSpent = remember(monthExpenses) { monthExpenses.sumOf { it.amount } }
    val totalBudget = effectiveBudget(budgets, BudgetEntity.TOTAL, selectedMonthKey)?.amount ?: 0.0

    val expenseCats = remember(categories) { categories.filter { it.type == "EXPENSE" }.map { it.name } }
    // Show categories that have a budget or any spending this month, budgeted first.
    val rows = remember(expenseCats, budgets, spentByCat, selectedMonthKey) {
        expenseCats.map { cat ->
            val b = effectiveBudget(budgets, cat, selectedMonthKey)?.amount ?: 0.0
            val s = spentByCat[cat] ?: 0.0
            Triple(cat, b, s)
        }.sortedWith(compareByDescending<Triple<String, Double, Double>> { it.second > 0 }.thenByDescending { it.third })
    }

    Column(modifier = Modifier.fillMaxSize().background(CanvasBg)) {
        // Month switcher
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("◀", color = MutedText, fontSize = 18.sp,
                modifier = Modifier.clickable { onMonthKeyChange(getPreviousMonthKey(selectedMonthKey)) }.padding(horizontal = 16.dp))
            Text(formatYearMonth(selectedMonthKey), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("▶", color = MutedText, fontSize = 18.sp,
                modifier = Modifier.clickable { onMonthKeyChange(getNextMonthKey(selectedMonthKey)) }.padding(horizontal = 16.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Overall budget card
            item {
                OverallBudgetCard(
                    budget = totalBudget,
                    spent = totalSpent,
                    onClick = { editingCategory = BudgetEntity.TOTAL }
                )
            }

            item {
                Text(
                    "CATEGORY BUDGETS",
                    color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }

            if (rows.isEmpty()) {
                item {
                    Text(
                        "No expense categories yet. Add categories from the transaction screen, then set budgets here.",
                        color = MutedText, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(rows) { (cat, budget, spent) ->
                    CategoryBudgetRow(
                        category = cat,
                        budget = budget,
                        spent = spent,
                        onClick = { editingCategory = cat }
                    )
                }
            }
        }
    }

    editingCategory?.let { cat ->
        val existing = effectiveBudget(budgets, cat, selectedMonthKey)
        SetBudgetDialog(
            category = cat,
            monthKey = selectedMonthKey,
            existing = existing,
            onDismiss = { editingCategory = null },
            onSave = { amount, applyEveryMonth ->
                viewModel.setBudget(cat, amount, if (applyEveryMonth) null else selectedMonthKey)
                editingCategory = null
            },
            onDelete = existing?.let { e -> { viewModel.deleteBudget(e); editingCategory = null } }
        )
    }
}

@Composable
private fun OverallBudgetCard(budget: Double, spent: Double, onClick: () -> Unit) {
    val remaining = budget - spent
    val frac = if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1f) else 0f
    val over = budget > 0 && spent > budget
    val barColor = when {
        budget <= 0 -> MutedText
        over -> RedExpense
        frac > 0.85f -> BudgetWarn
        else -> BudgetGood
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = LayerCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderHighlight)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Monthly budget", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(if (budget > 0) money(budget) else "Tap to set", color = if (budget > 0) Color.White else BudgetAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            ProgressBar(frac = frac, color = barColor)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Spent ${money(spent)}", color = MutedText, fontSize = 12.sp)
                if (budget > 0) {
                    Text(
                        if (over) "Over by ${money(-remaining)}" else "${money(remaining)} left",
                        color = if (over) RedExpense else BudgetGood, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBudgetRow(category: String, budget: Double, spent: Double, onClick: () -> Unit) {
    val remaining = budget - spent
    val frac = if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1f) else 0f
    val over = budget > 0 && spent > budget
    val barColor = when {
        budget <= 0 -> MutedText.copy(alpha = 0.5f)
        over -> RedExpense
        frac > 0.85f -> BudgetWarn
        else -> BudgetGood
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = LayerCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderHighlight)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    if (budget > 0) "${money(spent)} / ${money(budget)}" else money(spent),
                    color = MutedText, fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            ProgressBar(frac = if (budget > 0) frac else 0f, color = barColor)
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    budget <= 0 -> "No budget — tap to set"
                    over -> "Over by ${money(-remaining)}"
                    else -> "${money(remaining)} left"
                },
                color = when { budget <= 0 -> BudgetAccent; over -> RedExpense; else -> BudgetGood },
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ProgressBar(frac: Float, color: Color) {
    Box(
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2A2A))
    ) {
        Box(modifier = Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
    }
}

@Composable
private fun SetBudgetDialog(
    category: String,
    monthKey: String,
    existing: BudgetEntity?,
    onDismiss: () -> Unit,
    onSave: (amount: Double, applyEveryMonth: Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    val title = if (category == BudgetEntity.TOTAL) "Monthly budget" else category
    var amountStr by remember {
        mutableStateOf(existing?.amount?.takeIf { it > 0 }?.let { it.toBigDecimal().stripTrailingZeros().toPlainString() } ?: "")
    }
    // Default: keep existing scope, else "every month".
    var applyEveryMonth by remember { mutableStateOf(existing?.yearMonth == null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(color = LayerCard, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderHighlight)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Set budget", color = MutedText, fontSize = 12.sp)
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { new -> amountStr = new.filter { it.isDigit() || it == '.' } },
                    label = { Text("Amount (DH)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BudgetAccent,
                        unfocusedBorderColor = BorderHighlight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = BudgetAccent,
                        focusedLabelColor = BudgetAccent,
                        unfocusedLabelColor = MutedText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { applyEveryMonth = !applyEveryMonth },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = applyEveryMonth,
                        onCheckedChange = { applyEveryMonth = it },
                        colors = CheckboxDefaults.colors(checkedColor = BudgetAccent, uncheckedColor = MutedText)
                    )
                    Column {
                        Text("Repeat every month", color = Color.White, fontSize = 14.sp)
                        Text(
                            if (applyEveryMonth) "Applies to all months" else "Only ${formatYearMonth(monthKey)}",
                            color = MutedText, fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) { Text("Delete", color = RedExpense) }
                        Spacer(Modifier.weight(1f))
                    }
                    TextButton(onClick = onDismiss) { Text("Cancel", color = MutedText) }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (amt > 0) onSave(amt, applyEveryMonth)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BudgetAccent)
                    ) { Text("Save", color = Color.White) }
                }
            }
        }
    }
}
