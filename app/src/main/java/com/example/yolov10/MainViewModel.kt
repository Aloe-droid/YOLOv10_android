package com.example.yolov10

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

class MainViewModel(private val resources: Resources) : ViewModel() {
    private val _resultList = MutableStateFlow(mutableListOf<Result>())
    val resultList: StateFlow<MutableList<Result>> = _resultList.asStateFlow()

    private val confidenceThreshold = 0.45f
    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val ortSession: OrtSession = ortEnv.createSession(readModel())
    private val classes = readClasses()

    private val shape = (ortSession.inputInfo["images"]?.info as TensorInfo).shape
    private val width = shape[3].toInt()
    private val height = shape[2].toInt()
    private val imageStd = 255f

    private var dx = 1f
    private var dy = 1f
    private var diffY = 0f

    // 평균 추론 시간 : 350ms
    fun infer(imageProxy: ImageProxy) = viewModelScope.launch(Dispatchers.Default) {
        val inputTensor = preProcess(imageProxy)
        val rawOutput = process(inputTensor)
        val output = postProcess(rawOutput)

        _resultList.emit(output)
        imageProxy.close()
    }

    private fun preProcess(imageProxy: ImageProxy): OnnxTensor {
        val bitmap = imageProxy.toBitmap()
        val rescaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

        val cap = shape.reduce { acc, l -> acc * l }.toInt()
        val order = ByteOrder.nativeOrder()
        val buffer = ByteBuffer.allocateDirect(cap * Float.SIZE_BYTES).order(order).asFloatBuffer()
        val area = width * height

        for (i in 0 until width) {
            for (j in 0 until height) {
                val idx = width * i + j
                val pixelValue = rescaledBitmap.getPixel(j, i)

                buffer.put(idx, Color.red(pixelValue) / imageStd)
                buffer.put(idx + area, Color.green(pixelValue) / imageStd)
                buffer.put(idx + area * 2, Color.blue(pixelValue) / imageStd)
            }
        }
        return OnnxTensor.createTensor(ortEnv, buffer, shape)
    }

    private fun process(inputTensor: OnnxTensor): OrtSession.Result? {
        inputTensor.use {
            val inputName = ortSession.inputNames.first()
            return ortSession.run(Collections.singletonMap(inputName, inputTensor))
        }
    }

    private fun postProcess(rawOutput: OrtSession.Result?): MutableList<Result> {
        val output = mutableListOf<Result>()
        rawOutput?.run {
            val outputArray = (get(0).value as Array<*>)[0] as Array<*>
            outputArray.asSequence()
                .map { it as FloatArray }
                .filter { it[CONFIDENCE] > confidenceThreshold }
                .mapTo(output) { it.toResult() }
        }
        return output
    }

    private fun FloatArray.toResult(): Result {
        val left = max(0f, this[LEFT] * dx)
        val top = max(0f, this[TOP] * dy - diffY / 2)
        val width = min(width.toFloat(), max(0f, this[RIGHT] - this[LEFT])) * dx
        val height = min(height.toFloat(), max(0f, this[BOTTOM] - this[TOP])) * dy

        return Result(
            left = left,
            top = top,
            width = width,
            height = height,
            className = classes[this[CLASS_INDEX].toInt()],
            confidence = this[CONFIDENCE] * 100,
        )
    }

    fun setDiff(viewWidth: Int, viewHeight: Int) {
        dx = viewWidth / width.toFloat()
        dy = dx * 9f / 16f
        diffY = viewWidth * 9f / 16f - viewHeight
    }

    private fun readModel(): ByteArray =
        resources.openRawResource(R.raw.model).readBytes()

    private fun readClasses(): List<String> =
        resources.openRawResource(R.raw.classes).bufferedReader().readLines()

    override fun onCleared() {
        ortEnv.close()
        ortSession.close()
        super.onCleared()
    }

    companion object {
        const val LEFT = 0
        const val TOP = 1
        const val RIGHT = 2
        const val BOTTOM = 3
        const val CONFIDENCE = 4
        const val CLASS_INDEX = 5
    }
}