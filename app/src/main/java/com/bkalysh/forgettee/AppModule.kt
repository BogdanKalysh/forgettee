package com.bkalysh.forgettee

import androidx.room.Room
import com.bkalysh.forgettee.database.ForgetteeDatabase
import com.bkalysh.forgettee.database.repository.ToDoItemRepository
import com.bkalysh.forgettee.viewmodel.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Providing local forgettee database
    single {
        Room.databaseBuilder(androidContext(), ForgetteeDatabase::class.java, "forgettee_database").build()
    }
    // Provide Repositories
    single { ToDoItemRepository(get<ForgetteeDatabase>().toDoItemDao) }

    viewModel { MainViewModel(get()) }
}