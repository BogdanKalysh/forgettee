package com.bkalysh.forgettee.utils

import android.content.Context
import android.content.res.Resources
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bkalysh.forgettee.R
import com.bkalysh.forgettee.database.models.ToDoItem
import java.util.Date


object Utils {
    fun focusOnEditText(editText: EditText) {
        editText.requestFocus()
        val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun parseTodoItemFromInput(input: String, position: Int): ToDoItem {
        val now = Date()
        return ToDoItem(
            text = input,
            createdAt = now,
            isDone = false,
            doneAt = now,
            isRemoved = false,
            position = position
        )
    }

    fun increaseTodoItemsPositions(todoItems: List<ToDoItem>): List<ToDoItem> {
        return todoItems
                .map { item -> item.copy(position = item.position + 1) }
    }


    fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }

    fun setFirstLetterRed(textView: TextView) {
        val text = textView.text.toString()
        val spannable = SpannableString(text)
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(textView.context, R.color.theme_red)),
            0,
            1,
            SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textView.text = spannable
    }

    fun isDarkTheme(context: Context): Boolean {
        return (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    const val SHARED_PREFERENCES_SETTINGS_NAME = "settings"
    const val SHARED_PREFERENCES_THEME_MODE_ITEM = "theme_mode"
    const val SHARED_PREFERENCES_DB_PREPOPULATED_ITEM = "db_prepopulated"
}