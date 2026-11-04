package com.FreeRave.remindme.ui.reminder

internal sealed interface ReminderFormEvent {
    data class Title(val value: String) : ReminderFormEvent
    data class Description(val value: String) : ReminderFormEvent
    data class Prompt(val value: String) : ReminderFormEvent
    data class Category(val value: String) : ReminderFormEvent
    data class Time(val value: Long) : ReminderFormEvent
    data class Recurring(val enabled: Boolean) : ReminderFormEvent
    data class Interval(val value: Long?) : ReminderFormEvent
    data class EndTime(val value: Long?) : ReminderFormEvent
    data class LocationEnabled(val enabled: Boolean) : ReminderFormEvent
    data class Location(val latitude: Double?, val longitude: Double?, val radius: Float) : ReminderFormEvent
    data class Transition(val value: Int) : ReminderFormEvent
    data class DwellTime(val value: Long?) : ReminderFormEvent
    data class Priority(val value: Int) : ReminderFormEvent
    data class Save(val customId: String) : ReminderFormEvent
    data object SaveTemplate : ReminderFormEvent
    data object Parse : ReminderFormEvent
    data object Templates : ReminderFormEvent
    data object Back : ReminderFormEvent
}
