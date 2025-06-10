package com.bkalysh.forgettee.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "todo_items",
    indices = [Index(value = ["is_removed"])]
)
data class ToDoItem (
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "is_done") val isDone: Boolean,
    @ColumnInfo(name = "is_removed") val isRemoved: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Date,
    @ColumnInfo(name = "finished_at") val finishedAt: Date,
)