package com.pulse.checkin.ui.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.pulse.checkin.R
import com.pulse.checkin.domain.model.AppLanguage
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class PulseStrings(private val context: Context) {
    private val locale: Locale
        get() = context.resources.configuration.locales.get(0) ?: Locale.getDefault()

    val exportFailed: String get() = context.getString(R.string.export_failed)
    val importFailed: String get() = context.getString(R.string.import_failed)
    val tabToday: String get() = context.getString(R.string.tab_today)
    val tabHistory: String get() = context.getString(R.string.tab_history)
    val tabStats: String get() = context.getString(R.string.tab_stats)
    val tabSettings: String get() = context.getString(R.string.tab_settings)
    val add: String get() = context.getString(R.string.add)
    val tabDayEvents: String get() = context.getString(R.string.tab_day_events)

    val todayTitle: String get() = context.getString(R.string.today_title)
    val todayCountMetric: String get() = context.getString(R.string.today_count_metric)
    val reachedMetric: String get() = context.getString(R.string.reached_metric)
    val bestStreakMetric: String get() = context.getString(R.string.best_streak_metric)
    val createFirstHabitTitle: String get() = context.getString(R.string.create_first_habit_title)
    val createFirstHabitDesc: String get() = context.getString(R.string.create_first_habit_desc)
    fun streakDays(count: Int): String = context.getString(R.string.streak_days, count)
    fun targetTimes(count: Int): String = context.getString(R.string.target_times, count)
    val recordOnly: String get() = context.getString(R.string.record_only)
    fun latestCheckIn(elapsed: String): String = context.getString(R.string.latest_check_in, elapsed)
    val noCheckInToday: String get() = context.getString(R.string.no_check_in_today)
    val elapsedJustNow: String get() = context.getString(R.string.elapsed_just_now)
    fun elapsedMinutes(count: Int): String = context.getString(R.string.elapsed_minutes, count)
    fun elapsedHours(count: Int): String = context.getString(R.string.elapsed_hours, count)
    fun elapsedDays(count: Int): String = context.getString(R.string.elapsed_days, count)
    val checkIn: String get() = context.getString(R.string.check_in)
    val reachedToday: String get() = context.getString(R.string.reached_today)
    fun remainingToGoal(count: Int): String = context.getString(R.string.remaining_to_goal, count)
    val notStartedToday: String get() = context.getString(R.string.not_started_today)
    val canExpandTodayRecords: String get() = context.getString(R.string.can_expand_today_records)
    val noRecordsToday: String get() = context.getString(R.string.no_records_today)
    fun todayRecordsCount(count: Int): String = context.getString(R.string.today_records_count, count)
    val deleteRecordTitle: String get() = context.getString(R.string.delete_record_title)
    val confirmDeleteRecord: String get() = context.getString(R.string.confirm_delete_record)
    val confirmDelete: String get() = context.getString(R.string.confirm_delete)
    val cancel: String get() = context.getString(R.string.cancel)
    val confirmCheckInTitle: String get() = context.getString(R.string.confirm_check_in_title)
    fun confirmCheckInText(name: String): String = context.getString(R.string.confirm_check_in_text, name)
    val confirmCheckIn: String get() = context.getString(R.string.confirm_check_in)
    val deleteHabitTitle: String get() = context.getString(R.string.delete_habit_title)
    fun confirmDeleteHabit(name: String): String = context.getString(R.string.confirm_delete_habit, name)
    val longPressDeleteHint: String get() = context.getString(R.string.long_press_delete_hint)

    val historyTitle: String get() = context.getString(R.string.history_title)
    val habitFilterTitle: String get() = context.getString(R.string.habit_filter_title)
    val monthCountMetric: String get() = context.getString(R.string.month_count_metric)
    val completionRateMetric: String get() = context.getString(R.string.completion_rate_metric)
    val currentStreakMetric: String get() = context.getString(R.string.current_streak_metric)
    val noRecordsForFilter: String get() = context.getString(R.string.no_records_for_filter)
    val calendarTodayShort: String get() = context.getString(R.string.calendar_today_short)
    val weekHeaders: List<String>
        get() = listOf(
            context.getString(R.string.week_mon),
            context.getString(R.string.week_tue),
            context.getString(R.string.week_wed),
            context.getString(R.string.week_thu),
            context.getString(R.string.week_fri),
            context.getString(R.string.week_sat),
            context.getString(R.string.week_sun),
        )

    fun historyMonthText(month: YearMonth): String = month.format(
        DateTimeFormatter.ofPattern(context.getString(R.string.history_month_pattern), locale),
    )

    fun historyDetailDate(date: LocalDate): String = date.format(
        DateTimeFormatter.ofPattern(context.getString(R.string.history_detail_date_pattern), locale),
    )

    val dayReached: String get() = context.getString(R.string.day_reached)
    fun countTimes(count: Int): String = context.getString(R.string.count_times, count)
    val noDayRecords: String get() = context.getString(R.string.no_day_records)
    fun totalCheckIns(count: Int): String = context.getString(R.string.total_check_ins, count)
    val backfill: String get() = context.getString(R.string.backfill)
    val confirmBackfill: String get() = context.getString(R.string.confirm_backfill)
    fun confirmBackfillText(date: String, habitName: String): String = context.getString(R.string.confirm_backfill_text, date, habitName)
    val backfilledRecord: String get() = context.getString(R.string.backfilled_record)
    fun deleteRecordAt(time: String): String = context.getString(R.string.delete_record_at, time)
    fun nthCheckIn(index: Int): String = context.getString(R.string.nth_check_in, index + 1)

    val statsTitle: String get() = context.getString(R.string.stats_title)
    val noHabitDataTitle: String get() = context.getString(R.string.no_habit_data_title)
    val noHabitDataDesc: String get() = context.getString(R.string.no_habit_data_desc)
    val activeDaysMetric: String get() = context.getString(R.string.active_days_metric)
    val totalCheckInsMetric: String get() = context.getString(R.string.total_check_ins_metric)
    val longestStreakMetric: String get() = context.getString(R.string.longest_streak_metric)
    fun yearText(year: Int): String = context.getString(R.string.year_text, year)
    val trendTitle: String get() = context.getString(R.string.trend_title)
    fun trendPreview(month: YearMonth, count: Int): String = context.getString(R.string.trend_preview, statsMonthText(month), count)
    val hourlyDistributionTitle: String get() = context.getString(R.string.hourly_distribution_title)
    fun hourlyPreview(hour: Int, count: Int): String = context.getString(R.string.hourly_preview, hour, count)
    val monthlyDetailsTitle: String get() = context.getString(R.string.monthly_details_title)
    val detailMonth: String get() = context.getString(R.string.detail_month)
    val detailCompletion: String get() = context.getString(R.string.detail_completion)
    val detailDays: String get() = context.getString(R.string.detail_days)
    val detailCount: String get() = context.getString(R.string.detail_count)
    val detailStreak: String get() = context.getString(R.string.detail_streak)

    fun statsMonthText(month: YearMonth): String = month.format(
        DateTimeFormatter.ofPattern(context.getString(R.string.stats_month_pattern), locale),
    )

    val settingsTitle: String get() = context.getString(R.string.settings_title)
    val appearance: String get() = context.getString(R.string.appearance)
    val languageTitle: String get() = context.getString(R.string.language_title)
    val data: String get() = context.getString(R.string.data)
    val notifications: String get() = context.getString(R.string.notifications)
    val about: String get() = context.getString(R.string.about)
    val export: String get() = context.getString(R.string.export_label)
    val import: String get() = context.getString(R.string.import_label)
    val importOverwriteHint: String get() = context.getString(R.string.import_overwrite_hint)
    val enabled: String get() = context.getString(R.string.enabled)
    val disabled: String get() = context.getString(R.string.disabled)
    val enableNotifications: String get() = context.getString(R.string.enable_notifications)
    val aboutText: String get() = context.getString(R.string.about_text)
    fun versionName(version: String): String = context.getString(R.string.version_name, version)
    val lightMode: String get() = context.getString(R.string.light_mode)
    val systemMode: String get() = context.getString(R.string.system_mode)
    val darkMode: String get() = context.getString(R.string.dark_mode)
    val chinese: String get() = context.getString(R.string.chinese)
    val english: String get() = context.getString(R.string.english)
    val accountSection: String get() = context.getString(R.string.account_section)
    val accountSyncDesc: String get() = context.getString(R.string.account_sync_desc)
    val emailLabel: String get() = context.getString(R.string.email_label)
    val passwordLabel: String get() = context.getString(R.string.password_label)
    val login: String get() = context.getString(R.string.login)
    val register: String get() = context.getString(R.string.register)
    val logout: String get() = context.getString(R.string.logout)
    val syncNow: String get() = context.getString(R.string.sync_now)
    val syncing: String get() = context.getString(R.string.syncing)
    val lastSyncNever: String get() = context.getString(R.string.last_sync_never)
    fun lastSyncTime(time: String): String = context.getString(R.string.last_sync_time, time)
    val syncFailed: String get() = context.getString(R.string.sync_failed)
    val loginFailed: String get() = context.getString(R.string.login_failed)
    val emailRegistered: String get() = context.getString(R.string.email_registered)
    val invalidInput: String get() = context.getString(R.string.invalid_input)
    val networkUnavailable: String get() = context.getString(R.string.network_unavailable)
    val unknownError: String get() = context.getString(R.string.unknown_error)
    val logoutConfirmText: String get() = context.getString(R.string.logout_confirm_text)
    val noteLabel: String get() = context.getString(R.string.note_label)
    val notePlaceholder: String get() = context.getString(R.string.note_placeholder)
    val recordDetailTitle: String get() = context.getString(R.string.record_detail_title)
    val noNote: String get() = context.getString(R.string.no_note)
    val deleteRecordAction: String get() = context.getString(R.string.delete_record_action)
    val share: String get() = context.getString(R.string.share)
    val shareStatsTitle: String get() = context.getString(R.string.share_stats_title)
    val shareStatsDesc: String get() = context.getString(R.string.share_stats_desc)
    val saveToGallery: String get() = context.getString(R.string.save_to_gallery)
    val saveSuccess: String get() = context.getString(R.string.save_success)
    val saveFailed: String get() = context.getString(R.string.save_failed)
    val appName: String get() = context.getString(R.string.app_name)
    val shareHistoryDesc: String get() = context.getString(R.string.share_history_desc)
    val importantDatesTitle: String get() = context.getString(R.string.important_dates_title)
    val noImportantDatesTitle: String get() = context.getString(R.string.no_important_dates_title)
    val noImportantDatesDesc: String get() = context.getString(R.string.no_important_dates_desc)
    fun daysUntil(count: Int): String = context.getString(R.string.days_until, count)
    fun daysSince(count: Int): String = context.getString(R.string.days_since, count)
    val dayEventToday: String get() = context.getString(R.string.day_event_today)
    val dayEventName: String get() = context.getString(R.string.day_event_name)
    val dayEventNamePlaceholder: String get() = context.getString(R.string.day_event_name_placeholder)
    val dayEventDate: String get() = context.getString(R.string.day_event_date)
    val selectDate: String get() = context.getString(R.string.select_date)
    val repeatsYearly: String get() = context.getString(R.string.repeats_yearly)
    val repeatsYearlyDesc: String get() = context.getString(R.string.repeats_yearly_desc)
    val dayEventNote: String get() = context.getString(R.string.day_event_note)
    val dayEventNew: String get() = context.getString(R.string.day_event_new)
    val dayEventEdit: String get() = context.getString(R.string.day_event_edit)
    val confirmDeleteDayEvent: String get() = context.getString(R.string.confirm_delete_day_event)
    fun dayEventDateText(date: LocalDate): String = date.format(
        DateTimeFormatter.ofPattern(context.getString(R.string.day_event_date_format), locale),
    )
    val calendarSolar: String get() = context.getString(R.string.calendar_solar)
    val calendarLunar: String get() = context.getString(R.string.calendar_lunar)
    val lunarMonthLabel: String get() = context.getString(R.string.lunar_month_label)
    val lunarDayLabel: String get() = context.getString(R.string.lunar_day_label)
    val lunarLeapLabel: String get() = context.getString(R.string.lunar_leap_label)
    fun lunarDateText(month: Int, day: Int, leap: Boolean): String = context.getString(
        if (leap) R.string.lunar_date_leap_format else R.string.lunar_date_format,
        month,
        day,
    )
    val repeatLabel: String get() = context.getString(R.string.repeat_label)
    val repeatNone: String get() = context.getString(R.string.repeat_none)
    val repeatMonthly: String get() = context.getString(R.string.repeat_monthly)
    val repeatYearly: String get() = context.getString(R.string.repeat_yearly)
    val monthlyDayLabel: String get() = context.getString(R.string.monthly_day_label)
    fun monthlyDateText(day: Int): String = context.getString(R.string.monthly_date_format, day)
    val dayEventReminder: String get() = context.getString(R.string.day_event_reminder)
    val dayEventReminderDesc: String get() = context.getString(R.string.day_event_reminder_desc)
    fun dayEventReminderDays(count: Int): String = context.getString(R.string.day_event_reminder_days, count)
    val archivedHabits: String get() = context.getString(R.string.archived_habits)
    val archivedHabitsDesc: String get() = context.getString(R.string.archived_habits_desc)
    val restore: String get() = context.getString(R.string.restore)
    val noArchivedHabits: String get() = context.getString(R.string.no_archived_habits)
    val confirmDeleteHabitPermanent: String get() = context.getString(R.string.confirm_delete_habit_permanent)
    val editTime: String get() = context.getString(R.string.edit_time)
    val selectTime: String get() = context.getString(R.string.select_time)
    val timeHourLabel: String get() = context.getString(R.string.time_hour_label)
    val timeMinuteLabel: String get() = context.getString(R.string.time_minute_label)
    val exportCsv: String get() = context.getString(R.string.export_csv)
    val csvHeader: String get() = context.getString(R.string.csv_header)
    fun exportCsvSuccess(count: Int): String = context.getString(R.string.export_csv_success, count)
    val widgetTitle: String get() = context.getString(R.string.widget_title)
    fun widgetSummary(total: Int, completed: Int, target: Int): String =
        context.getString(R.string.widget_summary, total, completed, target)

    val habitEditorNew: String get() = context.getString(R.string.habit_editor_new)
    val habitEditorEdit: String get() = context.getString(R.string.habit_editor_edit)
    val habitName: String get() = context.getString(R.string.habit_name)
    val habitNamePlaceholder: String get() = context.getString(R.string.habit_name_placeholder)
    val habitIdentifier: String get() = context.getString(R.string.habit_identifier)
    val commonIcons: String get() = context.getString(R.string.common_icons)
    fun habitIconCategoryTitle(key: String): String = when (key) {
        "health" -> context.getString(R.string.habit_icon_category_health)
        "growth" -> context.getString(R.string.habit_icon_category_growth)
        "life" -> context.getString(R.string.habit_icon_category_life)
        "social" -> context.getString(R.string.habit_icon_category_social)
        else -> context.getString(R.string.common_icons)
    }
    val letters: String get() = context.getString(R.string.letters)
    val iconLetter: String get() = context.getString(R.string.icon_letter)
    val iconLetterSupport: String get() = context.getString(R.string.icon_letter_support)
    val primaryColor: String get() = context.getString(R.string.primary_color)
    val dailyTarget: String get() = context.getString(R.string.daily_target)
    val dailyTargetDesc: String get() = context.getString(R.string.daily_target_desc)
    val targetQuickSetHint: String get() = context.getString(R.string.target_quick_set_hint)
    val localReminder: String get() = context.getString(R.string.local_reminder)
    val localReminderDesc: String get() = context.getString(R.string.local_reminder_desc)
    val notificationPermissionOff: String get() = context.getString(R.string.notification_permission_off)
    val enableNotificationPermission: String get() = context.getString(R.string.enable_notification_permission)
    fun reminderTime(hour: Int, minute: Int): String = context.getString(R.string.reminder_time, hour, minute)
    val setReminderHint: String get() = context.getString(R.string.set_reminder_hint)
    val save: String get() = context.getString(R.string.save)
    fun exportSuccess(habitCount: Int, eventCount: Int): String = context.getString(R.string.export_success, habitCount, eventCount)
    fun importSuccess(habitCount: Int, eventCount: Int): String = context.getString(R.string.import_success, habitCount, eventCount)

    fun habitIconLabel(token: String, fallback: String): String {
        val resId = when (token) {
            ":dumbbell" -> R.string.habit_icon_dumbbell
            ":book-open" -> R.string.habit_icon_book_open
            ":droplet" -> R.string.habit_icon_droplet
            ":brain" -> R.string.habit_icon_brain
            ":moon" -> R.string.habit_icon_moon
            ":graduation-cap" -> R.string.habit_icon_graduation_cap
            ":pencil" -> R.string.habit_icon_pencil
            ":footprints" -> R.string.habit_icon_footprints
            ":apple" -> R.string.habit_icon_apple
            ":heart-pulse" -> R.string.habit_icon_heart_pulse
            ":coffee" -> R.string.habit_icon_coffee
            ":music" -> R.string.habit_icon_music
            ":palette" -> R.string.habit_icon_palette
            ":message-circle" -> R.string.habit_icon_message_circle
            ":sun" -> R.string.habit_icon_sun
            ":wind" -> R.string.habit_icon_wind
            ":flower-2" -> R.string.habit_icon_flower_2
            ":target" -> R.string.habit_icon_target
            ":sparkles" -> R.string.habit_icon_sparkles
            ":smile" -> R.string.habit_icon_smile
            ":bike" -> R.string.habit_icon_bike
            ":waves" -> R.string.habit_icon_waves
            ":activity" -> R.string.habit_icon_activity
            ":move-vertical" -> R.string.habit_icon_move_vertical
            ":mountain" -> R.string.habit_icon_mountain
            ":code" -> R.string.habit_icon_code
            ":languages" -> R.string.habit_icon_languages
            ":pen-tool" -> R.string.habit_icon_pen_tool
            ":book-text" -> R.string.habit_icon_book_text
            ":home" -> R.string.habit_icon_home
            ":coins" -> R.string.habit_icon_coins
            ":timer" -> R.string.habit_icon_timer
            ":cigarette-off" -> R.string.habit_icon_cigarette_off
            ":stethoscope" -> R.string.habit_icon_stethoscope
            ":pill" -> R.string.habit_icon_pill
            ":film" -> R.string.habit_icon_film
            ":gamepad-2" -> R.string.habit_icon_gamepad_2
            ":camera" -> R.string.habit_icon_camera
            ":users" -> R.string.habit_icon_users
            ":user-plus" -> R.string.habit_icon_user_plus
            ":hand-heart" -> R.string.habit_icon_hand_heart
            ":file-text" -> R.string.habit_icon_file_text
            ":video" -> R.string.habit_icon_video
            ":clipboard-check" -> R.string.habit_icon_clipboard_check
            else -> null
        }
        return resId?.let(context::getString) ?: fallback
    }
}

@Composable
fun rememberPulseStrings(language: AppLanguage): PulseStrings {
    val baseContext = LocalContext.current
    return remember(baseContext, language) {
        val locale = Locale.forLanguageTag(appLanguageTag(language))
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(locale)
            setLocales(android.os.LocaleList(locale))
        }
        PulseStrings(baseContext.createConfigurationContext(configuration))
    }
}

fun appLanguageTag(language: AppLanguage): String = when (language) {
    AppLanguage.ZH -> "zh-CN"
    AppLanguage.EN -> "en"
}

val LocalPulseStrings = staticCompositionLocalOf<PulseStrings> {
    error("PulseStrings not provided")
}
