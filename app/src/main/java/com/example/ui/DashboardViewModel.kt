package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel(
    private val repository: DashboardRepository,
    private val prefs: android.content.SharedPreferences? = null
) : ViewModel() {

    // --- State Streams ---
    val habits: StateFlow<List<HabitEntity>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val intents: StateFlow<List<IntentEntity>> = repository.allIntents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<GoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pointLogs: StateFlow<List<PointLogEntity>> = repository.allPointLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val learningItems: StateFlow<List<LearningEntity>> = repository.allLearningItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val words: StateFlow<List<WordEntity>> = repository.allWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sleepLogs: StateFlow<List<SleepLogEntity>> = repository.allSleepLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moneyAccounts: StateFlow<List<MoneyAccountEntity>> = repository.allMoneyAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reflection State
    private val _currentWeekKey = MutableStateFlow("2026-W22") // Default to active week key based on system time 2026-05-31
    val currentWeekKey: StateFlow<String> = _currentWeekKey.asStateFlow()

    val currentReflection: StateFlow<ReflectionEntity?> = _currentWeekKey
        .flatMapLatest { weekKey -> repository.getReflectionByWeek(weekKey) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Run database check & populate mock/default data on first launch
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val habitsList = repository.allHabits.first()
                if (habitsList.isEmpty()) {
                    populateDefaultData()
                } else {
                    // Check if day has changed to reset daily habit completion & update streaks
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val todayStr = sdf.format(Date())
                    var updatedAny = false
                    val updatedHabits = habitsList.map { habit ->
                        val lastUpdatedStr = if (habit.dateUpdated > 0) sdf.format(Date(habit.dateUpdated)) else ""
                        if (lastUpdatedStr != todayStr && lastUpdatedStr.isNotEmpty()) {
                            // New day — decide streak with a 1-day "freeze" / grace.
                            val graceKey = "grace_${habit.id}"
                            val wasCompleted = habit.isCompleted
                            val newStreak: Int = if (wasCompleted) {
                                prefs?.edit()?.remove(graceKey)?.apply() // back on track, clear grace
                                habit.streak
                            } else {
                                val graceUsed = prefs?.getBoolean(graceKey, false) ?: false
                                if (habit.streak > 0 && !graceUsed) {
                                    // First missed day: freeze the streak instead of resetting
                                    prefs?.edit()?.putBoolean(graceKey, true)?.apply()
                                    habit.streak
                                } else {
                                    // Missed again (or no streak): reset
                                    prefs?.edit()?.remove(graceKey)?.apply()
                                    0
                                }
                            }
                            updatedAny = true
                            habit.copy(
                                isCompleted = false,
                                streak = newStreak,
                                dateUpdated = System.currentTimeMillis()
                            )
                        } else {
                            habit
                        }
                    }
                    if (updatedAny) {
                        updatedHabits.forEach { repository.updateHabit(it) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setWeekKey(key: String) {
        _currentWeekKey.value = key
    }

    // ============================================================
    // BACKUP / RESTORE  (full DB export & import as JSON)
    // ============================================================
    suspend fun buildBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("schema", 1)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("habits", JSONArray().apply {
            repository.allHabits.first().forEach { put(JSONObject()
                .put("id", it.id).put("name", it.name).put("isCompleted", it.isCompleted)
                .put("streak", it.streak).put("dateUpdated", it.dateUpdated)) }
        })
        root.put("intents", JSONArray().apply {
            repository.allIntents.first().forEach { put(JSONObject()
                .put("id", it.id).put("name", it.name).put("isCompleted", it.isCompleted)) }
        })
        root.put("goals", JSONArray().apply {
            repository.allGoals.first().forEach { put(JSONObject()
                .put("id", it.id).put("name", it.name).put("why", it.why)
                .put("status", it.status).put("bonusPoints", it.bonusPoints.toDouble())) }
        })
        root.put("pointLogs", JSONArray().apply {
            repository.allPointLogs.first().forEach { put(JSONObject()
                .put("id", it.id).put("goalId", it.goalId).put("activity", it.activity)
                .put("hours", it.hours.toDouble()).put("dateAdded", it.dateAdded)) }
        })
        root.put("learning", JSONArray().apply {
            repository.allLearningItems.first().forEach { put(JSONObject()
                .put("id", it.id).put("name", it.name).put("subtext", it.subtext)
                .put("category", it.category).put("status", it.status)) }
        })
        root.put("words", JSONArray().apply {
            repository.allWords.first().forEach { put(JSONObject()
                .put("id", it.id).put("word", it.word).put("meaning", it.meaning).put("category", it.category)) }
        })
        root.put("sleep", JSONArray().apply {
            repository.allSleepLogs.first().forEach { put(JSONObject()
                .put("id", it.id).put("dateString", it.dateString).put("sleptAt", it.sleptAt)
                .put("wokeUp", it.wokeUp).put("hoursSlept", it.hoursSlept.toDouble()).put("timestamp", it.timestamp)) }
        })
        root.put("transactions", JSONArray().apply {
            repository.allTransactions.first().forEach { put(JSONObject()
                .put("id", it.id).put("type", it.type).put("amount", it.amount).put("category", it.category)
                .put("account", it.account).put("toAccount", it.toAccount ?: JSONObject.NULL)
                .put("dateString", it.dateString).put("timeString", it.timeString)
                .put("note", it.note).put("description", it.description ?: JSONObject.NULL).put("timestamp", it.timestamp)) }
        })
        root.put("accounts", JSONArray().apply {
            repository.allMoneyAccounts.first().forEach { put(JSONObject()
                .put("id", it.id).put("name", it.name).put("type", it.type)
                .put("balance", it.balance).put("outstBalance", it.outstBalance)) }
        })
        root.put("categories", JSONArray().apply {
            repository.allCategories.first().forEach { put(JSONObject()
                .put("id", it.id).put("name", it.name).put("type", it.type)) }
        })
        // reflections — read directly via week flow is per-key, so gather via a query helper
        root.put("reflections", JSONArray().apply {
            repository.allReflections().forEach { put(JSONObject()
                .put("weekKey", it.weekKey).put("reflection", it.reflection).put("intention", it.intention)) }
        })
        root.toString()
    }

    suspend fun restoreBackupJson(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(json)
            repository.clearAll()

            root.optJSONArray("habits")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
                repository.insertHabit(HabitEntity(o.getLong("id"), o.getString("name"), o.getBoolean("isCompleted"), o.getInt("streak"), o.getLong("dateUpdated"))) } }
            root.optJSONArray("intents")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
                repository.insertIntent(IntentEntity(o.getLong("id"), o.getString("name"), o.getBoolean("isCompleted"))) } }
            root.optJSONArray("goals")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
                repository.insertGoal(GoalEntity(o.getLong("id"), o.getString("name"), o.getString("why"), o.getString("status"), o.getDouble("bonusPoints").toFloat())) } }
            root.optJSONArray("pointLogs")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
                repository.insertPointLog(PointLogEntity(o.getLong("id"), o.getLong("goalId"), o.getString("activity"), o.getDouble("hours").toFloat(), o.getLong("dateAdded"))) } }
            root.optJSONArray("learning")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
                repository.insertLearning(LearningEntity(o.getLong("id"), o.getString("name"), o.getString("subtext"), o.getString("category"), o.getString("status"))) } }
            root.optJSONArray("words")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
                repository.insertWord(WordEntity(o.getLong("id"), o.getString("word"), o.getString("meaning"), o.getString("category"))) } }
            root.optJSONArray("sleep")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
                repository.insertSleepLog(SleepLogEntity(o.getLong("id"), o.getString("dateString"), o.getString("sleptAt"), o.getString("wokeUp"), o.getDouble("hoursSlept").toFloat(), o.getLong("timestamp"))) } }
            root.optJSONArray("transactions")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
                repository.insertTransaction(TransactionEntity(o.getLong("id"), o.getString("type"), o.getDouble("amount"), o.getString("category"), o.getString("account"), o.optString("toAccount").takeIf { !o.isNull("toAccount") }, o.getString("dateString"), o.getString("timeString"), o.optString("note"), o.optString("description").takeIf { !o.isNull("description") }, o.getLong("timestamp"))) } }
            root.optJSONArray("accounts")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
                repository.insertMoneyAccount(MoneyAccountEntity(o.getLong("id"), o.getString("name"), o.getString("type"), o.getDouble("balance"), o.getDouble("outstBalance"))) } }
            root.optJSONArray("categories")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
                repository.insertCategory(CategoryEntity(o.getLong("id"), o.getString("name"), o.getString("type"))) } }
            root.optJSONArray("reflections")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
                repository.insertReflection(ReflectionEntity(o.getString("weekKey"), o.getString("reflection"), o.getString("intention"))) } }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- Helper for Default Population (minimal — just functional scaffolding) ---
    private suspend fun populateDefaultData() {
        // Starter accounts so the Money tab works immediately (zero balances — edit to your own).
        val defaultAccounts = listOf(
            MoneyAccountEntity(name = "Cash", type = "CASH", balance = 0.0),
            MoneyAccountEntity(name = "Bank Account", type = "BANK", balance = 0.0),
            MoneyAccountEntity(name = "Card", type = "CARD", balance = 0.0)
        )
        defaultAccounts.forEach { repository.insertMoneyAccount(it) }

        // Starter spending / income categories (rename, delete or add your own anytime).
        val defaultExpenseCats = listOf(
            "Food & Dining", "Transport", "Shopping", "Entertainment",
            "Health & Fitness", "Utilities", "Education", "Personal Care",
            "Subscriptions", "Travel", "Other"
        )
        defaultExpenseCats.forEach { repository.insertCategory(CategoryEntity(name = it, type = "EXPENSE")) }
        val defaultIncomeCats = listOf("Salary", "Freelance", "Investment", "Gift", "Other")
        defaultIncomeCats.forEach { repository.insertCategory(CategoryEntity(name = it, type = "INCOME")) }
    }

    /** Wipes all user data and re-seeds the minimal scaffolding. Used by Settings → Start Fresh. */
    fun clearAllData() = viewModelScope.launch(Dispatchers.IO) {
        repository.clearAll()
        populateDefaultData()
    }

    // --- Habit Actions ---
    fun addHabit(name: String, initialStreak: Int = 0) = viewModelScope.launch {
        repository.insertHabit(HabitEntity(name = name, streak = initialStreak))
    }

    fun updateHabitStreak(habit: HabitEntity, newStreak: Int) = viewModelScope.launch {
        repository.updateHabit(habit.copy(streak = newStreak, dateUpdated = System.currentTimeMillis()))
    }

    fun toggleHabit(habit: HabitEntity) = viewModelScope.launch {
        val newDone = !habit.isCompleted
        val newStreak = if (newDone) habit.streak + 1 else maxOf(0, habit.streak - 1)
        if (newDone) prefs?.edit()?.remove("grace_${habit.id}")?.apply() // completed → no longer needs grace
        repository.updateHabit(habit.copy(isCompleted = newDone, streak = newStreak, dateUpdated = System.currentTimeMillis()))
    }

    fun deleteHabit(id: Long) = viewModelScope.launch {
        repository.deleteHabitById(id)
    }

    fun editHabit(habit: HabitEntity, newName: String) = viewModelScope.launch {
        repository.updateHabit(habit.copy(name = newName))
    }

    // --- Intent Actions ---
    fun addIntent(name: String) = viewModelScope.launch {
        repository.insertIntent(IntentEntity(name = name))
    }

    fun toggleIntent(intent: IntentEntity) = viewModelScope.launch {
        repository.updateIntent(intent.copy(isCompleted = !intent.isCompleted))
    }

    fun deleteIntent(id: Long) = viewModelScope.launch {
        repository.deleteIntentById(id)
    }

    fun editIntent(intent: IntentEntity, newName: String) = viewModelScope.launch {
        repository.updateIntent(intent.copy(name = newName))
    }

    // --- Goal Actions ---
    fun addGoal(name: String, why: String, status: String) = viewModelScope.launch {
        repository.insertGoal(GoalEntity(name = name, why = why, status = status))
    }

    fun addPointsToGoal(goalId: Long, activity: String, hours: Float) = viewModelScope.launch {
        repository.insertPointLog(PointLogEntity(goalId = goalId, activity = activity, hours = hours))
    }

    fun updateGoalStatus(goal: GoalEntity, newStatus: String) = viewModelScope.launch {
        repository.updateGoal(goal.copy(status = newStatus))
    }

    fun editGoal(goal: GoalEntity, newName: String, newWhy: String, newStatus: String) = viewModelScope.launch {
        repository.updateGoal(goal.copy(name = newName, why = newWhy, status = newStatus))
    }

    fun deleteGoal(id: Long) = viewModelScope.launch {
        repository.deleteGoalById(id)
    }

    fun deletePointLog(id: Long) = viewModelScope.launch {
        repository.deletePointLogById(id)
    }

    fun editPointLog(log: PointLogEntity, activity: String, hours: Float) = viewModelScope.launch {
        repository.updatePointLog(log.copy(activity = activity, hours = hours))
    }

    // --- Learning Actions ---
    fun addLearning(name: String, subtext: String, category: String, status: String) = viewModelScope.launch {
        repository.insertLearning(LearningEntity(name = name, subtext = subtext, category = category, status = status))
    }

    fun deleteLearning(id: Long) = viewModelScope.launch {
        repository.deleteLearningById(id)
    }

    fun editLearning(item: LearningEntity, name: String, subtext: String, category: String, status: String) = viewModelScope.launch {
        repository.updateLearning(item.copy(name = name, subtext = subtext, category = category, status = status))
    }

    // --- Word Actions ---
    fun addWord(word: String, meaning: String, category: String) = viewModelScope.launch {
        repository.insertWord(WordEntity(word = word, meaning = meaning, category = category))
    }

    fun deleteWord(id: Long) = viewModelScope.launch {
        repository.deleteWordById(id)
    }

    fun editWord(word: WordEntity, newWord: String, newMeaning: String, newCategory: String) = viewModelScope.launch {
        repository.updateWord(word.copy(word = newWord, meaning = newMeaning, category = newCategory))
    }

    // --- Sleep Actions ---
    fun addSleep(dateString: String, sleptAt: String, wokeUp: String) = viewModelScope.launch {
        val calculatedHours = calculateHoursSlept(sleptAt, wokeUp)
        repository.insertSleepLog(
            SleepLogEntity(
                dateString = dateString,
                sleptAt = sleptAt,
                wokeUp = wokeUp,
                hoursSlept = calculatedHours
            )
        )
    }

    fun deleteSleepLog(dateString: String) = viewModelScope.launch {
        repository.deleteSleepLogByDate(dateString)
    }

    fun editSleepLog(log: SleepLogEntity, sleptAt: String, wokeUp: String) = viewModelScope.launch {
        val newHours = calculateHoursSlept(sleptAt, wokeUp)
        repository.updateSleepLog(log.copy(sleptAt = sleptAt, wokeUp = wokeUp, hoursSlept = newHours))
    }

    private fun calculateHoursSlept(sleptAt: String, wokeUp: String): Float {
        return try {
            val sleepParts = sleptAt.split(":").map { it.toInt() }
            val wakeParts = wokeUp.split(":").map { it.toInt() }

            val sleepMinutes = sleepParts[0] * 60 + sleepParts[1]
            var wakeMinutes = wakeParts[0] * 60 + wakeParts[1]

            if (wakeMinutes < sleepMinutes) {
                wakeMinutes += 24 * 60 // Slipped into the next day
            }

            val totalMinutes = wakeMinutes - sleepMinutes
            val rawHours = totalMinutes / 60f
            // Round to 1 decimal place
            Math.round(rawHours * 10f) / 10f
        } catch (e: Exception) {
            5.0f // Default fallback
        }
    }

    // --- Reflection Actions ---
    fun saveReflection(weekKey: String, reflection: String, intention: String) = viewModelScope.launch {
        repository.insertReflection(ReflectionEntity(weekKey = weekKey, reflection = reflection, intention = intention))
    }

    // --- Money Manager Actions ---
    fun addTransaction(
        type: String,
        amount: Double,
        category: String,
        account: String,
        toAccount: String? = null,
        dateString: String,
        timeString: String,
        note: String = ""
    ) = viewModelScope.launch {
        repository.insertTransaction(
            TransactionEntity(
                type = type,
                amount = amount,
                category = category,
                account = account,
                toAccount = toAccount,
                dateString = dateString,
                timeString = timeString,
                note = note
            )
        )
        // Adjust the account balance
        updateBalancesAfterTx(type, amount, account, toAccount)
    }

    fun deleteTransaction(tx: TransactionEntity) = viewModelScope.launch {
        repository.deleteTransaction(tx)
        updateBalancesAfterTxDelete(tx)
    }

    fun editTransaction(oldTx: TransactionEntity, newTx: TransactionEntity) = viewModelScope.launch {
        updateBalancesAfterTxDelete(oldTx)
        repository.updateTransaction(newTx)
        updateBalancesAfterTx(newTx.type, newTx.amount, newTx.account, newTx.toAccount)
    }

    fun addMoneyAccount(name: String, type: String, initialBalance: Double) = viewModelScope.launch {
        repository.insertMoneyAccount(
            MoneyAccountEntity(
                name = name,
                type = type,
                balance = initialBalance
            )
        )
    }

    fun deleteMoneyAccount(id: Long) = viewModelScope.launch {
        repository.deleteMoneyAccountById(id)
    }

    fun editMoneyAccount(account: MoneyAccountEntity) = viewModelScope.launch {
        repository.updateMoneyAccount(account)
    }

    fun addCategory(name: String, type: String) = viewModelScope.launch {
        repository.insertCategory(CategoryEntity(name = name, type = type))
    }

    fun deleteCategory(id: Long) = viewModelScope.launch {
        repository.deleteCategoryById(id)
    }

    private suspend fun updateBalancesAfterTx(type: String, amount: Double, accountName: String, toAccountName: String?) {
        val allAccs = repository.getMoneyAccountsDirect()
        allAccs.find { it.name.trim().lowercase() == accountName.trim().lowercase() }?.let { acc ->
            val newBalance = when (type) {
                "EXPENSE" -> acc.balance - amount
                "INCOME" -> acc.balance + amount
                "TRANSFER" -> acc.balance - amount
                else -> acc.balance
            }
            repository.updateMoneyAccount(acc.copy(balance = newBalance))
        }
        if (type == "TRANSFER" && toAccountName != null) {
            allAccs.find { it.name.trim().lowercase() == toAccountName.trim().lowercase() }?.let { toAcc ->
                repository.updateMoneyAccount(toAcc.copy(balance = toAcc.balance + amount))
            }
        }
    }

    private suspend fun updateBalancesAfterTxDelete(tx: TransactionEntity) {
        val allAccs = repository.getMoneyAccountsDirect()
        allAccs.find { it.name.trim().lowercase() == tx.account.trim().lowercase() }?.let { acc ->
            val newBalance = when (tx.type) {
                "EXPENSE" -> acc.balance + tx.amount
                "INCOME" -> acc.balance - tx.amount
                "TRANSFER" -> acc.balance + tx.amount
                else -> acc.balance
            }
            repository.updateMoneyAccount(acc.copy(balance = newBalance))
        }
        if (tx.type == "TRANSFER" && tx.toAccount != null) {
            allAccs.find { it.name.trim().lowercase() == tx.toAccount.trim().lowercase() }?.let { toAcc ->
                repository.updateMoneyAccount(toAcc.copy(balance = toAcc.balance - tx.amount))
            }
        }
    }
}
