package com.bkalysh.forgettee.viewmodel

import androidx.lifecycle.ViewModel
import com.bkalysh.forgettee.database.repository.ToDoItemRepository

class MainViewModel(private val toDoItemRepository: ToDoItemRepository): ViewModel() {
}