package com.example.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** v5 -> v6: adds the money_budgets table (budgets feature). Non-destructive. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS money_budgets (" +
                "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "category TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "yearMonth TEXT)"
        )
    }
}

/** v6 -> v7: adds the money_recurring table (recurring transactions). Non-destructive. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Column types/nullability must match Room's generated schema exactly (no SQL defaults,
        // since the entity uses Kotlin default values rather than @ColumnInfo(defaultValue=...)).
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS money_recurring (" +
                "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "category TEXT NOT NULL, " +
                "account TEXT NOT NULL, " +
                "toAccount TEXT, " +
                "note TEXT NOT NULL, " +
                "frequency TEXT NOT NULL, " +
                "intervalCount INTEGER NOT NULL, " +
                "startDate TEXT NOT NULL, " +
                "nextDate TEXT NOT NULL, " +
                "endDate TEXT, " +
                "active INTEGER NOT NULL)"
        )
    }
}

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

    @Query("SELECT * FROM weekly_reflections")
    suspend fun getAllReflectionsDirect(): List<ReflectionEntity>

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

    // Money Manager Budgets
    @Query("SELECT * FROM money_budgets ORDER BY id ASC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM money_budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    // Money Manager Recurring rules
    @Query("SELECT * FROM money_recurring ORDER BY id ASC")
    fun getAllRecurring(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM money_recurring")
    suspend fun getAllRecurringDirect(): List<RecurringEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(rule: RecurringEntity): Long

    @Update
    suspend fun updateRecurring(rule: RecurringEntity)

    @Delete
    suspend fun deleteRecurring(rule: RecurringEntity)

    @Query("DELETE FROM money_recurring WHERE id = :id")
    suspend fun deleteRecurringById(id: Long)

    // ===== Clear-all (used by backup restore — no schema change) =====
    @Query("DELETE FROM habits") suspend fun clearHabits()
    @Query("DELETE FROM daily_intents") suspend fun clearIntents()
    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM point_logs") suspend fun clearPointLogs()
    @Query("DELETE FROM learning_items") suspend fun clearLearning()
    @Query("DELETE FROM vocabulary") suspend fun clearWords()
    @Query("DELETE FROM sleep_logs") suspend fun clearSleepLogs()
    @Query("DELETE FROM weekly_reflections") suspend fun clearReflections()
    @Query("DELETE FROM money_transactions") suspend fun clearTransactions()
    @Query("DELETE FROM money_accounts") suspend fun clearAccounts()
    @Query("DELETE FROM money_categories") suspend fun clearCategories()
    @Query("DELETE FROM money_budgets") suspend fun clearBudgets()
    @Query("DELETE FROM money_recurring") suspend fun clearRecurring()
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
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dashboardDao(): DashboardDao
}
