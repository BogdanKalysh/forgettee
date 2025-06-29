package com.bkalysh.forgettee.adapters

import android.icu.util.Calendar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bkalysh.forgettee.R
import com.bkalysh.forgettee.database.models.ToDoItem
import com.bkalysh.forgettee.databinding.ItemArchivedTodoBinding
import java.text.SimpleDateFormat
import java.util.Locale


class ArchiveTodoItemsRecyclerViewAdapter(private val onDeleteListener: OnDeleteTodoListener) : RecyclerView.Adapter<ArchiveTodoItemsRecyclerViewAdapter.TodoViewHolder>() {
    var todoItems: List<ToDoItem>
        get() = differ.currentList
        set(value) {
            differ.submitList(value)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        return TodoViewHolder(
            ItemArchivedTodoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )
    }

    override fun getItemCount(): Int = todoItems.size

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.binding.apply {
            val toDoItem = todoItems[position]
            val currentCalendar = Calendar.getInstance()
            currentCalendar.time = toDoItem.doneAt

            // Displaying task text
            tvTaskText.text = toDoItem.text


            // Displaying week separator
            val currentWeek = currentCalendar.get(Calendar.WEEK_OF_YEAR)
            val showWeekSeparator = position == 0 || run {
                val prevCalendar = Calendar.getInstance()
                prevCalendar.time = todoItems[position - 1].doneAt
                val prevWeek = prevCalendar.get(Calendar.WEEK_OF_YEAR)
                prevWeek != currentWeek
            }

            if (showWeekSeparator) {
                containerWeekSeparator.visibility = View.VISIBLE

                val weekStart = currentCalendar.clone() as Calendar
                weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)

                val weekEnd = currentCalendar.clone() as Calendar
                weekEnd.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek + 6)

                val formatter = SimpleDateFormat("d MMM", Locale.getDefault())
                val weekStartStr = formatter.format(weekStart.time)
                val weekEndStr = formatter.format(weekEnd.time)

                val weekRangeText = holder.itemView.context.getString(R.string.week_range, weekEndStr, weekStartStr)
                tvWeekSeparatorText.text = weekRangeText
            } else {
                containerWeekSeparator.visibility = View.GONE
            }


            // Displaying date separator
            val currentDay = currentCalendar.get(Calendar.DAY_OF_YEAR)
            val showDaySeparator = position == 0 || run {
                val prevCalendar = Calendar.getInstance()
                prevCalendar.time = todoItems[position - 1].doneAt
                val prevDay = prevCalendar.get(Calendar.DAY_OF_YEAR)
                prevDay != currentDay
            }

            if (showDaySeparator) {
                tvDaySeparator.visibility = View.VISIBLE
                val formatter = SimpleDateFormat("d MMM", Locale.getDefault())
                val dateStr = formatter.format(currentCalendar.time)
                tvDaySeparator.text = dateStr
            } else {
                tvDaySeparator.visibility = View.GONE
            }

            // Display day count on chip
            val daysToFinish = run {
                val startCalendar = Calendar.getInstance()
                startCalendar.time = toDoItem.createdAt
                currentCalendar.get(Calendar.DAY_OF_YEAR) - startCalendar.get(Calendar.DAY_OF_YEAR) + 1
            }

            val context = holder.itemView.context
            val dayCountText = context.resources.getQuantityString(R.plurals.day_count, daysToFinish, daysToFinish)
            tvDayCount.text = dayCountText

            // setting up delete item button
            btnDeleteItem.setOnClickListener {
                onDeleteListener.onTodoDelete(toDoItem)
            }
        }
    }

    inner class TodoViewHolder(val binding: ItemArchivedTodoBinding) : RecyclerView.ViewHolder(binding.root)
    private val diffCallback = object : DiffUtil.ItemCallback<ToDoItem>() {
        override fun areItemsTheSame(oldItem: ToDoItem, newItem: ToDoItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ToDoItem, newItem: ToDoItem): Boolean {
            return oldItem == newItem
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)

    interface OnDeleteTodoListener {
        fun onTodoDelete(toDoItem: ToDoItem)
    }
}