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
import androidx.core.graphics.withClip
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bkalysh.forgettee.R
import com.bkalysh.forgettee.adapters.TodoItemsRecyclerViewAdapter
import com.bkalysh.forgettee.utils.Utils.vibrate
import kotlin.math.abs
import kotlin.math.hypot


class TodoItemTouchHelperCallback(
    private val toDoItemsAdapter: TodoItemsRecyclerViewAdapter,
    private val context: Context) : ItemTouchHelper.Callback() {

    private val redPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.theme_red_translucent)
        isAntiAlias = true
    }
    private val bluePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.accent_blue_translucent)
        isAntiAlias = true
    }
    private var currentPaint = redPaint

    private var isSwipeBlocked = false

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
        circleStates[position]?.apply {
            removed = true // for animation
        }
    }

    fun blockSwipe() {isSwipeBlocked = true}
    fun unBlockSwipe() {isSwipeBlocked = false}

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, if (isSwipeBlocked) 0 else swipeFlags)
    }

    override fun isLongPressDragEnabled(): Boolean = true

    private val circleStates = mutableMapOf<Int, CircleState>()
    private var seekForward = true
    private var isRightSwipe = false

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
        if (isCurrentlyActive) {
            if (seekForward && swipeProgress > vibrationThreshold) {
                seekForward = false
                vibrate(viewHolder.itemView.context)
            } else if (!seekForward && swipeProgress < vibrationThreshold) {
                seekForward = true
                vibrate(viewHolder.itemView.context)
            }
        } else {
            seekForward = true
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

        // handling clearing swipe wave after removing
        if (swipeProgress <= 0.5f && circleState.removed) {
            circleState.reset()
        }

        // handling quick swipe direction change after expand
        if (swipeProgress < 0.5f && (isRightSwipe != dX > 0)) {
            circleState.reset()
        }

        // handling quick swiping the other view after delete
        if (animationPosition != viewHolder.adapterPosition &&
            viewHolder.adapterPosition != -1) {
            circleState.reset()
        }

        if (swipeProgress > 0.5f && !circleState.expanded && !circleState.animating) {
            isRightSwipe = dX > 0
            currentPaint = if (toDoItemsAdapter.todoItems[animationPosition].isDone) {
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

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}