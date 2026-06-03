package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel(private val repository: DashboardRepository) : ViewModel() {

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
                            // New day! Reset checks and check streak
                            val wasCompleted = habit.isCompleted
                            val newStreak = if (wasCompleted) habit.streak else 0
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

    // --- Helper for Default Population ---
    private suspend fun populateDefaultData() {
        // Compute dates relative to today so seed data always looks current
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        val today = sdf.format(cal.time)
        fun daysAgo(n: Int): String {
            val c = java.util.Calendar.getInstance()
            c.add(java.util.Calendar.DAY_OF_YEAR, -n)
            return sdf.format(c.time)
        }
        val year = cal.get(java.util.Calendar.YEAR)
        val month = java.lang.String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)

        // 1. Default Habits
        val defaultHabits = listOf(
            HabitEntity(name = "Morning workout (15+ min)", isCompleted = false, streak = 3),
            HabitEntity(name = "Read for 20 minutes", isCompleted = false, streak = 5),
            HabitEntity(name = "Drink 2L of water", isCompleted = false, streak = 1),
            HabitEntity(name = "No screens 30 min before bed", isCompleted = false, streak = 0)
        )
        defaultHabits.forEach { repository.insertHabit(it) }

        // 2. Default Intents
        val defaultIntents = listOf(
            IntentEntity(name = "Exercise every day — no excuses", isCompleted = false),
            IntentEntity(name = "Eat a healthy meal today", isCompleted = true),
            IntentEntity(name = "Sleep 7+ hours tonight", isCompleted = false),
            IntentEntity(name = "Spend 30 min learning something new", isCompleted = true),
            IntentEntity(name = "Practice gratitude — write 3 things", isCompleted = false)
        )
        defaultIntents.forEach { repository.insertIntent(it) }

        // 3. Default Goals
        val goalIds = listOf(
            repository.insertGoal(GoalEntity(name = "Build a consistent fitness habit", why = "More energy and confidence", status = "ACTIVE")),
            repository.insertGoal(GoalEntity(name = "Learn a new skill", why = "Career growth and fulfilment", status = "ACTIVE")),
            repository.insertGoal(GoalEntity(name = "Save DH 5,000 this year", why = "Financial security and freedom", status = "ACTIVE")),
            repository.insertGoal(GoalEntity(name = "Read 12 books this year", why = "Knowledge and perspective", status = "NEXT")),
            repository.insertGoal(GoalEntity(name = "Start a side project", why = "Creative outlet and extra income", status = "SOMEDAY"))
        )
        repository.insertPointLog(PointLogEntity(goalId = goalIds[0], activity = "Morning run — 30 min", hours = 2f))
        repository.insertPointLog(PointLogEntity(goalId = goalIds[0], activity = "Home workout — upper body", hours = 1.5f))
        repository.insertPointLog(PointLogEntity(goalId = goalIds[1], activity = "Online course — Chapter 3", hours = 2f))
        repository.insertPointLog(PointLogEntity(goalId = goalIds[1], activity = "Practice exercises completed", hours = 1f))
        repository.insertPointLog(PointLogEntity(goalId = goalIds[2], activity = "Monthly savings transfer done", hours = 5f))

        // 4. Default Learning Items
        val defaultLearning = listOf(
            LearningEntity(name = "Online Programming Course", subtext = "Working through exercises daily", category = "IT", status = "ACTIVE"),
            LearningEntity(name = "Public Speaking", subtext = "Watching talks and practising delivery", category = "COURSES", status = "ACTIVE"),
            LearningEntity(name = "Machine Learning Basics", subtext = "After current course finishes", category = "IT", status = "NEXT"),
            LearningEntity(name = "A New Language", subtext = "Pick one and start", category = "LANGUAGES", status = "SOMEDAY")
        )
        defaultLearning.forEach { repository.insertLearning(it) }

        // 5. Default Words
        val defaultWords = listOf(
            WordEntity(word = "مرحبا (Marhaban)", meaning = "Hello", category = "ARABIC"),
            WordEntity(word = "شكرا (Shukran)", meaning = "Thank you", category = "ARABIC"),
            WordEntity(word = "صباح الخير (Sabah al-khair)", meaning = "Good morning", category = "ARABIC"),
            WordEntity(word = "ありがとう (Arigatou)", meaning = "Thank you", category = "JAPANESE"),
            WordEntity(word = "こんにちは (Konnichiwa)", meaning = "Hello / Good afternoon", category = "JAPANESE"),
            WordEntity(word = "Discipline", meaning = "Training oneself to follow rules consistently", category = "ENGLISH"),
            WordEntity(word = "Perseverance", meaning = "Continued effort despite difficulty or delay", category = "ENGLISH")
        )
        defaultWords.forEach { repository.insertWord(it) }

        // 6. Default Sleep Logs (last 7 days, relative to today)
        val defaultSleeps = listOf(
            SleepLogEntity(dateString = today,       sleptAt = "23:30", wokeUp = "06:45", hoursSlept = 7.3f),
            SleepLogEntity(dateString = daysAgo(1),  sleptAt = "00:15", wokeUp = "06:30", hoursSlept = 6.3f),
            SleepLogEntity(dateString = daysAgo(2),  sleptAt = "23:00", wokeUp = "07:00", hoursSlept = 8.0f),
            SleepLogEntity(dateString = daysAgo(3),  sleptAt = "01:00", wokeUp = "06:30", hoursSlept = 5.5f),
            SleepLogEntity(dateString = daysAgo(4),  sleptAt = "23:45", wokeUp = "07:15", hoursSlept = 7.5f),
            SleepLogEntity(dateString = daysAgo(5),  sleptAt = "00:30", wokeUp = "06:00", hoursSlept = 5.5f),
            SleepLogEntity(dateString = daysAgo(6),  sleptAt = "22:30", wokeUp = "06:30", hoursSlept = 8.0f)
        )
        defaultSleeps.forEach { repository.insertSleepLog(it) }

        // 7. Default Reflection
        repository.insertReflection(
            ReflectionEntity(
                weekKey = java.text.SimpleDateFormat("yyyy-'W'ww", java.util.Locale.US).format(cal.time),
                reflection = "Good start to using LevelUp! Habits are building, sleep is improving.",
                intention = "Stay consistent with habits this week. Log every expense. Sleep before midnight."
            )
        )

        // 8. Default Money Accounts
        val defaultAccounts = listOf(
            MoneyAccountEntity(name = "Wallet",       type = "CASH", balance = 500.0),
            MoneyAccountEntity(name = "Savings Jar",  type = "CASH", balance = 1500.0),
            MoneyAccountEntity(name = "Bank Account", type = "BANK", balance = 4200.0),
            MoneyAccountEntity(name = "Credit Card",  type = "CARD", balance = 0.0)
        )
        defaultAccounts.forEach { repository.insertMoneyAccount(it) }

        // 9. Default Categories
        val defaultExpenseCats = listOf(
            "Food & Dining", "Transport", "Shopping", "Entertainment",
            "Health & Fitness", "Utilities", "Education", "Personal Care",
            "Subscriptions", "Travel", "Other"
        )
        defaultExpenseCats.forEach { repository.insertCategory(CategoryEntity(name = it, type = "EXPENSE")) }
        val defaultIncomeCats = listOf("Salary", "Freelance", "Investment Returns", "Gift", "Other")
        defaultIncomeCats.forEach { repository.insertCategory(CategoryEntity(name = it, type = "INCOME")) }

        // 10. Default Transactions (current month, relative dates)
        val d = "$year-$month-"
        val defaultTx = listOf(
            TransactionEntity(type = "INCOME",   amount = 5000.0, category = "Salary",         account = "Bank Account", dateString = "${d}01", timeString = "09:00 am", note = "Monthly salary"),
            TransactionEntity(type = "EXPENSE",  amount = 120.0,  category = "Food & Dining",   account = "Bank Account", dateString = "${d}02", timeString = "01:00 pm", note = "Groceries"),
            TransactionEntity(type = "EXPENSE",  amount = 45.0,   category = "Transport",        account = "Wallet",       dateString = "${d}03", timeString = "08:30 am", note = "Bus pass"),
            TransactionEntity(type = "EXPENSE",  amount = 200.0,  category = "Utilities",        account = "Bank Account", dateString = "${d}05", timeString = "10:00 am", note = "Internet bill"),
            TransactionEntity(type = "EXPENSE",  amount = 80.0,   category = "Entertainment",    account = "Bank Account", dateString = "${d}08", timeString = "07:00 pm", note = "Movie and dinner"),
            TransactionEntity(type = "TRANSFER", amount = 500.0,  category = "Savings",          account = "Bank Account", toAccount = "Savings Jar", dateString = "${d}10", timeString = "11:00 am", note = "Monthly savings"),
            TransactionEntity(type = "EXPENSE",  amount = 60.0,   category = "Health & Fitness", account = "Bank Account", dateString = "${d}12", timeString = "06:00 am", note = "Gym membership"),
            TransactionEntity(type = "EXPENSE",  amount = 35.0,   category = "Food & Dining",    account = "Wallet",       dateString = "${d}14", timeString = "12:30 pm", note = "Lunch out"),
            TransactionEntity(type = "EXPENSE",  amount = 150.0,  category = "Shopping",         account = "Bank Account", dateString = "${d}16", timeString = "03:00 pm", note = "Clothing"),
            TransactionEntity(type = "INCOME",   amount = 300.0,  category = "Freelance",        account = "Bank Account", dateString = "${d}18", timeString = "05:00 pm", note = "Side project payment")
        )
        defaultTx.forEach { repository.insertTransaction(it) }
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
