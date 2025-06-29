package com.bkalysh.forgettee.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bkalysh.forgettee.R
import com.bkalysh.forgettee.adapters.ArchiveTodoItemsRecyclerViewAdapter
import com.bkalysh.forgettee.database.models.ToDoItem
import com.bkalysh.forgettee.databinding.ActivityArchiveBinding
import com.bkalysh.forgettee.utils.Utils.setFirstLetterRed
import com.bkalysh.forgettee.viewmodel.ArchiveViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ArchiveActivity : AppCompatActivity() {
    private val viewModel: ArchiveViewModel by viewModel()

    private lateinit var binding: ActivityArchiveBinding
    private lateinit var toDoArchiveItemsAdapter: ArchiveTodoItemsRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArchiveBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setFirstLetterRed(binding.tvActivityName)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.doneTasks.collect { todoItems ->
                    toDoArchiveItemsAdapter.todoItems = todoItems
                }
            }
        }

        setupTodoArchiveRecyclerViewAdapter()
        setupBackButton()
    }

    private fun setupTodoArchiveRecyclerViewAdapter() {
        toDoArchiveItemsAdapter = ArchiveTodoItemsRecyclerViewAdapter(
            object : ArchiveTodoItemsRecyclerViewAdapter.OnDeleteTodoListener {
                override fun onTodoDelete(toDoItem: ToDoItem) {
                    val deletedIndex = viewModel.doneTasks.value.indexOf(toDoItem)
                    viewModel.deleteTodoItem(toDoItem)
                    if (deletedIndex < toDoArchiveItemsAdapter.todoItems.size) {
                        toDoArchiveItemsAdapter.notifyItemChanged(deletedIndex)
                    }
                }
            },
        )
        binding.rvTodoArchiveList.apply {
            adapter = toDoArchiveItemsAdapter
            layoutManager = LinearLayoutManager(this@ArchiveActivity)
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}