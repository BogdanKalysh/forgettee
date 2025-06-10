package com.bkalysh.forgettee.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import com.bkalysh.forgettee.R
import com.bkalysh.forgettee.adapters.TodoItemsRecyclerViewAdapter
import com.bkalysh.forgettee.database.models.ToDoItem
import com.bkalysh.forgettee.databinding.ActivityMainBinding
import com.bkalysh.forgettee.utils.Utils.focusOnEditText
import com.bkalysh.forgettee.utils.Utils.hideKeyboard
import com.bkalysh.forgettee.utils.Utils.parseTodoItemsFromInput
import com.bkalysh.forgettee.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Date

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModel()

    private lateinit var binding: ActivityMainBinding
    private lateinit var toDoItemsAdapter: TodoItemsRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val bottomInset = if (isKeyboardVisible) ime.bottom else systemBars.bottom

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomInset)
            insets
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeTasks.collect { todoItems ->
                    toDoItemsAdapter.todoItems = todoItems
                }
            }
        }

        setupTodoRecyclerViewAdapter()
        setupAddTodoPopupButton()
        setupDimmer()
        setupAddTodoItemButton()
    }

    private fun setupTodoRecyclerViewAdapter() {
        toDoItemsAdapter = TodoItemsRecyclerViewAdapter(
            object: TodoItemsRecyclerViewAdapter.OnTodoToggleListener {
                override fun onTodoClicked(toDoItem: ToDoItem) {
                    val updatedItem = toDoItem.copy(isDone = !toDoItem.isDone)

                    viewModel.updateTodoItem(updatedItem)
                }
            },
            object : TodoItemsRecyclerViewAdapter.OnItemSwipedListener {
                override fun onItemSwiped(toDoItem: ToDoItem) {
                    val updatedItem = toDoItem.copy(isRemoved = true, doneAt = Date())
                    viewModel.updateTodoItem(updatedItem)
                }
            })

        binding.rvTodoList.apply {
            adapter = toDoItemsAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            // disabling change animation for quick isDone state updates
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }

        setupItemSwipeHelper()
    }

    private fun setupItemSwipeHelper() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = toDoItemsAdapter.todoItems[position]
                toDoItemsAdapter.todoItems = toDoItemsAdapter.todoItems.toMutableList().apply { removeAt(position) }
                toDoItemsAdapter.onItemSwiped(item)
            }

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val position = viewHolder.adapterPosition
                val item = toDoItemsAdapter.todoItems[position]

                return if (item.isDone) {
                    val swipeFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                    makeMovementFlags(0, swipeFlags)
                } else {
                    makeMovementFlags(0, 0)
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.rvTodoList)
    }

    private fun setupAddTodoPopupButton() {
        binding.btnAdd.setOnClickListener { openAddPopup() }
    }

    private fun setupDimmer() {
        binding.dimmer.setOnClickListener { dismissAddPopup() }
    }

    private fun setupAddTodoItemButton() {
        binding.btnAddTodo.setOnClickListener {
            val todoText = binding.etTodoText.text.toString()
            val todoItems = parseTodoItemsFromInput(todoText)

            if (todoItems.isNotEmpty()) {
                todoItems.forEach(viewModel::insertTodoItem)
                dismissAddPopup()
            } else {
                Toast.makeText(this,
                    getString(R.string.enter_todo_task_description),
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openAddPopup() {
        binding.dimmer.visibility = View.VISIBLE
        focusOnEditText(binding.etTodoText)
        binding.popupAddTodo.visibility = View.VISIBLE
    }

    private fun dismissAddPopup() {
        binding.dimmer.visibility = View.INVISIBLE
        binding.etTodoText.text.clear()
        binding.popupAddTodo.visibility = View.INVISIBLE
        hideKeyboard(binding.etTodoText)
    }
}