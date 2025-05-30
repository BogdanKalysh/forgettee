package com.bkalysh.forgettee.database.repository

import com.bkalysh.forgettee.database.dao.ToDoItemDao
import com.bkalysh.forgettee.database.models.ToDoItem
import kotlinx.coroutines.flow.Flow

class ToDoItemRepository(private val dao: ToDoItemDao) {
    suspend fun upsert(item: ToDoItem) {
        dao.upsert(item)
    }

    suspend fun updateDoneState(id: Long, isDone: Boolean) {
        dao.updateDoneState(id, isDone)
    }

    suspend fun updateRemoveState(id: Long, isRemoved: Boolean) {
        dao.updateRemoveState(id, isRemoved)
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