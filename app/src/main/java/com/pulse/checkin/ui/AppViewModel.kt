package com.pulse.checkin.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pulse.checkin.AppContainer
import com.pulse.checkin.data.backup.BackupManager
import com.pulse.checkin.data.backup.BackupSummary
import com.pulse.checkin.data.cloud.Session
import com.pulse.checkin.data.cloud.SyncClock
import com.pulse.checkin.data.cloud.SyncError
import com.pulse.checkin.data.cloud.SyncException
import com.pulse.checkin.data.cloud.SyncManager
import com.pulse.checkin.data.cloud.SyncOutcome
import com.pulse.checkin.data.preferences.AppPreferences
import com.pulse.checkin.domain.model.AppLanguage
import com.pulse.checkin.domain.model.CalendarType
import com.pulse.checkin.domain.model.CheckInEvent
import com.pulse.checkin.domain.model.DayEvent
import com.pulse.checkin.domain.model.Habit
import com.pulse.checkin.domain.model.ThemeMode
import com.pulse.checkin.domain.model.UserPreferences
import com.pulse.checkin.domain.repository.CheckInRepository
import com.pulse.checkin.domain.repository.DayEventRepository
import com.pulse.checkin.domain.repository.HabitRepository
import com.pulse.checkin.domain.stats.MonthSnapshot
import com.pulse.checkin.domain.stats.LunarDate
import com.pulse.checkin.domain.stats.StatsCalculator
import com.pulse.checkin.domain.stats.TodaySnapshot
import com.pulse.checkin.domain.stats.YearSnapshot
import com.pulse.checkin.reminder.ReminderScheduler
import com.pulse.checkin.ui.util.AndroidLunarCalendar
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTab {
    TODAY,
    HISTORY,
    STATS,
    SETTINGS,
    DAY_EVENTS,
}

data class HabitDraft(
    val id: String = "",
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

data class SyncUiState(
    val session: Session? = null,
    val isSyncing: Boolean = false,
    val lastSyncAtEpochMillis: Long? = null,
    val syncError: SyncError? = null,
    val authError: SyncError? = null,
)

data class DayEventDraft(
    val id: String = "",
    val name: String = "",
    val date: LocalDate = LocalDate.now(),
    val repeatsMonthly: Boolean = false,
    val repeatsYearly: Boolean = false,
    val note: String? = null,
    val calendarType: CalendarType = CalendarType.SOLAR,
    val lunarMonth: Int? = null,
    val lunarDay: Int? = null,
    val lunarLeap: Boolean = false,
) {
    companion object {
        fun fromDayEvent(event: DayEvent): DayEventDraft = DayEventDraft(
            id = event.id,
            name = event.name,
            date = event.date,
            repeatsMonthly = event.repeatsMonthly,
            repeatsYearly = event.repeatsYearly,
            note = event.note,
            calendarType = event.calendarType,
            lunarMonth = event.lunarMonth,
            lunarDay = event.lunarDay,
            lunarLeap = event.lunarLeap,
        )
    }
}

data class AppUiState(
    val selectedTab: AppTab = AppTab.TODAY,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedHistoryHabitId: String? = null,
    val selectedStatsYear: Int = LocalDate.now().year,
    val selectedStatsHabitId: String? = null,
    val preferences: UserPreferences = UserPreferences(),
    val habits: List<Habit> = emptyList(),
    val dayEvents: List<DayEvent> = emptyList(),
    val todaySnapshot: TodaySnapshot = TodaySnapshot.Empty,
    val historySnapshot: MonthSnapshot = MonthSnapshot.Empty,
    val yearSnapshot: YearSnapshot = YearSnapshot.Empty,
    val isLoading: Boolean = true,
)

private data class BaseUiInputs(
    val habits: List<Habit>,
    val events: List<CheckInEvent>,
    val dayEvents: List<DayEvent>,
    val preferences: UserPreferences,
    val selectedTab: AppTab,
    val selectedDate: LocalDate,
)

private data class DerivedUiInputs(
    val base: BaseUiInputs,
    val rawHistoryHabitId: String?,
    val statsYear: Int,
    val rawStatsHabitId: String?,
)

class AppViewModel(
    private val habitRepository: HabitRepository,
    private val checkInRepository: CheckInRepository,
    private val dayEventRepository: DayEventRepository,
    private val appPreferences: AppPreferences,
    private val backupManager: BackupManager,
    private val statsCalculator: StatsCalculator,
    private val reminderScheduler: ReminderScheduler,
    private val syncManager: SyncManager,
    private val syncClock: SyncClock,
) : ViewModel() {
    private val selectedTab = MutableStateFlow(AppTab.TODAY)
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val selectedHistoryHabitId = MutableStateFlow<String?>(null)
    private val selectedStatsYear = MutableStateFlow(LocalDate.now().year)
    private val selectedStatsHabitId = MutableStateFlow<String?>(null)
    private val currentDate = MutableStateFlow(LocalDate.now())
    private val syncUiStateInternal = MutableStateFlow(SyncUiState())
    private var syncJob: Job? = null

    val syncUiState: StateFlow<SyncUiState> = syncUiStateInternal

    init {
        viewModelScope.launch {
            while (isActive) {
                val today = LocalDate.now()
                if (currentDate.value != today) {
                    currentDate.value = today
                }
                delay(60_000L)
            }
        }
        viewModelScope.launch {
            val lastSyncAt = syncManager.currentLastSyncAt()
            syncUiStateInternal.update {
                it.copy(lastSyncAtEpochMillis = lastSyncAt.takeIf { value -> value > 0L })
            }
        }
        viewModelScope.launch {
            syncManager.sessionFlow.collect { session ->
                syncUiStateInternal.update { it.copy(session = session) }
            }
        }
        viewModelScope.launch {
            if (syncManager.sessionFlow.first() != null) {
                runSyncInternal()
            }
        }
    }

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
            dayEvents = emptyList(),
            preferences = preferences,
            selectedTab = tab,
            selectedDate = date,
        )
    }.combine(dayEventRepository.observeDayEvents()) { base, dayEvents ->
        base.copy(dayEvents = dayEvents)
    }.combine(selectedHistoryHabitId) { base, rawHistoryHabitId ->
        base to rawHistoryHabitId
    }.combine(selectedStatsYear) { (base, rawHistoryHabitId), statsYear ->
        Pair(base, rawHistoryHabitId) to statsYear
    }.combine(selectedStatsHabitId) { pairWithYear, rawStatsHabitId ->
        val (baseAndHistory, statsYear) = pairWithYear
        val (base, rawHistoryHabitId) = baseAndHistory
        DerivedUiInputs(
            base = base,
            rawHistoryHabitId = rawHistoryHabitId,
            statsYear = statsYear,
            rawStatsHabitId = rawStatsHabitId,
        )
    }.combine(currentDate) { derived, today ->
        val base = derived.base
        val rawHistoryHabitId = derived.rawHistoryHabitId
        val statsYear = derived.statsYear
        val rawStatsHabitId = derived.rawStatsHabitId
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
            dayEvents = base.dayEvents,
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

    fun selectHistoryHabit(habitId: String?) {
        selectedHistoryHabitId.value = habitId
    }

    fun shiftMonth(delta: Long) {
        val current = YearMonth.from(selectedDate.value)
        val maxMonth = YearMonth.now()
        val next = current.plusMonths(delta).coerceAtMost(maxMonth)
        val targetDay = selectedDate.value.dayOfMonth.coerceAtMost(next.lengthOfMonth())
        selectedDate.value = next.atDay(targetDay)
    }

    fun selectStatsHabit(habitId: String) {
        selectedStatsHabitId.value = habitId
    }

    fun shiftStatsYear(delta: Int) {
        selectedStatsYear.value = (selectedStatsYear.value + delta).coerceAtMost(LocalDate.now().year)
    }

    fun backToCurrentStatsYear() {
        selectedStatsYear.value = LocalDate.now().year
    }

    fun checkInHabit(habitId: String, note: String) {
        viewModelScope.launch {
            checkInRepository.addCheckIn(habitId, note = note.trim().ifBlank { null })
            triggerSync()
        }
    }

    fun backfillHabit(habitId: String, targetDate: LocalDate, note: String) {
        if (!targetDate.isBefore(LocalDate.now())) return
        viewModelScope.launch {
            checkInRepository.addCheckIn(
                habitId = habitId,
                occurredAtEpochMillis = System.currentTimeMillis(),
                localDate = targetDate,
                isBackfilled = true,
                note = note.trim().ifBlank { null },
            )
            triggerSync()
        }
    }

    fun deleteCheckInRecord(eventId: String) {
        viewModelScope.launch {
            checkInRepository.deleteCheckIn(eventId)
            triggerSync()
        }
    }

    fun saveHabit(draft: HabitDraft) {
        viewModelScope.launch {
            val existing = if (draft.id.isNotBlank()) habitRepository.getHabit(draft.id) else null
            val sortOrder = existing?.sortOrder
                ?: (uiState.value.habits.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0)
            val habit = Habit(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = draft.name.trim(),
                colorArgb = draft.colorArgb,
                glyph = draft.glyph,
                sortOrder = sortOrder,
                reminderEnabled = draft.reminderEnabled,
                reminderHour = if (draft.reminderEnabled) draft.reminderHour else null,
                reminderMinute = if (draft.reminderEnabled) draft.reminderMinute else null,
                targetEnabled = draft.targetEnabled,
                dailyTargetCount = if (draft.targetEnabled) draft.targetCount.coerceAtLeast(1) else null,
                createdAtEpochMillis = existing?.createdAtEpochMillis ?: syncClock.nowMillis(),
                archived = false,
                updatedAtEpochMillis = syncClock.nowMillis(),
            )
            habitRepository.upsert(habit)
            if (habit.reminderEnabled) {
                reminderScheduler.scheduleForHabit(habit)
            } else {
                reminderScheduler.cancelForHabit(habit.id)
            }
            triggerSync()
        }
    }

    suspend fun exportBackup(uri: Uri): Result<BackupSummary> {
        return backupManager.exportToUri(uri)
    }

    suspend fun importBackup(uri: Uri): Result<BackupSummary> {
        uiState.value.habits.forEach { reminderScheduler.cancelForHabit(it.id) }
        val result = backupManager.importFromUri(uri)
        if (result.isSuccess) {
            selectedHistoryHabitId.value = null
            selectedStatsHabitId.value = null
            selectedDate.value = LocalDate.now()
            selectedStatsYear.value = LocalDate.now().year
            reminderScheduler.syncAll(habitRepository.getActiveReminderHabits())
            triggerSync()
        }
        return result
    }

    fun archiveHabit(habitId: String) {
        viewModelScope.launch {
            habitRepository.setArchived(habitId, true)
            reminderScheduler.cancelForHabit(habitId)
            triggerSync()
        }
    }

    fun saveDayEvent(draft: DayEventDraft) {
        viewModelScope.launch {
            val existing = if (draft.id.isNotBlank()) dayEventRepository.getDayEvent(draft.id) else null
            val sortOrder = existing?.sortOrder
                ?: (uiState.value.dayEvents.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0)
            val calendarType = draft.calendarType
            val lunarMonth = if (calendarType == CalendarType.LUNAR) draft.lunarMonth else null
            val lunarDay = if (calendarType == CalendarType.LUNAR) draft.lunarDay else null
            val lunarLeap = if (calendarType == CalendarType.LUNAR) draft.lunarLeap else false
            val date = if (calendarType == CalendarType.LUNAR) {
                AndroidLunarCalendar.toGregorian(
                    LocalDate.now().year,
                    LunarDate(lunarMonth ?: 1, lunarDay ?: 1, lunarLeap),
                )
            } else {
                draft.date
            }
            val event = DayEvent(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = draft.name.trim(),
                date = date,
                repeatsMonthly = draft.repeatsMonthly,
                repeatsYearly = draft.repeatsYearly,
                note = draft.note?.trim()?.ifBlank { null },
                sortOrder = sortOrder,
                calendarType = calendarType,
                lunarMonth = lunarMonth,
                lunarDay = lunarDay,
                lunarLeap = lunarLeap,
                createdAtEpochMillis = existing?.createdAtEpochMillis ?: syncClock.nowMillis(),
                archived = false,
                updatedAtEpochMillis = syncClock.nowMillis(),
            )
            dayEventRepository.upsert(event)
            triggerSync()
        }
    }

    fun archiveDayEvent(eventId: String) {
        viewModelScope.launch {
            dayEventRepository.setArchived(eventId, true)
            triggerSync()
        }
    }

    fun reorderDayEvents(orderedIds: List<String>) {
        viewModelScope.launch {
            dayEventRepository.reorderDayEvents(orderedIds)
            triggerSync()
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            syncUiStateInternal.update { it.copy(isSyncing = true, authError = null) }
            syncManager.login(email, password).fold(
                onSuccess = {
                    syncUiStateInternal.update { it.copy(isSyncing = false, authError = null) }
                    runSyncInternal()
                },
                onFailure = { exception ->
                    syncUiStateInternal.update {
                        it.copy(
                            isSyncing = false,
                            authError = (exception as? SyncException)?.error ?: SyncError.UNKNOWN,
                        )
                    }
                },
            )
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            syncUiStateInternal.update { it.copy(isSyncing = true, authError = null) }
            syncManager.register(email, password).fold(
                onSuccess = {
                    syncUiStateInternal.update { it.copy(isSyncing = false, authError = null) }
                    runSyncInternal()
                },
                onFailure = { exception ->
                    syncUiStateInternal.update {
                        it.copy(
                            isSyncing = false,
                            authError = (exception as? SyncException)?.error ?: SyncError.UNKNOWN,
                        )
                    }
                },
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            syncManager.logout()
            syncUiStateInternal.update {
                it.copy(
                    session = null,
                    authError = null,
                    syncError = null,
                    lastSyncAtEpochMillis = null,
                )
            }
            selectedHistoryHabitId.value = null
            selectedStatsHabitId.value = null
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            runSyncInternal()
        }
    }

    private fun triggerSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            delay(800)
            runSyncInternal()
        }
    }

    private suspend fun runSyncInternal() {
        syncUiStateInternal.update { it.copy(isSyncing = true, syncError = null) }
        when (val outcome = syncManager.syncOnce()) {
            is SyncOutcome.Success -> {
                syncUiStateInternal.update {
                    it.copy(
                        isSyncing = false,
                        syncError = null,
                        lastSyncAtEpochMillis = System.currentTimeMillis(),
                    )
                }
                reminderScheduler.syncAll(habitRepository.getActiveReminderHabits())
            }
            is SyncOutcome.SignedOut -> syncUiStateInternal.update { it.copy(isSyncing = false) }
            is SyncOutcome.Failure -> syncUiStateInternal.update {
                it.copy(isSyncing = false, syncError = outcome.error)
            }
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            appPreferences.setThemeMode(themeMode)
        }
    }

    fun setAppLanguage(appLanguage: AppLanguage) {
        viewModelScope.launch {
            appPreferences.setAppLanguage(appLanguage)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppViewModel(
                    habitRepository = container.habitRepository,
                    checkInRepository = container.checkInRepository,
                    dayEventRepository = container.dayEventRepository,
                    appPreferences = container.preferences,
                    backupManager = container.backupManager,
                    statsCalculator = container.statsCalculator,
                    reminderScheduler = container.reminderScheduler,
                    syncManager = container.syncManager,
                    syncClock = container.syncClock,
                )
            }
        }
    }
}
