package com.bkalysh.forgettee.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.bkalysh.forgettee.utils.Utils.SHARED_PREFERENCES_DB_PREPOPULATED_ITEM
import com.bkalysh.forgettee.utils.Utils.SHARED_PREFERENCES_SETTINGS_NAME
import com.bkalysh.forgettee.utils.Utils.SHARED_PREFERENCES_THEME_MODE_ITEM
import com.bkalysh.forgettee.utils.Utils.isDarkTheme

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModel()

    private lateinit var binding: ActivityMainBinding
    private lateinit var toDoItemsAdapter: TodoItemsRecyclerViewAdapter

    private lateinit var todoPopupBinding: PopupAddTodoBinding
    private lateinit var todoPopup: BottomSheetDialog

    private lateinit var itemSwipeHelperCallback: TodoItemTouchHelperCallback

    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // applying insets via custom spacers, so the dimmer can cover the whole screen
            val topLP = binding.flTopSpacer.layoutParams
            topLP.height = systemBars.top
            binding.flTopSpacer.layoutParams = topLP
            val bottomLP = binding.flBottomSpacer.layoutParams
            bottomLP.height = systemBars.bottom
            binding.flBottomSpacer.layoutParams = bottomLP
            insets
        }
        sharedPref = getSharedPreferences(SHARED_PREFERENCES_SETTINGS_NAME, MODE_PRIVATE)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeTasks.collect { todoItems ->
                    toDoItemsAdapter.todoItems = todoItems
                    if (todoItems.isEmpty()) {
                        binding.containerEmptyListPlaceholder.visibility = View.VISIBLE
                    } else {
                        binding.containerEmptyListPlaceholder.visibility = View.GONE
                    }
                }
            }
        }

        setupTodoRecyclerViewAdapter()
        setupAddTodoPopupButton()
        setupMenu()
        setupDimmer()
        prepopulateDbIfNeeded()
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
                override fun onItemSwipedRight(toDoItem: ToDoItem) {
                    val updatedItem = toDoItem.copy(isRemoved = true, doneAt = Date())
                    viewModel.updateTodoItem(updatedItem)
                }
                override fun onItemSwipedLeft(toDoItem: ToDoItem) {
                    itemSwipeHelperCallback.blockSwipe()
                    openEditTodoPopup(toDoItem)
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
        itemSwipeHelperCallback = TodoItemTouchHelperCallback(toDoItemsAdapter, this)
        val itemTouchHelper = ItemTouchHelper(itemSwipeHelperCallback)
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
        setupArchiveButton()
        setupDarkThemeButton()
    }

    private fun openAddTodoPopup() {
        todoPopupBinding = PopupAddTodoBinding.inflate(layoutInflater)
        todoPopupBinding.btnDeleteItem.visibility = View.GONE
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
        todoPopup.setOnDismissListener {
            // on dismissing the popup without editing task we need to add item back to the adapter
            toDoItemsAdapter.todoItems = viewModel.activeTasks.value

            //unblocking the swipe after clicking away
            itemSwipeHelperCallback.unBlockSwipe()
        }
        todoPopup.show()
        setupSaveTodoItemButton(toDoItem)
        setupDeleteTodoItemButton(toDoItem)
        setupTodoEdittext()
    }

    private fun setupSaveTodoItemButton(toDoItem: ToDoItem) {
        todoPopupBinding.btnAddOrSaveTodo.text = getString(R.string.save_button)
        todoPopupBinding.btnAddOrSaveTodo.setOnClickListener {
            vibrate(this@MainActivity)
            val updatedTodoText = todoPopupBinding.etTodoText.text.toString().trim()
            if (updatedTodoText.isNotEmpty()) {
                itemSwipeHelperCallback.unBlockSwipe() //unblocking the swipe after updating item
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
    }

    private fun setupDeleteTodoItemButton(toDoItem: ToDoItem) {
        todoPopupBinding.btnDeleteItem.setOnClickListener {
            viewModel.deleteTodoItem(toDoItem)
            vibrate(this@MainActivity)
            Toast.makeText(this, getString(R.string.deleted_toast_text, toDoItem.text), Toast.LENGTH_SHORT).show()
            todoPopup.dismiss()
        }
    }

    private fun setupAddTodoItemButton() {
        todoPopupBinding.btnAddOrSaveTodo.setOnClickListener {
            vibrate(this@MainActivity)
            val todoText = todoPopupBinding.etTodoText.text.toString().trim()

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
        todoPopupBinding.etTodoText.doOnTextChanged { text,_,_,_ ->
            val textStr = text.toString().trim()
            if (todoPopupBinding.textviewEmptyWarning.isVisible && !isWarningAnimatingClose && textStr.isNotEmpty()) {
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

    private fun setupArchiveButton() {
        binding.btnArchive.setOnClickListener {
            val archiveIntent = Intent(this, ArchiveActivity::class.java)
            startActivity(archiveIntent)
            closeAllPopups()
        }
    }

    private fun setupDarkThemeButton() {
        val themeMode = sharedPref.getInt(SHARED_PREFERENCES_THEME_MODE_ITEM, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)

        binding.tvChangeTheme.setOnClickListener {
            val newThemeMode =
                if (isDarkTheme(this)) {
                    AppCompatDelegate.MODE_NIGHT_NO
                } else {
                    AppCompatDelegate.MODE_NIGHT_YES
                }
            AppCompatDelegate.setDefaultNightMode(newThemeMode)
            sharedPref.edit { putInt(SHARED_PREFERENCES_THEME_MODE_ITEM, newThemeMode) }
        }
    }

    private fun prepopulateDbIfNeeded() {
        val wasDbPrepopulated = sharedPref.getBoolean(SHARED_PREFERENCES_DB_PREPOPULATED_ITEM, false)

        if (!wasDbPrepopulated) {
            val taskNames = listOf(
                getString(R.string.tutorial_task_finish),
                getString(R.string.tutorial_task_edit),
                getString(R.string.tutorial_task_delete),
                getString(R.string.tutorial_task_reorder),
            )

            val tasks = taskNames.mapIndexed { index, text ->
                val todo = parseTodoItemFromInput(text, index)
                // setting one item to done state for deleting tutorial
                todo.copy(isDone = todo.text == getString(R.string.tutorial_task_delete))
            }

            tasks.forEach { todo ->
                viewModel.insertTodoItem(todo)
            }
            sharedPref.edit { putBoolean(SHARED_PREFERENCES_DB_PREPOPULATED_ITEM, true) }
        }
    }

    private fun openMenu() {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        binding.dimmer.visibility = View.VISIBLE
        val animation = AnimationUtils.loadAnimation(this, R.anim.open_scale_down)
        binding.clMenu.startAnimation(animation)
        binding.clMenu.visibility = View.VISIBLE
    }

    private fun closeAllPopups() {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isDarkTheme(this)
        binding.dimmer.visibility = View.INVISIBLE
        val animation = AnimationUtils.loadAnimation(this, R.anim.close_make_transparent)
        binding.clMenu.startAnimation(animation)
        binding.clMenu.visibility = View.INVISIBLE
    }
}