package com.pulse.checkin.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pulse.checkin.AppContainer
import com.pulse.checkin.data.backup.BackupManager
import com.pulse.checkin.data.preferences.AppPreferences
import com.pulse.checkin.domain.model.CheckInEvent
import com.pulse.checkin.domain.model.Habit
import com.pulse.checkin.domain.model.ThemeMode
import com.pulse.checkin.domain.model.UserPreferences
import com.pulse.checkin.domain.repository.CheckInRepository
import com.pulse.checkin.domain.repository.HabitRepository
import com.pulse.checkin.domain.stats.MonthSnapshot
import com.pulse.checkin.domain.stats.StatsCalculator
import com.pulse.checkin.domain.stats.TodaySnapshot
import com.pulse.checkin.domain.stats.YearSnapshot
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
    STATS,
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
    val selectedHistoryHabitId: Long? = null,
    val selectedStatsYear: Int = LocalDate.now().year,
    val selectedStatsHabitId: Long? = null,
    val preferences: UserPreferences = UserPreferences(),
    val habits: List<Habit> = emptyList(),
    val todaySnapshot: TodaySnapshot = TodaySnapshot.Empty,
    val historySnapshot: MonthSnapshot = MonthSnapshot.Empty,
    val yearSnapshot: YearSnapshot = YearSnapshot.Empty,
    val isLoading: Boolean = true,
)

private data class BaseUiInputs(
    val habits: List<Habit>,
    val events: List<CheckInEvent>,
    val preferences: UserPreferences,
    val selectedTab: AppTab,
    val selectedDate: LocalDate,
)

class AppViewModel(
    private val habitRepository: HabitRepository,
    private val checkInRepository: CheckInRepository,
    private val appPreferences: AppPreferences,
    private val backupManager: BackupManager,
    private val statsCalculator: StatsCalculator,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val selectedTab = MutableStateFlow(AppTab.TODAY)
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val selectedHistoryHabitId = MutableStateFlow<Long?>(null)
    private val selectedStatsYear = MutableStateFlow(LocalDate.now().year)
    private val selectedStatsHabitId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<AppUiState> = combine(
        habitRepository.observeHabits(),
        checkInRepository.observeAllEvents(),
        appPreferences.userPreferences,
        selectedTab,
        selectedDate,
    ) { habits, events, preferences, tab, date ->
        BaseUiInputs(
            habits = habits,
            events = events,
            preferences = preferences,
            selectedTab = tab,
            selectedDate = date,
        )
    }.combine(selectedHistoryHabitId) { base, rawHistoryHabitId ->
        base to rawHistoryHabitId
    }.combine(selectedStatsYear) { (base, rawHistoryHabitId), statsYear ->
        Triple(base, rawHistoryHabitId, statsYear)
    }.combine(selectedStatsHabitId) { (base, rawHistoryHabitId, statsYear), rawStatsHabitId ->
        val today = LocalDate.now()
        val effectiveHistoryHabitId = rawHistoryHabitId?.takeIf { id -> base.habits.any { habit -> habit.id == id } }
            ?: base.habits.minByOrNull { it.sortOrder }?.id
        val effectiveStatsHabitId = rawStatsHabitId?.takeIf { id -> base.habits.any { habit -> habit.id == id } }
            ?: base.habits.minByOrNull { it.sortOrder }?.id
        val historyHabits = effectiveHistoryHabitId?.let { filterId ->
            base.habits.filter { it.id == filterId }
        }.orEmpty()
        val historyEvents = effectiveHistoryHabitId?.let { filterId ->
            base.events.filter { it.habitId == filterId }
        }.orEmpty()
        AppUiState(
            selectedTab = base.selectedTab,
            selectedDate = base.selectedDate,
            selectedHistoryHabitId = effectiveHistoryHabitId,
            selectedStatsYear = statsYear,
            selectedStatsHabitId = effectiveStatsHabitId,
            preferences = base.preferences,
            habits = base.habits,
            todaySnapshot = statsCalculator.buildTodaySnapshot(base.habits, base.events, today),
            historySnapshot = statsCalculator.buildMonthSnapshot(
                habits = historyHabits,
                events = historyEvents,
                month = YearMonth.from(base.selectedDate),
                selectedDate = base.selectedDate,
                today = today,
            ),
            yearSnapshot = statsCalculator.buildYearSnapshot(
                habits = base.habits,
                events = base.events,
                year = statsYear,
                selectedHabitId = effectiveStatsHabitId,
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

    fun selectHistoryHabit(habitId: Long?) {
        selectedHistoryHabitId.value = habitId
    }

    fun shiftMonth(delta: Long) {
        val current = YearMonth.from(selectedDate.value)
        val maxMonth = YearMonth.now()
        val next = current.plusMonths(delta).coerceAtMost(maxMonth)
        val targetDay = selectedDate.value.dayOfMonth.coerceAtMost(next.lengthOfMonth())
        selectedDate.value = next.atDay(targetDay)
    }

    fun selectStatsHabit(habitId: Long) {
        selectedStatsHabitId.value = habitId
    }

    fun shiftStatsYear(delta: Int) {
        selectedStatsYear.value = (selectedStatsYear.value + delta).coerceAtMost(LocalDate.now().year)
    }

    fun backToCurrentStatsYear() {
        selectedStatsYear.value = LocalDate.now().year
    }

    fun checkInHabit(habitId: Long) {
        viewModelScope.launch {
            checkInRepository.addCheckIn(habitId)
        }
    }

    fun deleteCheckInRecord(eventId: Long) {
        viewModelScope.launch {
            checkInRepository.deleteCheckIn(eventId)
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

    suspend fun exportBackup(uri: Uri): Result<String> {
        return backupManager.exportToUri(uri).map { summary ->
            "\u5df2\u5bfc\u51fa ${summary.habitCount} \u4e2a\u4e60\u60ef\u3001${summary.eventCount} \u6761\u8bb0\u5f55"
        }
    }

    suspend fun importBackup(uri: Uri): Result<String> {
        uiState.value.habits.forEach { reminderScheduler.cancelForHabit(it.id) }
        val result = backupManager.importFromUri(uri)
        if (result.isSuccess) {
            selectedHistoryHabitId.value = null
            selectedStatsHabitId.value = null
            selectedDate.value = LocalDate.now()
            selectedStatsYear.value = LocalDate.now().year
            reminderScheduler.syncAll(habitRepository.getActiveReminderHabits())
        }
        return result.map { summary ->
            "\u5df2\u5bfc\u5165 ${summary.habitCount} \u4e2a\u4e60\u60ef\u3001${summary.eventCount} \u6761\u8bb0\u5f55"
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
                    backupManager = container.backupManager,
                    statsCalculator = container.statsCalculator,
                    reminderScheduler = container.reminderScheduler,
                )
            }
        }
    }
}
