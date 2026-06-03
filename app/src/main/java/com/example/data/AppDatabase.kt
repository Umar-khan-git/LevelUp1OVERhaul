package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {
    // Habits
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)

    // Daily Intents
    @Query("SELECT * FROM daily_intents ORDER BY id ASC")
    fun getAllIntents(): Flow<List<IntentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntent(intent: IntentEntity)

    @Update
    suspend fun updateIntent(intent: IntentEntity)

    @Delete
    suspend fun deleteIntent(intent: IntentEntity)

    @Query("DELETE FROM daily_intents WHERE id = :id")
    suspend fun deleteIntentById(id: Long)

    // Goals
    @Query("SELECT * FROM goals ORDER BY id ASC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)

    // Point Logs
    @Query("SELECT * FROM point_logs ORDER BY dateAdded DESC")
    fun getAllPointLogs(): Flow<List<PointLogEntity>>

    @Query("SELECT * FROM point_logs WHERE goalId = :goalId ORDER BY dateAdded DESC")
    fun getPointLogsForGoal(goalId: Long): Flow<List<PointLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPointLog(log: PointLogEntity)

    @Update
    suspend fun updatePointLog(log: PointLogEntity)

    @Delete
    suspend fun deletePointLog(log: PointLogEntity)

    @Query("DELETE FROM point_logs WHERE id = :id")
    suspend fun deletePointLogById(id: Long)

    // Learning Items
    @Query("SELECT * FROM learning_items ORDER BY id ASC")
    fun getAllLearningItems(): Flow<List<LearningEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearning(learning: LearningEntity)

    @Update
    suspend fun updateLearning(learning: LearningEntity)

    @Delete
    suspend fun deleteLearning(learning: LearningEntity)

    @Query("DELETE FROM learning_items WHERE id = :id")
    suspend fun deleteLearningById(id: Long)

    // Vocabulary
    @Query("SELECT * FROM vocabulary ORDER BY id DESC")
    fun getAllWords(): Flow<List<WordEntity>>

    @Query("SELECT * FROM vocabulary WHERE category = :category ORDER BY id DESC")
    fun getWordsByCategory(category: String): Flow<List<WordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity)

    @Update
    suspend fun updateWord(word: WordEntity)

    @Delete
    suspend fun deleteWord(word: WordEntity)

    @Query("DELETE FROM vocabulary WHERE id = :id")
    suspend fun deleteWordById(id: Long)

    // Sleep Logs
    @Query("SELECT * FROM sleep_logs ORDER BY dateString DESC")
    fun getAllSleepLogs(): Flow<List<SleepLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepLog(log: SleepLogEntity)

    @Update
    suspend fun updateSleepLog(log: SleepLogEntity)

    @Delete
    suspend fun deleteSleepLog(log: SleepLogEntity)

    @Query("DELETE FROM sleep_logs WHERE dateString = :dateString")
    suspend fun deleteSleepLogByDate(dateString: String)

    // Weekly Reflections
    @Query("SELECT * FROM weekly_reflections WHERE weekKey = :weekKey LIMIT 1")
    suspend fun getReflectionByWeek(weekKey: String): ReflectionEntity?

    @Query("SELECT * FROM weekly_reflections WHERE weekKey = :weekKey")
    fun getReflectionByWeekFlow(weekKey: String): Flow<ReflectionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReflection(reflection: ReflectionEntity)

    // Money Manager Transactions
    @Query("SELECT * FROM money_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(tx: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(tx: TransactionEntity)

    @Query("DELETE FROM money_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    // Money Manager Accounts
    @Query("SELECT * FROM money_accounts ORDER BY id ASC")
    fun getAllMoneyAccounts(): Flow<List<MoneyAccountEntity>>

    @Query("SELECT * FROM money_accounts ORDER BY id ASC")
    suspend fun getMoneyAccountsDirect(): List<MoneyAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoneyAccount(acc: MoneyAccountEntity): Long

    @Update
    suspend fun updateMoneyAccount(acc: MoneyAccountEntity)

    @Delete
    suspend fun deleteMoneyAccount(acc: MoneyAccountEntity)

    @Query("DELETE FROM money_accounts WHERE id = :id")
    suspend fun deleteMoneyAccountById(id: Long)

    // Money Manager Categories
    @Query("SELECT * FROM money_categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("DELETE FROM money_categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)
}

@Database(
    entities = [
        HabitEntity::class,
        IntentEntity::class,
        GoalEntity::class,
        PointLogEntity::class,
        LearningEntity::class,
        WordEntity::class,
        SleepLogEntity::class,
        ReflectionEntity::class,
        TransactionEntity::class,
        MoneyAccountEntity::class,
        CategoryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dashboardDao(): DashboardDao
}
