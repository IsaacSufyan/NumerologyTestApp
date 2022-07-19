package com.isaacsufyan.numerologycompose.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class SharedViewModel : ViewModel() {
    val pieValueStateFlow = MutableStateFlow(0)
    var inputValueLiveData = MutableLiveData<String>()
}