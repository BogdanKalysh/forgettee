package com.bkalysh.forgettee.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsViewModel: ViewModel() {
    val appTheme = MutableStateFlow(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
}