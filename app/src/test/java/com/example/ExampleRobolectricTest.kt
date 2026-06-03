package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.launch

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Umar Tracker", appName)
  }

  @Test
  fun test_activity_launch() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertNotNull(activity)
  }

  @Test
  fun test_add_transaction() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.DashboardRepository(db.dashboardDao())
    val vm = com.example.ui.DashboardViewModel(repo)

    // Let's first ensure we insert the default elements to avoid foreign key or lookup issues
    kotlinx.coroutines.MainScope().launch {
        repo.insertMoneyAccount(com.example.data.MoneyAccountEntity(name = "Cash", type = "CASH", balance = 500.0))
        repo.insertCategory(com.example.data.CategoryEntity(name = "Food", type = "EXPENSE"))

        vm.addTransaction(
          type = "EXPENSE",
          amount = 50.0,
          category = "Food",
          account = "Cash",
          toAccount = null,
          dateString = "2026-06-01",
          timeString = "12:00 PM",
          note = "TestNote"
        )
    }
    // Block thread or run current loop
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    db.close()
  }
}
