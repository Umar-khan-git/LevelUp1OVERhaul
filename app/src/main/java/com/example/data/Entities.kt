package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCompleted: Boolean = false,
    val streak: Int = 0,
    val dateUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_intents")
data class IntentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val why: String,
    val status: String, // "ACTIVE", "NEXT", "SOMEDAY"
    val bonusPoints: Float = 0f
)

@Entity(tableName = "point_logs")
data class PointLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val activity: String,
    val hours: Float,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "learning_items")
data class LearningEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val subtext: String,
    val category: String, // "IT", "LANGUAGES", "COURSES"
    val status: String // "ACTIVE", "NEXT", "SOMEDAY"
)

@Entity(tableName = "vocabulary")
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val meaning: String,
    val category: String // "ARABIC", "JAPANESE", "ENGLISH"
)

@Entity(tableName = "sleep_logs")
data class SleepLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // "YYYY-MM-DD"
    val sleptAt: String, // "HH:MM"
    val wokeUp: String, // "HH:MM"
    val hoursSlept: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "weekly_reflections")
data class ReflectionEntity(
    @PrimaryKey val weekKey: String, // e.g. "2026-W22"
    val reflection: String,
    val intention: String
)

@Entity(tableName = "money_transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "INCOME", "EXPENSE", "TRANSFER"
    val amount: Double,
    val category: String, // e.g., "Appi", "Sans bus", "Food", "wifi", etc.
    val account: String, // source account e.g., "My Adcb"
    val toAccount: String? = null, // destination account for Transfer
    val dateString: String, // "2026-05-31" (YYYY-MM-DD)
    val timeString: String, // "9:23 pm"
    val note: String = "",
    val description: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "money_accounts")
data class MoneyAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g., "Cash", "Saving box", "My Adcb"
    val type: String, // "CASH", "BANK", "CARD"
    val balance: Double = 0.0,
    val outstBalance: Double = 0.0
)

@Entity(tableName = "money_categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String // "EXPENSE" or "INCOME"
)

@Entity(tableName = "money_budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,          // category name; BudgetEntity.TOTAL = overall monthly budget
    val amount: Double,
    val yearMonth: String? = null  // "2026-06" = that month only; null = repeats every month (default)
) {
    companion object {
        const val TOTAL = "__TOTAL__" // sentinel for the overall monthly budget
    }
}

@Entity(tableName = "money_recurring")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,               // "INCOME", "EXPENSE", "TRANSFER"
    val amount: Double,
    val category: String,
    val account: String,            // source account
    val toAccount: String? = null,  // destination (transfers)
    val note: String = "",
    val frequency: String,          // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val intervalCount: Int = 1,     // every N units of frequency
    val startDate: String,          // "yyyy-MM-dd"
    val nextDate: String,           // "yyyy-MM-dd" — next occurrence still to post
    val endDate: String? = null,    // optional last date (inclusive); null = forever
    val active: Boolean = true
)

