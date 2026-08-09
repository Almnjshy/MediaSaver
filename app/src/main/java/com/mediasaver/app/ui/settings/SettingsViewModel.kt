package com.mediasaver.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediasaver.app.data.AppSettings
import com.mediasaver.app.data.DefaultQuality
import com.mediasaver.app.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings()
    )

    fun setDefaultQuality(quality: DefaultQuality) {
        viewModelScope.launch { repository.setDefaultQuality(quality) }
    }

    fun setSaveToMoviesFolder(toSubfolder: Boolean) {
        viewModelScope.launch { repository.setSaveToMoviesFolder(toSubfolder) }
    }
}
