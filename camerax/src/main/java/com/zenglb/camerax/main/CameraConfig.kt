package com.zenglb.camerax.main

import android.os.Environment
import android.os.Parcelable
import androidx.camera.core.ImageCapture
import kotlinx.android.parcel.Parcelize

/***
 * CameraX 相机配置
 *
 */
@Parcelize
open class CameraConfig private constructor(val builder: Builder) : Parcelable {

    companion object {
        //1.多媒体模式
        const val MEDIA_MODE_PHOTO = 1  //仅仅拍照
        const val MEDIA_MODE_VIDEO = 2  //仅仅视频
        const val MEDIA_MODE_ALL = 3    //拍照视频都可以

        //2.闪光灯模式
        const val FLASH_MODE_AUTO = ImageCapture.FLASH_MODE_AUTO
        const val FLASH_MODE_ON = ImageCapture.FLASH_MODE_ON
        const val FLASH_MODE_OFF = ImageCapture.FLASH_MODE_OFF
        const val FLASH_MODE_ALL_ON = 777; //常亮模式

    }



    var flashMode: Int          //闪光灯常亮模式
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
        internal var cacheMediaDir2: String = Environment.getExternalStorageDirectory().toString() + "/cameraX/images/"

        internal var cacheMediaDir: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/cameraX/images/"

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
