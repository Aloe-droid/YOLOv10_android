package com.example.yolov10

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.Executors

@Composable
fun CameraView(modifier: Modifier, viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProvider = ProcessCameraProvider.getInstance(context).get()
    val previewView = PreviewView(context)

    val resolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
        .build()

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    val preview = Preview.Builder()
        .setResolutionSelector(resolutionSelector)
        .build()
        .apply { setSurfaceProvider(previewView.surfaceProvider) }

    val analysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setResolutionSelector(resolutionSelector)
        .build()
        .apply { setAnalyzer(Executors.newSingleThreadExecutor()) { viewModel.infer(it) } }

    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysis)

    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
fun CanvasView(modifier: Modifier, viewModel: MainViewModel) {
    val resultList by viewModel.resultList.collectAsState()
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(fontSize = 15.sp)

    Canvas(modifier = modifier, onDraw = {
        resultList.forEach {
            val topLeft = Offset(it.left, it.top)
            val size = Size(it.width, it.height)

            drawRect(
                color = Color.Green,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = 2.dp.toPx())
            )

            drawText(
                textMeasurer = textMeasurer,
                text = "${it.className}: ${String.format("%.2f", it.confidence)}%",
                topLeft = topLeft.copy(x = topLeft.x + 40),
                style = textStyle
            )
        }
    })
}
