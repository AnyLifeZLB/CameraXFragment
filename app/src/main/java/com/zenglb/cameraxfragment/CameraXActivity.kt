package com.zenglb.cameraxfragment

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.zenglb.camerax.main.*
import com.zenglb.camerax.main.CameraXFragment.Companion.FLASH_ALL_ON
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import kotlinx.android.synthetic.main.activity_camera_x.*
import java.io.File


/**
 * 演示如何使用CameraX Fragment
 *
 * 1.CameraX Extensions 是可选插件，您可以在支持的设备上添加效果。这些效果包括人像、HDR、夜间模式和美颜
 * 2.图片分析：无缝访问缓冲区以便在算法中使用，例如传入 MLKit
 * 3.切换为录制视频模式的时候还会闪屏黑屏
 * 4.第一个版本先不录制视频了
 *
 *
 */
class CameraXActivity : AppCompatActivity() {

    private val cacheMediasDir2: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/cameraX/images/"
    private val cacheMediasDir = Environment.getExternalStorageDirectory().toString() + "/cameraX/images/"
    private lateinit var cameraXFragment: CameraXFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_x)

        val cameraConfig=CameraConfig.Builder()
            .flashMode(CameraConfig.FLASH_MODE_OFF)
            .mediaMode(CameraConfig.MEDIA_MODE_ALL) //视频拍照都可以
            .cacheMediasDir(cacheMediasDir)
            .build()

        cameraXFragment = CameraXFragment.newInstance(cameraConfig)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, cameraXFragment).commit()


        //拍照，拍视频的UI 操作的各种状态处理
        capture_btn.setCaptureListener(object : CaptureListener {
            override fun takePictures() {
                cameraXFragment.takePhoto()
            }

            //开始录制视频
            override fun recordStart() {
                cameraXFragment.takeVideo()
            }

            //录制视频结束
            override fun recordEnd(time: Long) {
                cameraXFragment.stopTakeVideo(time)
            }

            //长按拍视频的时候，在屏幕滑动可以调整焦距缩放
            override fun recordZoom(zoom: Float) {
                val a = zoom
            }

            //录制视频错误（拍照也有错误，这里还是不处理了吧）
            override fun recordError(message: String) {

            }
        })


        //拍照录视频操作结果通知回调
        cameraXFragment.setCaptureResultListener(object : CaptureResultListener {
            override fun onVideoRecorded(filePath: String) {
                Log.d("CameraXFragment", "onVideoRecorded：$filePath")

                val photoURI: Uri = FileProvider.getUriForFile(
                    baseContext,
                    baseContext.getApplicationContext().getPackageName().toString() + ".provider",
                    File(filePath)
                )
                intent = Intent(this@CameraXActivity, VideoPlayerActivity::class.java)
                startActivity(intent.putExtra("mMP4Path", photoURI.toString()))
            }

            override fun onPhotoTaken(filePath: String) {
                Log.d("CameraXFragment", "onPhotoTaken： $filePath")
                runOnUiThread {
                    Glide.with(baseContext)
                        .load(filePath)
                        .circleCrop()
                        .into(photo_view_btn)
                }
            }

        })

//        //切换摄像头
//        switch_btn.setOnClickListener {
//            if (cameraXFragment.canSwitchCamera())
//                cameraXFragment.switchCamera()
//        }
//
//        flush_btn.setOnClickListener{
//            cameraXFragment.setFlashMode(FLASH_ALL_ON)
//        }



        flush_btn.setOnClickListener {
            if(flash_layout.visibility== View.VISIBLE){
                flash_layout.visibility= View.INVISIBLE
                switch_btn.visibility= View.VISIBLE
            }else{
                flash_layout.visibility= View.VISIBLE
                switch_btn.visibility= View.INVISIBLE
            }
        }


        flash_on.setOnClickListener {
            initFlashSelectColor()
            flash_on.setTextColor(resources.getColor(R.color.flash_selected))
            flush_btn.setBackgroundResource(R.drawable.flash_on)
            cameraXFragment.switchFlashMode()
            cameraXFragment.setFlashMode(ImageCapture.FLASH_MODE_ON)
        }

        flash_off.setOnClickListener {
            initFlashSelectColor()
            flash_off.setTextColor(resources.getColor(R.color.flash_selected))
            flush_btn.setBackgroundResource(R.drawable.flash_off)
            cameraXFragment.setFlashMode(ImageCapture.FLASH_MODE_OFF)
        }

        flash_auto.setOnClickListener {
            initFlashSelectColor()
            flash_auto.setTextColor(resources.getColor(R.color.flash_selected))
            flush_btn.setBackgroundResource(R.drawable.flash_auto)
            cameraXFragment.setFlashMode(ImageCapture.FLASH_MODE_AUTO)
        }

        flash_all_on.setOnClickListener {
            initFlashSelectColor()
            flash_all_on.setTextColor(resources.getColor(R.color.flash_selected))
            flush_btn.setBackgroundResource(R.drawable.flash_all_on)
            cameraXFragment.setFlashMode(FLASH_ALL_ON)
        }


        //切换摄像头
        switch_btn.setOnClickListener {
            //要保持闪光灯上一次的模式
            if (cameraXFragment.canSwitchCamera()){
                cameraXFragment.switchCamera()

//                cameraXFragment.setFlashMode(cameraXFragment.getF )
            }
        }


        //去浏览媒体资源，使用的是知乎的开源库 Matisse，用法参考官方说明
        photo_view_btn.setOnClickListener {
            Matisse.from(this@CameraXActivity)
                .choose(MimeType.ofImage())
                .countable(true)
                .maxSelectable(9)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.45f)
                .imageEngine(GlideEngine())
                .forResult(10000)
        }

        close_btn.setOnClickListener {
            this@CameraXActivity.finish()
        }
    }


    private fun initFlashSelectColor(){
        flash_on.setTextColor(resources.getColor(R.color.white))
        flash_off.setTextColor(resources.getColor(R.color.white))
        flash_auto.setTextColor(resources.getColor(R.color.white))
        flash_all_on.setTextColor(resources.getColor(R.color.white))

        flash_layout.visibility= View.INVISIBLE
        switch_btn.visibility= View.VISIBLE
    }


    /**
     * 音量减按钮触发拍照，如果需要复制这份代码就可以
     *
     * When key down event is triggered,
     * relay it via local broadcast so fragments can handle it
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val intent = Intent(KEY_CAMERA_EVENT_ACTION).apply {
                    putExtra(
                        KEY_CAMERA_EVENT_EXTRA,
                        keyCode
                    )
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }


}