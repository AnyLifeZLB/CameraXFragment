package com.zenglb.camerax.main;

/**
 * 拍照，视频后的回调路径
 *
 */
public interface CaptureResultListener {

    //Called when the video record is finished and saved
    fun onVideoRecorded(filePath:String);

    //called when the photo is taken and saved
    fun  onPhotoTaken(filePath:String );

}
