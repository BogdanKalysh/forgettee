package com.bkalysh.forgettee.activities

import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import com.bkalysh.forgettee.R
import com.bkalysh.forgettee.adapters.TodoItemsRecyclerViewAdapter
import com.bkalysh.forgettee.database.models.ToDoItem
import com.bkalysh.forgettee.databinding.ActivityMainBinding
import com.bkalysh.forgettee.utils.Utils.focusOnEditText
import com.bkalysh.forgettee.utils.Utils.parseTodoItemFromInput
import com.bkalysh.forgettee.utils.Utils.increaseTodoItemsPositions
import com.bkalysh.forgettee.utils.Utils.vibrate
import com.bkalysh.forgettee.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.bkalysh.forgettee.adapters.helpers.TodoItemTouchHelperCallback
import com.bkalysh.forgettee.databinding.PopupAddTodoBinding
import com.bkalysh.forgettee.utils.Utils.setFirstLetterRed
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Date

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModel()

    private lateinit var binding: ActivityMainBinding
    private lateinit var toDoItemsAdapter: TodoItemsRecyclerViewAdapter

    private lateinit var todoPopupBinding: PopupAddTodoBinding
    private lateinit var todoPopup: BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
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
        setupMenu()
        setupDimmer()

        setFirstLetterRed(binding.tvAppName)
    }

    private fun setupTodoRecyclerViewAdapter() {
        toDoItemsAdapter = TodoItemsRecyclerViewAdapter(
            object: TodoItemsRecyclerViewAdapter.OnTodoToggleListener {
                override fun onTodoClicked(toDoItem: ToDoItem) {
                    val updatedItem = toDoItem.copy(isDone = !toDoItem.isDone)
                    vibrate(this@MainActivity)
                    viewModel.updateTodoItem(updatedItem)
                }
            },
            object : TodoItemsRecyclerViewAdapter.OnItemSwipedListener {
                override fun onItemSwiped(toDoItem: ToDoItem) {
                    if (toDoItem.isDone) {
                        val updatedItem = toDoItem.copy(isRemoved = true, doneAt = Date())
                        viewModel.updateTodoItem(updatedItem)
                    } else {
                        openEditTodoPopup(toDoItem)
                    }
                }
            },
            object : TodoItemsRecyclerViewAdapter.OnReorderListener {
                override fun onReorder(reorderedItems: List<ToDoItem>) {
                    viewModel.updateAllTodoItems(reorderedItems)
                }
            },
            object : TodoItemsRecyclerViewAdapter.OnNewItemAddedToTopListener {
                override fun onTopItemAdded() {
                    binding.rvTodoList.scrollToPosition(0)
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
        val itemTouchHelper = ItemTouchHelper(TodoItemTouchHelperCallback(toDoItemsAdapter, this))
        itemTouchHelper.attachToRecyclerView(binding.rvTodoList)
    }

    private fun setupAddTodoPopupButton() {
        binding.btnAdd.setOnClickListener { openAddTodoPopup() }
    }

    private fun setupDimmer() {
        binding.dimmer.setOnClickListener { closeAllPopups() }
    }

    private fun setupMenu() {
        binding.btnMenu.setOnClickListener {
            openMenu()
        }
        setupDeleteAllButton()
        setupArchiveButton()
    }

    private fun openAddTodoPopup() {
        todoPopupBinding = PopupAddTodoBinding.inflate(layoutInflater)
        todoPopup = BottomSheetDialog(this)
        focusOnEditText(todoPopupBinding.etTodoText)
        todoPopup.setContentView(todoPopupBinding.root)
        todoPopup.show()
        setupAddTodoItemButton()
        setupTodoEdittext()
    }

    private fun openEditTodoPopup(toDoItem: ToDoItem) {
        todoPopupBinding = PopupAddTodoBinding.inflate(layoutInflater)
        todoPopupBinding.etTodoText.setText(toDoItem.text)
        todoPopup = BottomSheetDialog(this)
        focusOnEditText(todoPopupBinding.etTodoText)
        todoPopup.setContentView(todoPopupBinding.root)
        todoPopup.show()
        setupSaveTodoItemButton(toDoItem)
        setupTodoEdittext()
    }

    private fun setupSaveTodoItemButton(toDoItem: ToDoItem) {
        todoPopupBinding.btnAddOrSaveTodo.text = getString(R.string.save_button)
        todoPopupBinding.btnAddOrSaveTodo.setOnClickListener {
            vibrate(this@MainActivity)
            val updatedTodoText = todoPopupBinding.etTodoText.text.toString()
            if (updatedTodoText.isNotEmpty()) {
                val updatedTodo = toDoItem.copy(text = updatedTodoText)
                viewModel.updateTodoItem(updatedTodo)
                todoPopup.dismiss()
            } else {
                if (todoPopupBinding.textviewEmptyWarning.isGone) {
                    val animation = AnimationUtils.loadAnimation(this, R.anim.open_scale_up)
                    todoPopupBinding.textviewEmptyWarning.startAnimation(animation)
                    todoPopupBinding.textviewEmptyWarning.visibility = View.VISIBLE
                }
            }
        }

        // on dismissing the popup without editing task we need to add item back to the adapter
        todoPopup.setOnDismissListener {
            toDoItemsAdapter.todoItems = viewModel.activeTasks.value
        }
    }

    private fun setupAddTodoItemButton() {
        todoPopupBinding.btnAddOrSaveTodo.setOnClickListener {
            vibrate(this@MainActivity)
            val todoText = todoPopupBinding.etTodoText.text.toString()

            if (todoText.isNotEmpty()) {
                val newTodoItem = parseTodoItemFromInput(todoText, 0)
                val updatedItems = increaseTodoItemsPositions(viewModel.activeTasks.value)
                updatedItems.forEach(viewModel::updateTodoItem) // updating positions to free the 0 position for the new item
                viewModel.insertTodoItem(newTodoItem)
                todoPopup.dismiss()
            } else {
                if (todoPopupBinding.textviewEmptyWarning.isGone) {
                    val animation = AnimationUtils.loadAnimation(this, R.anim.open_scale_up)
                    todoPopupBinding.textviewEmptyWarning.startAnimation(animation)
                    todoPopupBinding.textviewEmptyWarning.visibility = View.VISIBLE
                }
            }
        }
    }

    private var isWarningAnimatingClose = false
    private fun setupTodoEdittext() {
        todoPopupBinding.etTodoText.doOnTextChanged { _,_,_,_ ->
            if (todoPopupBinding.textviewEmptyWarning.isVisible && !isWarningAnimatingClose) {
                isWarningAnimatingClose = true
                val animation = AnimationUtils.loadAnimation(this, R.anim.close_scale_down)
                todoPopupBinding.textviewEmptyWarning.startAnimation(animation)
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationRepeat(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        todoPopupBinding.textviewEmptyWarning.visibility = View.GONE
                        isWarningAnimatingClose = false
                    }
                })
            }
        }
    }

    private fun setupDeleteAllButton() {
        binding.btnDeleteAll.setOnClickListener {
            vibrate(this@MainActivity)
            viewModel.removeAllActiveTodoItems()
            closeAllPopups()
        }
    }

    private fun setupArchiveButton() {
        binding.btnArchive.setOnClickListener {
            Toast.makeText(this, "Not added yet 😢", Toast.LENGTH_SHORT).show()
            closeAllPopups()
        }
    }

    private fun openMenu() {
        binding.dimmer.visibility = View.VISIBLE
        val animation = AnimationUtils.loadAnimation(this, R.anim.open_scale_down)
        binding.clMenu.startAnimation(animation)
        binding.clMenu.visibility = View.VISIBLE
    }

    private fun closeAllPopups() {
        binding.dimmer.visibility = View.INVISIBLE
        val animation = AnimationUtils.loadAnimation(this, R.anim.close_scale_up)
        binding.clMenu.startAnimation(animation)
        binding.clMenu.visibility = View.INVISIBLE
    }
}