package com.bkalysh.forgettee.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bkalysh.forgettee.database.models.ToDoItem
import com.bkalysh.forgettee.databinding.ItemArchiveDaySeparatorBinding
import com.bkalysh.forgettee.databinding.ItemArchiveWeekSeparatorBinding
import com.bkalysh.forgettee.databinding.ItemArchivedTodoBinding
import com.bkalysh.forgettee.utils.ArchiveTodoAdapterUtils.formatCompleteTime

class ArchiveTodoItemsRecyclerViewAdapter(private val onDeleteListener: OnDeleteTodoListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var archiveItems: List<UiItem>
        get() = differ.currentList
        set(value) { differ.submitList(value) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_TODO -> {
                val binding = ItemArchivedTodoBinding.inflate(inflater, parent, false)
                TodoViewHolder(binding)
            }
            VIEW_TYPE_WEEK_HEADER -> {
                val binding = ItemArchiveWeekSeparatorBinding.inflate(inflater, parent, false)
                WeekSeparatorViewHolder(binding)
            }
            VIEW_TYPE_DAY_HEADER -> {
                val binding = ItemArchiveDaySeparatorBinding.inflate(inflater, parent, false)
                DaySeparatorViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (archiveItems[position]) {
            is UiItem.ToDo -> VIEW_TYPE_TODO
            is UiItem.WeekSeparator -> VIEW_TYPE_WEEK_HEADER
            is UiItem.DaySeparator -> VIEW_TYPE_DAY_HEADER
        }
    }

    override fun getItemCount(): Int = archiveItems.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val archiveItem = archiveItems[position]
        when {
            holder is TodoViewHolder && archiveItem is UiItem.ToDo -> {
                holder.bindData(archiveItem.item)
            }
            holder is WeekSeparatorViewHolder && archiveItem is UiItem.WeekSeparator -> {
                holder.bindData(archiveItem.text)
            }
            holder is DaySeparatorViewHolder && archiveItem is UiItem.DaySeparator -> {
                holder.bindData(archiveItem.date, archiveItem.weekDay)
            }
        }
    }

    inner class TodoViewHolder(private val binding: ItemArchivedTodoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(toDoItem: ToDoItem) {
            binding.apply {
                // Display task text
                tvTaskText.text = toDoItem.text
                // Display completion time
                tvCompletionTime.text = formatCompleteTime(toDoItem.doneAt)
                // Setting up delete item button
                btnDeleteItem.setOnClickListener {
                    onDeleteListener.onTodoDelete(toDoItem)
                }
            }
        }
    }
    inner class WeekSeparatorViewHolder(private val binding: ItemArchiveWeekSeparatorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(weekHeader: String) {
            binding.apply {
                tvWeekSeparatorText.text = weekHeader
            }
        }
    }
    inner class DaySeparatorViewHolder(private val binding: ItemArchiveDaySeparatorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(dayHeader: String, weekDayHeader: String) {
            binding.apply {
                tvDate.text = dayHeader
                tvWeekday.text = weekDayHeader
            }
        }
    }

    private val diffCallback = object : DiffUtil.ItemCallback<UiItem>() {
        override fun areItemsTheSame(oldItem: UiItem, newItem: UiItem): Boolean {
            return when {
                oldItem is UiItem.ToDo && newItem is UiItem.ToDo -> oldItem.item.id == newItem.item.id
                oldItem is UiItem.WeekSeparator && newItem is UiItem.WeekSeparator -> oldItem == newItem
                oldItem is UiItem.DaySeparator && newItem is UiItem.DaySeparator -> oldItem == newItem
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: UiItem, newItem: UiItem): Boolean {
            return oldItem == newItem
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)

    interface OnDeleteTodoListener {
        fun onTodoDelete(toDoItem: ToDoItem)
    }

    sealed class UiItem {
        data class ToDo(val item: ToDoItem) : UiItem()
        data class WeekSeparator(val text: String) : UiItem()
        data class DaySeparator(val date: String, val weekDay: String) : UiItem()
    }

    private companion object {
        const val VIEW_TYPE_TODO = 0
        const val VIEW_TYPE_WEEK_HEADER = 1
        const val VIEW_TYPE_DAY_HEADER = 2
    }
}