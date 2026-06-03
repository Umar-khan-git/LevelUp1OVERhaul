package com.example.data

import kotlinx.coroutines.flow.Flow

class DashboardRepository(private val dao: DashboardDao) {
    // Habits
    val allHabits: Flow<List<HabitEntity>> = dao.getAllHabits()
    suspend fun insertHabit(habit: HabitEntity) = dao.insertHabit(habit)
    suspend fun updateHabit(habit: HabitEntity) = dao.updateHabit(habit)
    suspend fun deleteHabit(habit: HabitEntity) = dao.deleteHabit(habit)
    suspend fun deleteHabitById(id: Long) = dao.deleteHabitById(id)

    // Daily Intents
    val allIntents: Flow<List<IntentEntity>> = dao.getAllIntents()
    suspend fun insertIntent(intent: IntentEntity) = dao.insertIntent(intent)
    suspend fun updateIntent(intent: IntentEntity) = dao.updateIntent(intent)
    suspend fun deleteIntent(intent: IntentEntity) = dao.deleteIntent(intent)
    suspend fun deleteIntentById(id: Long) = dao.deleteIntentById(id)

    // Goals
    val allGoals: Flow<List<GoalEntity>> = dao.getAllGoals()
    suspend fun insertGoal(goal: GoalEntity) = dao.insertGoal(goal)
    suspend fun updateGoal(goal: GoalEntity) = dao.updateGoal(goal)
    suspend fun deleteGoal(goal: GoalEntity) = dao.deleteGoal(goal)
    suspend fun deleteGoalById(id: Long) = dao.deleteGoalById(id)

    // Point Logs
    val allPointLogs: Flow<List<PointLogEntity>> = dao.getAllPointLogs()
    fun getPointLogsForGoal(goalId: Long): Flow<List<PointLogEntity>> = dao.getPointLogsForGoal(goalId)
    suspend fun insertPointLog(log: PointLogEntity) = dao.insertPointLog(log)
    suspend fun updatePointLog(log: PointLogEntity) = dao.updatePointLog(log)
    suspend fun deletePointLog(log: PointLogEntity) = dao.deletePointLog(log)
    suspend fun deletePointLogById(id: Long) = dao.deletePointLogById(id)

    // Learning Items
    val allLearningItems: Flow<List<LearningEntity>> = dao.getAllLearningItems()
    suspend fun insertLearning(learning: LearningEntity) = dao.insertLearning(learning)
    suspend fun updateLearning(learning: LearningEntity) = dao.updateLearning(learning)
    suspend fun deleteLearning(learning: LearningEntity) = dao.deleteLearning(learning)
    suspend fun deleteLearningById(id: Long) = dao.deleteLearningById(id)

    // Vocabulary
    val allWords: Flow<List<WordEntity>> = dao.getAllWords()
    fun getWordsByCategory(category: String): Flow<List<WordEntity>> = dao.getWordsByCategory(category)
    suspend fun insertWord(word: WordEntity) = dao.insertWord(word)
    suspend fun updateWord(word: WordEntity) = dao.updateWord(word)
    suspend fun deleteWord(word: WordEntity) = dao.deleteWord(word)
    suspend fun deleteWordById(id: Long) = dao.deleteWordById(id)

    // Sleep Logs
    val allSleepLogs: Flow<List<SleepLogEntity>> = dao.getAllSleepLogs()
    suspend fun insertSleepLog(log: SleepLogEntity) = dao.insertSleepLog(log)
    suspend fun updateSleepLog(log: SleepLogEntity) = dao.updateSleepLog(log)
    suspend fun deleteSleepLog(log: SleepLogEntity) = dao.deleteSleepLog(log)
    suspend fun deleteSleepLogByDate(dateString: String) = dao.deleteSleepLogByDate(dateString)

    // Reflections
    fun getReflectionByWeek(weekKey: String): Flow<ReflectionEntity?> = dao.getReflectionByWeekFlow(weekKey)
    suspend fun insertReflection(reflection: ReflectionEntity) = dao.insertReflection(reflection)

    // Money Manager Transactions
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    suspend fun insertTransaction(tx: TransactionEntity) = dao.insertTransaction(tx)
    suspend fun updateTransaction(tx: TransactionEntity) = dao.updateTransaction(tx)
    suspend fun deleteTransaction(tx: TransactionEntity) = dao.deleteTransaction(tx)
    suspend fun deleteTransactionById(id: Long) = dao.deleteTransactionById(id)

    // Money Manager Accounts
    val allMoneyAccounts: Flow<List<MoneyAccountEntity>> = dao.getAllMoneyAccounts()
    suspend fun getMoneyAccountsDirect(): List<MoneyAccountEntity> = dao.getMoneyAccountsDirect()
    suspend fun insertMoneyAccount(acc: MoneyAccountEntity) = dao.insertMoneyAccount(acc)
    suspend fun updateMoneyAccount(acc: MoneyAccountEntity) = dao.updateMoneyAccount(acc)
    suspend fun deleteMoneyAccount(acc: MoneyAccountEntity) = dao.deleteMoneyAccount(acc)
    suspend fun deleteMoneyAccountById(id: Long) = dao.deleteMoneyAccountById(id)

    // Money Manager Categories
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    suspend fun insertCategory(category: CategoryEntity) = dao.insertCategory(category)
    suspend fun deleteCategoryById(id: Long) = dao.deleteCategoryById(id)
}
