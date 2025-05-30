package com.bkalysh.forgettee.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "to_do_items",
    indices = [Index(value = ["owner_id"]), Index(value = ["device_model_id"])]
)
data class ToDoItem (
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "created_at") val createdAt: Date,
    @ColumnInfo(name = "is_done") val isDone: Boolean,
    @ColumnInfo(name = "is_removed") val isRemoved: Boolean
)