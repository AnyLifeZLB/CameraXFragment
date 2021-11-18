# CameraXFragment

2021/06/18
Android 自定义相机要考虑的东西还是非常多的，特别是兼容性问题，尽管Camera 2已经替换了Camera1,但还是很难用。
Google 爸爸给大家准备了CameraX，可以很方便的适配Android 5.0 + 机型来拍照，图片分析，拍摄视频

简单的封装了拍照，录制视频的CameraXFragment,在真实项目中已经上线1个月了，目前情况稳定。



## 使用说明

        val cameraConfig=CameraConfig.Builder()
            .flashMode(CameraConfig.FLASH_MODE_OFF)
            .mediaMode(CameraConfig.MEDIA_MODE_ALL) //视频拍照都可以
            .cacheMediasDir(cacheMediasDir)
            .build()

        cameraXFragment = CameraXFragment.newInstance(cameraConfig)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, cameraXFragment).commit()
## 依赖
   First:
       repositories {
        google()
        mavenCentral() // 添加mavenCentral 依赖，Google 已经停止Jcenter
       }

   Second:
       implementation "io.github.anylifezlb:CameraXFragment:1.1.2"


### 更多说明请下载Demo

![image.png](https://upload-images.jianshu.io/upload_images/2376786-c119a43268ad31c3.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
