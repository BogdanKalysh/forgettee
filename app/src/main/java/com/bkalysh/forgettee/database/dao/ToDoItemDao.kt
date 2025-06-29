package com.bkalysh.forgettee.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bkalysh.forgettee.database.models.ToDoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ToDoItemDao {
    @Insert
    suspend fun insert(item: ToDoItem)

    @Update
    suspend fun update(item: ToDoItem)

    @Update
    suspend fun updateAll(items: List<ToDoItem>)

    @Query("SELECT * FROM todo_items WHERE is_removed = 0 ORDER BY position")
    fun getAllActive(): Flow<List<ToDoItem>>

    @Query("SELECT * FROM todo_items WHERE is_removed = 1 ORDER BY done_at DESC")
    fun getAllDone(): Flow<List<ToDoItem>>

    @Delete
    suspend fun delete(item: ToDoItem)
}