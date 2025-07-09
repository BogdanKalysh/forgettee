package com.bkalysh.forgettee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bkalysh.forgettee.database.models.ToDoItem
import com.bkalysh.forgettee.database.repository.ToDoItemRepository
import com.bkalysh.forgettee.utils.Utils.increaseTodoItemsPositions
import com.bkalysh.forgettee.utils.Utils.parseTodoItemFromInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ToDoItemRepository): ViewModel() {
    private val _activeTasks = MutableStateFlow<List<ToDoItem>>(emptyList())
    val activeTasks: StateFlow<List<ToDoItem>> = _activeTasks.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllActive().collect { tasks ->
                _activeTasks.value = tasks
            }
        }
    }

    fun createNewTodoItem(todoText: String, addToEnd: Boolean) {
        viewModelScope.launch {
            if (todoText.isNotEmpty()) {
                val currentItems = activeTasks.value

                if (addToEnd) {
                    val newItem = parseTodoItemFromInput(todoText, currentItems.lastOrNull()?.position?.plus(1) ?: 0)
                    repository.insert(newItem)
                } else {
                    val newItem = parseTodoItemFromInput(todoText, 0)
                    val updatedItems = increaseTodoItemsPositions(currentItems)
                    updatedItems.forEach { updateTodoItem(it) }
                    repository.insert(newItem)
                }
            }
        }
    }

    fun insertTodoItem(item: ToDoItem) {
        viewModelScope.launch {
            repository.insert(item)
        }
    }

    fun updateAllTodoItems(items: List<ToDoItem>) {
        viewModelScope.launch {
            repository.updateAll(items)
        }
    }

    fun updateTodoItem(item: ToDoItem) {
        // Instantly updating the list
        val updatedList = _activeTasks.value.map {
            if (it.id == item.id) item else it
        }
        _activeTasks.value = updatedList
        viewModelScope.launch {
            repository.update(item)
        }
    }

    fun deleteTodoItem(item: ToDoItem) {
        // instantly deleting the item from the list
        _activeTasks.value = _activeTasks.value.filter { it.id != item.id }

        viewModelScope.launch {
            repository.delete(item)
        }
    }
}