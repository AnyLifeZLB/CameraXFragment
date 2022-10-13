# CameraXFragment

![icon](https://user-images.githubusercontent.com/15169396/147327054-5065aafc-5bb3-4477-8877-21b39212f4a9.png)


2021/10/18
Android 自定义相机要考虑的东西还是非常多的，特别是兼容性问题，每家手机厂商的相机方案还有差异，要是能像苹果一样有稳定统一的系统API组合使用就好了。
尽管Camera 2已经替换了Camera1,但还是很难用，Google 爸爸给大家准备了CameraX，可以很方便的适配Android 5.0 + 机型来拍照，图片分析，拍摄视频

简单的封装了拍照，录制视频的CameraXFragment,在真实项目中已经上线2个月了，目前情况稳定。



## 依赖
   First，   
   
       repositories {
        google()
        mavenCentral() // 添加mavenCentral 依赖，Google 已经停止Jcenter
       }

   Second，   
   
       implementation "io.github.anylifezlb:CameraXFragment:2.4.0" //请根据version log 升级

       
## 使用说明

        val cameraConfig=CameraConfig.Builder()
            .flashMode(CameraConfig.FLASH_MODE_OFF) //默认是关闭的
            .mediaMode(CameraConfig.MEDIA_MODE_ALL) //视频拍照都可以
            .cacheMediasDir(cacheMediasDir) //还没有适配存储分区，2022会的
            .build()

        cameraXFragment = CameraXFragment.newInstance(cameraConfig)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, cameraXFragment).commit()


### 更多说明请下载体验Demo
![image](https://user-images.githubusercontent.com/15169396/142362234-4300c052-cee6-4a1d-b835-baab7ae9e9b6.png)

