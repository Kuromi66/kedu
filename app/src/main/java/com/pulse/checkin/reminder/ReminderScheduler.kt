package com.pulse.checkin.reminder

import com.pulse.checkin.domain.model.Habit

interface ReminderScheduler {
    fun scheduleForHabit(habit: Habit)
    fun cancelForHabit(habitId: String)
    fun syncAll(habits: List<Habit>)
}
