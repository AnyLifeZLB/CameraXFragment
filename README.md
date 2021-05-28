# CameraXFragment

## Take photos and videos for Android,  Powered by CameraX

Android 自定义相机要考虑的东西还是非常多的，特别是兼容性问题，尽管Camera 2已经替换了Camera1,但还是很难用。
Google 爸爸给大家准备了CameraX，可以很方便的适配Android 5.0 + 机型来拍照，图片分析，拍摄视频（录制视频还在测试中
我实验了几款机型都是可以的）

简单的封装了拍照，录制视频的CameraXFragment,大家有兴趣可以体验一下是否有问题，目前还在开发中

可以先试用一下啊兼容性：implementation 'anylife.CameraXFragment:camerax:1.0.3'

gradle 中添加下面的依赖：

        maven {
            url  "https://dl.bintray.com/anylifezlb/CameraXFragment"
        }


Demo 链接：https://github.com/AnyLifeZLB/CameraXFragmentDemo

Apk下载链接：https://github.com/AnyLifeZLB/CameraXFragmentDemo/releases/download/1.0/app-debug.apk

![image.png](https://upload-images.jianshu.io/upload_images/2376786-d2379a8523f3dc64.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
