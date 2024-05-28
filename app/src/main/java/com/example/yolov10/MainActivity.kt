package com.example.yolov10

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yolov10.AppViewModelFactory.FACTORY

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissions = arrayOf(Manifest.permission.CAMERA)
        PermissionHelper(this, permissions).launchPermission()

        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = FACTORY)
            val modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { mainViewModel.setDiff(it.width, it.height) }

            CameraView(modifier = modifier, viewModel = mainViewModel)
            CanvasView(modifier = modifier, viewModel = mainViewModel)
        }
    }
}
