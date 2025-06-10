package com.bkalysh.forgettee.utils

import android.content.Context
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

    fun parseTodoItemsFromInput(input: String): List<ToDoItem> {
        val now = Date()
        return input
            .lines()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    ToDoItem(
                        text = trimmed,
                        isDone = false,
                        isRemoved = false,
                        createdAt = now,
                        finishedAt = now
                    )
                } else null
            }
    }
}