package com.zenglb.camerax.main

import android.os.Environment
import android.os.Parcelable
import androidx.camera.core.ImageCapture
import kotlinx.android.parcel.Parcelize

/***
 * Camera X 配置
 *
 * 1 FLASH_MODE （ON/OFF）DEFAULT OFF
 * 1 CAMERA_SELECTOR (FRONT/BACK) DEFAULT BACK
 * 1 FLASH_MODE （ON/OFF）DEFAULT OFF
 * 1 FLASH_MODE （ON/OFF）DEFAULT OFF
 */
@Parcelize
class CameraConfig private constructor(val builder: Builder) : Parcelable {
    var flashMode: Int
    var cacheMediaDir: String

    init {
        flashMode = builder.flashMode
        cacheMediaDir=builder.cacheMediaDir
    }


    @Parcelize
    class Builder : Parcelable {
        internal var flashMode: Int = ImageCapture.FLASH_MODE_OFF //Default Value
        internal var cacheMediaDir: String = Environment.getExternalStorageDirectory().toString() + "/cameraX/images/"

        fun flashMode(flashMode: Int): Builder {
            this.flashMode = flashMode
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
