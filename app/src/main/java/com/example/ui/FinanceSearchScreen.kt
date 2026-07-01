package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryEntity
import com.example.data.MoneyAccountEntity
import com.example.data.TransactionEntity
import com.example.ui.theme.*

private val SearchAccent = Color(0xFFFD5A4E)

// ============================================
// SEARCH / FILTER OVERLAY
// ============================================
@Composable
fun FinanceSearchScreen(
    transactions: List<TransactionEntity>,
    accounts: List<MoneyAccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    viewModel: DashboardViewModel
) {
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("ALL") } // ALL, INCOME, EXPENSE, TRANSFER
    var catFilter by remember { mutableStateOf<String?>(null) }
    var accFilter by remember { mutableStateOf<String?>(null) }

    val results = remember(transactions, query, typeFilter, catFilter, accFilter) {
        val q = query.trim()
        transactions.filter { tx ->
            (typeFilter == "ALL" || tx.type == typeFilter) &&
                (catFilter == null || tx.category.equals(catFilter, ignoreCase = true)) &&
                (accFilter == null || tx.account.equals(accFilter, ignoreCase = true)) &&
                (q.isEmpty() || listOfNotNull(tx.category, tx.account, tx.note, tx.description, tx.tags)
                    .any { it.contains(q, ignoreCase = true) })
        }
    }
    val income = results.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expense = results.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    Surface(modifier = Modifier.fillMaxSize(), color = CanvasBg) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF141414))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Close search", tint = Color.White)
                }
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search notes, category, tags...", color = MutedText, fontSize = 14.sp) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = SearchAccent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MutedText)
                    }
                }
            }

            // Type filter chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL" to "All", "INCOME" to "Income", "EXPENSE" to "Expense", "TRANSFER" to "Transfer").forEach { (t, lbl) ->
                    val sel = typeFilter == t
                    Surface(
                        modifier = Modifier.weight(1f).clickable { typeFilter = t },
                        color = if (sel) SearchAccent else Color(0xFF222222),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(lbl, color = if (sel) Color.White else MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    }
                }
            }

            // Category + account dropdown filters
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    FilterDropdown(
                        label = catFilter ?: "All categories",
                        options = listOf("All categories") + categories.map { it.name }.distinct(),
                        onSelect = { catFilter = if (it == "All categories") null else it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    FilterDropdown(
                        label = accFilter ?: "All accounts",
                        options = listOf("All accounts") + accounts.map { it.name }.distinct(),
                        onSelect = { accFilter = if (it == "All accounts") null else it }
                    )
                }
            }

            // Summary bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${results.size} result${if (results.size == 1) "" else "s"}", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(String.format("+DH %,.0f", income), color = BlueIncome, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("-DH %,.0f", expense), color = RedExpense, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = BorderHighlight)

            if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matching transactions", color = MutedText, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results) { tx ->
                        TransactionRowItem(
                            tx = tx,
                            onDelete = { viewModel.deleteTransaction(tx) },
                            onEdit = { updated -> viewModel.editTransaction(tx, updated) }
                        )
                        HorizontalDivider(color = BorderHighlight.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(label: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            color = Color(0xFF222222),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, BorderHighlight)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = Color.White, fontSize = 12.sp, maxLines = 1)
                Text("▾", color = MutedText, fontSize = 11.sp)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}
