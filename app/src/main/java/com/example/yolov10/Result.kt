package com.example.yolov10

data class Result(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val className: String,
    val confidence: Float
)
