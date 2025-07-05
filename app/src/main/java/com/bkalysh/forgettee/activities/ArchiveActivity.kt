package com.bkalysh.forgettee.activities

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bkalysh.forgettee.R
import com.bkalysh.forgettee.adapters.ArchiveTodoItemsRecyclerViewAdapter
import com.bkalysh.forgettee.database.models.ToDoItem
import com.bkalysh.forgettee.databinding.ActivityArchiveBinding
import com.bkalysh.forgettee.utils.ArchiveActivityMode
import com.bkalysh.forgettee.utils.ArchiveTodoAdapterUtils.generateUiItems
import com.bkalysh.forgettee.utils.Utils.focusOnEditText
import com.bkalysh.forgettee.utils.Utils.hideKeyboard
import com.bkalysh.forgettee.utils.Utils.setFirstLetterRed
import com.bkalysh.forgettee.utils.Utils.vibrate
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

        setupArchiveItemsObserving()
        setupTodoArchiveRecyclerViewAdapter()
        setupBackButton()
        setupArchiveModeObserver()
        setupSearch()
        setupBackPressedObserver()
    }

    private fun setupTodoArchiveRecyclerViewAdapter() {
        toDoArchiveItemsAdapter = ArchiveTodoItemsRecyclerViewAdapter(
            object : ArchiveTodoItemsRecyclerViewAdapter.OnDeleteTodoListener {
                override fun onTodoDelete(toDoItem: ToDoItem) {
                    vibrate(this@ArchiveActivity)
                    viewModel.deleteTodoItem(toDoItem)
                }
            },
        )
        binding.rvTodoArchiveList.apply {
            adapter = toDoArchiveItemsAdapter
            layoutManager = LinearLayoutManager(this@ArchiveActivity)
        }
    }

    private fun setupArchiveItemsObserving() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.doneTasks.collect { todoItems ->
                    toDoArchiveItemsAdapter.archiveItems = generateUiItems(todoItems,
                        shouldDisplayWeekSeparator(), this@ArchiveActivity)

                    if (viewModel.archiveMode.value == ArchiveActivityMode.FULL_ARCHIVE_MODE) {
                        binding.tvPlaceholderDescription.setText(R.string.placeholder_archive_description)
                    } else {
                        binding.tvPlaceholderDescription.setText(R.string.placeholder_search_archive_description)
                    }

                    binding.containerEmptyListPlaceholder.visibility =
                        if (todoItems.isEmpty()) { View.VISIBLE } else { View.GONE }
                }
            }
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupArchiveModeObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.archiveMode.collect { archiveMode ->
                    when (archiveMode) {
                        ArchiveActivityMode.SEARCH_MODE -> {
                            binding.btnSearch.setImageResource(R.drawable.icn_close)
                            binding.tvActivityName.visibility = View.GONE
                            binding.btnBack.visibility = View.GONE
                            binding.etSearch.visibility = View.VISIBLE
                            focusOnEditText(binding.etSearch)
                        }
                        ArchiveActivityMode.FULL_ARCHIVE_MODE -> {
                            binding.btnSearch.setImageResource(R.drawable.icn_search)
                            binding.tvActivityName.visibility = View.VISIBLE
                            binding.btnBack.visibility = View.VISIBLE
                            binding.etSearch.visibility = View.GONE
                            binding.etSearch.setText("")
                            hideKeyboard(binding.etSearch)
                        }
                    }
                }
            }
        }
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            lifecycleScope.launch {
                viewModel.archiveMode.value =
                    when (viewModel.archiveMode.value) {
                        ArchiveActivityMode.FULL_ARCHIVE_MODE -> ArchiveActivityMode.SEARCH_MODE
                        ArchiveActivityMode.SEARCH_MODE -> ArchiveActivityMode.FULL_ARCHIVE_MODE
                    }
            }
        }
        binding.etSearch.doOnTextChanged { searchFilter,_,_,_ ->
            lifecycleScope.launch {
                viewModel.archiveSearchFilter.value = searchFilter.toString().trim()
            }
        }
    }

    private fun setupBackPressedObserver() {
        onBackPressedDispatcher.addCallback(this) {
            if (viewModel.archiveMode.value == ArchiveActivityMode.SEARCH_MODE) {
                viewModel.archiveMode.value = ArchiveActivityMode.FULL_ARCHIVE_MODE
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun shouldDisplayWeekSeparator(): Boolean{
        return viewModel.archiveMode.value == ArchiveActivityMode.FULL_ARCHIVE_MODE ||
                viewModel.archiveSearchFilter.value.trim().isEmpty()
    }
}