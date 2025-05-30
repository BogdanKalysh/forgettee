package com.bkalysh.forgettee.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bkalysh.forgettee.database.dao.ToDoItemDao
import com.bkalysh.forgettee.database.models.ToDoItem

@Database(
    entities = [
        ToDoItem::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ForgetteeDatabase: RoomDatabase() {
    abstract val toDoItemDao: ToDoItemDao
}