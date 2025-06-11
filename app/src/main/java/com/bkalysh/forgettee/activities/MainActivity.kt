package com.bkalysh.forgettee.activities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.animation.ValueAnimator
import androidx.core.graphics.withClip
import androidx.core.view.ViewCompat
import android.view.animation.AccelerateInterpolator
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
import com.bkalysh.forgettee.utils.Utils.vibrate
import com.bkalysh.forgettee.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import java.util.Date
import kotlin.math.abs
import kotlin.math.hypot

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
        setupMenu()
        setupDimmer()
        setupAddTodoItemButton()
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
                    val updatedItem = toDoItem.copy(isRemoved = true, doneAt = Date())
                    viewModel.updateTodoItem(updatedItem)
                }
            },
            object : TodoItemsRecyclerViewAdapter.OnReorderListener {
                override fun onReorder(reorderedItems: List<ToDoItem>) {
                    viewModel.updateAllTodoItems(reorderedItems)
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



    // TODO move out of Main activity


    data class CircleState(
        var radius: Float = 0f,
        var animating: Boolean = false,
        var expanded: Boolean = false,
        var removed: Boolean = false
    ) {
        fun reset() {
            radius = 0f
            removed = false
            expanded = false
            animating = false
        }
    }

    private fun setupItemSwipeHelper() {
        val paint = Paint().apply {
            color = Color.RED
            isAntiAlias = true
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun onMove(
                recyclerView: RecyclerView,
                source: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return toDoItemsAdapter.onItemMove(source.adapterPosition, target.adapterPosition)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = toDoItemsAdapter.todoItems[position]
                toDoItemsAdapter.todoItems = toDoItemsAdapter.todoItems.toMutableList().apply { removeAt(position) }
                toDoItemsAdapter.onItemSwiped(item)
                circleState.apply {
                    removed = true
                }
            }

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val position = viewHolder.adapterPosition
                val item = toDoItemsAdapter.todoItems[position]

                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END

                return if (item.isDone) {
                    makeMovementFlags(dragFlags, swipeFlags)
                } else {
                    makeMovementFlags(dragFlags, 0)
                }
            }

            override fun isLongPressDragEnabled(): Boolean = true

            private val circleState = CircleState()

            private var seekForward = true
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                // implementing vibration on swipe
                val itemView = viewHolder.itemView
                val swipeThreshold = 0.5f
                val swipeProgress = abs(dX) / itemView.width

                if (isCurrentlyActive) {
                    if (seekForward && swipeProgress > swipeThreshold) {
                        seekForward = false
                        vibrate(viewHolder.itemView.context)
                    } else if (!seekForward && swipeProgress < swipeThreshold) {
                        seekForward = true
                        vibrate(viewHolder.itemView.context)
                    }
                } else {
                    seekForward = true
                }

                //Drawing back wave
                val width = itemView.width.toFloat()
                val height = itemView.height.toFloat()
                val centerX = if (dX > 0) itemView.left.toFloat() else itemView.right.toFloat()
                val centerY = itemView.top + height / 2f
                val maxRadius = hypot(width, height)

                val radiusState = circleState

                val itemCornerRadius = resources.getDimensionPixelSize(R.dimen.item_corner_radius)
                val clipPath = Path().apply {
                    addRoundRect(
                        itemView.left.toFloat(),
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat(),
                        itemCornerRadius.toFloat(),
                        itemCornerRadius.toFloat(),
                        Path.Direction.CW
                    )
                }

                // handling clearing swipe wave after removing
                if (swipeProgress <= 0.5f && radiusState.removed) {
                    radiusState.reset()
                }

                if (swipeProgress > 0.5f && !radiusState.expanded && !radiusState.animating) {
                    radiusState.removed = false
                    radiusState.animating = true
                    ValueAnimator.ofFloat(radiusState.radius, maxRadius).apply {
                        duration = 300
                        interpolator = AccelerateDecelerateInterpolator()
                        addUpdateListener {
                            radiusState.radius = it.animatedValue as Float
                            recyclerView.invalidate()
                        }
                        doOnEnd {
                            radiusState.animating = false
                            radiusState.expanded = true
                        }
                        start()
                    }
                } else if (swipeProgress <= 0.5f && radiusState.expanded && !radiusState.animating) {
                    radiusState.animating = true
                    ValueAnimator.ofFloat(radiusState.radius, 0f).apply {
                        duration = 200
                        interpolator = AccelerateInterpolator()
                        addUpdateListener {
                            radiusState.radius = it.animatedValue as Float
                            recyclerView.invalidate()
                        }
                        doOnEnd {
                            radiusState.animating = false
                            radiusState.expanded = false
                        }
                        start()
                    }
                }

                if (radiusState.radius > 0f) {
                    c.withClip(clipPath) {
                        c.drawCircle(centerX, centerY, radiusState.radius, paint)
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.rvTodoList)
    }

    // TODO move out of Main activity

    private fun setupAddTodoPopupButton() {
        binding.btnAdd.setOnClickListener { openAddPopup() }
    }

    private fun setupDimmer() {
        binding.dimmer.setOnClickListener { closeAllPopups() }
    }

    private fun setupMenu() {
        binding.btnMenu.setOnClickListener {
            openMenu()
        }
        setupDeleteAllButton()
    }

    private fun setupAddTodoItemButton() {
        binding.btnAddTodo.setOnClickListener {
            val todoText = binding.etTodoText.text.toString()
            val listSize = toDoItemsAdapter.todoItems.size
            val todoItems = parseTodoItemsFromInput(todoText, listSize)

            if (todoItems.isNotEmpty()) {
                todoItems.forEach(viewModel::insertTodoItem)
                closeAllPopups()
            } else {
                Toast.makeText(this,
                    getString(R.string.enter_todo_task_description),
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDeleteAllButton() {
        binding.btnDeleteAll.setOnClickListener {
            viewModel.removeAllActiveTodoItems()
            closeAllPopups()
        }
    }

    private fun openMenu() {
        binding.dimmer.visibility = View.VISIBLE
        binding.clMenu.visibility = View.VISIBLE
    }

    private fun openAddPopup() {
        binding.dimmer.visibility = View.VISIBLE
        focusOnEditText(binding.etTodoText)
        binding.popupAddTodo.visibility = View.VISIBLE
    }

    private fun closeAllPopups() {
        binding.dimmer.visibility = View.INVISIBLE
        binding.etTodoText.text.clear()
        binding.popupAddTodo.visibility = View.INVISIBLE
        binding.clMenu.visibility = View.INVISIBLE
        hideKeyboard(binding.etTodoText)
    }
}