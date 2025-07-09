package com.bkalysh.forgettee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bkalysh.forgettee.database.models.ToDoItem
import com.bkalysh.forgettee.database.repository.ToDoItemRepository
import com.bkalysh.forgettee.utils.ArchiveActivityMode
import com.bkalysh.forgettee.utils.Utils.increaseTodoItemsPositions
import com.bkalysh.forgettee.utils.Utils.parseTodoItemFromInput
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class) // for using flatMapLatest
class ArchiveViewModel(private val repository: ToDoItemRepository): ViewModel() {
    private val _doneTasks = MutableStateFlow<List<ToDoItem>>(emptyList())
    val doneTasks: StateFlow<List<ToDoItem>> = _doneTasks.asStateFlow()

    val archiveMode = MutableStateFlow(ArchiveActivityMode.FULL_ARCHIVE_MODE)
    val archiveSearchFilter = MutableStateFlow("")

    init {
        viewModelScope.launch {
            combine(archiveMode, archiveSearchFilter) { mode, filter ->
                mode to filter
            }.flatMapLatest { (mode, filter) ->
                when (mode) {
                    ArchiveActivityMode.FULL_ARCHIVE_MODE -> repository.getAllDone()
                    ArchiveActivityMode.SEARCH_MODE -> {
                        if (filter.isBlank()) {
                            flowOf(emptyList())
                        } else {
                            repository.getAllDoneFiltered(filter)
                        }
                    }
                }
            }.collect { tasks ->
                _doneTasks.value = tasks
            }
        }
    }

    fun deleteTodoItem(item: ToDoItem) {
        // instantly deleting the item from the list
        _doneTasks.value = _doneTasks.value.filter { it.id != item.id }

        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun returnFromArchive(item: ToDoItem) {
        // instantly deleting the item from the list of done tasks
        _doneTasks.value = _doneTasks.value.filter { it.id != item.id }

        val undoneToDo = item.copy(isDone = false, isRemoved = false)
        viewModelScope.launch {
            repository.update(undoneToDo)
        }
    }

    fun copyFromArchive(item: ToDoItem, addToEnd: Boolean) {
        addNewTodoItem(item.text, addToEnd)
    }

    private fun updateTodoItem(item: ToDoItem) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    private fun addNewTodoItem(todoText: String, addToEnd: Boolean) {
        viewModelScope.launch {
            if (todoText.isNotEmpty()) {
                val currentItems = repository.getAllActive().first()

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
}