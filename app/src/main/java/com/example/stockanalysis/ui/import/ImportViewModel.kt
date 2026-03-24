package com.example.stockanalysis.ui.import

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.import.ImportResult
import com.example.stockanalysis.data.import.SmartImportService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val smartImportService: SmartImportService
) : ViewModel() {

    private val _importResult = MutableLiveData<Result<ImportResult>>()
    val importResult: LiveData<Result<ImportResult>> = _importResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun importFromImage(imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = smartImportService.importFromImage(imageUri)
            _importResult.value = result
            _isLoading.value = false
        }
    }

    fun importFromCsv(csvUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = smartImportService.importFromCsv(csvUri)
            _importResult.value = result
            _isLoading.value = false
        }
    }

    fun importFromClipboard(text: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = smartImportService.importFromClipboard(text)
            _importResult.value = result
            _isLoading.value = false
        }
    }
}
