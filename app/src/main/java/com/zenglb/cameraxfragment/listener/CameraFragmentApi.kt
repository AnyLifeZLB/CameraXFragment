package com.zenglb.cameraxfragment.listener;


/**
 *
 */
public interface CameraFragmentApi {

    fun takePhotoOrCaptureVideo(
        listener: OperateListener,
        directoryPath: String,
        fileName: String
    );

    fun openSettingDialog();

    fun switchCameraTypeFrontBack();

    fun switchActionPhotoVideo();

    fun toggleFlashMode();

    fun setStateListener(cameraFragmentStateListener: CameraFragmentStateListener);

//    fun setControlsListener(CameraFragmentControlsListener cameraFragmentControlsListener);

    fun setResultListener(fragmentListener: OperateListener);

}
