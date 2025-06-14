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

    private val paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.theme_red_translucent)
        isAntiAlias = true
    }

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
    private var isRightSwipe = false
    private var animationPosition = 0

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
        if (swipeProgress <= 0.5f && radiusState.removed) {
            radiusState.reset()
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

        if (swipeProgress > 0.5f && !radiusState.expanded && !radiusState.animating) {
            isRightSwipe = dX > 0
            animationPosition = viewHolder.adapterPosition
            radiusState.removed = false
            radiusState.animating = true
            ValueAnimator.ofFloat(radiusState.radius, maxRadius).apply {
                duration = 150
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
                duration = 150
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
}