package com.zenglb.camerax.main

import android.os.Environment
import android.os.Parcelable
import androidx.camera.core.ImageCapture
import kotlinx.android.parcel.Parcelize

/***
 * Camera X 配置
 *
 * 1 FLASH_MODE （ON/OFF）DEFAULT OFF
 * 2 CAMERA_SELECTOR (FRONT/BACK) DEFAULT BACK
 * 3 MEDIA_MODE （PIC/VIDOE&PIC）DEFAULT PIC ONLY
 * 4
 */
@Parcelize
open class CameraConfig private constructor(val builder: Builder) : Parcelable {

    companion object {
        //1.多媒体模式
        const val MEDIA_MODE_PHOTO = 1  //仅仅拍照
        const val MEDIA_MODE_VIDEO = 2  //仅仅视频
        const val MEDIA_MODE_ALL = 3    //拍照视频都可以

        //2.闪光灯模式
        const val FLASH_MODE_AUTO = 0
        const val FLASH_MODE_ON = 1
        const val FLASH_MODE_OFF = 2
    }


    //
    var flashMode: Int
    var cacheMediaDir: String
    var mediaMode: Int


    init {
        flashMode = builder.flashMode
        cacheMediaDir=builder.cacheMediaDir
        mediaMode=builder.mediaMode
    }


    @Parcelize
    class Builder : Parcelable {
        internal var flashMode: Int = FLASH_MODE_OFF //Default Value
        internal var cacheMediaDir: String = Environment.getExternalStorageDirectory().toString() + "/cameraX/images/"
        internal var mediaMode: Int = MEDIA_MODE_PHOTO

        fun flashMode(flashMode: Int): Builder {
            this.flashMode = flashMode
            return this
        }

        fun mediaMode(mediaMode: Int): Builder {
            this.mediaMode = mediaMode
            return this
        }

        fun cacheMediasDir(dir: String): Builder {
            this.cacheMediaDir = dir
            return this
        }


        fun build(): CameraConfig {
            return CameraConfig(this)
        }
    }


}
