package com.bkalysh.forgettee.adapters.helpers

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.withClip
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bkalysh.forgettee.R
import com.bkalysh.forgettee.adapters.TodoItemsRecyclerViewAdapter
import com.bkalysh.forgettee.utils.Utils.isDarkTheme
import com.bkalysh.forgettee.utils.Utils.vibrate
import kotlin.math.abs
import kotlin.math.hypot


class TodoItemTouchHelperCallback(
    private val toDoItemsAdapter: TodoItemsRecyclerViewAdapter,
    private val context: Context) : ItemTouchHelper.Callback() {

    private val redPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.theme_red_translucent_50)
        isAntiAlias = true
    }
    private val bluePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.accent_blue_translucent_50)
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = if (isDarkTheme(context)) {
            ContextCompat.getColor(context, R.color.bright_91)
        } else {
            ContextCompat.getColor(context, R.color.dark_27)
        }
        isAntiAlias = true
    }
    private var currentPaint = redPaint

    private var isSwipeBlocked = false

    private data class CircleState(
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
        when (direction) {
            ItemTouchHelper.RIGHT -> toDoItemsAdapter.onItemSwipedRight(item)
            ItemTouchHelper.LEFT -> toDoItemsAdapter.onItemSwipedLeft(item)
        }
        circleStates[position]?.apply {
            removed = true // for animation
        }
        seekForward = true // start to seek forward vibration threshold after swipe
    }

    fun blockSwipe() {isSwipeBlocked = true}
    fun unBlockSwipe() {isSwipeBlocked = false}

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN

        val toDoItem = toDoItemsAdapter.todoItems.getOrNull(viewHolder.adapterPosition)
        val swipeFlags = ItemTouchHelper.LEFT or
                if (toDoItem != null && toDoItem.isDone) {
                    ItemTouchHelper.RIGHT
                } else {
                    0
                }
        return makeMovementFlags(dragFlags, if (isSwipeBlocked) 0 else swipeFlags)
    }

    override fun isLongPressDragEnabled(): Boolean = true

    // a map of animation states to be able to draw two animations
    // simultaneously if two items are swiped quickly
    private val circleStates = mutableMapOf<Int, CircleState>()

    // Flag which shows if we need to seek for passing the threshold
    // value for vibration
    private var seekForward = true

    // Flag which shows if animation was expanded to opposite direction
    // in case of quick swipe direction change. To not draw a fully
    // expanded animation on the opposite side
    private var isRightSwipeExpanded = false

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val swipeProgress = abs(dX) / itemView.width
        val width = itemView.width.toFloat()
        val height = itemView.height.toFloat()
        val centerX = if (dX > 0) itemView.left.toFloat() else itemView.right.toFloat()
        val centerY = itemView.top + height / 2f
        val animationPosition = viewHolder.adapterPosition
        val maxRadius = hypot(width, height)

        // Vibrating on swipe
        val vibrationThreshold = 0.5f
        val vibrationLimit = 1f
        if (seekForward && swipeProgress > vibrationThreshold
            && swipeProgress < vibrationLimit // don't vibrate on frames drawn after swipe
            ) {
            seekForward = false
            vibrate(viewHolder.itemView.context)
        } else if (!seekForward && swipeProgress < vibrationThreshold) {
            seekForward = true
            vibrate(viewHolder.itemView.context)
        }

        //Drawing back wave
        if (animationPosition == -1) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }
        if (circleStates[animationPosition] == null) {
            circleStates[animationPosition] = CircleState()
        }
        val circleState = circleStates.getOrDefault(animationPosition, CircleState())

        val itemCornerRadius = context.resources.getDimensionPixelSize(R.dimen.item_corner_radius)
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

        // handling quick return to 0 while animation still in progress
        if (swipeProgress == 0f) {
            circleState.reset()
        }

        // handling clearing swipe wave after removing
        if (swipeProgress <= 0.5f && circleState.removed) {
            circleState.reset()
        }

        // handling quick swipe direction change after expand
        if (swipeProgress < 0.5f && (isRightSwipeExpanded != dX > 0)) {
            circleState.reset()
        }

        // handling quick swiping the other view after delete
        if (animationPosition != viewHolder.adapterPosition &&
            viewHolder.adapterPosition != -1) {
            circleState.reset()
        }

        if (swipeProgress > 0.5f && !circleState.expanded && !circleState.animating) {
            isRightSwipeExpanded = dX > 0
            currentPaint = if (dX > 0) {
                redPaint
            } else {
                bluePaint
            }
            circleState.removed = false
            circleState.animating = true
            ValueAnimator.ofFloat(circleState.radius, maxRadius).apply {
                duration = 150
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    circleState.radius = it.animatedValue as Float
                    recyclerView.invalidate()
                }
                doOnEnd {
                    circleState.animating = false
                    circleState.expanded = true
                }
                start()
            }
        } else if (swipeProgress <= 0.5f && circleState.expanded && !circleState.animating) {
            circleState.animating = true
            ValueAnimator.ofFloat(circleState.radius, 0f).apply {
                duration = 150
                interpolator = AccelerateInterpolator()
                addUpdateListener {
                    circleState.radius = it.animatedValue as Float
                    recyclerView.invalidate()
                }
                doOnEnd {
                    circleState.animating = false
                    circleState.expanded = false
                }
                start()
            }
        }

        if (circleState.radius > 0f) {
            c.withClip(clipPath) {
                c.drawCircle(centerX, centerY, circleState.radius, currentPaint)
            }
        }

        // drawing text with swipe function explanation§
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && abs(dX) > 0) {
            val textCenterY = itemView.top + itemView.height / 2f + textPaint.textSize / 3

            val text = if (dX > 0) {
                context.getString(R.string.complete)
            } else {
                context.getString(R.string.edit)
            }

            textPaint.textSize = context.resources.getDimension(R.dimen.swipe_function_text_size)
            textPaint.typeface = ResourcesCompat.getFont(context, R.font.montserrat_semibold)
            textPaint.textAlign = Paint.Align.CENTER

            val textCenterX = if (dX > 0) {
                itemView.left + dX / 2f
            } else {
                itemView.right + dX / 2f
            }

            c.withClip(clipPath) {
                c.drawText(text, textCenterX, textCenterY, textPaint)
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            val context = viewHolder?.itemView?.context ?: return
            val drawable = ContextCompat.getDrawable(context, R.drawable.todo_item_hover_foreground)
            viewHolder.itemView.foreground = drawable
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.foreground = null
    }
}