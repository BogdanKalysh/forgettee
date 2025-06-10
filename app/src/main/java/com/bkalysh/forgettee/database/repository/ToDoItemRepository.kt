package com.bkalysh.forgettee.database.repository

import com.bkalysh.forgettee.database.dao.ToDoItemDao
import com.bkalysh.forgettee.database.models.ToDoItem
import kotlinx.coroutines.flow.Flow

class ToDoItemRepository(private val dao: ToDoItemDao) {
    suspend fun insert(item: ToDoItem) {
        dao.insert(item)
    }

    suspend fun update(item: ToDoItem) {
        dao.update(item)
    }

    suspend fun updateAll(items: List<ToDoItem>) {
        dao.updateAll(items)
    }

    suspend fun delete(item: ToDoItem) { // todo will be used on task archive activity
        dao.delete(item)
    }

    suspend fun removeAllActive() {
        dao.removeAllActive()
    }

    fun getAll(): Flow<List<ToDoItem>> { // todo will be used on task archive activity
        return dao.getAll()
    }

    fun getAllActive(): Flow<List<ToDoItem>> {
        return dao.getAllActive()
    }
}