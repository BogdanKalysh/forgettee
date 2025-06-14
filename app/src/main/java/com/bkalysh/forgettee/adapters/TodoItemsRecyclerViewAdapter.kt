package com.bkalysh.forgettee.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bkalysh.forgettee.R
import com.bkalysh.forgettee.database.models.ToDoItem
import com.bkalysh.forgettee.databinding.ItemTodoBinding
import com.bkalysh.forgettee.utils.Utils.isDarkTheme


class TodoItemsRecyclerViewAdapter(
    private val toggleListener: OnTodoToggleListener,
    private val swipeListener: OnItemSwipedListener,
    private val reorderListener: OnReorderListener,
) : RecyclerView.Adapter<TodoItemsRecyclerViewAdapter.TodoViewHolder>(),
    ItemTouchHelperListener {
    var todoItems: List<ToDoItem>
        get() = differ.currentList
        set(value) {
            differ.submitList(value)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        return TodoViewHolder(
            ItemTodoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )
    }

    override fun getItemCount(): Int = todoItems.size

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.binding.apply {
            val toDoItem = todoItems[position]
            tvTodoText.text = toDoItem.text

            if (toDoItem.isDone) {
                tvTodoText.paintFlags = tvTodoText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                root.setBackgroundResource(R.drawable.todo_item_done_background)
                val textColor = if (isDarkTheme(root.context)) {
                    // In future will change to dark theme color
                    ContextCompat.getColor(root.context, R.color.night_39)
                } else {
                    ContextCompat.getColor(root.context, R.color.gray_50)
                }
                tvTodoText.setTextColor(textColor)
            } else {
                tvTodoText.paintFlags = tvTodoText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                root.setBackgroundResource(R.drawable.todo_item_background)
                val textColor = if (isDarkTheme(root.context)) {
                    // In future will change to dark theme color
                    ContextCompat.getColor(root.context, R.color.bright_91)
                } else {
                    ContextCompat.getColor(root.context, R.color.dark_27)
                }
                tvTodoText.setTextColor(textColor)
            }


            root.setOnClickListener {
                toggleListener.onTodoClicked(toDoItem)
            }
        }
    }

    fun onItemSwiped(toDoItem: ToDoItem) {
        swipeListener.onItemSwiped(toDoItem)
    }

    inner class TodoViewHolder(val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root)

    interface OnTodoToggleListener {
        fun onTodoClicked(toDoItem: ToDoItem)
    }

    interface OnItemSwipedListener {
        fun onItemSwiped(toDoItem: ToDoItem)
    }

    interface OnReorderListener {
        fun onReorder(reorderedItems: List<ToDoItem>)
    }

    private val diffCallback = object : DiffUtil.ItemCallback<ToDoItem>() {
        override fun areItemsTheSame(oldItem: ToDoItem, newItem: ToDoItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ToDoItem, newItem: ToDoItem): Boolean {
            return oldItem == newItem
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        val mutableList = todoItems.toMutableList()
        val movedItem = mutableList.removeAt(fromPosition)
        mutableList.add(toPosition, movedItem)

        mutableList.forEachIndexed { index, item ->
            mutableList[index] = item.copy(position = index)
        }

        todoItems = mutableList
        reorderListener.onReorder(mutableList)

        return true
    }
}

interface ItemTouchHelperListener {
    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean
}