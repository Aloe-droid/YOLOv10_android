package com.example.yolov10

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

object AppViewModelFactory {
    val FACTORY = viewModelFactory {
        initializer { MainViewModel(getApplication().applicationContext.resources) }
    }

    private fun CreationExtras.getApplication() =
        this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
}