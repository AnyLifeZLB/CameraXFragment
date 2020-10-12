package com.zenglb.cameraxfragment.test

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.zenglb.cameraxfragment.KEY_EVENT_ACTION
import com.zenglb.cameraxfragment.KEY_EVENT_EXTRA
import com.zenglb.cameraxfragment.R
import com.zenglb.cameraxfragment.captureView.CaptureListener
import com.zenglb.cameraxfragment.listener.OperateListener
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import kotlinx.android.synthetic.main.activity_main.*


/**
 * 演示如何使用CameraX Fragment
 *
 *
 * 图片视频选择器：https://www.jianshu.com/p/aab0b0e42824
 * https://github.com/LuckSiege/PictureSelector/issues
 *
 */
class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_CHOOSE_MEDIA: Int = 10000;
    private lateinit var cameraXFragment: CameraXFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //todo 这里要能定制各种相机的参数
        cameraXFragment = CameraXFragment.newInstance("", "")

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, cameraXFragment).commitAllowingStateLoss()

        photo_view_btn.setOnClickListener {

            Matisse.from(this@MainActivity)
                .choose(MimeType.ofAll())
                .countable(true)
                .maxSelectable(9)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.85f)
                .imageEngine(GlideEngine())
                .forResult(REQUEST_CODE_CHOOSE_MEDIA)
        }


        capture_btn.setCaptureLisenter(object : CaptureListener {
            override fun takePictures() {
                cameraXFragment.takePhoto()
            }

            override fun recordShort(time: Long) {
                TODO("Not yet implemented")
            }

            override fun recordStart() {
                cameraXFragment.takeVideo()
            }

            override fun recordEnd(time: Long) {
                cameraXFragment.stopTakeVideo()
            }

            override fun recordZoom(zoom: Float) {

            }

            override fun recordError() {

            }
        })


//        //去拍照
//        capture_btn.setOnClickListener {
//            cameraXFragment.takePhoto()
//        }


        //切换摄像头
        switch_btn.setOnClickListener {
            if (cameraXFragment.canSwitchCamera())
                cameraXFragment.switchCamera()
        }

        /**
         * 拍照成功，拍视频成功的监听
         */
        cameraXFragment.setOperateListener(object : OperateListener {
            override fun onVideoRecorded(filePath: String) {
                Log.e("CameraXFragment", filePath)
            }

            override fun onPhotoTaken(filePath: String) {
                Log.e("CameraXFragment", filePath)
                runOnUiThread {
                    Glide.with(baseContext)
                        .load(filePath)
                        .into(photo_view_btn)
                }
            }
        })

    }


    /**
     * 选择图片，这里
     */
    var mSelected: List<Uri>? = null
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE_MEDIA && resultCode == Activity.RESULT_OK) {
            mSelected = Matisse.obtainResult(data)
            Log.d("Matisse", "mSelected:" + mSelected?.get(0).toString())

            Glide.with(baseContext)
                .load(mSelected?.get(0))
                .into(photo_view_btn); //测试一下选择图片

        }
    }


    // 向下按钮被触发后，通过本地广播传递给fragment，然后在其中处理拍照动作
    /** When key down event is triggered, relay it via local broadcast so fragments can handle it */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val intent = Intent(KEY_EVENT_ACTION).apply { putExtra(KEY_EVENT_EXTRA, keyCode) }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }


}