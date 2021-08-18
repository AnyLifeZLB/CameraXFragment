package com.zenglb.camerax.main


interface CaptureListener {

    fun takePictures()

    fun recordStart()

    fun recordEnd(time: Long)

    fun recordZoom(zoom: Float)

    fun recordError(message:String)

}