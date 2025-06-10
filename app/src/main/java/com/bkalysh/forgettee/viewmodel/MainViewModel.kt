package com.bkalysh.forgettee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bkalysh.forgettee.database.models.ToDoItem
import com.bkalysh.forgettee.database.repository.ToDoItemRepository
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

    fun insertTodoItem(item: ToDoItem) {
        viewModelScope.launch {
            repository.insert(item)
        }
    }

    fun updateTodoItem(item: ToDoItem) {
        viewModelScope.launch {
            repository.update(item)
        }
    }
}