package com.pulse.checkin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pulse.checkin.AppContainer
import com.pulse.checkin.data.preferences.AppPreferences
import com.pulse.checkin.domain.model.Habit
import com.pulse.checkin.domain.model.ThemeMode
import com.pulse.checkin.domain.model.UserPreferences
import com.pulse.checkin.domain.repository.CheckInRepository
import com.pulse.checkin.domain.repository.HabitRepository
import com.pulse.checkin.domain.stats.MonthSnapshot
import com.pulse.checkin.domain.stats.StatsCalculator
import com.pulse.checkin.domain.stats.TodaySnapshot
import com.pulse.checkin.reminder.ReminderScheduler
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    TODAY,
    HISTORY,
    SETTINGS,
}

data class HabitDraft(
    val id: Long = 0,
    val name: String = "",
    val glyph: String = "P",
    val colorArgb: Long = 0xFF101828,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val targetEnabled: Boolean = false,
    val targetCount: Int = 8,
) {
    companion object {
        fun fromHabit(habit: Habit): HabitDraft = HabitDraft(
            id = habit.id,
            name = habit.name,
            glyph = habit.glyph,
            colorArgb = habit.colorArgb,
            reminderEnabled = habit.reminderEnabled,
            reminderHour = habit.reminderHour ?: 20,
            reminderMinute = habit.reminderMinute ?: 0,
            targetEnabled = habit.targetEnabled,
            targetCount = habit.dailyTargetCount ?: 1,
        )
    }
}

data class AppUiState(
    val selectedTab: AppTab = AppTab.TODAY,
    val selectedDate: LocalDate = LocalDate.now(),
    val preferences: UserPreferences = UserPreferences(),
    val habits: List<Habit> = emptyList(),
    val todaySnapshot: TodaySnapshot = TodaySnapshot.Empty,
    val historySnapshot: MonthSnapshot = MonthSnapshot.Empty,
    val isLoading: Boolean = true,
)

class AppViewModel(
    private val habitRepository: HabitRepository,
    private val checkInRepository: CheckInRepository,
    private val appPreferences: AppPreferences,
    private val statsCalculator: StatsCalculator,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val selectedTab = MutableStateFlow(AppTab.TODAY)
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<AppUiState> = combine(
        habitRepository.observeHabits(),
        checkInRepository.observeAllEvents(),
        appPreferences.userPreferences,
        selectedTab,
        selectedDate,
    ) { habits, events, preferences, tab, selectedDate ->
        val today = LocalDate.now()
        val selectedMonth = YearMonth.from(selectedDate)
        AppUiState(
            selectedTab = tab,
            selectedDate = selectedDate,
            preferences = preferences,
            habits = habits,
            todaySnapshot = statsCalculator.buildTodaySnapshot(habits, events, today),
            historySnapshot = statsCalculator.buildMonthSnapshot(
                habits = habits,
                events = events,
                month = selectedMonth,
                selectedDate = selectedDate,
                today = today,
            ),
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(),
    )

    fun selectTab(tab: AppTab) {
        selectedTab.value = tab
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun shiftMonth(delta: Long) {
        val current = YearMonth.from(selectedDate.value)
        val next = current.plusMonths(delta)
        val targetDay = selectedDate.value.dayOfMonth.coerceAtMost(next.lengthOfMonth())
        selectedDate.value = next.atDay(targetDay)
    }

    fun incrementHabit(habitId: Long) {
        viewModelScope.launch {
            checkInRepository.addCheckIn(habitId)
        }
    }

    fun decrementHabit(habitId: Long) {
        viewModelScope.launch {
            checkInRepository.removeLatestForDay(habitId, LocalDate.now())
        }
    }

    fun saveHabit(draft: HabitDraft) {
        viewModelScope.launch {
            val existing = if (draft.id != 0L) habitRepository.getHabit(draft.id) else null
            val sortOrder = existing?.sortOrder
                ?: (uiState.value.habits.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0)
            val habit = Habit(
                id = existing?.id ?: 0,
                name = draft.name.trim(),
                colorArgb = draft.colorArgb,
                glyph = draft.glyph,
                sortOrder = sortOrder,
                reminderEnabled = draft.reminderEnabled,
                reminderHour = if (draft.reminderEnabled) draft.reminderHour else null,
                reminderMinute = if (draft.reminderEnabled) draft.reminderMinute else null,
                targetEnabled = draft.targetEnabled,
                dailyTargetCount = if (draft.targetEnabled) draft.targetCount.coerceAtLeast(1) else null,
                createdAtEpochMillis = existing?.createdAtEpochMillis ?: System.currentTimeMillis(),
                archived = false,
            )
            val savedId = habitRepository.upsert(habit)
            val finalHabit = habit.copy(id = if (habit.id == 0L) savedId else habit.id)
            if (finalHabit.reminderEnabled) {
                reminderScheduler.scheduleForHabit(finalHabit)
            } else {
                reminderScheduler.cancelForHabit(finalHabit.id)
            }
        }
    }

    fun archiveHabit(habitId: Long) {
        viewModelScope.launch {
            habitRepository.setArchived(habitId, true)
            reminderScheduler.cancelForHabit(habitId)
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            appPreferences.setThemeMode(themeMode)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppViewModel(
                    habitRepository = container.habitRepository,
                    checkInRepository = container.checkInRepository,
                    appPreferences = container.preferences,
                    statsCalculator = container.statsCalculator,
                    reminderScheduler = container.reminderScheduler,
                )
            }
        }
    }
}
