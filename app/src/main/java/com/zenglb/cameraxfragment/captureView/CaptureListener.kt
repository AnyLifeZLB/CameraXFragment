package com.zenglb.cameraxfragment.captureView


/**
 * author hbzhou
 * date 2019/12/13 11:13
 */
public interface CaptureListener {

    public fun takePictures()

    fun recordShort(time: Long)

    fun recordStart()

    fun recordEnd(time: Long)

    fun recordZoom(zoom: Float)

    fun recordError()
}