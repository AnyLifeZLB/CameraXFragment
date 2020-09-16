package com.zenglb.cameraxfragment.fragments

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.zenglb.cameraxfragment.KEY_EVENT_ACTION
import com.zenglb.cameraxfragment.KEY_EVENT_EXTRA
import com.zenglb.cameraxfragment.utils.ANIMATION_FAST_MILLIS
import com.zenglb.cameraxfragment.utils.ANIMATION_SLOW_MILLIS
import com.zenglb.cameraxfragment.utils.simulateClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import com.zenglb.cameraxfragment.R


/**
 * Main fragment for this app. Implements all camera operations including:
 * - Viewfinder
 * - Photo taking
 * - Image analysis
 *
 * 不能因为某个技术栈 有很多优点就敢上.
 * 商业项目注重的是稳定性与维护成本,而不是整天特性,特性与商业无关.
 * 在一个不能称为成熟,到处坑的阶段, 回归原来最稳定的技术一定不会错.
 */
class CameraFragment : Fragment() {

    private lateinit var container: ConstraintLayout
    private lateinit var cameraPreview: PreviewView
    private lateinit var outputDirectory: File
    private lateinit var broadcastManager: LocalBroadcastManager

    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /**
     * 使用此执行器执行相机阻塞操作
     * Blocking camera operations are performed using this executor
     *
     */
    private lateinit var cameraExecutor: ExecutorService


    /**
     * 音量下降按钮接收器用于触发快门
     * Volume down button receiver used to trigger shutter
     */
    private val volumeDownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
                // When the volume down button is pressed, simulate a shutter button click
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val shutter = container
                            .findViewById<ImageButton>(R.id.camera_capture_button)
                    shutter.simulateClick()
                }
            }
        }
    }

    /**
     * 横竖屏切换
     *
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    /**
     * onResume，回来需要检查一下权限是不是别关闭 回收了之类的
     *
     */
    override fun onResume() {
        super.onResume()

        //保证Resume 的时候所有权限都能重新的回来
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            //Navigation 组件也要知道。这里可能会把照片预览给解耦出来
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                    CameraFragmentDirections.actionCameraToPermissions()
            )
        }
    }

    /**
     * 销毁后各种处理后事啊
     *
     */
    override fun onDestroyView() {
        super.onDestroyView()
        // Shut down our background executor
        cameraExecutor.shutdown()
        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(volumeDownReceiver)
        displayManager.unregisterDisplayListener(displayListener)
    }


    /**
     * Return :View? =
     *         inflater.inflate(R.layout.fragment_camera, container, false)
     */
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_camera, container, false)



    /**
     * 相机相关的状态初始化
     *
     */
    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //大容器VIEW
        container = view as ConstraintLayout

        cameraPreview = container.findViewById(R.id.camera_preview)

        // 初始化我们的后台执行器 Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        broadcastManager = LocalBroadcastManager.getInstance(view.context)

        // 设置意图过滤器，从我们的main activity接收事件
        // Set up the intent filter that will receive events from our main activity
        val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }

        broadcastManager.registerReceiver(volumeDownReceiver, filter)

        // 每当设备的方向改变时，就对用例进行更新旋转
        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)

        // 确定图片保存的目录
        outputDirectory = getOutputDirectory(requireContext())

        // 等待所有的View 都能正确的显示出
        // Wait for the views to be properly laid out
        cameraPreview.post {
            // Keep track of the display in which this view is attached
            displayId = cameraPreview.display.displayId

            // 构建UI 控制
            updateCameraUi()

            // Set up the camera and its use cases
            setUpCamera()
        }
    }

    /**
     * 配置变更，横竖屏啥的
     *
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
     * screen for devices that run Android 9 or below.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Redraw the camera UI controls
        updateCameraUi()

        // Enable or disable switching between cameras
        updateCameraSwitchButton()
    }

    /**
     * 初始化摄像头，绑定处理问题
     *
     * Initialize CameraX, and prepare to bind the camera use cases
     */
    private fun setUpCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            //CameraProvider
            cameraProvider = cameraProviderFuture.get()

            //先默认是后置的摄像头，没有后面的再切换到前面的来
            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            // 更新摄像头是否能前后切换状态
            updateCameraSwitchButton()

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }


    /**
     * 声明并绑定预览，拍照和图片分析等用例
     *
     * Declare and bind preview, capture and analysis use cases
     */
    private fun bindCameraUseCases() {
        // 获得屏幕的尺寸数据来设置全屏的分辨率
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { cameraPreview.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = cameraPreview.display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider
                ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // 预览 Preview
        preview = Preview.Builder()
                // 我们要去宽高比，但是没有分辨率
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                //设置初始的旋转
                // Set initial target rotation
                .setTargetRotation(rotation)
                .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // 我们要求长宽比，但没有分辨率匹配预览配置，但让 CameraX优化为任何特定的解决方案，最适合我们的用例
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(screenAspectRatio)
                //设置初始目标旋转，如果旋转改变，我们将不得不再次调用它在此用例的生命周期中
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()

        // 再次重新绑定前应该先解绑
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(cameraPreview.createSurfaceProvider())
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    /**
     *  检测是4：3 还是16：9 好点
     *
     *  [androidx.camera.core.ImageAnalysis Config] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width   预览的宽
     *  @param height - preview height 预览的高
     *  @return suitable aspect ratio 合适的比例
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /**
     * 每次配置更改后都会调用本方法，重新的绘制相机相关的UI 控制
     *
     * Method used to re-draw the camera UI controls, called every time configuration changes.
     */
    private fun updateCameraUi() {
        // 去除 container 中的控制Layout
        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            container.removeView(it)
        }

        // 把控制Layout 塞回去
        // Inflate a new view containing all UI for controlling the camera
        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, container)


        // 加载最后一张拍摄的照片
        // In the background, load latest photo taken (if any) for gallery thumbnail
        lifecycleScope.launch(Dispatchers.IO) {
            outputDirectory.listFiles { file ->
                EXTENSION_WHITELIST.contains(file.extension.toUpperCase(Locale.ROOT))
            }?.max()?.let {
                setGalleryThumbnail(Uri.fromFile(it))
            }
        }

        /**
         * 拍照按钮 监听 --> 拍照
         *
         */
        controls.findViewById<ImageButton>(R.id.camera_capture_button).setOnClickListener {

            // Get a stable reference of the modifiable image capture use case
            imageCapture?.let { imageCapture ->

                // 创建文件来接收照片
                val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)

                // 设置拍照的元数据
                val metadata = Metadata().apply {

                    // 用前置摄像头的话要镜像画面
                    // Mirror image when using the front camera
                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                }

                // Create output options object which contains file + metadata
                // 创建输出选项，包含有图片文件和其中的元数据
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                        .setMetadata(metadata)
                        .build()

                // 设置拍照监听回调，当拍照动作被触发的时候
                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(
                        outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {

                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                        Log.d(TAG, "Photo capture succeeded: $savedUri")

                        // We can only change the foreground Drawable using API level 23+ API
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // Update the gallery thumbnail with latest picture taken
                            setGalleryThumbnail(savedUri)
                        }

                        // Implicit broadcasts will be ignored for devices running API level >= 24
                        // so if you only target API level 24+ you can remove this statement
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                            requireActivity().sendBroadcast(
                                    Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
                            )
                        }

                        // If the folder selected is an external media directory, this is
                        // unnecessary but otherwise other apps will not be able to access our
                        // images unless we scan them using [MediaScannerConnection]
                        val mimeType = MimeTypeMap.getSingleton()
                                .getMimeTypeFromExtension(savedUri.toFile().extension)
                        MediaScannerConnection.scanFile(
                                context,
                                arrayOf(savedUri.toFile().absolutePath),
                                arrayOf(mimeType)
                        ) { _, uri ->
                            Log.d(TAG, "Image capture scanned into media store: $uri")
                        }
                    }
                })

                // We can only change the foreground Drawable using API level 23+ API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    // 显示一个闪光动画来告知用户照片已经拍好了。在华为等手机上好像有点问题啊
                    // Display flash animation to indicate that photo was captured
                    container.postDelayed({
                        container.foreground = ColorDrawable(Color.WHITE)
                        container.postDelayed({ container.foreground = null }, ANIMATION_FAST_MILLIS)
                    }, ANIMATION_SLOW_MILLIS)
                }
            }
        }

        // 设置按钮用来切换摄像头
        // Setup for button used to switch cameras
        controls.findViewById<ImageButton>(R.id.camera_switch_button).let {

            // Disable the button until the camera is set up
            it.isEnabled = false

            // Listener for button used to switch cameras. Only called if the button is enabled
            it.setOnClickListener {
                lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
                // Re-bind use cases to update selected camera
                bindCameraUseCases()
            }
        }

        // 预览最近拍的照片
        // Listener for button used to view the most recent photo
        controls.findViewById<ImageButton>(R.id.photo_view_button).setOnClickListener {
            // Only navigate when the gallery has photos
            if (true == outputDirectory.listFiles()?.isNotEmpty()) {
                Navigation.findNavController(
                        requireActivity(), R.id.fragment_container
                ).navigate(
                    CameraFragmentDirections
                        .actionCameraToGallery(outputDirectory.absolutePath))
            }
        }

    }


    /**
     * 有些设备只有一个摄像头那就不能切换摄像头了
     *
     */
    private fun updateCameraSwitchButton() {
        val switchCamerasButton = container.findViewById<ImageButton>(R.id.camera_switch_button)
        try {
            switchCamerasButton.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            switchCamerasButton.isEnabled = false
        }
    }


    /**
     * 检查设备是否有后置摄像头
     *
     */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /**
     * 检查设备是否有前置摄像头
     *
     */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }


    /**
     * 相片Gallery
     *
     */
    private fun setGalleryThumbnail(uri: Uri) {
        // Reference of the view that holds the gallery thumbnail
        val thumbnail = container.findViewById<ImageButton>(R.id.photo_view_button)

        // Run the operations in the view's thread
        thumbnail.post {
            // Remove thumbnail padding
            thumbnail.setPadding(resources.getDimension(R.dimen.stroke_small).toInt())

            // Load thumbnail into circular button using Glide
            Glide.with(thumbnail)
                    .load(uri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(thumbnail)
        }
    }


    companion object {
        private const val TAG = "CameraXFragment"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
                File(baseFolder, SimpleDateFormat(format, Locale.CHINA)
                        .format(System.currentTimeMillis()) + extension)

        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, "CameraXFragment").apply { mkdirs() }
            }

            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }

}
