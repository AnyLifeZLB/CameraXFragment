package com.zenglb.cameraxfragment

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.zenglb.camerax.main.*
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import kotlinx.android.synthetic.main.activity_camera_x.*
import java.io.File


/**
 * 演示如何使用CameraX Fragment
 *
 *
 * 1.CameraX Extensions 是可选插件，您可以在支持的设备上添加效果。这些效果包括人像、HDR、夜间模式和美颜
 * 2.图片分析：无缝访问缓冲区以便在算法中使用，例如传入 MLKit
 * 3.
 *
 *
 */
class CameraXActivity : AppCompatActivity() {
    private val REQUEST_CODE_CHOOSE_MEDIA: Int = 10000;
    private lateinit var cameraXFragment: CameraXFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_x)

        //todo 这里要能定制各种相机的参数 ！！
        cameraXFragment = CameraXFragment.newInstance("", "")

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, cameraXFragment).commitAllowingStateLoss()

        //去浏览媒体资源
        photo_view_btn.setOnClickListener {
            //用的是知乎的库。自己写？NO TIME ！
            Matisse.from(this@CameraXActivity)
                .choose(MimeType.ofAll())
                .countable(true)
                .maxSelectable(9)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.45f)
                .imageEngine(GlideEngine())
                .forResult(REQUEST_CODE_CHOOSE_MEDIA)
        }

        //切换摄像头
        switch_btn.setOnClickListener {
            if (cameraXFragment.canSwitchCamera())
                cameraXFragment.switchCamera()
        }

        //拍照，拍视频的各种状态处理
        capture_btn.setCaptureLisenter(object : CaptureListener {
            override fun takePictures() {
                cameraXFragment.takePhoto()
            }

            //录制视频时间太短
            override fun recordShort(time: Long) {
                Log.e("Video", "Too short $time")
                Toast.makeText(this@CameraXActivity,"时间太短，视频无效",Toast.LENGTH_SHORT).show()
            }

            //开始录制视频
            override fun recordStart() {
                cameraXFragment.takeVideo()
            }

            //录制视频结束
            override fun recordEnd(time: Long) {
                cameraXFragment.stopTakeVideo()
            }

            //长按拍视频的时候拉焦距缩放
            override fun recordZoom(zoom: Float) {
                val a = zoom
            }

            //录制视频错误（拍照也有错误，这里还是不处理了吧）
            override fun recordError() {

            }
        })


        //拍照成功，拍视频成功的监听
        cameraXFragment.setOperateListener(object : OperateListener {
            override fun onVideoRecorded(filePath: String) {
                //时间太短不要回来了吧
                Log.e("CameraXFragment", "onVideoRecorded：$filePath")

                val photoURI: Uri = FileProvider.getUriForFile(
                    baseContext,
                    baseContext.getApplicationContext().getPackageName().toString() + ".provider",
                    File(filePath)
                )
                intent = Intent(this@CameraXActivity, VideoPlayerActivity::class.java)
                intent.putExtra("mMP4Path", photoURI.toString())

                startActivity(intent)
            }

            override fun onPhotoTaken(filePath: String) {
                Log.e("CameraXFragment", "onPhotoTaken： $filePath")
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
     *
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


    /** When key down event is triggered, relay it via local broadcast so fragments can handle it */
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