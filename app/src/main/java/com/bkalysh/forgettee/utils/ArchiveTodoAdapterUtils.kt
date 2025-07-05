package com.bkalysh.forgettee.utils

import android.content.Context
import android.icu.util.Calendar
import com.bkalysh.forgettee.R
import com.bkalysh.forgettee.adapters.ArchiveTodoItemsRecyclerViewAdapter.UiItem
import com.bkalysh.forgettee.database.models.ToDoItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ArchiveTodoAdapterUtils {
    fun generateUiItems(toDoItems: List<ToDoItem>, withWeekSeparator: Boolean, context: Context): List<UiItem> {
        val groupedByWeek = toDoItems
            .sortedByDescending { it.doneAt }
            .groupBy { getWeekKey(it.doneAt) }

        val uiItems = mutableListOf<UiItem>()

        groupedByWeek.values.forEach { itemsInWeek ->
            if (withWeekSeparator) {
                val weekLabel = formatWeekRange(itemsInWeek.first().doneAt, context)
                uiItems.add(UiItem.WeekSeparator(weekLabel))
            }

            val groupedByDay = itemsInWeek.groupBy { getDayKey(it.doneAt) }

            groupedByDay.values.forEach { itemsInDay ->
                val dayLabel = formatDay(itemsInDay.first().doneAt)
                val weekDayLabel = formatWeekDay(itemsInDay.first().doneAt)
                uiItems.add(UiItem.DaySeparator(dayLabel, weekDayLabel))

                uiItems.addAll(itemsInDay.map { UiItem.ToDo(it) })
            }
        }

        return uiItems
    }

    fun formatCompleteTime(dateCompleted: Date): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(dateCompleted)
    }

    private fun getWeekKey(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val week = calendar.get(Calendar.WEEK_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        return "$year-W$week"
    }

    private fun getDayKey(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val day = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        return "$year-D$day"
    }

    private fun formatWeekRange(date: Date, context: Context): String {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val weekStart = calendar.clone() as Calendar
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)

        val weekEnd = calendar.clone() as Calendar
        weekEnd.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek + 6)

        val formatter = SimpleDateFormat("d MMM", Locale.getDefault())
        val weekStartStr = formatter.format(weekStart.time)
        val weekEndStr = formatter.format(weekEnd.time)

        val weekEndYear = weekEnd.get(Calendar.YEAR)
        val yearSuffix = if (weekEndYear < currentYear) ", $weekEndYear" else ""

        return context.getString(R.string.week_range, weekStartStr, weekEndStr, yearSuffix)
    }


    private fun formatDay(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val formatter = SimpleDateFormat("d MMMM", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    private fun formatWeekDay(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val formatter = SimpleDateFormat("EEEE", Locale.getDefault())
        return formatter.format(calendar.time)
    }
}