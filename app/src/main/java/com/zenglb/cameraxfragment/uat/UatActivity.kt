package com.zenglb.cameraxfragment.uat

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.amap.api.location.*
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode
import com.amap.api.location.AMapLocationClientOption.AMapLocationProtocol
import com.bumptech.glide.Glide
import com.zenglb.camerax.main.*
import com.zenglb.camerax.main.CameraConfig.Companion.CAMERA_FLASH_ALL_ON
import com.zenglb.camerax.main.CameraConfig.Companion.CAMERA_FLASH_AUTO
import com.zenglb.camerax.main.CameraConfig.Companion.CAMERA_FLASH_OFF
import com.zenglb.camerax.main.CameraConfig.Companion.CAMERA_FLASH_ON
import com.zenglb.cameraxfragment.R
import com.zenglb.cameraxfragment.VideoPlayerActivity
import com.zenglb.cameraxfragment.utils.PermissionTipsDialog
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
 * 4.要适配分区存储
 * 5.
 *
 */
class UatActivity : AppCompatActivity(), CameraXFragment.OnPermissionRequestListener {
    private var cacheMediasDir =
        Environment.getExternalStorageDirectory().toString() + "/cameraX/images/"
    private lateinit var cameraXFragment: CameraXFragment
    private lateinit var mOrientationListener: OrientationEventListener

    private lateinit var locationClient: AMapLocationClient
    private lateinit var locationOption: AMapLocationClientOption


    //有些应用希望能添加位置信息在水印上面
    private val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cameraConfig = CameraConfig.Builder()
            .flashMode(CameraConfig.CAMERA_FLASH_OFF)
            .mediaMode(CameraConfig.MEDIA_MODE_ALL)   //视频拍照都可以
            .cacheMediasDir(cacheMediasDir)
            .build()

        cameraXFragment = CameraXFragment.newInstance(cameraConfig)
        cameraXFragment.addRequestPermission(perms) //添加需要多申请的权限，比如水印相机需要定位权限

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, cameraXFragment).commit()


        initLocation()


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

            //录制视频错误（拍照也会有错误，先不处理了吧）
            override fun recordError(message: String) {

            }
        })

        //拍照录视频操作结果通知回调
        cameraXFragment.setCaptureResultListener(object : CaptureResultListener {
            override fun onVideoRecorded(filePath: String) {
                Log.d("CameraXFragment", "onVideoRecorded：$filePath")
                if (filePath.isEmpty()) return

                val photoURI: Uri = FileProvider.getUriForFile(
                    baseContext,
                    baseContext.getApplicationContext().getPackageName().toString() + ".provider",
                    File(filePath)
                )
                intent = Intent(this@UatActivity, VideoPlayerActivity::class.java)
                startActivity(intent.putExtra("mMP4Path", photoURI.toString()))
            }

            override fun onPhotoTaken(filePath: String) {
                if (filePath.isEmpty()) return

                Log.d("CameraXFragment", "onPhotoTaken： $filePath")
                runOnUiThread {
                    Glide.with(baseContext)
                        .load(filePath)
                        .circleCrop()
                        .into(photo_view_btn)
                }
            }
        })

        flush_btn.setOnClickListener {
            if (flash_layout.visibility == View.VISIBLE) {
                flash_layout.visibility = View.INVISIBLE
                switch_btn.visibility = View.VISIBLE
            } else {
                flash_layout.visibility = View.VISIBLE
                switch_btn.visibility = View.INVISIBLE
            }
        }

        //切换摄像头
        switch_btn.setOnClickListener {
            //要保持闪光灯上一次的模式
            if (cameraXFragment.canSwitchCamera()) {
                cameraXFragment.switchCamera()
            }
        }


        flash_on.setOnClickListener {
            initFlashSelectColor()
            flash_on.setTextColor(resources.getColor(R.color.flash_selected))
            flush_btn.setImageResource(R.drawable.flash_on)
            cameraXFragment.setFlashMode(CAMERA_FLASH_ON)
        }
        flash_off.setOnClickListener {
            initFlashSelectColor()
            flash_off.setTextColor(resources.getColor(R.color.flash_selected))
            flush_btn.setImageResource(R.drawable.flash_off)
            cameraXFragment.setFlashMode(CAMERA_FLASH_OFF)
        }
        flash_auto.setOnClickListener {
            initFlashSelectColor()
            flash_auto.setTextColor(resources.getColor(R.color.flash_selected))
            flush_btn.setImageResource(R.drawable.flash_auto)
            cameraXFragment.setFlashMode(CAMERA_FLASH_AUTO)
        }
        flash_all_on.setOnClickListener {
            initFlashSelectColor()
            flash_all_on.setTextColor(resources.getColor(R.color.flash_selected))
            flush_btn.setImageResource(R.drawable.flash_all_on)
            cameraXFragment.setFlashMode(CAMERA_FLASH_ALL_ON)
        }


        //去浏览媒体资源，使用的是知乎的开源库 Matisse，用法参考官方说明
        photo_view_btn.setOnClickListener {
            Matisse.from(this@UatActivity)
                .choose(MimeType.ofImage())
                .countable(true)
                .maxSelectable(9)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.45f)
                .imageEngine(GlideEngine())
                .forResult(10000)
        }

        close_btn.setOnClickListener {
            this@UatActivity.finish()
        }


        //相机的UI在横竖屏幕可以对应修改UI 啊
        mOrientationListener = object : OrientationEventListener(baseContext) {
            override fun onOrientationChanged(orientation: Int) {
                // Monitors orientation values to determine the target rotation value
                // 这个可以微调
                val rotation: Int = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                when (rotation) {
                    Surface.ROTATION_270 -> {

                    }
                    Surface.ROTATION_180 -> {

                    }
                    Surface.ROTATION_90 -> {

                    }
                    Surface.ROTATION_0 -> {

                    }
                }
            }
        }

        //还没有适配分区储存呢，估计短时间内没有办法了
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!Environment.isExternalStorageLegacy()) {
                Toast.makeText(
                    baseContext,
                    "检测到你的应用以分区存储特性运行，但CameraXFragment库还没有适配分区储存",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        startLocation()

    }

    override fun onResume() {
        super.onResume()
        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable()
        } else {
            mOrientationListener.disable()
        }
    }


    override fun onPause() {
        super.onPause()
        mOrientationListener.disable();
    }


    private fun initFlashSelectColor() {
        flash_on.setTextColor(resources.getColor(R.color.white))
        flash_off.setTextColor(resources.getColor(R.color.white))
        flash_auto.setTextColor(resources.getColor(R.color.white))
        flash_all_on.setTextColor(resources.getColor(R.color.white))

        flash_layout.visibility = View.INVISIBLE
        switch_btn.visibility = View.VISIBLE
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

    /**
     * 根据工信部的要求申请权限前需要向用户说明权限的明确具体用途，请根据业务和法务要求组织语言进行描述
     *
     *
     * @param permissions 需要申请的权限组合，如果为空说明底层CameraX 所需要的权限都申请好了
     * @param requestCode 有权限需要申请值为{@link CameraXFragment} 否则为0
     */
    override fun onBeforePermissionRequest(permissions: Array<String>, requestCode: Int) {
        if (permissions.isEmpty()) return

        //提示可以根据自己的业务自行开展
        PermissionTipsDialog(this@UatActivity, permissions,
            object : PermissionTipsDialog.PermissionCallBack {
                override fun onContinue() {
                    cameraXFragment.onRequestPermission(permissions, requestCode)
                }

                override fun onCancel() {

                }
            }
        ).show()

    }

    /**
     * 被拒绝的权限
     *
     */
    override fun onAfterPermissionDeny(permissions: Array<String>, requestCode: Int) {
        val a = 1;
    }


    private fun initLocation() {
        //初始化client
        try {
            locationClient = AMapLocationClient(this.applicationContext)
            locationOption = getDefaultOption()
            //设置定位参数
            locationClient.setLocationOption(locationOption)
            // 设置定位监听
            locationClient.setLocationListener(locationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getDefaultOption(): AMapLocationClientOption {
        val mOption = AMapLocationClientOption()
        mOption.locationMode =
            AMapLocationMode.Hight_Accuracy //可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        mOption.isGpsFirst = true //可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mOption.httpTimeOut = 30000 //可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        mOption.interval = 3000 //可选，设置定位间隔。默认为2秒
        mOption.isNeedAddress = true //可选，设置是否返回逆地理地址信息。默认是true
        mOption.isOnceLocation = false //可选，设置是否单次定位。默认是false
        mOption.isOnceLocationLatest =
            false //可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        AMapLocationClientOption.setLocationProtocol(AMapLocationProtocol.HTTP) //可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        mOption.isSensorEnable = false //可选，设置是否使用传感器。默认是false
        mOption.isWifiScan =
            true //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        mOption.isLocationCacheEnable = true //可选，设置是否使用缓存定位，默认为true
        mOption.geoLanguage =
            AMapLocationClientOption.GeoLanguage.DEFAULT //可选，设置逆地理信息的语言，默认值为默认语言（根据所在地区选择语言）
        return mOption
    }


    var locationListener =
        AMapLocationListener { location ->
            if (null != location) {
                val sb = StringBuffer()
                //errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
                if (location.errorCode == 0) {

                    sb.append("***定位质量报告***").append("\n")
                    sb.append("* WIFI开关：")
                        .append(if (location.locationQualityReport.isWifiAble) "开启" else "关闭")
                        .append("\n")

                    sb.append("* GPS星数：").append(location.locationQualityReport.gpsSatellites)
                        .append("\n")
                    sb.append("* 网络类型：" + location.locationQualityReport.networkType).append("\n")
                    sb.append("* 网络耗时：" + location.locationQualityReport.netUseTime).append("\n")
                    sb.append("****************").append("\n")

                    //解析定位结果，
                    val result = sb.toString()
                } else {
                }
            }
        }


    fun getGPSStatusString(statusCode: Int): String? {
        var str = ""
        when (statusCode) {
            AMapLocationQualityReport.GPS_STATUS_OK -> str = "GPS状态正常"
            AMapLocationQualityReport.GPS_STATUS_NOGPSPROVIDER -> str =
                "手机中没有GPS Provider，无法进行GPS定位"
            AMapLocationQualityReport.GPS_STATUS_OFF -> str = "GPS关闭，建议开启GPS，提高定位质量"
            AMapLocationQualityReport.GPS_STATUS_MODE_SAVING -> str =
                "选择的定位模式中不包含GPS定位，建议选择包含GPS定位的模式，提高定位质量"
            AMapLocationQualityReport.GPS_STATUS_NOGPSPERMISSION -> str =
                "没有GPS定位权限，建议开启gps定位权限"
        }
        return str
    }


    fun startLocation() {
        try {
            // 设置定位参数
            locationClient.setLocationOption(locationOption)
            // 启动定位
            locationClient.startLocation()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    fun stopLocation() {
        try {
            // 停止定位
            locationClient.stopLocation()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun destroyLocation() {
        if (null != locationClient) {
            /**
             * 如果AMapLocationClient是在当前Activity实例化的，
             * 在Activity的onDestroy中一定要执行AMapLocationClient的onDestroy
             */
            locationClient.onDestroy()
        }
    }


}