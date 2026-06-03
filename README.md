# UmarOS — Personal Self-Improvement Hub

A native Android app built with Jetpack Compose for tracking daily habits, goals, sleep, finances, vocabulary, and weekly reflections. All data is stored locally on-device using Room (SQLite) — no internet connection or account required.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features & Screens](#features--screens)
- [Database Schema](#database-schema)
- [Project Structure](#project-structure)
- [Building the APK](#building-the-apk)
- [Known Behaviour](#known-behaviour)

---

## Overview

| Property | Value |
|---|---|
| App name | UmarOS |
| Package ID | `com.aistudio.personaltracker.elcken` |
| Min Android | API 24 (Android 7.0) |
| Target Android | API 36 (Android 16) |
| Language | Kotlin |
| UI toolkit | Jetpack Compose (Material 3) |
| Database | Room 2.7 (SQLite) |
| Architecture | MVVM + Repository |

---

## Tech Stack

| Layer | Library / Tool |
|---|---|
| UI | Jetpack Compose, Material 3, Canvas |
| State management | StateFlow, collectAsStateWithLifecycle |
| Navigation | HorizontalPager (swipe) + custom bottom nav |
| Local database | Room 2.7 with KSP code generation |
| Async | Kotlin Coroutines, ViewModelScope |
| Notifications | AlarmManager + BroadcastReceiver |
| Local settings | SharedPreferences |
| Build | Gradle 9.3.1, AGP 9.1.1, KSP 2.3.5 |
| CI / APK build | GitHub Actions |

---

## Architecture

```
┌─────────────────────────────────────┐
│           UI Layer (Compose)        │
│  MainActivity, Tab screens,         │
│  FinanceTabScreen, dialogs          │
└────────────────┬────────────────────┘
                 │ observes StateFlow
┌────────────────▼────────────────────┐
│         ViewModel Layer             │
│  DashboardViewModel                 │
│  (single ViewModel for all tabs)    │
└────────────────┬────────────────────┘
                 │ suspend functions
┌────────────────▼────────────────────┐
│         Repository Layer            │
│  DashboardRepository                │
│  (wraps all DAO operations)         │
└────────────────┬────────────────────┘
                 │ Room DAO
┌────────────────▼────────────────────┐
│         Database Layer              │
│  AppDatabase (Room / SQLite)        │
│  DashboardDao (all queries)         │
└─────────────────────────────────────┘
```

### Data flow
1. UI collects `StateFlow` from `DashboardViewModel`
2. User actions call ViewModel functions (e.g. `addTransaction`, `toggleHabit`)
3. ViewModel launches coroutines via `viewModelScope` and calls Repository
4. Repository delegates to `DashboardDao` (Room handles IO threading internally)
5. Room emits updated `Flow` → StateFlow updates → UI recomposes

---

## Features & Screens

### Splash Screen
- Fade + scale animation on every launch
- Shows "UmarOS" with gradient text and a loading spinner
- Auto-transitions to main app after ~2.2 seconds

### Navigation
- **Bottom navigation bar** with 7 tabs: Today, Goals, Learn, Sleep, Stats, Money, Week
- **Swipe left/right** between tabs using Compose `HorizontalPager`
- Bottom nav and swipe stay in sync — tapping nav jumps the pager, swiping updates the nav
- **App open streak** (🔥 N DAYS) shown in header — tracks consecutive days the app is opened, stored in SharedPreferences

---

### Today Tab
Tracks daily habits and non-negotiable daily intents.

**Habits**
- Toggle completion with a tap (gradient checkmark when done)
- Streak counter per habit (🔥 N) — increments each day the habit is completed
- Daily auto-reset: at the start of a new day, all habits reset to incomplete. If a habit was completed the previous day its streak is preserved; otherwise it resets to 0
- Add custom habits, delete by tapping the trash icon

**Daily Intents (Non-negotiables)**
- Fixed list of daily commitments (e.g. "Sleep at 12 AM — no blue light after 11:30 PM")
- Toggle done/undone with a tap
- Add / delete intents

---

### Goals Tab
Point-based goal tracking system.

- Each goal has: name, a "why" reason, and status (ACTIVE / NEXT / SOMEDAY)
- Progress is measured in **points** — 1 hour of logged activity = 1 point = 1% toward 100%
- Log activities with time spent → contributes to the goal's progress bar
- Gradient progress bar shown on each goal card
- Last 3 activity logs shown inline per goal card
- Add / delete goals; add / delete individual log entries

---

### Learn Tab
Tracks learning topics and a personal vocabulary dictionary.

**Topics**
- Each item: name, short note, category (IT / Languages / Courses), status (Active / Next / Someday)
- Filter tabs: All, IT, Arabic, Japanese, Vocabulary, Courses

**Vocabulary Dictionary**
- Arabic, Japanese, and English word entries
- Each entry: word, meaning, language category
- Grouped by language in the scrollable list

---

### Sleep Tab
Sleep routine tracker and calculator.

- Log sleep time (HH:MM) and wake time (HH:MM)
- Auto-calculates hours slept — handles past-midnight sleep correctly (e.g. 01:00 → 05:30)
- **Weekly grid** showing the last 7 days with colour coding:
  - Gradient = 6.5 h+ (good night)
  - Red = under 6.5 h (sleep debt)
  - Grey = no entry logged
- Stats cards: average hours per night, 7 h target, weekly sleep debt
- Last 10 logs shown in a list with delete option

---

### Stats Tab
Visual analytics dashboard.

- **Habit donut ring** — today's completion percentage with a gradient arc
- **Sleep bar chart** — custom Canvas-drawn bars for the last 7 days
- **Goal progress** — horizontal progress bars for all active goals
- Aggregate stat cards: habits %, words learned count, average goal %

---

### Money Tab (Finance)
Full personal finance tracker with 4 internal sub-tabs.

#### Trans (Transactions)
- **Daily view** — all transactions grouped by date, sorted newest first. Each date group shows that day's income and expense totals
- **Calendar view** — monthly grid; each day shows income (blue) and expense (red) amounts if any transactions exist
- **Monthly view** — weekly breakdown of income and expenses for the selected month
- Month switcher (◀ ▶) to browse history
- Summary bar at top: Income / Expenses / Total for the selected month
- Long-press any row to delete a transaction
- Red FAB (+) opens the Add Transaction screen

**Adding a transaction:**
1. Tap the red + FAB
2. Choose type: Income / Expense / Transfer
3. Enter amount via the custom number pad (DH or ₹ currency)
4. Select a category from the horizontal chip list (or add a new custom category inline)
5. Select the source account (or add a new account inline)
6. Optionally add a note
7. Tap **DONE** — transaction is saved and account balance is updated automatically

#### Stats (Finance Charts)
- **Pie chart** with leader lines — each slice shows the category name and percentage directly on the chart
- Slices under 3% are grouped into a single "Others" slice to keep the chart readable
- Toggle between EXPENSES breakdown and INCOME breakdown
- Coloured category list below the chart with amount and percentage pill per category

#### Accounts
- Three account types: **Cash**, **Bank**, **Card**
- Summary card: Total Assets / Total Liabilities / Net Worth
- Each account row shows name, type, and current balance
- Long-press a row to delete an account
- "Add New Account" button at the bottom

#### Total
- Monthly income / expense / net summary for the selected month
- **Monthly budget goal** — tap to set a spending limit; shows percentage spent, remaining, and an "Over budget!" warning in red when exceeded. Budget is saved per month in SharedPreferences
- Breakdown rows: Cash & Account expenses, Card expenses, Transfers
- Month-over-month expense comparison (% relative to previous month)
- Export button (placeholder — shows a toast)

---

### Week Tab
Weekly reflection journal.

- Emoji row showing sleep quality for the past 7 days (😎 / 😐 / 😴 based on hours slept)
- Free-text **Weekly Feedback** field ("How was your week?")
- Free-text **Next Week Action Steps** field
- Saves to database keyed by week (e.g. `2026-W22`); loading the same week key restores the saved text

---

## Database Schema

All 11 entities live in a single Room database named `umar_tracker_db_v5` (schema version 4).

| Table | Key columns |
|---|---|
| `habits` | id, name, isCompleted, streak, dateUpdated |
| `daily_intents` | id, name, isCompleted |
| `goals` | id, name, why, status, bonusPoints |
| `point_logs` | id, goalId, activity, hours, dateAdded |
| `learning_items` | id, name, subtext, category, status |
| `vocabulary` | id, word, meaning, category |
| `sleep_logs` | id, dateString (YYYY-MM-DD), sleptAt, wokeUp, hoursSlept, timestamp |
| `weekly_reflections` | weekKey PK (e.g. 2026-W22), reflection, intention |
| `money_transactions` | id, type, amount, category, account, toAccount, dateString, timeString, note, timestamp |
| `money_accounts` | id, name, type (CASH/BANK/CARD), balance, outstBalance |
| `money_categories` | id, name, type (EXPENSE/INCOME) |

**First launch:** the database is seeded with default data (sample habits, goals, May 2026 transactions, sleep logs, vocabulary) so the app looks populated immediately.

**Migration policy:** `fallbackToDestructiveMigration()` is used — if the Room `version` number is bumped, the entire database is wiped and re-seeded on next launch. Do not bump the version without exporting user data first.

---

## Project Structure

```
app/src/main/
├── java/com/example/
│   ├── MainActivity.kt           # Entry point, splash screen, swipe navigation,
│   │                             # AppHeader, all tab screen composables (Today,
│   │                             # Goals, Learn, Sleep, Stats, Week)
│   ├── NotificationReceiver.kt   # Daily 11 AM reminder via AlarmManager
│   ├── data/
│   │   ├── AppDatabase.kt        # Room database definition + DashboardDao
│   │   ├── Entities.kt           # All 11 @Entity data classes
│   │   └── Repository.kt         # Repository wrapping every DAO call
│   └── ui/
│       ├── DashboardViewModel.kt # Single ViewModel — all state & business logic,
│       │                         # default data seeding, balance calculations
│       ├── FinanceTabScreen.kt   # Entire Money tab UI (~2400 lines):
│       │                         # Trans/Stats/Accounts/Total sub-screens,
│       │                         # AddTransactionDialog, pie chart, calendar view
│       └── theme/
│           ├── Color.kt          # Brand colours (CanvasBg, LayerCard, MutedText…)
│           ├── Theme.kt          # MyApplicationTheme (dark theme)
│           └── Type.kt           # Typography
└── res/
    ├── drawable/
    │   ├── uos_icon.png                  # UmarOS app icon (full size)
    │   ├── ic_launcher_background.xml    # Adaptive icon background layer
    │   └── ic_launcher_foreground.xml    # Adaptive icon foreground layer (legacy)
    ├── mipmap-hdpi/                      # ic_launcher.png  72×72
    ├── mipmap-mdpi/                      # ic_launcher.png  48×48
    ├── mipmap-xhdpi/                     # ic_launcher.png  96×96
    ├── mipmap-xxhdpi/                    # ic_launcher.png 144×144
    ├── mipmap-xxxhdpi/                   # ic_launcher.png 192×192
    ├── mipmap-anydpi-v26/
    │   ├── ic_launcher.xml               # Adaptive icon → uos_icon.png
    │   └── ic_launcher_round.xml         # Round adaptive icon → uos_icon.png
    └── values/
        ├── strings.xml                   # app_name = "UmarOS"
        ├── colors.xml                    # ic_launcher_background = #0D0D0D + palette
        └── themes.xml                    # Theme.MyApplication
```

---

## Building the APK

This project has **no `gradlew` file committed**. The GitHub Actions workflow generates it automatically. You do not need Android Studio or any Android SDK installed locally.

### Via GitHub Actions (recommended — no local install needed)

1. Push any change to `main` or `master` — the workflow triggers automatically.  
   Or go to **Actions → Build APK → Run workflow** to trigger manually.
2. The workflow:
   - Sets up JDK 17
   - Creates the required `.env` and `.env.example` files (Secrets Gradle plugin needs them)
   - Generates a temporary debug keystore
   - Generates Gradle wrapper 9.3.1
   - Runs `./gradlew assembleDebug`
3. When the run turns green, click it → scroll to **Artifacts** → download **app-debug**
4. Unzip → transfer `app-debug.apk` to your Android phone → tap to install

> **First install:** Enable *Install unknown apps* in Android Settings for whichever app you use to open the APK (Files, Chrome, etc.)

> **Updating:** If the old version is already installed, just install the new APK on top — your data (Room database) is preserved between installs as long as the package ID and database version stay the same.

### Workflow file
`.github/workflows/build.yml`

```yaml
# Key steps in the workflow:
- Create missing files (.env, .env.example, debug.keystore)
- Setup Gradle 9.3.1
- gradle wrapper --gradle-version 9.3.1
- ./gradlew assembleDebug
```

---

## Known Behaviour

| Behaviour | Detail |
|---|---|
| App open streak | Increments once per calendar day. If the app is not opened for a day the streak resets to 1 on next open |
| Habit streak | Increments each day a habit is marked complete. If missed (not completed before the next day's reset), streak resets to 0 |
| Account balances | Automatically adjusted up/down when transactions are added or deleted |
| Transaction date | Always set to today's date — there is no manual date picker by design (quick logging) |
| Export to Excel | Tapping the export button shows a placeholder toast — not yet implemented |
| Swipe vs scroll | On screens with horizontal content (category chips), the swipe-between-tabs gesture may compete. Use the bottom nav to switch tabs from those screens |
| Database wipe | Bumping the Room `version` field in `AppDatabase.kt` triggers `fallbackToDestructiveMigration`, wiping all user data on next launch |
| Notification | Daily 11 AM reminder. Requires *Post Notifications* permission on Android 13+ (API 33). Rescheduled automatically on device reboot |
| Dark mode | The app is dark-only — it does not respond to the system light/dark mode toggle |
| No cloud sync | All data is stored locally. Uninstalling the app permanently deletes all data |
