package com.bkalysh.forgettee.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.bkalysh.forgettee.database.models.ToDoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ToDoItemDao {
    @Upsert
    suspend fun upsert(item: ToDoItem): Long

    @Query("UPDATE to_do_items SET is_done = :isDone WHERE id = :id")
    suspend fun updateDoneState(id: Long, isDone: Boolean)

    @Query("UPDATE to_do_items SET is_removed = :isRemoved WHERE id = :id")
    suspend fun updateRemoveState(id: Long, isRemoved: Boolean)

    @Query("SELECT * FROM to_do_items")
    fun getAll(): Flow<List<ToDoItem>>

    @Query("SELECT * FROM to_do_items WHERE is_removed = 0")
    fun getAllActive(): Flow<List<ToDoItem>>

    @Delete
    suspend fun delete(item: ToDoItem)
}