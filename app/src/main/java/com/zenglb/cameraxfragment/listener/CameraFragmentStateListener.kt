package com.zenglb.cameraxfragment.listener;

import java.io.File;

/**
 *
 */
public interface CameraFragmentStateListener {

    //when the current displayed camera is the back
    fun onCurrentCameraBack();

    //when the current displayed camera is the front
    fun onCurrentCameraFront();

    //when the flash is at mode auto
    fun onFlashAuto();
    //when the flash is at on
    fun onFlashOn();
    //when the flash is off
    fun onFlashOff();

    //if the camera is ready to take a photo
    fun onCameraSetupForPhoto();

    //if the camera is ready to take a video
    fun onCameraSetupForVideo();

    //when the camera state is "ready to record a video"
    fun onRecordStateVideoReadyForRecord();
    //when the camera state is "recording a video"
    fun onRecordStateVideoInProgress();
    //when the camera state is "ready to take a photo"
    fun onRecordStatePhoto();

    //after the rotation of the screen / camera
    fun shouldRotateControls(degrees:Int);

    fun onStartVideoRecord(outputFile:File);
    fun onStopVideoRecord();
}
