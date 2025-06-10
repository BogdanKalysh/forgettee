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

    suspend fun delete(item: ToDoItem) {
        dao.delete(item)
    }

    fun getAll(): Flow<List<ToDoItem>> {
        return dao.getAll()
    }

    fun getAllActive(): Flow<List<ToDoItem>> {
        return dao.getAllActive()
    }
}