package com.bkalysh.forgettee.activities

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bkalysh.forgettee.R
import com.bkalysh.forgettee.databinding.ActivitySettingsBinding
import com.bkalysh.forgettee.utils.Utils.SHARED_PREFERENCES_24_HOUR_FORMAT_ITEM
import com.bkalysh.forgettee.utils.Utils.SHARED_PREFERENCES_NEW_TASKS_ADD_TO_END_ITEM
import com.bkalysh.forgettee.utils.Utils.SHARED_PREFERENCES_SETTINGS_NAME
import com.bkalysh.forgettee.utils.Utils.SHARED_PREFERENCES_THEME_MODE_ITEM
import com.bkalysh.forgettee.utils.Utils.setFirstLetterRed
import com.bkalysh.forgettee.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsActivity: AppCompatActivity()  {
    private val viewModel: SettingsViewModel by viewModel()

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        sharedPref = getSharedPreferences(SHARED_PREFERENCES_SETTINGS_NAME, MODE_PRIVATE)
        setFirstLetterRed(binding.tvActivityName)
        setupAppTheme()
        setupBackButton()
        setupThemeRadioButtonsUpdates()
        setupThemeRadioButtonsClicks()
        setupNewTasksAddToEndSwitch()
        setup24hourFormatSwitch()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupAppTheme() {
        val themeMode = sharedPref.getInt(SHARED_PREFERENCES_THEME_MODE_ITEM, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        viewModel.appTheme.value = themeMode
    }

    private fun setupThemeRadioButtonsUpdates() {
        val checkedBackground = R.drawable.radio_button_checked_background
        val checkedTextColor = R.color.theme_red
        val uncheckedBackground = R.drawable.radio_button_unchecked_background
        val uncheckedTextColor = R.color.on_primary_color

        val themeMap = mapOf(
            AppCompatDelegate.MODE_NIGHT_NO to binding.rbLightTheme,
            AppCompatDelegate.MODE_NIGHT_YES to binding.rbDarkTheme,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM to binding.rbSystemTheme
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appTheme.collect { newThemeMode ->
                    AppCompatDelegate.setDefaultNightMode(newThemeMode)

                    themeMap.forEach { (mode, radioButton) ->
                        radioButton.background = AppCompatResources.getDrawable(
                            this@SettingsActivity,
                            if (newThemeMode == mode) checkedBackground else uncheckedBackground
                        )
                        radioButton.setTextColor(
                            getColor(if (newThemeMode == mode) checkedTextColor else uncheckedTextColor)
                        )
                    }
                }
            }
        }
    }

    private fun setupThemeRadioButtonsClicks() {
        binding.rbLightTheme.setOnClickListener {
            val newThemeMode = AppCompatDelegate.MODE_NIGHT_NO
            sharedPref.edit { putInt(SHARED_PREFERENCES_THEME_MODE_ITEM, newThemeMode) }
            viewModel.appTheme.value = newThemeMode
        }

        binding.rbDarkTheme.setOnClickListener {
            val newThemeMode = AppCompatDelegate.MODE_NIGHT_YES
            sharedPref.edit { putInt(SHARED_PREFERENCES_THEME_MODE_ITEM, newThemeMode) }
            viewModel.appTheme.value = newThemeMode
        }

        binding.rbSystemTheme.setOnClickListener {
            val newThemeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            sharedPref.edit { putInt(SHARED_PREFERENCES_THEME_MODE_ITEM, newThemeMode) }
            viewModel.appTheme.value = newThemeMode
        }
    }

    private fun setupNewTasksAddToEndSwitch() {
        val isAddingToEnd = sharedPref.getBoolean(SHARED_PREFERENCES_NEW_TASKS_ADD_TO_END_ITEM, false)
        binding.swAddTaskPositionSetting.isChecked = isAddingToEnd

        binding.swAddTaskPositionSetting.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit { putBoolean(SHARED_PREFERENCES_NEW_TASKS_ADD_TO_END_ITEM, isChecked) }
        }
    }

    private fun setup24hourFormatSwitch() {
        val is24HourFormat = sharedPref.getBoolean(SHARED_PREFERENCES_24_HOUR_FORMAT_ITEM, true)
        binding.swTimeFormatSetting.isChecked = is24HourFormat

        binding.swTimeFormatSetting.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit { putBoolean(SHARED_PREFERENCES_24_HOUR_FORMAT_ITEM, isChecked) }
        }
    }
}