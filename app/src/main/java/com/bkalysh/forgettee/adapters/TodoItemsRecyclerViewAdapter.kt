package com.bkalysh.forgettee.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bkalysh.forgettee.database.models.ToDoItem
import com.bkalysh.forgettee.databinding.ItemTodoBinding

class TodoItemsRecyclerViewAdapter(private val context: Context, private val toggleListener: OnTodoToggleListener) : RecyclerView.Adapter<TodoItemsRecyclerViewAdapter.TodoViewHolder>() {
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

            //TODO implement item done state displaying
            root.setOnClickListener {
                // TODO implement done state change
                toggleListener.onTodoClicked(toDoItem)
            }
        }
    }


    inner class TodoViewHolder(val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root)

    interface OnTodoToggleListener {
        fun onTodoClicked(toDoItem: ToDoItem)
    }

    private val diffCallback = object : DiffUtil.ItemCallback<ToDoItem>() {
        override fun areItemsTheSame(oldItem: ToDoItem, newItem: ToDoItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ToDoItem, newItem: ToDoItem): Boolean {
            return oldItem.text == newItem.text
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)
}