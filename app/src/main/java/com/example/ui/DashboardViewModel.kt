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
        // 1. Default Habits
        val defaultHabits = listOf(
            HabitEntity(name = "Drink weight gainer (12 AM)", isCompleted = false, streak = 4),
            HabitEntity(name = "Exercise with workout app", isCompleted = false, streak = 7),
            HabitEntity(name = "No gaming before 9 PM", isCompleted = false, streak = 2),
            HabitEntity(name = "Sleep before 12:30 AM", isCompleted = false, streak = 0)
        )
        defaultHabits.forEach { repository.insertHabit(it) }

        // 2. Default Intents
        val defaultIntents = listOf(
            IntentEntity(name = "Eat 4 eggs daily with milk", isCompleted = true),
            IntentEntity(name = "Sleep at 12 AM — no blue light after 11:30 PM", isCompleted = false),
            IntentEntity(name = "Study daily for cert (YT / Coursera / Udemy)", isCompleted = true),
            IntentEntity(name = "Work on IT project — push to GitHub", isCompleted = false),
            IntentEntity(name = "Watch meaningful YouTube (philosophy / language / knowledge)", isCompleted = true),
            IntentEntity(name = "Daily Arabic words — share to WhatsApp group", isCompleted = true),
            IntentEntity(name = "Daily Japanese words — share to WhatsApp group", isCompleted = false)
        )
        defaultIntents.forEach { repository.insertIntent(it) }

        // 3. Default Goals
        val goalIds = listOf(
            repository.insertGoal(GoalEntity(name = "CompTIA A+ cert", why = "IT career foundation", status = "ACTIVE")),
            repository.insertGoal(GoalEntity(name = "Improve sleep schedule", why = "More energy", status = "ACTIVE")),
            repository.insertGoal(GoalEntity(name = "Learn Arabic", why = "Culture & community", status = "ACTIVE")),
            repository.insertGoal(GoalEntity(name = "Learn Japanese", why = "Personal dream", status = "ACTIVE")),
            repository.insertGoal(GoalEntity(name = "Gain 10 KG by end of 2026", why = "Mass gainer + eating", status = "ACTIVE")),
            repository.insertGoal(GoalEntity(name = "Start YT gaming channel", why = "Creative outlet", status = "NEXT")),
            repository.insertGoal(GoalEntity(name = "Buy ESP32 beginner kit", why = "Start electronics", status = "NEXT")),
            repository.insertGoal(GoalEntity(name = "Machine Learning", why = "Future career path", status = "SOMEDAY"))
        )

        // Add some default points/logs for Active Goals
        // CompTIA A+ points (default total: 8.5)
        repository.insertPointLog(PointLogEntity(goalId = goalIds[0], activity = "Watched Professor Messer videos", hours = 1.5f))
        repository.insertPointLog(PointLogEntity(goalId = goalIds[0], activity = "Read CompTIA study guide", hours = 2f))
        repository.insertPointLog(PointLogEntity(goalId = goalIds[0], activity = "Bonus Points: Passed practice test", hours = 5f))

        // Improve sleep schedule (default total: 10)
        repository.insertPointLog(PointLogEntity(goalId = goalIds[1], activity = "Slept exactly at 11:45 PM", hours = 3f))
        repository.insertPointLog(PointLogEntity(goalId = goalIds[1], activity = "Bonus Points: Avoided screens for 3 days", hours = 7f))

        // Learn Arabic (default total: 5)
        repository.insertPointLog(PointLogEntity(goalId = goalIds[2], activity = "Studied core alphabet verbs", hours = 3f))
        repository.insertPointLog(PointLogEntity(goalId = goalIds[2], activity = "Shared Arabic vocab cards with group", hours = 2f))

        // Learn Japanese (default total: 3)
        repository.insertPointLog(PointLogEntity(goalId = goalIds[3], activity = "Memorized Hiragana set A-K", hours = 3f))

        // 4. Default Learning items
        val defaultLearning = listOf(
            LearningEntity(name = "CompTIA A+", subtext = "Core 1 & Core 2 — 30 min/day", category = "IT", status = "ACTIVE"),
            LearningEntity(name = "ESP32 / Electronics", subtext = "Kit not bought yet", category = "IT", status = "NEXT"),
            LearningEntity(name = "Data Center Engineering", subtext = "Career goal — TBD", category = "IT", status = "SOMEDAY"),
            LearningEntity(name = "Arabic", subtext = "Daily words via WhatsApp", category = "LANGUAGES", status = "ACTIVE"),
            LearningEntity(name = "Japanese", subtext = "Daily words via WhatsApp", category = "LANGUAGES", status = "ACTIVE"),
            LearningEntity(name = "Google IT Support", subtext = "Coursera — in progress", category = "COURSES", status = "ACTIVE"),
            LearningEntity(name = "Machine Learning", subtext = "After A+ cert", category = "IT", status = "SOMEDAY")
        )
        defaultLearning.forEach { repository.insertLearning(it) }

        // 5. Default Words
        val defaultWords = listOf(
            WordEntity(word = "مرحبا (Marhaban)", meaning = "Hello", category = "ARABIC"),
            WordEntity(word = "شكرا (Shukran)", meaning = "Thank you", category = "ARABIC"),
            WordEntity(word = "صباح الخير (Sabah al-khair)", meaning = "Good morning", category = "ARABIC"),
            WordEntity(word = "ありがとう (Arigatou)", meaning = "Thank you", category = "JAPANESE"),
            WordEntity(word = "こんにちは (Konnichiwa)", meaning = "Hello", category = "JAPANESE"),
            WordEntity(word = "おはよう (Ohayou)", meaning = "Good morning", category = "JAPANESE"),
            WordEntity(word = "Perseverance", meaning = "Continued effort despite difficulty", category = "ENGLISH"),
            WordEntity(word = "Discipline", meaning = "Training oneself to follow rules and behavior", category = "ENGLISH")
        )
        defaultWords.forEach { repository.insertWord(it) }

        // 6. Default Sleep Logs (Last week)
        val defaultSleeps = listOf(
            SleepLogEntity(dateString = "2026-05-31", sleptAt = "01:10", wokeUp = "05:30", hoursSlept = 4.3f), // today
            SleepLogEntity(dateString = "2026-05-30", sleptAt = "01:30", wokeUp = "05:30", hoursSlept = 4.0f),
            SleepLogEntity(dateString = "2026-05-29", sleptAt = "11:45", wokeUp = "06:45", hoursSlept = 7.0f),
            SleepLogEntity(dateString = "2026-05-28", sleptAt = "00:30", wokeUp = "05:30", hoursSlept = 5.0f),
            SleepLogEntity(dateString = "2026-05-27", sleptAt = "01:00", wokeUp = "05:30", hoursSlept = 4.5f),
            SleepLogEntity(dateString = "2026-05-26", sleptAt = "23:50", wokeUp = "07:15", hoursSlept = 7.4f),
            SleepLogEntity(dateString = "2026-05-25", sleptAt = "11:40", wokeUp = "05:30", hoursSlept = 5.8f)
        )
        defaultSleeps.forEach { repository.insertSleepLog(it) }

        // 7. Default Reflection
        repository.insertReflection(
            ReflectionEntity(
                weekKey = "2026-W22",
                reflection = "Target 12 AM sleep is tough because of cooking and hobbies late night, but feeling productive daily. Averaging 5.3 hours, need to fix this deficit next week.",
                intention = "Sleep before 12:15 AM every single evening. Start winding down by avoiding blue light by 11:30."
            )
        )

        // 8. Default Money Accounts (from Screenshot 2)
        val defaultAccounts = listOf(
            MoneyAccountEntity(name = "Cash", type = "CASH", balance = 590.0),
            MoneyAccountEntity(name = "Saving box", type = "CASH", balance = 1200.0),
            MoneyAccountEntity(name = "Cash in wallet", type = "CASH", balance = 590.0),
            MoneyAccountEntity(name = "Appi adcb", type = "BANK", balance = 2731.0),
            MoneyAccountEntity(name = "My Adcb", type = "BANK", balance = 1517.0),
            MoneyAccountEntity(name = "Card", type = "CARD", balance = 0.0)
        )
        defaultAccounts.forEach { repository.insertMoneyAccount(it) }

        // 9. Default Categories (customizable)
        val defaultExpenseCats = listOf(
            "Food", "Baqer", "Transport", "Eva VR", "emi TV", "LuLu", "wifi",
            "Tabby", "Tamara", "Appi", "haircut", "cleaners cash", "theatre",
            "noon", "recharge", "Marvel rivals", "Apparel", "Amazon", "watchman eidi",
            "Anemania Abu dhabi", "khwaja bhai", "Sanjay", "Earpods", "Bluetooth dongle",
            "Samosa office", "Monitor", "Thermostat ac plug", "Charoli", "Iftar",
            "Cake appi", "To her", "Abdullah Jafza party", "Debt", "Other", "appi",
            "Sans bus", "store", "genshin impact", "kq 200", "Aly", "Temu"
        )
        defaultExpenseCats.forEach { repository.insertCategory(CategoryEntity(name = it, type = "EXPENSE")) }

        val defaultIncomeCats = listOf("Salary", "Freelance", "To balance the red", "Debt", "Modified Bal.")
        defaultIncomeCats.forEach { repository.insertCategory(CategoryEntity(name = it, type = "INCOME")) }

        // 10. Default Money Transactions (Imported from Excel — Money Manager_01-06-2026)
        val defaultTx = listOf(
            TransactionEntity(type = "EXPENSE", amount = 7.0, category = "Food", account = "My Adcb", dateString = "2026-05-29", timeString = "07:31 pm", note = "Beverages"),
            TransactionEntity(type = "EXPENSE", amount = 3.0, category = "Baqer", account = "My Adcb", dateString = "2026-05-29", timeString = "01:32 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 68.0, category = "Baqer", account = "My Adcb", dateString = "2026-05-29", timeString = "01:17 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 11.0, category = "Baqer", account = "My Adcb", dateString = "2026-05-29", timeString = "01:16 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 20.0, category = "Transport", account = "My Adcb", dateString = "2026-05-28", timeString = "10:57 pm", note = "Bus"),
            TransactionEntity(type = "EXPENSE", amount = 31.0, category = "Food", account = "My Adcb", dateString = "2026-05-28", timeString = "09:26 pm", note = "Eating out"),
            TransactionEntity(type = "EXPENSE", amount = 125.0, category = "Eva VR", account = "My Adcb", dateString = "2026-05-28", timeString = "09:09 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 36.0, category = "Food", account = "My Adcb", dateString = "2026-05-28", timeString = "07:52 pm", note = "Eating out"),
            TransactionEntity(type = "EXPENSE", amount = 270.0, category = "emi TV", account = "My Adcb", dateString = "2026-05-28", timeString = "07:34 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 46.0, category = "Food", account = "My Adcb", dateString = "2026-05-27", timeString = "05:39 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 236.0, category = "LuLu", account = "My Adcb", dateString = "2026-05-27", timeString = "05:18 pm", note = ""),
            TransactionEntity(type = "TRANSFER", amount = 100.0, category = "Saving box", account = "Cash in wallet", toAccount = "Saving box", dateString = "2026-05-27", timeString = "06:58 am", note = ""),
            TransactionEntity(type = "TRANSFER", amount = 500.0, category = "Cash in wallet", account = "My Adcb", toAccount = "Cash in wallet", dateString = "2026-05-27", timeString = "06:19 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 7.0, category = "Food", account = "My Adcb", dateString = "2026-05-27", timeString = "06:19 am", note = "Beverages"),
            TransactionEntity(type = "EXPENSE", amount = 24.0, category = "Food", account = "My Adcb", dateString = "2026-05-26", timeString = "11:12 pm", note = "Dinner"),
            TransactionEntity(type = "EXPENSE", amount = 12.0, category = "Food", account = "My Adcb", dateString = "2026-05-26", timeString = "11:12 pm", note = "Eating out"),
            TransactionEntity(type = "EXPENSE", amount = 208.0, category = "wifi", account = "My Adcb", dateString = "2026-05-24", timeString = "07:13 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 44.0, category = "Tabby", account = "My Adcb", dateString = "2026-05-24", timeString = "07:10 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 84.0, category = "Tamara", account = "My Adcb", dateString = "2026-05-24", timeString = "07:09 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 2500.0, category = "Appi", account = "My Adcb", dateString = "2026-05-24", timeString = "07:06 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 51.0, category = "Food", account = "My Adcb", dateString = "2026-05-23", timeString = "08:52 pm", note = "Dinner"),
            TransactionEntity(type = "INCOME", amount = 6000.0, category = "Salary", account = "My Adcb", dateString = "2026-05-22", timeString = "06:59 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 4.0, category = "Baqer", account = "My Adcb", dateString = "2026-05-21", timeString = "09:25 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 20.0, category = "Baqer", account = "My Adcb", dateString = "2026-05-20", timeString = "09:11 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 32.0, category = "Baqer", account = "Appi adcb", dateString = "2026-05-19", timeString = "07:43 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 6.0, category = "Food", account = "My Adcb", dateString = "2026-05-18", timeString = "12:32 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 15.0, category = "haircut", account = "My Adcb", dateString = "2026-05-17", timeString = "07:19 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 60.0, category = "cleaners cash", account = "Cash in wallet", dateString = "2026-05-16", timeString = "03:03 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 14.0, category = "Food", account = "My Adcb", dateString = "2026-05-15", timeString = "07:30 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 4.0, category = "Food", account = "My Adcb", dateString = "2026-05-15", timeString = "02:00 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 28.0, category = "Food", account = "My Adcb", dateString = "2026-05-15", timeString = "01:24 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 8.0, category = "Food", account = "My Adcb", dateString = "2026-05-15", timeString = "10:18 am", note = "breakfast"),
            TransactionEntity(type = "EXPENSE", amount = 26.0, category = "Baqer", account = "My Adcb", dateString = "2026-05-13", timeString = "07:42 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 6.0, category = "Food", account = "My Adcb", dateString = "2026-05-13", timeString = "10:11 am", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 16.0, category = "Food", account = "My Adcb", dateString = "2026-05-13", timeString = "10:09 am", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 14.0, category = "Food", account = "My Adcb", dateString = "2026-05-12", timeString = "07:36 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 28.0, category = "Food", account = "My Adcb", dateString = "2026-05-11", timeString = "08:40 pm", note = "Dinner"),
            TransactionEntity(type = "EXPENSE", amount = 15.0, category = "Food", account = "My Adcb", dateString = "2026-05-11", timeString = "08:31 pm", note = "Dinner"),
            TransactionEntity(type = "EXPENSE", amount = 9.0, category = "Food", account = "My Adcb", dateString = "2026-05-10", timeString = "07:27 pm", note = "Eating out"),
            TransactionEntity(type = "EXPENSE", amount = 13.0, category = "Food", account = "My Adcb", dateString = "2026-05-10", timeString = "04:20 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 28.0, category = "Food", account = "My Adcb", dateString = "2026-05-10", timeString = "03:54 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 70.0, category = "theatre", account = "My Adcb", dateString = "2026-05-09", timeString = "08:40 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 25.0, category = "Baqer", account = "My Adcb", dateString = "2026-05-09", timeString = "02:26 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 33.0, category = "Food", account = "My Adcb", dateString = "2026-05-08", timeString = "10:54 pm", note = "Dinner"),
            TransactionEntity(type = "EXPENSE", amount = 26.0, category = "Food", account = "My Adcb", dateString = "2026-05-08", timeString = "09:49 am", note = "breakfast"),
            TransactionEntity(type = "INCOME", amount = 200.0, category = "appi", account = "My Adcb", dateString = "2026-05-08", timeString = "09:48 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 12.0, category = "Food", account = "My Adcb", dateString = "2026-05-07", timeString = "10:01 pm", note = "Dinner"),
            TransactionEntity(type = "EXPENSE", amount = 8.0, category = "Food", account = "My Adcb", dateString = "2026-05-07", timeString = "01:14 pm", note = "Lunch"),
            TransactionEntity(type = "INCOME", amount = 152.0, category = "Modified Bal.", account = "My Adcb", dateString = "2026-05-06", timeString = "09:59 am", note = "Difference"),
            TransactionEntity(type = "EXPENSE", amount = 48.0, category = "Other", account = "My Adcb", dateString = "2026-05-06", timeString = "09:56 am", note = "Food and grocery"),
            TransactionEntity(type = "EXPENSE", amount = 200.0, category = "cleaners cash", account = "Cash in wallet", dateString = "2026-05-06", timeString = "09:54 am", note = "And other stuff"),
            TransactionEntity(type = "TRANSFER", amount = 500.0, category = "Saving box", account = "Cash in wallet", toAccount = "Saving box", dateString = "2026-05-06", timeString = "09:53 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 39.0, category = "Baqer", account = "My Adcb", dateString = "2026-05-05", timeString = "07:33 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 9.0, category = "Food", account = "My Adcb", dateString = "2026-05-05", timeString = "01:11 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 28.0, category = "Food", account = "My Adcb", dateString = "2026-05-04", timeString = "07:44 pm", note = "Dinner"),
            TransactionEntity(type = "EXPENSE", amount = 8.0, category = "Food", account = "My Adcb", dateString = "2026-05-04", timeString = "01:15 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 14.0, category = "Food", account = "My Adcb", dateString = "2026-05-02", timeString = "07:53 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 20.0, category = "Baqer", account = "My Adcb", dateString = "2026-05-02", timeString = "02:39 pm", note = ""),
            TransactionEntity(type = "TRANSFER", amount = 300.0, category = "Cash in wallet", account = "My Adcb", toAccount = "Cash in wallet", dateString = "2026-05-02", timeString = "02:36 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 1200.0, category = "Sans bus", account = "My Adcb", dateString = "2026-05-02", timeString = "02:33 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 210.0, category = "wifi", account = "My Adcb", dateString = "2026-05-02", timeString = "02:24 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 14.0, category = "Baqer", account = "My Adcb", dateString = "2026-05-02", timeString = "11:27 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 66.0, category = "noon", account = "My Adcb", dateString = "2026-05-01", timeString = "09:01 pm", note = "Fan"),
            TransactionEntity(type = "EXPENSE", amount = 7.0, category = "Food", account = "My Adcb", dateString = "2026-05-01", timeString = "01:32 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 3.0, category = "Food", account = "My Adcb", dateString = "2026-05-01", timeString = "10:15 am", note = "Beverages"),
            TransactionEntity(type = "EXPENSE", amount = 10.0, category = "Food", account = "My Adcb", dateString = "2026-05-01", timeString = "09:57 am", note = "breakfast"),
            TransactionEntity(type = "EXPENSE", amount = 8.0, category = "Food", account = "My Adcb", dateString = "2026-04-30", timeString = "03:18 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 10.0, category = "Food", account = "My Adcb", dateString = "2026-04-30", timeString = "01:17 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 18.0, category = "Food", account = "My Adcb", dateString = "2026-04-29", timeString = "09:41 pm", note = "Dinner"),
            TransactionEntity(type = "EXPENSE", amount = 16.0, category = "Food", account = "My Adcb", dateString = "2026-04-29", timeString = "11:03 am", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 4000.0, category = "Appi", account = "My Adcb", dateString = "2026-04-29", timeString = "08:29 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 30.0, category = "recharge", account = "My Adcb", dateString = "2026-04-29", timeString = "08:23 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 8.0, category = "Food", account = "My Adcb", dateString = "2026-04-28", timeString = "10:29 pm", note = "Dinner"),
            TransactionEntity(type = "EXPENSE", amount = 25.0, category = "Food", account = "My Adcb", dateString = "2026-04-28", timeString = "01:39 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 7.0, category = "Food", account = "My Adcb", dateString = "2026-04-28", timeString = "11:23 am", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 45.0, category = "Tabby", account = "My Adcb", dateString = "2026-04-27", timeString = "07:20 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 84.0, category = "Tamara", account = "My Adcb", dateString = "2026-04-27", timeString = "07:19 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 73.0, category = "Tamara", account = "My Adcb", dateString = "2026-04-27", timeString = "06:57 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 6.0, category = "Food", account = "My Adcb", dateString = "2026-04-27", timeString = "01:24 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 10.0, category = "Food", account = "My Adcb", dateString = "2026-04-27", timeString = "01:21 pm", note = "Lunch"),
            TransactionEntity(type = "INCOME", amount = 6000.0, category = "Salary", account = "My Adcb", dateString = "2026-04-27", timeString = "11:59 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 135.0, category = "Tabby", account = "My Adcb", dateString = "2026-04-27", timeString = "11:58 am", note = "Abu dhabi comic con"),
            TransactionEntity(type = "EXPENSE", amount = 54.0, category = "Baqer", account = "My Adcb", dateString = "2026-04-26", timeString = "06:51 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 13.0, category = "Food", account = "My Adcb", dateString = "2026-04-26", timeString = "03:30 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 30.0, category = "Food", account = "My Adcb", dateString = "2026-04-26", timeString = "03:26 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 76.0, category = "Food", account = "My Adcb", dateString = "2026-04-25", timeString = "04:14 pm", note = "Lunch - Mcd"),
            TransactionEntity(type = "EXPENSE", amount = 40.0, category = "Marvel rivals", account = "My Adcb", dateString = "2026-04-25", timeString = "12:20 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 12.0, category = "Food", account = "My Adcb", dateString = "2026-04-24", timeString = "01:23 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 24.0, category = "Food", account = "My Adcb", dateString = "2026-04-22", timeString = "09:46 pm", note = "Lunch"),
            TransactionEntity(type = "TRANSFER", amount = 600.0, category = "Cash in wallet", account = "My Adcb", toAccount = "Cash in wallet", dateString = "2026-04-22", timeString = "09:26 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 12.0, category = "Food", account = "My Adcb", dateString = "2026-04-22", timeString = "10:35 am", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 7.0, category = "Food", account = "My Adcb", dateString = "2026-04-22", timeString = "10:33 am", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 350.0, category = "Apparel", account = "My Adcb", dateString = "2026-04-21", timeString = "10:22 pm", note = "Clothing"),
            TransactionEntity(type = "EXPENSE", amount = 21.0, category = "Food", account = "My Adcb", dateString = "2026-04-21", timeString = "12:34 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 6.0, category = "Food", account = "My Adcb", dateString = "2026-04-21", timeString = "10:00 am", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 5.0, category = "Baqer", account = "My Adcb", dateString = "2026-04-18", timeString = "10:41 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 14.0, category = "Food", account = "My Adcb", dateString = "2026-04-17", timeString = "11:19 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 15.0, category = "Food", account = "My Adcb", dateString = "2026-04-17", timeString = "07:29 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 23.0, category = "Food", account = "My Adcb", dateString = "2026-04-17", timeString = "04:03 pm", note = "Lunch - Sanjay"),
            TransactionEntity(type = "EXPENSE", amount = 10.0, category = "Food", account = "My Adcb", dateString = "2026-04-17", timeString = "09:58 am", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 8.0, category = "Food", account = "My Adcb", dateString = "2026-04-16", timeString = "11:08 am", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 13.0, category = "Food", account = "My Adcb", dateString = "2026-04-16", timeString = "11:07 am", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 40.0, category = "Tamara", account = "My Adcb", dateString = "2026-04-16", timeString = "10:30 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 4.0, category = "Food", account = "My Adcb", dateString = "2026-04-16", timeString = "09:03 am", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 20.0, category = "Food", account = "My Adcb", dateString = "2026-04-15", timeString = "08:31 am", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 4.0, category = "Food", account = "My Adcb", dateString = "2026-04-14", timeString = "12:57 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 45.0, category = "Tabby", account = "My Adcb", dateString = "2026-04-13", timeString = "11:04 pm", note = "Earpods"),
            TransactionEntity(type = "EXPENSE", amount = 34.0, category = "Baqer", account = "My Adcb", dateString = "2026-04-11", timeString = "08:00 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 16.0, category = "Food", account = "My Adcb", dateString = "2026-04-10", timeString = "08:34 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 20.0, category = "Food", account = "My Adcb", dateString = "2026-04-10", timeString = "08:33 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 40.0, category = "Food", account = "My Adcb", dateString = "2026-04-09", timeString = "10:00 pm", note = "Dinner"),
            TransactionEntity(type = "EXPENSE", amount = 24.0, category = "Food", account = "My Adcb", dateString = "2026-04-08", timeString = "09:39 pm", note = "Dinner"),
            TransactionEntity(type = "EXPENSE", amount = 13.0, category = "Baqer", account = "My Adcb", dateString = "2026-04-05", timeString = "06:54 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 25.0, category = "store", account = "My Adcb", dateString = "2026-04-04", timeString = "10:52 pm", note = "Bluetooth dongle"),
            TransactionEntity(type = "EXPENSE", amount = 50.0, category = "Food", account = "Cash in wallet", dateString = "2026-04-03", timeString = "08:52 pm", note = "Samosa office"),
            TransactionEntity(type = "EXPENSE", amount = 6.0, category = "Food", account = "My Adcb", dateString = "2026-04-03", timeString = "01:43 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 15.0, category = "Food", account = "My Adcb", dateString = "2026-04-03", timeString = "01:43 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 6.0, category = "Food", account = "My Adcb", dateString = "2026-04-02", timeString = "11:19 am", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 8.0, category = "Food", account = "My Adcb", dateString = "2026-04-02", timeString = "11:18 am", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 4.0, category = "Baqer", account = "My Adcb", dateString = "2026-03-31", timeString = "09:39 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 15.0, category = "Food", account = "My Adcb", dateString = "2026-03-31", timeString = "05:59 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 84.0, category = "Tamara", account = "My Adcb", dateString = "2026-03-31", timeString = "12:06 am", note = "Monitor"),
            TransactionEntity(type = "EXPENSE", amount = 22.0, category = "Apparel", account = "My Adcb", dateString = "2026-03-30", timeString = "01:49 pm", note = "Laundry"),
            TransactionEntity(type = "EXPENSE", amount = 3507.0, category = "Appi", account = "My Adcb", dateString = "2026-03-30", timeString = "11:14 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 28.0, category = "Food", account = "My Adcb", dateString = "2026-03-28", timeString = "01:52 pm", note = "Lunch"),
            TransactionEntity(type = "EXPENSE", amount = 171.0, category = "Amazon", account = "My Adcb", dateString = "2026-03-27", timeString = "11:59 am", note = "Thermostat ac plug"),
            TransactionEntity(type = "EXPENSE", amount = 5.0, category = "Baqer", account = "My Adcb", dateString = "2026-03-27", timeString = "11:20 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 169.0, category = "Food", account = "Cash in wallet", dateString = "2026-03-27", timeString = "11:00 am", note = ""),
            TransactionEntity(type = "TRANSFER", amount = 500.0, category = "Saving box", account = "Cash in wallet", toAccount = "Saving box", dateString = "2026-03-27", timeString = "10:59 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 73.0, category = "Tamara", account = "My Adcb", dateString = "2026-03-27", timeString = "10:37 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 135.0, category = "Tabby", account = "My Adcb", dateString = "2026-03-27", timeString = "10:36 am", note = "Abu dhabi comic con"),
            TransactionEntity(type = "INCOME", amount = 6000.0, category = "Salary", account = "My Adcb", dateString = "2026-03-27", timeString = "10:35 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 39.0, category = "Food", account = "Appi adcb", dateString = "2026-03-26", timeString = "08:25 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 18.0, category = "Baqer", account = "Cash in wallet", dateString = "2026-03-22", timeString = "07:00 pm", note = "Charoli"),
            TransactionEntity(type = "EXPENSE", amount = 3.0, category = "Food", account = "My Adcb", dateString = "2026-03-21", timeString = "10:15 pm", note = "Water"),
            TransactionEntity(type = "EXPENSE", amount = 105.0, category = "watchman eidi", account = "Cash in wallet", dateString = "2026-03-21", timeString = "05:14 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 12.0, category = "Food", account = "My Adcb", dateString = "2026-03-20", timeString = "07:43 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 42.0, category = "Baqer", account = "My Adcb", dateString = "2026-03-20", timeString = "07:42 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 48.0, category = "Food", account = "My Adcb", dateString = "2026-03-17", timeString = "04:58 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 20.0, category = "Baqer", account = "My Adcb", dateString = "2026-03-16", timeString = "09:13 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 70.0, category = "Food", account = "Cash in wallet", dateString = "2026-03-16", timeString = "05:40 pm", note = "Water - Water"),
            TransactionEntity(type = "EXPENSE", amount = 20.0, category = "Food", account = "My Adcb", dateString = "2026-03-16", timeString = "05:08 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 15.0, category = "Transport", account = "My Adcb", dateString = "2026-03-16", timeString = "04:47 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 58.0, category = "Food", account = "My Adcb", dateString = "2026-03-16", timeString = "04:47 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 56.0, category = "Food", account = "My Adcb", dateString = "2026-03-13", timeString = "05:52 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 64.0, category = "Food", account = "My Adcb", dateString = "2026-03-10", timeString = "06:01 pm", note = "Mcd"),
            TransactionEntity(type = "EXPENSE", amount = 209.0, category = "wifi", account = "My Adcb", dateString = "2026-03-09", timeString = "04:59 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 45.0, category = "Baqer", account = "My Adcb", dateString = "2026-03-08", timeString = "06:17 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 56.0, category = "Food", account = "My Adcb", dateString = "2026-03-06", timeString = "06:19 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 1100.0, category = "Sans bus", account = "Cash in wallet", dateString = "2026-03-06", timeString = "03:20 pm", note = ""),
            TransactionEntity(type = "TRANSFER", amount = 2000.0, category = "Cash in wallet", account = "My Adcb", toAccount = "Cash in wallet", dateString = "2026-03-05", timeString = "09:14 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 126.0, category = "Baqer", account = "My Adcb", dateString = "2026-03-05", timeString = "09:13 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 16.0, category = "Transport", account = "My Adcb", dateString = "2026-03-03", timeString = "08:33 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 116.0, category = "Food", account = "My Adcb", dateString = "2026-03-03", timeString = "07:30 pm", note = "Iftar"),
            TransactionEntity(type = "EXPENSE", amount = 11.0, category = "Food", account = "My Adcb", dateString = "2026-03-02", timeString = "06:09 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 8.0, category = "Food", account = "My Adcb", dateString = "2026-03-02", timeString = "06:04 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 20.0, category = "genshin impact", account = "My Adcb", dateString = "2026-03-02", timeString = "10:07 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 18.0, category = "Marvel rivals", account = "My Adcb", dateString = "2026-03-02", timeString = "01:19 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 20.0, category = "Marvel rivals", account = "My Adcb", dateString = "2026-03-02", timeString = "01:14 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 37.0, category = "Marvel rivals", account = "My Adcb", dateString = "2026-03-02", timeString = "01:14 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 47.0, category = "Baqer", account = "My Adcb", dateString = "2026-02-27", timeString = "11:29 pm", note = "Cake appi"),
            TransactionEntity(type = "EXPENSE", amount = 136.0, category = "Tabby", account = "My Adcb", dateString = "2026-02-27", timeString = "04:42 pm", note = "Abu dhabi comic con"),
            TransactionEntity(type = "EXPENSE", amount = 2500.0, category = "Appi", account = "My Adcb", dateString = "2026-02-27", timeString = "04:29 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 73.0, category = "Tamara", account = "My Adcb", dateString = "2026-02-27", timeString = "04:08 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 50.0, category = "recharge", account = "My Adcb", dateString = "2026-02-27", timeString = "04:03 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 200.0, category = "kq 200", account = "My Adcb", dateString = "2026-02-27", timeString = "03:01 pm", note = ""),
            TransactionEntity(type = "INCOME", amount = 6000.0, category = "Salary", account = "My Adcb", dateString = "2026-02-27", timeString = "03:01 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 4.0, category = "Baqer", account = "My Adcb", dateString = "2026-02-25", timeString = "09:06 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 17.0, category = "Baqer", account = "My Adcb", dateString = "2026-02-22", timeString = "05:38 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 79.0, category = "Baqer", account = "My Adcb", dateString = "2026-02-21", timeString = "05:09 pm", note = ""),
            TransactionEntity(type = "TRANSFER", amount = 50.0, category = "My Adcb", account = "Appi adcb", toAccount = "My Adcb", dateString = "2026-02-21", timeString = "04:45 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 300.0, category = "cleaners cash", account = "My Adcb", dateString = "2026-02-21", timeString = "12:44 pm", note = "To her"),
            TransactionEntity(type = "TRANSFER", amount = 400.0, category = "My Adcb", account = "Appi adcb", toAccount = "My Adcb", dateString = "2026-02-21", timeString = "12:44 pm", note = "For cleaners"),
            TransactionEntity(type = "INCOME", amount = 14.0, category = "Other", account = "My Adcb", dateString = "2026-02-21", timeString = "12:43 pm", note = "To balance the red"),
            TransactionEntity(type = "EXPENSE", amount = 27.0, category = "Food", account = "My Adcb", dateString = "2026-02-17", timeString = "02:15 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 20.0, category = "Food", account = "My Adcb", dateString = "2026-02-15", timeString = "06:58 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 37.0, category = "Food", account = "My Adcb", dateString = "2026-02-15", timeString = "06:56 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 30.0, category = "Transport", account = "Cash in wallet", dateString = "2026-02-15", timeString = "04:16 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 15.0, category = "Transport", account = "Cash in wallet", dateString = "2026-02-15", timeString = "04:15 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 5.0, category = "Other", account = "My Adcb", dateString = "2026-02-14", timeString = "09:39 pm", note = "Anemania"),
            TransactionEntity(type = "EXPENSE", amount = 36.0, category = "Food", account = "My Adcb", dateString = "2026-02-14", timeString = "09:39 pm", note = "Anemania"),
            TransactionEntity(type = "EXPENSE", amount = 66.0, category = "Food", account = "My Adcb", dateString = "2026-02-14", timeString = "05:10 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 30.0, category = "Transport", account = "Cash in wallet", dateString = "2026-02-14", timeString = "01:31 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 3.0, category = "Food", account = "My Adcb", dateString = "2026-02-14", timeString = "01:13 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 33.0, category = "Transport", account = "Cash in wallet", dateString = "2026-02-14", timeString = "10:51 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 30.0, category = "Transport", account = "Cash in wallet", dateString = "2026-02-14", timeString = "10:50 am", note = ""),
            TransactionEntity(type = "TRANSFER", amount = 200.0, category = "Cash in wallet", account = "Appi adcb", toAccount = "Cash in wallet", dateString = "2026-02-14", timeString = "10:50 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 26.0, category = "Food", account = "My Adcb", dateString = "2026-02-13", timeString = "02:25 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 14.0, category = "Food", account = "My Adcb", dateString = "2026-02-12", timeString = "08:32 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 8.0, category = "Food", account = "My Adcb", dateString = "2026-02-12", timeString = "01:19 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 12.0, category = "Food", account = "My Adcb", dateString = "2026-02-11", timeString = "08:01 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 210.0, category = "Anemania Abu dhabi", account = "My Adcb", dateString = "2026-02-11", timeString = "06:28 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 600.0, category = "Sans bus", account = "Saving box", dateString = "2026-02-11", timeString = "10:21 am", note = ""),
            TransactionEntity(type = "INCOME", amount = 200.0, category = "khwaja bhai", account = "Saving box", dateString = "2026-02-11", timeString = "10:20 am", note = "Debt"),
            TransactionEntity(type = "EXPENSE", amount = 7.0, category = "Food", account = "My Adcb", dateString = "2026-02-11", timeString = "10:14 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 8.0, category = "Food", account = "My Adcb", dateString = "2026-02-11", timeString = "10:13 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 22.0, category = "Food", account = "My Adcb", dateString = "2026-02-10", timeString = "07:55 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 10.0, category = "Food", account = "My Adcb", dateString = "2026-02-10", timeString = "12:25 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 12.0, category = "Food", account = "My Adcb", dateString = "2026-02-09", timeString = "07:49 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 8.0, category = "Food", account = "My Adcb", dateString = "2026-02-09", timeString = "12:47 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 8.0, category = "Baqer", account = "My Adcb", dateString = "2026-02-08", timeString = "08:15 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 10.0, category = "recharge", account = "My Adcb", dateString = "2026-02-08", timeString = "08:15 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 40.0, category = "Food", account = "My Adcb", dateString = "2026-02-08", timeString = "08:15 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 4.0, category = "Food", account = "My Adcb", dateString = "2026-02-06", timeString = "07:45 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 4.0, category = "Food", account = "My Adcb", dateString = "2026-02-06", timeString = "07:58 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 199.0, category = "Food", account = "My Adcb", dateString = "2026-02-05", timeString = "12:56 pm", note = "Abdullah Jafza party"),
            TransactionEntity(type = "EXPENSE", amount = 4.0, category = "Food", account = "My Adcb", dateString = "2026-02-04", timeString = "01:57 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 209.0, category = "wifi", account = "My Adcb", dateString = "2026-02-04", timeString = "12:39 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 4.0, category = "Food", account = "My Adcb", dateString = "2026-02-03", timeString = "01:47 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 4.0, category = "Food", account = "My Adcb", dateString = "2026-02-02", timeString = "01:30 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 8.0, category = "Food", account = "My Adcb", dateString = "2026-02-02", timeString = "12:53 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 88.0, category = "Baqer", account = "My Adcb", dateString = "2026-02-01", timeString = "01:33 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 43.0, category = "Food", account = "My Adcb", dateString = "2026-01-31", timeString = "09:15 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 34.0, category = "Marvel rivals", account = "My Adcb", dateString = "2026-01-31", timeString = "01:18 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 23.0, category = "Food", account = "My Adcb", dateString = "2026-01-30", timeString = "08:43 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 166.0, category = "Food", account = "My Adcb", dateString = "2026-01-30", timeString = "08:39 pm", note = "Baqer"),
            TransactionEntity(type = "EXPENSE", amount = 50.0, category = "Food", account = "My Adcb", dateString = "2026-01-30", timeString = "01:28 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 37.0, category = "Food", account = "My Adcb", dateString = "2026-01-29", timeString = "08:05 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 40.0, category = "Aly", account = "My Adcb", dateString = "2026-01-29", timeString = "08:05 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 8.0, category = "Food", account = "My Adcb", dateString = "2026-01-29", timeString = "01:13 pm", note = ""),
            TransactionEntity(type = "TRANSFER", amount = 3500.0, category = "Appi adcb", account = "My Adcb", toAccount = "Appi adcb", dateString = "2026-01-29", timeString = "11:22 am", note = ""),
            TransactionEntity(type = "TRANSFER", amount = 5000.0, category = "My Adcb", account = "Appi adcb", toAccount = "My Adcb", dateString = "2026-01-29", timeString = "11:21 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 285.0, category = "Aly", account = "Appi adcb", dateString = "2026-01-28", timeString = "10:32 pm", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 73.0, category = "Tamara", account = "Appi adcb", dateString = "2026-01-28", timeString = "07:23 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 140.0, category = "Temu", account = "Appi adcb", dateString = "2026-01-28", timeString = "07:22 am", note = ""),
            TransactionEntity(type = "EXPENSE", amount = 50.0, category = "recharge", account = "Appi adcb", dateString = "2026-01-28", timeString = "07:21 am", note = ""),
            TransactionEntity(type = "TRANSFER", amount = 500.0, category = "Saving box", account = "Appi adcb", toAccount = "Saving box", dateString = "2026-01-28", timeString = "07:21 am", note = ""),
            TransactionEntity(type = "INCOME", amount = 6000.0, category = "Salary", account = "Appi adcb", dateString = "2026-01-28", timeString = "07:21 am", note = ""),
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
