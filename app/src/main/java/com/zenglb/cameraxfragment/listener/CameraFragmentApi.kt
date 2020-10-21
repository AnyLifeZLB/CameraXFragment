package com.zenglb.cameraxfragment.listener;

import com.zenglb.camerax.main.CaptureResultListener


/**
 *
 */
public interface CameraFragmentApi {

    fun takePhotoOrCaptureVideo(
        listener: CaptureResultListener,
        directoryPath: String,
        fileName: String
    );

    fun openSettingDialog();

    fun switchCameraTypeFrontBack();

    fun switchActionPhotoVideo();

    fun toggleFlashMode();

    fun setStateListener(cameraFragmentStateListener: CameraFragmentStateListener);

//    fun setControlsListener(CameraFragmentControlsListener cameraFragmentControlsListener);

    fun setResultListener(fragmentListener: CaptureResultListener);

}
