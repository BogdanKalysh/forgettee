package com.bkalysh.forgettee.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.bkalysh.forgettee.database.models.ToDoItem
import java.util.Date


object Utils {
    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun focusOnEditText(editText: EditText) {
        editText.requestFocus()
        val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    fun parseTodoItemsFromInput(input: String, listSize: Int): List<ToDoItem> {
        val now = Date()
        return input
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { index, line ->
                ToDoItem(
                    text = line,
                    createdAt = now,
                    isDone = false,
                    doneAt = now,
                    isRemoved = false,
                    position = listSize + index
                )
            }
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
}