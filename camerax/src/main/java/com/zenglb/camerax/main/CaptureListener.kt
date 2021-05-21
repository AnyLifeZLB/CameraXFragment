package com.zenglb.camerax.main


/**
 * author hbzhou
 * date 2019/12/13 11:13
 */
interface CaptureListener {

    fun takePictures()

    fun recordStart()

    fun recordEnd(time: Long)

    fun recordZoom(zoom: Float)

    fun recordError(message:String)
}