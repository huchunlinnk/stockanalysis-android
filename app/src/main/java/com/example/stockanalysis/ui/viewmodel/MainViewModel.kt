package com.example.stockanalysis.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 主ViewModel
 * 管理主界面的状态和导航
 */
@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _currentTab = MutableLiveData(0)
    val currentTab: LiveData<Int> = _currentTab

    /**
     * 设置当前选中的Tab
     */
    fun setCurrentTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }
}
