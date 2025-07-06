package com.bkalysh.forgettee.activities

import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
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
import com.bkalysh.forgettee.utils.Utils.dp
import com.bkalysh.forgettee.utils.Utils.focusOnEditText
import com.bkalysh.forgettee.utils.Utils.hideKeyboard
import com.bkalysh.forgettee.utils.Utils.isDarkTheme
import com.bkalysh.forgettee.utils.Utils.setFirstLetterRed
import com.bkalysh.forgettee.utils.Utils.vibrate
import com.bkalysh.forgettee.viewmodel.ArchiveViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bkalysh.forgettee.utils.Utils.SHARED_PREFERENCES_24_HOUR_FORMAT_ITEM
import com.bkalysh.forgettee.utils.Utils.SHARED_PREFERENCES_SETTINGS_NAME

class ArchiveActivity : AppCompatActivity() {
    private val viewModel: ArchiveViewModel by viewModel()

    private lateinit var binding: ActivityArchiveBinding
    private lateinit var toDoArchiveItemsAdapter: ArchiveTodoItemsRecyclerViewAdapter

    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArchiveBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // applying insets via custom spacers, so the dimmer can cover the whole screen
            val topLP = binding.flTopSpacer.layoutParams
            topLP.height = systemBars.top
            binding.flTopSpacer.layoutParams = topLP
            val bottomLP = binding.flBottomSpacer.layoutParams
            bottomLP.height = systemBars.bottom
            binding.flBottomSpacer.layoutParams = bottomLP
            insets
        }
        sharedPref = getSharedPreferences(SHARED_PREFERENCES_SETTINGS_NAME, MODE_PRIVATE)
        setFirstLetterRed(binding.tvActivityName)

        setupArchiveItemsObserving()
        setupTodoArchiveRecyclerViewAdapter()
        setupBackButton()
        setupArchiveModeObserver()
        setupSearch()
        setupBackPressedObserver()
        setupDimmer()
        setupCurrentDateDisplay()
    }

    private fun setupTodoArchiveRecyclerViewAdapter() {
        toDoArchiveItemsAdapter = ArchiveTodoItemsRecyclerViewAdapter(
            object : ArchiveTodoItemsRecyclerViewAdapter.OnTodoClickListener {
                override fun onTodoClicked(toDoItem: ToDoItem, y: Float, buttonHeight: Float) {
                    showTodoItemContextMenu(toDoItem, y, buttonHeight)
                }
            },
            sharedPref.getBoolean(SHARED_PREFERENCES_24_HOUR_FORMAT_ITEM, true)
        )
        binding.rvTodoArchiveList.apply {
            adapter = toDoArchiveItemsAdapter
            layoutManager = LinearLayoutManager(this@ArchiveActivity)
        }

        // Dynamic search button hiding/showing on scroll
        var isSearchButtonAnimationRunning = false
        var isSearchButtonHidden = false
        binding.rvTodoArchiveList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (isSearchButtonAnimationRunning) return

                if (dy > 0 && !isSearchButtonHidden) {
                    binding.btnSearch.animate()
                        .translationY(binding.btnSearch.height.toFloat() + 100)
                        .setDuration(300)
                        .withEndAction {
                            isSearchButtonAnimationRunning = false
                        }
                        .start()
                    isSearchButtonHidden = true
                    isSearchButtonAnimationRunning = true
                } else if (dy < 0 && isSearchButtonHidden) {
                    binding.btnSearch.animate()
                        .translationY(0f)
                        .setDuration(300)
                        .withEndAction { isSearchButtonAnimationRunning = false }
                        .start()
                    isSearchButtonHidden = false
                    isSearchButtonAnimationRunning = true
                }

            }
        })
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
                            binding.clCurrentDateContainer.visibility = View.GONE
                            binding.btnSearch.visibility = View.GONE
                            binding.tvActivityName.visibility = View.GONE
                            binding.btnBack.visibility = View.GONE
                            binding.etSearch.visibility = View.VISIBLE
                            binding.btnCloseSearch.visibility = View.VISIBLE
                            focusOnEditText(binding.etSearch)
                        }
                        ArchiveActivityMode.FULL_ARCHIVE_MODE -> {
                            binding.clCurrentDateContainer.visibility = View.VISIBLE
                            binding.btnSearch.visibility = View.VISIBLE
                            binding.tvActivityName.visibility = View.VISIBLE
                            binding.btnBack.visibility = View.VISIBLE
                            binding.btnCloseSearch.visibility = View.GONE
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
        binding.btnCloseSearch.setOnClickListener {
            lifecycleScope.launch {
                viewModel.archiveMode.value = ArchiveActivityMode.FULL_ARCHIVE_MODE
            }
        }
        binding.btnSearch.setOnClickListener {
            lifecycleScope.launch {
                viewModel.archiveMode.value = ArchiveActivityMode.SEARCH_MODE
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
            if (binding.dimmer.isVisible) {
                closeAllPopups()
            } else if (viewModel.archiveMode.value == ArchiveActivityMode.SEARCH_MODE) {
                viewModel.archiveMode.value = ArchiveActivityMode.FULL_ARCHIVE_MODE
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setupDimmer() {
        binding.dimmer.setOnClickListener { closeAllPopups() }
    }

    private fun setupCurrentDateDisplay() {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString()
        val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)

        binding.tvCurrentDateNumber.text = day
        binding.tvCurrentDateMonth.text = month
    }

    private fun shouldDisplayWeekSeparator(): Boolean{
        return viewModel.archiveMode.value == ArchiveActivityMode.FULL_ARCHIVE_MODE ||
                viewModel.archiveSearchFilter.value.trim().isEmpty()
    }

    private fun showTodoItemContextMenu(toDoItem: ToDoItem, yCord: Float, buttonHeight: Float) {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        hideKeyboard(binding.etSearch)

        // calculating display coordinates
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val bottomSpacerHeight = binding.flBottomSpacer.height.toFloat()
        val contextMenuHeight = binding.clContextMenu.height

        var displayYCord = yCord - (contextMenuHeight / 2) + (buttonHeight / 2)

        if (displayYCord + contextMenuHeight > screenHeight - bottomSpacerHeight) {
            displayYCord = screenHeight - contextMenuHeight - bottomSpacerHeight - 8.dp
        }
        binding.clContextMenu.translationY = displayYCord

        // enabling the views
        binding.dimmer.visibility = View.VISIBLE
        val animation = AnimationUtils.loadAnimation(this, R.anim.open_scale_left)
        binding.clContextMenu.startAnimation(animation)
        binding.clContextMenu.visibility = View.VISIBLE

        //setting up the buttons
        binding.btnReturnItem.setOnClickListener {
            vibrate(this@ArchiveActivity)
            binding.btnDeleteItem.isEnabled = false
            closeAllPopups()
            viewModel.returnFromArchive(toDoItem)
        }
        binding.btnDeleteItem.setOnClickListener {
            vibrate(this@ArchiveActivity)
            binding.btnReturnItem.isEnabled = false
            closeAllPopups()
            viewModel.deleteTodoItem(toDoItem)
            Toast.makeText(this, getString(R.string.deleted_toast_text, toDoItem.text), Toast.LENGTH_SHORT).show()
        }
    }

    private fun closeAllPopups() {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isDarkTheme(this)
        binding.dimmer.visibility = View.INVISIBLE
        val animation = AnimationUtils.loadAnimation(this, R.anim.close_make_transparent)
        binding.clContextMenu.startAnimation(animation)
        binding.clContextMenu.visibility = View.INVISIBLE
    }
}