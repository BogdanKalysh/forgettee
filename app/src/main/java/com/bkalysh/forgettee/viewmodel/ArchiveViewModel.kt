package com.bkalysh.forgettee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bkalysh.forgettee.database.models.ToDoItem
import com.bkalysh.forgettee.database.repository.ToDoItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArchiveViewModel(private val repository: ToDoItemRepository): ViewModel() {
    private val _doneTasks = MutableStateFlow<List<ToDoItem>>(emptyList())
    val doneTasks: StateFlow<List<ToDoItem>> = _doneTasks.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllDone().collect { tasks ->
                _doneTasks.value = tasks
            }
        }
    }

    fun deleteTodoItem(item: ToDoItem) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }
}